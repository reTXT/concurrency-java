package io.retxt.dispatch;

import io.retxt.dispatch.InternalDispatchQueue.QueuedTask.Listener;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkArgument;



/**
 * Queue that executes its tasks concurrently.
 * <p>
 * Created by kdubb on 1/31/16.
 */
public class ConcurrentDispatchQueue extends InternalDispatchQueue implements PrivateDispatchQueue {

  private int priority;
  private ConcurrentLinkedQueue<QueuedTask> pauseQueue = new ConcurrentLinkedQueue<>();
  private ConcurrentLinkedQueue<QueuedTask> barrierQueue = new ConcurrentLinkedQueue<>();
  private AtomicBoolean suspended = new AtomicBoolean(false);
  private Barrier barrier = new Barrier();

  public ConcurrentDispatchQueue(int priority) {
    checkArgument(priority >= Thread.MIN_PRIORITY && priority <= Thread.MAX_PRIORITY, "Priority out of range");
    this.priority = priority;
  }

  private void execute(QueuedTask task) {

    if(suspended.get()) {

      pauseQueue.add(task);

    }
    else if(barrier.enabled.get()) {

      barrierQueue.add(task);

    }
    else {

      executor.execute(task);
    }

  }

  @Override
  public void execute(Runnable task) {
    execute(new QueuedTask(task, priority, barrier.checkingListener));
  }

  public void executeBarrier(Runnable task) {
    execute(new QueuedTask(task, priority, barrier.blockingListener));
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
    Set<QueuedTask> registerdTasks = new ConcurrentSkipListSet<>();
    AtomicBoolean enabled = new AtomicBoolean(false);

    Listener checkingListener = new Listener() {

      @Override
      public void scheduled(QueuedTask task) {
        registerdTasks.add(task);
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
      registerdTasks.remove(task);
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

          if(registerdTasks.isEmpty()) {
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
