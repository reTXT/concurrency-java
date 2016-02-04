package io.retxt.dispatch;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;



/**
 * Dispatch queue to run tasks on a {@link Looper} via an attached Handler.
 * <p>
 * Created by kdubb on 1/28/16.
 */
public class LooperQueue extends InternalDispatchQueue {

  private Handler handler;

  LooperQueue(Looper looper) {
    handler = new Handler(looper);
  }

  @Override
  public void execute(Runnable task) {
    handler.post(task);
  }

  @Override
  public void executeAfter(long time, TimeUnit timeUnit, Runnable task) {
    handler.postDelayed(task, timeUnit.toMillis(time));
  }

}
