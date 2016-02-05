package io.retxt.operations;

import io.retxt.operations.conditions.Condition;
import io.retxt.operations.conditions.Evaluator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;



/**
 * {@link Operation} that supports {@link Condition Conditions} and advanced {@link Observer Observers}.
 * <p>
 * Created by kdubb on 2/3/16.
 */
public class AdvancedOperation extends Operation {

  private static final Logger logger = LogManager.getLogger();

  public static boolean DEBUG = false;



  public static class Observer {

    public void started(AdvancedOperation operation) {
    }

    public void produced(AdvancedOperation operation, Operation newOperation) {
    }

    public void finished(AdvancedOperation operation, Collection<Throwable> errors) {
    }

  }



  private enum State {
    Initialized,
    Pending,
    EvaluatingConditions,
    Ready,
    Executing,
    Finishing,
    Finished,
  }



  private State state = State.Initialized;
  private List<Condition> conditions = new ArrayList<>();
  private Collection<Observer> observers = new ArrayList<>();
  private Collection<Throwable> errors = new ArrayList<>();

  public AdvancedOperation(String name) {
    super(name);
  }

  public List<Condition> getConditions() {
    return conditions;
  }

  public Collection<Observer> getObservers() {
    return observers;
  }

  public Collection<Throwable> getErrors() {
    return errors;
  }

  protected State getState() {
    return state;
  }

  protected void setState(State state) {

    if(DEBUG) {
      logger.debug("%s: %s", this, state);
    }

    checkState(state.ordinal() > this.state.ordinal());

    this.state = state;

    fireStateChanged();
  }

  public void produceOperation(Operation operation) {

    for(Observer observer : observers) {
      observer.produced(this, operation);
    }
  }

  @Override
  public boolean isReady() {

    switch(state) {
      case Pending:
        if(super.isReady()) {
          evaluateConditions();
        }
        return false;

      case Ready:
        return super.isReady();

      default:
        return false;
    }

  }

  @Override
  public boolean isExecuting() {
    return state == State.Executing;
  }

  @Override
  public boolean isFinished() {
    return state == State.Finished;
  }

  public void cancelWithErrors(Collection<Throwable> errors) {
    this.errors.addAll(errors);
    cancel();
  }

  protected void willEnqueue() {
    setState(State.Pending);
  }

  private void evaluateConditions() {
    checkState(state == State.Pending);

    setState(State.EvaluatingConditions);

    Evaluator.evaluate(conditions, this, failures -> {

      if(!failures.isEmpty()) {
        cancelWithErrors(failures);
      }

      setState(State.Ready);
    });
  }

  public void addConditions(Condition... conditions) {
    addConditions(asList(conditions));
  }

  public void addConditions(Collection<Condition> conditions) {
    for(Condition condition : conditions) {
      addCondition(condition);
    }
  }

  public void addCondition(Condition condition) {
    checkState(state.ordinal() < State.EvaluatingConditions.ordinal(),
               "Cannot modify conditions after evaluation has begun");

    conditions.add(condition);
  }

  public void addObserver(Observer observer) {
    checkState(state.ordinal() < State.EvaluatingConditions.ordinal(),
               "Cannot modify observers after evaluation has begun");

    observers.add(observer);
  }

  @Override
  public void addDependency(Operation operation) {
    checkState(state.ordinal() < State.EvaluatingConditions.ordinal(),
               "Cannot modify conditions after evaluation has begun");

    super.addDependency(operation);
  }

  @Override
  protected void beginExecution() {

    setState(State.Executing);

    for(Observer observer : observers) {
      observer.started(this);
    }

    super.beginExecution();
  }

  @Override
  protected void run() {
    finish();
  }

  protected void finish(Collection<Throwable> errors) {

    if(state.ordinal() < State.Finishing.ordinal()) {

      setState(State.Finishing);

      this.errors.addAll(errors);

      for(Observer observer : observers) {
        observer.finished(this, this.errors);
      }

      setState(State.Finished);

    }

  }

  protected void finish() {
    finish(emptySet());
  }

}
