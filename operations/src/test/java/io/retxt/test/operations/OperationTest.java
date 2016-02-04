package io.retxt.test.operations;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import io.retxt.dispatch.DispatchQueues;
import io.retxt.dispatch.SerialDispatchQueue;
import io.retxt.operations.Operation;
import io.retxt.operations.OperationQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;



/**
 * Unit tests for Operation class.
 * <p>
 * Created by kdubb on 1/29/16.
 */
public class OperationTest {

  private static Logger logger = LogManager.getLogger();



  class WaitOperation extends Operation {

    private AtomicBoolean executed = new AtomicBoolean(false);
    private int seconds;

    public WaitOperation(String name, int seconds) {
      super(name);
      this.seconds = seconds;
    }

    boolean isExecuted() {
      return executed.get();
    }

    protected void run() {
      logger.trace("{} Started", getName());
      try {
        SECONDS.sleep(seconds);
        executed.set(true);
      }
      catch(InterruptedException ignored) {
      }
      logger.trace("{} Finished", getName());
    }

  }



  class SimpleOperation extends Operation {

    private AtomicBoolean executed = new AtomicBoolean(false);

    public SimpleOperation(String name) {
      super(name);
    }

    boolean isExecuted() {
      return executed.get();
    }

    protected void run() {
      logger.trace("{} Started", getName());
      executed.set(true);
      logger.trace("{} Finished", getName());
    }
  }



  class AppendingOperation<T> extends Operation {

    private Collection<T> collection;
    private T val;

    public AppendingOperation(String name, Collection<T> collection, T val) {
      super(name);
      this.collection = collection;
      this.val = val;
    }

    protected void run() {
      logger.trace("{} Started", getName());
      collection.add(val);
      logger.trace("{} Finished", getName());
    }

  }

  @Test
  public void testDelayedDependencyChain() throws InterruptedException {

    OperationQueue queue = new OperationQueue();

    WaitOperation a = new WaitOperation("A", 1);
    WaitOperation b = new WaitOperation("B", 1);
    SimpleOperation c = new SimpleOperation("C");

    b.addDependency(a);
    c.addDependency(b);

    queue.addOperations(a, b, c);

    Thread.sleep(50);

    assertThat(a.isReady(), is(false));
    assertThat(a.isExecuting(), is(true));
    assertThat(a.isFinished(), is(false));
    assertThat(a.isCancelled(), is(false));
    assertThat(a.isExecuted(), is(false));

    assertThat(b.isReady(), is(false));
    assertThat(b.isExecuting(), is(false));
    assertThat(b.isFinished(), is(false));
    assertThat(b.isCancelled(), is(false));
    assertThat(b.isExecuted(), is(false));

    assertThat(c.isReady(), is(false));
    assertThat(c.isExecuting(), is(false));
    assertThat(c.isFinished(), is(false));
    assertThat(c.isCancelled(), is(false));
    assertThat(c.isExecuted(), is(false));

    assertThat(a.waitForCompletion(5, SECONDS), is(true));
    Thread.sleep(50);

    assertThat(a.isReady(), is(false));
    assertThat(a.isExecuting(), is(false));
    assertThat(a.isFinished(), is(true));
    assertThat(a.isCancelled(), is(false));
    assertThat(a.isExecuted(), is(true));

    assertThat(b.isReady(), is(false));
    assertThat(b.isExecuting(), is(true));
    assertThat(b.isFinished(), is(false));
    assertThat(b.isCancelled(), is(false));
    assertThat(b.isExecuted(), is(false));

    assertThat(c.isReady(), is(false));
    assertThat(c.isExecuting(), is(false));
    assertThat(c.isFinished(), is(false));
    assertThat(c.isCancelled(), is(false));
    assertThat(c.isExecuted(), is(false));

    assertThat(c.waitForCompletion(5, SECONDS), is(true));
    Thread.sleep(50);

    assertThat(a.isReady(), is(false));
    assertThat(a.isExecuting(), is(false));
    assertThat(a.isFinished(), is(true));
    assertThat(a.isCancelled(), is(false));
    assertThat(a.isExecuted(), is(true));

    assertThat(b.isReady(), is(false));
    assertThat(b.isExecuting(), is(false));
    assertThat(b.isFinished(), is(true));
    assertThat(b.isCancelled(), is(false));
    assertThat(b.isExecuted(), is(true));

    assertThat(c.isReady(), is(false));
    assertThat(c.isExecuting(), is(false));
    assertThat(c.isFinished(), is(true));
    assertThat(c.isCancelled(), is(false));
    assertThat(c.isExecuted(), is(true));

  }

