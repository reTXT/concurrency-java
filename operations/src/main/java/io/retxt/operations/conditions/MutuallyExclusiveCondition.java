package io.retxt.operations.conditions;

import io.retxt.operations.AdvancedOperation;
import io.retxt.operations.Operation;

import java.util.function.Consumer;



/**
 * A generic class for describing kinds of operations that may not execute concurrently.
 * <p>
 * Created by kdubb on 2/3/16.
 */
public class MutuallyExclusiveCondition<T> extends Condition {

  private final Class<T> category;

  public MutuallyExclusiveCondition(Class<T> category) {
    super("Mutually Exclusive [" + category.getSimpleName() + "]", true);
    this.category = category;
  }

  @Override
  public Operation dependencyForOperation(AdvancedOperation operation) {
    return null;
  }

  @Override
  public void evaluateForOperation(AdvancedOperation operation, Consumer<Result> completion) {
    completion.accept(Result.SATISFIED);
  }

}
