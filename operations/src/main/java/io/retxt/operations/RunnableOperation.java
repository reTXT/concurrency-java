package io.retxt.operations;

/**
 * Operation that uses a runnable as its task.
 * <p>
 * Created by kdubb on 1/30/16.
 */
public class RunnableOperation extends Operation {

  private Runnable task;

  public RunnableOperation(String name, Runnable task) {
    super(name);
    this.task = task;
  }

  @Override
  protected void run() {
    task.run();
  }
  
}
