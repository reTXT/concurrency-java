package io.retxt.dispatch;

/**
 * Created by kdubb on 2/3/16.
 */
public interface PrivateDispatchQueue extends DispatchQueue {

  boolean isSuspended();

  void suspend();

  void resume();

}
