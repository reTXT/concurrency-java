package io.retxt.dispatch;

import io.retxt.dispatch.InternalDispatchQueue.Priority;

import java.util.ServiceLoader;

import static com.google.common.collect.Iterables.getFirst;



/**
 * Dispatch queues primary interface.
 * <p>
 * Created by kdubb on 8/30/14.
 */
public class DispatchQueues {

  private static PlatformQueues platformQueues =
      getFirst(ServiceLoader.load(PlatformQueues.class), new NullPlatformQueues());

  static {
    if(platformQueues == null) {
      throw new IllegalStateException("Platform support library is required at runtime (e.g. android-support)");
    }
  }

  public static final DispatchQueue MAIN = platformQueues.createMainQueue();

  public static final DispatchQueue UI = platformQueues.createUIQueue();

  public static final DispatchQueue HIGH = new GlobalDispatchQueue(Priority.High);

  public static final DispatchQueue MEDIUM = new GlobalDispatchQueue(Priority.Medium);

  public static final DispatchQueue LOW = new GlobalDispatchQueue(Priority.Low);

}
