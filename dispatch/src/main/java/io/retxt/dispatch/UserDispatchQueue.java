package io.retxt.dispatch;

/**
 * Dispatch queue create by end user
 * <p>
 * Allows controlling the state of dispatching; global queues do not have this ability.
 * <p>
 * Created by kdubb on 2/3/16.
 */
public interface UserDispatchQueue extends DispatchQueue {

  boolean isSuspended();

  void suspend();

  void resume();

}
