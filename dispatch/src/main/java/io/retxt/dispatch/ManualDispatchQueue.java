package io.retxt.dispatch;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;



/**
 * A dispatch queue that requires manual draining via {@see #drain}.
 * <p>
 * Useful for queues that need to always run from a specific thread when that thread is controlled by some other
 * process. A classic example is the implementation of a "main" thread queue by periodically calling drainAvailable
 * from the applications main thread.
 * <p>
 * Created by kdubb on 2/12/16.
 */
public class ManualDispatchQueue extends InternalDispatchQueue {

  BlockingQueue<Runnable> taskQueue = new ArrayBlockingQueue<>(50);

  @Override
  protected void _execute(Runnable runnable) {
    taskQueue.add(runnable);
  }

  public void drain(long timeout, TimeUnit timeUnit) throws InterruptedException {

    Runnable task;
    while((task = taskQueue.poll(timeout, timeUnit)) != null) {
      task.run();
    }

  }

  public void drainAvailable() {

    Runnable task;
    while((task = taskQueue.poll()) != null) {
      task.run();
    }

  }

  @SuppressWarnings("InfiniteLoopStatement")
  public void drain() {

    while(true) {
      try {
        taskQueue.take().run();
      }
      catch(InterruptedException ignored) {
      }
    }

  }

}