  @Test
  public void testMultiDependencies() throws InterruptedException {

    OperationQueue queue = new OperationQueue();

    WaitOperation a = new WaitOperation("A", 1);
    SimpleOperation b = new SimpleOperation("B");
    SimpleOperation c = new SimpleOperation("C");

    b.addDependency(a);
    c.addDependency(a);

    queue.addOperations(a, b, c);

    Thread.sleep(50);

    assertThat(a.isReady(), is(false));
    assertThat(a.isExecuting(), is(true));
    assertThat(a.isFinished(), is(false));
    assertThat(a.isCancelled(), is(false));
    assertThat(a.isExecuted(), is(false));

    assertThat(b.isReady(), is(false));
    assertThat(b.isExecuting(), is(false));
    assertThat(b.isFinished(), is(false));
    assertThat(b.isCancelled(), is(false));
    assertThat(b.isExecuted(), is(false));

    assertThat(c.isReady(), is(false));
    assertThat(c.isExecuting(), is(false));
    assertThat(c.isFinished(), is(false));
    assertThat(c.isCancelled(), is(false));
    assertThat(c.isExecuted(), is(false));

    assertThat(b.waitForCompletion(5, SECONDS), is(true));
    assertThat(c.waitForCompletion(5, SECONDS), is(true));

    assertThat(a.isReady(), is(false));
    assertThat(a.isExecuting(), is(false));
    assertThat(a.isFinished(), is(true));
    assertThat(a.isCancelled(), is(false));
    assertThat(a.isExecuted(), is(true));

    assertThat(b.isReady(), is(false));
    assertThat(b.isExecuting(), is(false));
    assertThat(b.isFinished(), is(true));
    assertThat(b.isCancelled(), is(false));
    assertThat(b.isExecuted(), is(true));

    assertThat(c.isReady(), is(false));
    assertThat(c.isExecuting(), is(false));
    assertThat(c.isFinished(), is(true));
    assertThat(c.isCancelled(), is(false));
    assertThat(c.isExecuted(), is(true));

  }

  @Test
  public void testCancelWithDelayedDependency() throws InterruptedException {

    OperationQueue queue = new OperationQueue();

    WaitOperation a = new WaitOperation("A", 2);
    SimpleOperation b = new SimpleOperation("B");

    b.addDependency(a);

    queue.addOperations(a, b);

    b.cancel();

    Thread.sleep(500);

    assertThat(b.isReady(), is(false));
    assertThat(b.isExecuting(), is(false));
    assertThat(b.isFinished(), is(true));
    assertThat(b.isCancelled(), is(true));
    assertThat(b.isExecuted(), is(false));

    assertThat(a.isReady(), is(false));
    assertThat(a.isExecuting(), is(true));
    assertThat(a.isFinished(), is(false));
    assertThat(a.isCancelled(), is(false));
    assertThat(a.isExecuted(), is(false));

    assertThat(a.waitForCompletion(5, SECONDS), is(true));

    assertThat(a.isReady(), is(false));
    assertThat(a.isExecuting(), is(false));
    assertThat(a.isFinished(), is(true));
    assertThat(a.isExecuted(), is(true));
  }

  @Test
  public void testMultipleOperationsOnSerialQueue() throws InterruptedException {

    OperationQueue queue = new OperationQueue(new SerialDispatchQueue(DispatchQueues.HIGH));

    List<Integer> results = new ArrayList<>();
    List<Operation> operations = new ArrayList<>();

    for(int c = 0; c < 100; ++c) {
      operations.add(new AppendingOperation<>("a", results, c));
    }

    queue.addOperations(operations);

    assertThat(Iterables.getLast(operations).waitForCompletion(5, SECONDS), is(true));

    List<Integer> sortedResults = FluentIterable.from(results)
        .toSortedList(Ordering.natural());

    assertThat(results, is(sortedResults));
  }

}
