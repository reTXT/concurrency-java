package io.retxt.operations.conditions;

import io.retxt.dispatch.DispatchQueues;
import io.retxt.operations.AdvancedOperation;
import io.retxt.operations.Operation;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;



/**
 * Created by kdubb on 2/3/16.
 */
public class DelayCondition extends Condition {

  private long timeout;
  private TimeUnit timeUnit;

  public DelayCondition(long timeout, TimeUnit timeUnit) {
    super("Delay for " + timeout + " " + timeUnit.name(), false);
    this.timeout = timeout;
    this.timeUnit = timeUnit;
  }

  @Override
  public Operation dependencyForOperation(AdvancedOperation operation) {
    return null;
  }

  @Override
  public void evaluateForOperation(AdvancedOperation operation, Consumer<Result> completion) {
    DispatchQueues.HIGH.dispatchAfter(timeout, timeUnit, () -> completion.accept(Result.SATISFIED));
  }

}
