package io.retxt.dispatch;

import java.util.concurrent.TimeUnit;



/**
 * Dispatch queue that runs tasks immediately on the current thread.
 * <p>
 * Note: executeAfter uses Thread.sleep to achieve the requisite delay.
 * <p>
 * Note: execute &amp; executeSync are essentially the same and block the calling thread.
 * <p>
 * Created by kdubb on 9/2/14.
 */
public class ImmediateDispatchQueue implements DispatchQueue {

  @Override
  public void executeSync(Runnable task) throws InterruptedException {
    task.run();
  }

  @Override
  public boolean executeSync(long timeout, TimeUnit timeUnit, Runnable task) throws InterruptedException {
    task.run();
    return true;
  }

  @Override
  public void execute(Runnable runnable) {
    runnable.run();
  }

  @Override
  public void executeAfter(long delay, TimeUnit unit, Runnable runnable) {

    try {
      Thread.sleep(unit.toMillis(delay));
    }
    catch(InterruptedException ignored) {

    }

    runnable.run();
  }

}
