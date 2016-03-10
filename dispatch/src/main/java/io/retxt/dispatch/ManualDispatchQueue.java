package io.retxt.dispatch;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;



/**
 * A dispatch queue that requires manual draining via {@link #drain}.
 * <p>
 * Useful for queues that need to always run from a specific thread when that thread is controlled by some other
 * process. A classic example is the implementation of a "main" thread queue by periodically calling drainAvailable
 * from the applications main thread.
 * <p>
 * Created by kdubb on 2/12/16.
 */
public class ManualDispatchQueue extends InternalDispatchQueue {

  BlockingQueue<Block> blockQueue = new ArrayBlockingQueue<>(50);

  @Override
  protected void _dispatch(Block block) {
    blockQueue.add(block);
  }

  public void drainAvailable(long timeout, TimeUnit timeUnit) throws InterruptedException {

    Block block;
    while((block = blockQueue.poll(timeout, timeUnit)) != null) {
      try {
        block.run();
      }
      catch(Throwable t) {
        Thread thread = Thread.currentThread();
        Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(thread, t);
      }
    }

  }

  public void drainAvailable() {

    Block block;
    while((block = blockQueue.poll()) != null) {
      try {
        block.run();
      }
      catch(Throwable t) {
        Thread thread = Thread.currentThread();
        Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(thread, t);
      }
    }

  }

  @SuppressWarnings("InfiniteLoopStatement")
  public void drain() {

    while(true) {
      try {
        blockQueue.take().run();
      }
      catch(Throwable t) {
        Thread thread = Thread.currentThread();
        Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(thread, t);
      }
    }

  }

}
