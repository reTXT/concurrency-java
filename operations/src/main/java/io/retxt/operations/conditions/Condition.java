package io.retxt.operations.conditions;

import io.retxt.operations.AdvancedOperation;
import io.retxt.operations.Operation;

import java.util.function.Consumer;



/**
 * An interface for defining conditions that must be satisfied in order for an operation to begin execution.
 * <p>
 * Created by kdubb on 2/3/16.
 */
public abstract class Condition {

  public static class Result {

    public enum State {
      Satisfied,
      Failed
    }



    public static final Result SATISFIED = new Result(State.Satisfied);
    public static final Result FAILED = new Result(new ConditionFailed());

    State state;
    Throwable error;

    public Result(State state) {
      this.state = state;
    }

    public Result(Throwable error) {
      this.state = State.Failed;
      this.error = error;
    }

    public State getState() {
      return state;
    }

    public Throwable getError() {
      return error;
    }
  }



  private String name;
  private boolean mutuallyExclusive;

  public Condition(String name) {
    this.name = name;
    this.mutuallyExclusive = false;
  }

  public Condition(String name, boolean mutuallyExclusive) {
    this.name = name;
    this.mutuallyExclusive = mutuallyExclusive;
  }

  /**
   * Some conditions may have the ability to satisfy the condition if another
   * operation is executed first. Use this method to return an operation that
   * (for example) asks for permission to perform the operation
   *
   * @param operation The `Operation` to which the Condition has been added.
   *
   * @return An `Operation`, if a dependency should be automatically added. Otherwise, null.
   *
   * @apiNote Only a single operation may be returned as a dependency. If you
   * find that you need to return multiple operations, then you should be
   * expressing that as multiple conditions. Alternatively, you could return
   * a single `GroupOperation` that executes multiple operations internally.
   */
  public abstract Operation dependencyForOperation(AdvancedOperation operation);

  /**
   * Evaluate the condition, to see if it has been satisfied or not.
   *
   * @param operation The 'Operation' to which the Condition has been added.
   * @param completion A function interface that must be invoked when the evaluation is complete.
   */
  public abstract void evaluateForOperation(AdvancedOperation operation, Consumer<Result> completion);

  /**
   * The name of the condition. This is used in userInfo dictionaries of `.ConditionFailed`
   * errors as the value of the `OperationConditionKey` key.
   */
  public String getName() {
    return name;
  }

  /**
   * Specifies whether multiple instances of the conditionalized operation may
   * be executing simultaneously.
   */
  public boolean isMutuallyExclusive() {
    return mutuallyExclusive;
  }

}
