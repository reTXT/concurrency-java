package io.retxt.dispatch;

/**
 * Default PlatformQueues implementation that initializes them with {@link ManualDispatchQueue
 * ManualDispatchQueues}
 * <p>
 * Created by kdubb on 1/29/16.
 */
class DefaultPlatformQueues implements PlatformQueues {

  @Override
  public ManualDispatchQueue createMainQueue() {
    return new ManualDispatchQueue();
  }

  @Override
  public ManualDispatchQueue createUIQueue() {
    return new ManualDispatchQueue();
  }

}
