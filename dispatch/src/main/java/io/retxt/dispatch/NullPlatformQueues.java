package io.retxt.dispatch;

/**
 * Default PlatformQueues implementation that initializes them with {@link ImmediateDispatchQueue
 * ImmediateDispatchQueues}
 * <p>
 * Created by kdubb on 1/29/16.
 */
public class NullPlatformQueues implements PlatformQueues {

  @Override
  public DispatchQueue createMainQueue() {
    return new ImmediateDispatchQueue();
  }

  @Override
  public DispatchQueue createUIQueue() {
    return new ImmediateDispatchQueue();
  }

}
