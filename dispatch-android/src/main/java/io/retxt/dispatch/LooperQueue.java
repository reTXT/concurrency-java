package io.retxt.dispatch;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.TimeUnit;



/**
 * Dispatch queue to execute blocks on a {@link Looper} via an attached Handler.
 * <p>
 * Created by kdubb on 1/28/16.
 */
public class LooperQueue extends InternalDispatchQueue {

  private Handler handler;

  LooperQueue(Looper looper) {
    handler = new Handler(looper);
  }

  @Override
  public void _dispatch(Block block) {
    handler.post(new RunnableBlock(block));
  }

  @Override
  public void _dispatchAfter(long time, TimeUnit timeUnit, Block block) {
    handler.postDelayed(new RunnableBlock(block), timeUnit.toMillis(time));
  }

}
