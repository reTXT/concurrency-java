package io.retxt.dispatch.android;

import android.os.Handler;
import android.os.Looper;
import io.retxt.dispatch.DispatchQueue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;



/**
 * Dispatch queue to run tasks on a Looper via an attached Handler.
 * <p>
 * Created by kdubb on 1/28/16.
 */
public class LooperQueue implements DispatchQueue {

  private Handler handler;

  LooperQueue(Looper looper) {
    handler = new Handler(looper);
  }

  @Override
  public void executeSync(Runnable runnable) throws InterruptedException {
    _executeSync(runnable).await();
  }

  @Override
  public boolean executeSync(long timeout, TimeUnit timeUnit, Runnable runnable) throws InterruptedException {
    return _executeSync(runnable).await(timeout, timeUnit);
  }

  private CountDownLatch _executeSync(Runnable runnable) {
    CountDownLatch completionLatch = new CountDownLatch(1);
    execute(() -> {
      try {
        runnable.run();
      }
      finally {
        completionLatch.countDown();
      }
    });
    return completionLatch;
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

