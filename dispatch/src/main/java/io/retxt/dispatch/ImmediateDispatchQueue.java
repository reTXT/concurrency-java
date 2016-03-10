package io.retxt.dispatch;

import java.util.concurrent.TimeUnit;



/**
 * Dispatch queue that executes blocks immediately on the current thread.
 * <p>
 * Note: executeAfter uses Thread.sleep to achieve the requisite delay.
 * <p>
 * Note: execute &amp; executeSync are essentially the same and block the calling thread.
 * <p>
 * Created by kdubb on 9/2/14.
 */
public class ImmediateDispatchQueue implements DispatchQueue {

  @Override
  public void dispatchSync(Block block) {
    try {
      block.run();
    }
    catch(Throwable t) {
      Thread thread = Thread.currentThread();
      Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(thread, t);
    }
  }

  @Override
  public boolean dispatchSync(long timeout, TimeUnit timeUnit, Block block) {
    try {
      block.run();
    }
    catch(Throwable t) {
      Thread thread = Thread.currentThread();
      Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(thread, t);
    }
    return true;
  }

  @Override
  public void dispatch(Block block) {
    try {
      block.run();
    }
    catch(Throwable t) {
      Thread thread = Thread.currentThread();
      Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(thread, t);
    }
  }

  @Override
  public void dispatchAfter(long delay, TimeUnit unit, Block block) {

    try {
      Thread.sleep(unit.toMillis(delay));
    }
    catch(InterruptedException ignored) {

    }

    try {
      block.run();
    }
    catch(Throwable t) {
      Thread thread = Thread.currentThread();
      Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(thread, t);
    }
  }

}
