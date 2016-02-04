package io.retxt.test.operations;

import io.retxt.operations.AdvancedOperation;
import io.retxt.operations.AdvancedOperationQueue;
import io.retxt.operations.Operation;
import io.retxt.operations.conditions.Condition;
import io.retxt.operations.conditions.DelayCondition;
import io.retxt.operations.conditions.MutuallyExclusiveCondition;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;



/**
 * Unit tests for AdvancedOperation test.
 * <p>
 * Created by kdubb on 2/3/16.
 */
public class AdvancedOperationTest {

  static class FailedCondition extends Condition {

    public FailedCondition() {
      super("Failed");
    }

    @Override
    public Operation dependencyForOperation(AdvancedOperation operation) {
      return null;
    }

    @Override
    public void evaluateForOperation(AdvancedOperation operation, Consumer<Result> completion) {
      completion.accept(Result.FAILED);
    }

  }

  @Test
  public void testConditionDelays() throws InterruptedException {

    AdvancedOperation operation = new AdvancedOperation("Test");
    operation.addCondition(new DelayCondition(1, SECONDS));

    AdvancedOperationQueue queue = new AdvancedOperationQueue();
    queue.addOperation(operation);

    assertThat(operation.isFinished(), is(false));
    assertThat(operation.waitForCompletion(5, SECONDS), is(true));
    assertThat(operation.isFinished(), is(true));
  }

  @Test
  public void testMultipleConditionsDelay() throws InterruptedException {

    AdvancedOperation operation = new AdvancedOperation("Test");
    operation.addConditions(
        new DelayCondition(1, SECONDS),
        new DelayCondition(2, SECONDS)
    );

    AdvancedOperationQueue queue = new AdvancedOperationQueue();
    queue.addOperation(operation);

    assertThat(operation.isFinished(), is(false));
    assertThat(operation.waitForCompletion(1500, MILLISECONDS), is(false));
    assertThat(operation.isFinished(), is(false));
    assertThat(operation.waitForCompletion(5, SECONDS), is(true));
    assertThat(operation.isFinished(), is(true));
    assertThat(operation.isCancelled(), is(false));
  }

  @Test
  public void testConditionFails() throws InterruptedException {

    CountDownLatch latch = new CountDownLatch(1);
    AdvancedOperation operation = new AdvancedOperation("Test") {

      @Override
      protected void run() {
        latch.countDown();
        super.run();
      }
    };
    operation.addCondition(new FailedCondition());

    AdvancedOperationQueue queue = new AdvancedOperationQueue();
    queue.addOperation(operation);

    assertThat(operation.waitForCompletion(1, SECONDS), is(true));
    assertThat(latch.getCount(), is(1L));
  }

  class TestMutuallyExclusiveCondition extends MutuallyExclusiveCondition<Test> {

    public TestMutuallyExclusiveCondition() {
      super(Test.class);
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

  @Test
  public void testExclusiveConditions() throws InterruptedException {

    AdvancedOperation operation1 = new AdvancedOperation("Test1");
    operation1.addConditions(new TestMutuallyExclusiveCondition(), new DelayCondition(1, SECONDS));

    AdvancedOperation operation2 = new AdvancedOperation("Test2");
    operation2.addCondition(new TestMutuallyExclusiveCondition());

    AdvancedOperationQueue queue = new AdvancedOperationQueue();
    queue.addOperations(operation1, operation2);

    assertThat(operation1.waitForCompletion(300, MILLISECONDS), is(false));
    assertThat(operation1.isFinished(), is(false));
    assertThat(operation2.isFinished(), is(false));
    assertThat(operation2.waitForCompletion(5, SECONDS), is(true));
    assertThat(operation1.isFinished(), is(true));
    assertThat(operation2.isFinished(), is(true));
  }

}
