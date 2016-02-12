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
  public void executeSync(Runnable task) {
    task.run();
  }

  @Override
  public boolean executeSync(long timeout, TimeUnit timeUnit, Runnable task) {
    task.run();
    return true;
  }

  @Override
  public void execute(Runnable task) {
    task.run();
  }

  @Override
  public void executeAfter(long delay, TimeUnit unit, Runnable task) {

    try {
      Thread.sleep(unit.toMillis(delay));
    }
    catch(InterruptedException ignored) {

    }

    task.run();
  }

}
