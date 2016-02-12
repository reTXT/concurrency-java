package io.retxt.dispatch;

import io.retxt.dispatch.InternalDispatchQueue.QueuedTask.Listener;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkArgument;



/**
 * Queue that executes its tasks concurrently.
 * <p>
 * Created by kdubb on 1/31/16.
 */
public class ConcurrentDispatchQueue extends InternalDispatchQueue implements UserDispatchQueue {

  private int priority;
  private ConcurrentLinkedQueue<QueuedTask> pauseQueue = new ConcurrentLinkedQueue<>();
  private ConcurrentLinkedQueue<QueuedTask> barrierQueue = new ConcurrentLinkedQueue<>();
  private AtomicBoolean suspended = new AtomicBoolean(false);
  private Barrier barrier = new Barrier();

  public ConcurrentDispatchQueue(int priority) {
    checkArgument(priority >= Thread.MIN_PRIORITY && priority <= Thread.MAX_PRIORITY, "Priority out of range");
    this.priority = priority;
  }

  protected void _execute(Runnable task) {

    if(suspended.get()) {

      pauseQueue.add((QueuedTask) task);

    }
    else if(barrier.enabled.get()) {

      barrierQueue.add((QueuedTask) task);

    }
    else {

      executor.execute(task);
    }

  }

  @Override
  public void execute(Runnable task) {
    _execute(new QueuedTask(task, priority, barrier.notifyingListener));
  }

  public void executeBarrier(Runnable task) {
    _execute(new QueuedTask(task, priority, barrier.blockingListener));
  }

  public void executeBarrierSync(Runnable task) {
    executeSync(new QueuedTask(task, priority, barrier.blockingListener));
  }

  public void executeBarrierSync(long timeout, TimeUnit timeUnit, Runnable task) {
    executeSync(timeout, timeUnit, new QueuedTask(task, priority, barrier.blockingListener));
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

      QueuedTask task;
      while((task = pauseQueue.poll()) != null) {
        executor.execute(task);
      }

    }

  }

  private class Barrier {

    ReentrantLock lock = new ReentrantLock();
    Condition passCondition = lock.newCondition();
    Set<QueuedTask> registeredTasks = new ConcurrentSkipListSet<>();
    AtomicBoolean enabled = new AtomicBoolean(false);

    Listener notifyingListener = new Listener() {

      @Override
      public void scheduled(QueuedTask task) {
        registeredTasks.add(task);
      }

      @Override
      public void enter(QueuedTask task) {
      }

      @Override
      public void exit(QueuedTask task) {
        passed(task);
      }

    };

    Listener blockingListener = new Listener() {

      @Override
      public void scheduled(QueuedTask task) {
      }

      @Override
      public void enter(QueuedTask task) {
        enable();
      }

      @Override
      public void exit(QueuedTask task) {
        disable();
      }

    };

    public void passed(QueuedTask task) {

      registeredTasks.remove(task);

      lock.lock();

      try {
        passCondition.signalAll();
      }
      finally {
        lock.unlock();
      }

    }

    public void enable() {

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

    public void disable() {

      enabled.set(false);

      QueuedTask task;
      while((task = barrierQueue.poll()) != null) {
        executor.execute(task);
      }

    }

  }

}
