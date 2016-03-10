package io.retxt.dispatch;

import java.util.concurrent.TimeUnit;



/**
 * Primary dispatch queue interface.
 * <p>
 * Created by kdubb on 11/21/14.
 */
public interface DispatchQueue {

  void dispatchSync(Block block);

  boolean dispatchSync(long timeout, TimeUnit timeUnit, Block block);

  void dispatch(Block block);

  void dispatchAfter(long time, TimeUnit timeUnit, Block block);

}
