package io.retxt.dispatch;

import io.retxt.dispatch.InternalDispatchQueue.QueuedBlock.Listener;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkArgument;



/**
 * Queue that dispatches its blocks concurrently.
 * <p>
 * Created by kdubb on 1/31/16.
 */
public class ConcurrentDispatchQueue extends InternalDispatchQueue implements UserDispatchQueue {

  private int priority;
  private ConcurrentLinkedQueue<QueuedBlock> pauseQueue = new ConcurrentLinkedQueue<>();
  private ConcurrentLinkedQueue<QueuedBlock> barrierQueue = new ConcurrentLinkedQueue<>();
  private AtomicBoolean suspended = new AtomicBoolean(false);
  private Barrier barrier = new Barrier();

  public ConcurrentDispatchQueue(int priority) {
    checkArgument(priority >= Thread.MIN_PRIORITY && priority <= Thread.MAX_PRIORITY, "Priority out of range");
    this.priority = priority;
  }

  private void _dispatch(QueuedBlock block) {

    if(suspended.get()) {

      pauseQueue.add(block);

    }
    else if(barrier.enabled.get()) {

      barrierQueue.add(block);

    }
    else {

      executor.execute(block);
    }

  }

  protected void _dispatch(Block block) {
    _dispatch(new QueuedBlock(block, priority, barrier.notifyingListener));
  }

  public void dispatchBarrier(Block block) {
    _dispatch(new QueuedBlock(block, priority, barrier.blockingListener));
  }

  public void dispatchBarrierSync(Block block) {
    Barrier.SyncBlockingListener listener = barrier.new SyncBlockingListener();
    _dispatch(new QueuedBlock(block, priority, listener));
    listener.await();
  }

  public void dispatchBarrierSync(long timeout, TimeUnit timeUnit, Block block) {
    Barrier.SyncBlockingListener listener = barrier.new SyncBlockingListener();
    _dispatch(new QueuedBlock(block, priority, listener));
    listener.await(timeout, timeUnit);
  }

  @Override
  public boolean isSuspended() {
    return suspended.get();
  }

  @Override
  public void suspend() {
    suspended.set(true);
  }

  @Override
  public void resume() {

    if(suspended.compareAndSet(true, false)) {

      QueuedBlock block;
      while((block = pauseQueue.poll()) != null) {
        executor.execute(block);
      }

    }

  }

  private class Barrier {

    ReentrantLock lock = new ReentrantLock();
    Condition passCondition = lock.newCondition();
    Set<QueuedBlock> registeredTasks = new ConcurrentSkipListSet<>();
    AtomicBoolean enabled = new AtomicBoolean(false);

    Listener notifyingListener = new Listener() {

      @Override
      public void scheduled(QueuedBlock block) {
        registeredTasks.add(block);
      }

      @Override
      public void enter(QueuedBlock block) {
      }

      @Override
      public void exit(QueuedBlock block) {
        passed(block);
      }

    };

    Listener blockingListener = new Listener() {

      @Override
      public void scheduled(QueuedBlock block) {
      }

      @Override
      public void enter(QueuedBlock block) {
        enable();
      }

      @Override
      public void exit(QueuedBlock block) {
        disable();
      }

    };



    class SyncBlockingListener implements Listener {

      CountDownLatch latch = new CountDownLatch(1);

      void await() {
        while(true) {
          try {
            latch.await();
            return;
          }
          catch(InterruptedException ignored) {
          }
        }
      }

      void await(long timeout, TimeUnit timeUnit) {
        while(true) {
          try {
            latch.await(timeout, timeUnit);
            return;
          }
          catch(InterruptedException ignored) {
          }
        }
      }

      @Override
      public void scheduled(QueuedBlock block) {
        blockingListener.scheduled(block);
      }

      @Override
      public void enter(QueuedBlock block) {
        blockingListener.enter(block);
      }

      @Override
      public void exit(QueuedBlock block) {
        blockingListener.exit(block);
        latch.countDown();
      }

    }

    void passed(QueuedBlock block) {

      registeredTasks.remove(block);

      lock.lock();

      try {
        passCondition.signalAll();
      }
      finally {
        lock.unlock();
      }

    }

    void enable() {

      lock.lock();
      try {

        enabled.set(true);

        for(; ; ) {

          if(registeredTasks.isEmpty()) {
            return;
          }

          passCondition.awaitUninterruptibly();

        }

      }
      finally {
        lock.unlock();
      }

    }

    void disable() {

      enabled.set(false);

      QueuedBlock block;
      while((block = barrierQueue.poll()) != null) {
        executor.execute(block);
      }

    }

  }

}
