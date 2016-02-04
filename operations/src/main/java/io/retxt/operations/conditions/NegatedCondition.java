package io.retxt.operations.conditions;

import io.retxt.operations.AdvancedOperation;
import io.retxt.operations.Operation;

import java.util.function.Consumer;



/**
 * Created by kdubb on 2/3/16.
 */
public class NegatedCondition extends Condition {

  private Condition condition;

  public NegatedCondition(Condition condition) {
    super("Negated [" + condition.getName() + "]", false);
    this.condition = condition;
  }

  @Override
  public Operation dependencyForOperation(AdvancedOperation operation) {
    return condition.dependencyForOperation(operation);
  }

  @Override
  public void evaluateForOperation(AdvancedOperation operation, Consumer<Result> completion) {
    condition.evaluateForOperation(operation, result -> {
      switch(result.getState()) {
        case Satisfied:
          completion.accept(Result.FAILED);
          break;
        case Failed:
          completion.accept(Result.SATISFIED);
          break;
      }
    });
  }

}
