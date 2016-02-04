package io.retxt.operations;

import com.google.common.collect.FluentIterable;
import io.retxt.operations.conditions.ExclusivityController;

import java.util.Collection;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;



/**
 * AdvacnedOperationQueue is an @{@link OperationQueue} subclass that implements a large number of "extra features"
 * related to the {@link AdvancedOperation} class:
 * <p>
 * - Notifying a delegate of all operation completion
 * - Extracting generated dependencies from operation conditions
 * - Setting up dependencies to enforce mutual exclusivity
 * <p>
 * Created by kdubb on 2/3/16.
 */
public class AdvancedOperationQueue extends OperationQueue {

  public interface Observer {

    void operationAdded(AdvancedOperationQueue queue, Operation operation);

    void operationFinished(AdvancedOperationQueue queue, Operation operation, Collection<Throwable> errors);
  }



  public static class ObserverAdapter implements Observer {

    @Override
    public void operationAdded(AdvancedOperationQueue queue, Operation operation) {
    }

    @Override
    public void operationFinished(AdvancedOperationQueue queue, Operation operation, Collection<Throwable> errors) {
    }

  }



  private Observer observer;

  public Observer getObserver() {
    return observer;
  }

  public void setObserver(Observer observer) {
    this.observer = observer;
  }

  @Override
  public void addOperations(Operation... operations) {
    addOperations(asList(operations));
  }

  @Override
  public void addOperations(Collection<Operation> operations) {
    for(Operation operation : operations) {
      addOperation(operation);
    }
  }

  @Override
  public void addOperation(Operation operation) {

    if(operation instanceof AdvancedOperation) {

      AdvancedOperation advancedOperation = (AdvancedOperation) operation;

      // Add an observer to invoke delegate & management methods

      advancedOperation.addObserver(new AdvancedOperation.Observer() {

        @Override
        public void produced(AdvancedOperation observedOperation, Operation newOperation) {
          addOperation(newOperation);
        }

        @Override
        public void finished(AdvancedOperation observedOperation, Collection<Throwable> errors) {
          if(observer != null) {
            observer.operationFinished(AdvancedOperationQueue.this, observedOperation, errors);
          }
        }
      });

      // Extract dependencies needed by the operation

      Collection<Operation> dependencies = FluentIterable.from(advancedOperation.getConditions())
          .transform(condition -> condition.dependencyForOperation(advancedOperation))
          .filter(condition -> condition != null)
          .toList();

      // Wire up dependencies

      for(Operation dependency : dependencies) {

        advancedOperation.addDependency(dependency);

        addOperation(dependency);
      }

      // Enforce mutual-exclusivity for operations that need it

      Collection<String> concurrencyCategories = FluentIterable.from(advancedOperation.getConditions())
          .transformAndConcat(condition -> condition.isMutuallyExclusive() ?
                                           singleton(condition.getClass().getName()) :
                                           emptySet())
          .toList();

      if(!concurrencyCategories.isEmpty()) {

        ExclusivityController.addOperation(advancedOperation, concurrencyCategories);

        advancedOperation.addObserver(new AdvancedOperation.Observer() {

          @Override
          public void started(AdvancedOperation operation) {

          }

          @Override
          public void produced(AdvancedOperation operation, Operation newOperation) {

          }

          @Override
          public void finished(AdvancedOperation operation, Collection<Throwable> errors) {
            ExclusivityController.removeOperation(operation, concurrencyCategories);
          }
        });
      }

      advancedOperation.willEnqueue();

    }
    else {

      operation.addStateObserver(changedOperation -> {

        if(changedOperation.isFinished()) {

          if(observer != null) {
            observer.operationFinished(this, changedOperation, emptySet());
          }

        }

      });

    }

    if(observer != null) {
      observer.operationAdded(this, operation);
    }

    super.addOperation(operation);
  }

}
