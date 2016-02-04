package io.retxt.operations;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.synchronizedCollection;



/**
 * Operation that supports dependencies & execution on an OperationQueue
 * <p>
 * Created by kdubb on 1/28/16.
 */
public abstract class Operation {

  public interface StateObserver {

    void operationStateChanged(Operation operation);

  }



  private String name;
  private OperationQueue queue;
  private Collection<StateObserver> observers = synchronizedCollection(new HashSet<>());
  private Collection<Operation> waitingDependencies = synchronizedCollection(new HashSet<>());
  private AtomicBoolean cancelled = new AtomicBoolean(false);
  private AtomicBoolean started = new AtomicBoolean(false);
  private AtomicBoolean finished = new AtomicBoolean(false);
  private CountDownLatch completionLatch = new CountDownLatch(1);
  private Runnable completionBlock;

  public Operation(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public boolean isReady() {
    return !started.get() && waitingDependencies.isEmpty();
  }

  public boolean isExecuting() {
    return started.get() && !finished.get();
  }

  public boolean isFinished() {
    return finished.get();
  }

  public boolean isCancelled() {
    return cancelled.get();
  }

  public void cancel() {
    cancelled.set(true);
    waitingDependencies.clear();
    fireStateChanged();
  }

  public void addDependency(Operation dependency) {
    checkState(queue == null,
               "Cannot add dependencies after adding the operation to a queue");
    checkState(!isExecuting() && !isFinished() && !isCancelled(),
               "Cannot add dependencies after operation has started");

    waitingDependencies.add(dependency);
    dependency.observers.add(this::dependencyStateChanged);
    dependencyStateChanged(dependency);
  }

  public void removeDependency(Operation dependency) {
    checkState(queue == null,
               "Cannot add dependencies after adding the operation to a queue");
    checkState(!isExecuting() && !isFinished() && !isCancelled(),
               "Cannot add dependencies after operation has started");

    dependency.observers.remove((StateObserver) this::dependencyStateChanged);
    waitingDependencies.remove(dependency);
    dependencyStateChanged(dependency);
  }

  public void addStateObserver(StateObserver observer) {
    observers.add(observer);
  }

  public void waitForCompletion() throws InterruptedException {
    completionLatch.await();
  }

  public boolean waitForCompletion(long timeout, TimeUnit timeUnit) throws InterruptedException {
    return completionLatch.await(timeout, timeUnit);
  }

  public Runnable getCompletionBlock() {
    return completionBlock;
  }

  public void setCompletionBlock(Runnable completionBlock) {
    this.completionBlock = completionBlock;
  }

  protected abstract void run();

  protected void execute() {

    beginExecution();
    try {

      if(!isCancelled()) {

        run();

      }

    }
    catch(Throwable e) {
      Thread thread = Thread.currentThread();
      thread.getUncaughtExceptionHandler().uncaughtException(thread, e);
    }
    finally {

      endExecution();

    }

  }

  void setQueue(OperationQueue queue) {

    if(this.queue != null) {
      throw new IllegalStateException("Operation already in queue");
    }

    synchronized(this) {

      this.queue = queue;
      observers.add(this.queue::operationStateChanged);

    }

  }

  void unsetQueue() {

    synchronized(this) {

      if(isExecuting() || isFinished() || isCancelled()) {
        throw new IllegalStateException("Operation in invalid state to remove ");
      }

      observers.remove((StateObserver) this.queue::operationStateChanged);
      this.queue = null;

    }

  }

  protected void beginExecution() {
    started.set(true);
    fireStateChanged();
  }

  protected void endExecution() {
    finished.set(true);
    fireStateChanged();

    completionLatch.countDown();

    if(completionBlock != null) {
      completionBlock.run();
    }
  }

  void fireStateChanged() {
    for(StateObserver observer : observers) {
      observer.operationStateChanged(this);
    }
  }

  void dependencyStateChanged(Operation dependency) {

    if(dependency.isFinished()) {
      waitingDependencies.remove(dependency);
    }

    fireStateChanged();
  }

}
