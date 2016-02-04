package io.retxt.operations;

import io.retxt.operations.AdvancedOperationQueue.ObserverAdapter;

import java.util.ArrayList;
import java.util.Collection;



/**
 * A subclass of {@link Operation} that executes zero or more operations as part of its
 * own execution. This class of operation is very useful for abstracting several
 * smaller operations into a larger operation.
 * <p>
 * Additionally, GroupOperations are useful if you establish a chain of dependencies,
 * but part of the chain may "loop". For example, if you have an operation that
 * requires the user to be authenticated, you may consider putting the "login"
 * operation inside a group operation. That way, the "login" operation may produce
 * subsequent operations (still within the outer `GroupOperation`) that will all
 * be executed before the rest of the operations in the initial chain of operations.
 * <p>
 * Created by kdubb on 2/4/16.
 */
public class GroupOperation extends AdvancedOperation {

  private AdvancedOperationQueue internalQueue = new AdvancedOperationQueue();
  private Operation startingOperation = new RunnableOperation("Group Start", () -> {});
  private Operation finishingOperation= new RunnableOperation("Group Finish", () -> {});;
  private Collection<Throwable> aggregatedErrors = new ArrayList<>();

  public GroupOperation(String name, Collection<Operation> operations) {
    super(name);

    internalQueue.setObserver(new ObserverAdapter() {

      @Override
      public void operationAdded(AdvancedOperationQueue queue, Operation operation) {

        /*
            Some operation in this group has produced a new operation to execute.
            We want to allow that operation to execute before the group completes,
            so we'll make the finishing operation dependent on this newly-produced operation.
        */
        if(operation != finishingOperation) {
          finishingOperation.addDependency(operation);
        }

        /*
            All operations should be dependent on the "startingOperation".
            This way, we can guarantee that the conditions for other operations
            will not evaluate until just before the operation is about to run.
            Otherwise, the conditions could be evaluated at any time, even
            before the internal operation queue is unsuspended.
        */
        if(operation != startingOperation) {
          operation.addDependency(startingOperation);
        }
      }

      @Override
      public void operationFinished(AdvancedOperationQueue queue, Operation operation, Collection<Throwable> errors) {
        aggregatedErrors.addAll(errors);

        if(operation == finishingOperation) {
          internalQueue.suspend();
          finish(aggregatedErrors);
        }
        else if (operation != startingOperation) {

        }
      }
    });

    internalQueue.suspend();
    internalQueue.addOperation(startingOperation);
    internalQueue.addOperations(operations);
  }

  public void addOperations(Operation... operations) {
    internalQueue.addOperations(operations);
  }

  public void addOperations(Collection<Operation> operations) {
    internalQueue.addOperations(operations);
  }

  public void addOperation(Operation operation) {
    internalQueue.addOperation(operation);
  }

  protected void aggregateError(Throwable error) {
    aggregatedErrors.add(error);
  }

  protected void childOperationFinished(Operation operation) {
  }

  @Override
  public void run() {
    internalQueue.resume();
    internalQueue.addOperation(finishingOperation);
  }

}
