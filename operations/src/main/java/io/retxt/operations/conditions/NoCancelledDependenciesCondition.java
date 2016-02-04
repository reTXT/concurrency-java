package io.retxt.operations.conditions;

import io.retxt.operations.AdvancedOperation;
import io.retxt.operations.Operation;

import java.util.function.Consumer;



/**
 * Created by kdubb on 2/3/16.
 */
public class NoCancelledDependenciesCondition extends Condition {

  public NoCancelledDependenciesCondition() {
    super("No Cancelled Dependencies", false);
  }

  @Override
  public Operation dependencyForOperation(AdvancedOperation operation) {
    return null;
  }

  @Override
  public void evaluateForOperation(AdvancedOperation operation, Consumer<Result> completion) {

  }
}
