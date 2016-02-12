package io.retxt.dispatch;

/**
 * Created by kdubb on 2/3/16.
 */
public interface UserDispatchQueue extends DispatchQueue {

  boolean isSuspended();

  void suspend();

  void resume();

}
