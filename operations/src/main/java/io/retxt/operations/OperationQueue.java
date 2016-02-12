package io.retxt.operations;

import io.retxt.dispatch.ConcurrentDispatchQueue;
import io.retxt.dispatch.UserDispatchQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashSet;

import static java.util.Arrays.asList;
import static java.util.Collections.synchronizedCollection;
import static java.util.Collections.unmodifiableCollection;



/**
 * Operation queue for executing Operation objects.
 * <p>
 * Created by kdubb on 1/28/16.
 */
public class OperationQueue {

  private static final Logger logger = LogManager.getLogger();

  private UserDispatchQueue executionQueue;
  private Collection<Operation> queuedOperations = synchronizedCollection(new HashSet<>());

  public OperationQueue() {
    this(new ConcurrentDispatchQueue(Thread.NORM_PRIORITY));
  }

  public OperationQueue(UserDispatchQueue dispatchQueue) {
    this.executionQueue = dispatchQueue;
  }

  public Collection<Operation> getOperations() {
    return unmodifiableCollection(queuedOperations);
  }

  public boolean isPaused() {
    return executionQueue.isSuspended();
  }

  public void suspend() {
    executionQueue.suspend();
  }

  public void resume() {
    executionQueue.resume();
  }

  public void addOperations(Operation... operationsArray) {
    addOperations(asList(operationsArray));
  }

  public void addOperations(Collection<Operation> operations) {
    queuedOperations.addAll(operations);
    for(Operation operation : operations) {
      operation.setQueue(this);
      operationStateChanged(operation);
    }
  }

  public void addOperation(Operation operation) {
    queuedOperations.add(operation);
    operation.setQueue(this);
    operationStateChanged(operation);
  }

  public void removeOperation(Operation operation) {
    operation.unsetQueue();
    queuedOperations.remove(operation);
  }

  void operationStateChanged(Operation operation) {

    logger.trace("Operation state changed: {}", operation.getName());

    if(operation.isReady() && queuedOperations.remove(operation)) {

      logger.trace("Executing operation: {}", operation.getName());

      executionQueue.execute(operation::execute);
    }

  }

}
