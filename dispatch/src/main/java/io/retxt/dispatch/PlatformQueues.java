package io.retxt.dispatch;

/**
 * Interface for creating platform specific queues.
 *
 * Created by kdubb on 1/28/16.
 */
public interface PlatformQueues {

  DispatchQueue createMainQueue();

  DispatchQueue createUIQueue();

}
