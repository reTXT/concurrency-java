package io.retxt.operations.observers;

import io.retxt.operations.AdvancedOperation;
import io.retxt.operations.AdvancedOperation.Observer;
import io.retxt.operations.Operation;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;



/**
 * Adapts {@link Runnable Runnables} to an {@link AdvancedOperation.Observer} object.
 *
 * Created by kdubb on 2/3/16.
 */
public class RunnableObserver extends Observer {

  private Consumer<AdvancedOperation> started;
  private BiConsumer<AdvancedOperation, Collection<Throwable>> finished;

  public RunnableObserver(Consumer<AdvancedOperation> started) {
    this.started = started;
  }

  public RunnableObserver(BiConsumer<AdvancedOperation, Collection<Throwable>> finished) {
    this.finished = finished;
  }

  @Override
  public void started(AdvancedOperation operation) {
    if(started != null) {
      started.accept(operation);
    }
  }

  @Override
  public void produced(AdvancedOperation operation, Operation newOperation) {

  }

  @Override
  public void finished(AdvancedOperation operation, Collection<Throwable> errors) {
    if(finished != null) {
      finished.accept(operation, errors);
    }
  }

}
