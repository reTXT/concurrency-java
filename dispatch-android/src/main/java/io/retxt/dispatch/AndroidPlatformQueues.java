package io.retxt.dispatch;

import android.os.Looper;



/**
 * Android platform queues implementation.
 * <p>
 * Created by kdubb on 1/28/16.
 */
public class AndroidPlatformQueues implements PlatformQueues {

  @Override
  public DispatchQueue createMainQueue() {
    return new LooperQueue(Looper.getMainLooper());
  }

  @Override
  public DispatchQueue createUIQueue() {
    return new LooperQueue(Looper.getMainLooper());
  }

}
