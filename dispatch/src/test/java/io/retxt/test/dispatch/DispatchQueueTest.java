package io.retxt.test.dispatch;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Ordering;
import io.retxt.dispatch.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;



/**
 * Unit tests for DispatchQueue
 * <p>
 * Created by kdubb on 1/30/16.
 */
@SuppressWarnings("Duplicates")
public class DispatchQueueTest {

  private static final Logger logger = LogManager.getLogger();
  private static final int NUM_THREADS_PER_QUEUE = 8;

  static {
    System.setProperty("io.retxt.dispatch.globalQueues.maxThreads", String.valueOf(NUM_THREADS_PER_QUEUE));
  }



  class WaitingRunnable implements Runnable {

    private String name;
    private long sleepTimeMS;
    private AtomicBoolean executed = new AtomicBoolean();

    public WaitingRunnable(String name, long sleepTime, TimeUnit timeUnit) {
      this.name = name;
      this.sleepTimeMS = timeUnit.toMillis(sleepTime);
    }

    @Override
    public void run() {
      logger.trace("{} Started", name);
      try {
        Thread.sleep(sleepTimeMS);
      }
      catch(InterruptedException ignored) {
      }
      executed.set(true);
      logger.trace("{} Finished", name);
    }

  }



  class SimpleRunnable implements Runnable {

    private String name;
    private AtomicBoolean executed = new AtomicBoolean();

    public SimpleRunnable(String name) {
      this.name = name;
    }

    @Override
    public void run() {
      logger.trace("{} Started", name);
      executed.set(true);
      logger.trace("{} Finished", name);
    }

  }

  @Test
  public void testExecuteSyncTimeoutSuccess() throws InterruptedException {

    AtomicBoolean executed = new AtomicBoolean(false);
    boolean result = DispatchQueues.HIGH.executeSync(10, MILLISECONDS, () -> {
      try {
        Thread.sleep(100);
      }
      catch(InterruptedException ignored) {
      }
      executed.set(true);
    });
    assertThat(result, is(false));
    assertThat(executed.get(), is(false));
  }

  @Test
  public void testExecuteSyncTimeoutFail() throws InterruptedException {

    AtomicBoolean executed = new AtomicBoolean(false);
    boolean result = DispatchQueues.HIGH.executeSync(100, MILLISECONDS, () -> {
      try {
        Thread.sleep(10);
      }
      catch(InterruptedException ignored) {
      }
      executed.set(true);
    });
    assertThat(result, is(true));
    assertThat(executed.get(), is(true));
  }

  @Test
  public void testExceptionsDontStopDispatchingSerialQueue() throws InterruptedException {

    SerialDispatchQueue queue = new SerialDispatchQueue(DispatchQueues.HIGH);

    int taskCount = 200;
    int exceptionTask = new Random().nextInt(taskCount);

    CountDownLatch latch = new CountDownLatch(taskCount - 1);

    for(int c = 0; c < taskCount; ++c) {
      final int finalC = c;
      queue.execute(() -> {
        if(exceptionTask == finalC) {
          throw new RuntimeException();
        }
        try {
          Thread.sleep(10);
        }
        catch(InterruptedException ignored) {
        }
        finally {
          latch.countDown();
        }
      });
    }

    assertThat(latch.await(5, SECONDS), is(true));
  }

  @Test
  public void testExceptionsDontStopDispatchingConcurrentQueue() throws InterruptedException {

    ConcurrentDispatchQueue queue = new ConcurrentDispatchQueue(Thread.NORM_PRIORITY);

    int taskCount = 200;
    int exceptionTask = new Random().nextInt(taskCount);

    CountDownLatch latch = new CountDownLatch(taskCount - 1);

    for(int c = 0; c < taskCount; ++c) {
      final int finalC = c;
      queue.execute(() -> {
        if(exceptionTask == finalC) {
          throw new RuntimeException();
        }
        try {
          Thread.sleep(10);
        }
        catch(InterruptedException ignored) {
        }
        finally {
          latch.countDown();
        }
      });
    }

    assertThat(latch.await(5, SECONDS), is(true));
  }

  @Test
  public void testConcurrentQueue() throws InterruptedException {

    ConcurrentDispatchQueue queue = new ConcurrentDispatchQueue(Thread.NORM_PRIORITY);

    int taskCount = 200;

    CountDownLatch latch = new CountDownLatch(taskCount);

    for(int c = 0; c < taskCount; ++c) {
      queue.execute(() -> {
        try {
          Thread.sleep(10);
        }
        catch(InterruptedException ignored) {
        }
        finally {
          latch.countDown();
        }
      });
    }

    assertThat(latch.await(5, SECONDS), is(true));
  }

  @Test
  public void testConcurrentQueueBarrier() throws InterruptedException {

    ConcurrentDispatchQueue queue = new ConcurrentDispatchQueue(Thread.NORM_PRIORITY);

    ReentrantLock lock = new ReentrantLock();

    for(int c = 0; c < 200; ++c) {
      queue.execute(() -> {
        lock.lock();
        try {
          Thread.sleep(10);
        }
        catch(InterruptedException ignored) {
        }
        finally {
          lock.unlock();
        }
      });
    }

    CountDownLatch latch = new CountDownLatch(1);
    queue.executeBarrier(() -> {
      assertThat(lock.tryLock(), is(true));
      latch.countDown();
    });

    assertThat(latch.await(5, SECONDS), is(true));
  }

  @Test
  public void testExecuteSync() throws InterruptedException {

    AtomicBoolean completed = new AtomicBoolean(false);
    DispatchQueues.LOW.executeSync(() -> completed.set(true));
    assertThat(completed.get(), is(true));
  }

  @Test
  public void testThreadsStartedBeforeQueueing() throws InterruptedException {

    List<WaitingRunnable> objs = new ArrayList<>();
    for(int c = 0; c < NUM_THREADS_PER_QUEUE; ++c) {
      WaitingRunnable obj = new WaitingRunnable("" + c, 1, SECONDS);
      objs.add(obj);
      DispatchQueues.HIGH.execute(obj);
    }

    MILLISECONDS.sleep(1500);

    for(WaitingRunnable obj : objs) {
      assertThat(obj.executed.get(), is(true));
    }

  }

  @Test
  public void testThreadsQueue() throws InterruptedException {

    // Fill up queue with waiting items
    for(int c = 0; c < NUM_THREADS_PER_QUEUE; ++c) {
      DispatchQueues.HIGH.execute(new WaitingRunnable("executed task", 1, SECONDS));
    }

    // Start threads that shouldn't get executed
    List<WaitingRunnable> objs = new ArrayList<>();
    for(int c = 0; c < 10; ++c) {
      WaitingRunnable obj = new WaitingRunnable("" + c, 1, SECONDS);
      objs.add(obj);
      DispatchQueues.HIGH.execute(obj);
    }

    SECONDS.sleep(1);

    for(WaitingRunnable obj : objs) {
      assertThat(obj.executed.get(), is(false));
    }

  }

  @Test
  public void testScheduling() throws InterruptedException {

    SimpleRunnable a = new SimpleRunnable("a");
    SimpleRunnable b = new SimpleRunnable("b");

    DispatchQueues.HIGH.executeAfter(1500, MILLISECONDS, a);
    DispatchQueues.HIGH.executeAfter(1500, MILLISECONDS, b);

    SECONDS.sleep(1);

    assertThat(a.executed.get(), is(false));
    assertThat(b.executed.get(), is(false));

    SECONDS.sleep(1);

    assertThat(a.executed.get(), is(true));
    assertThat(b.executed.get(), is(true));
  }

  @Test
  public void testSerialQueue() throws InterruptedException {

    DispatchQueue queue = new SerialDispatchQueue(DispatchQueues.HIGH);

    List<Integer> results = new ArrayList<>();

    for(int c = 0; c < 100; ++c) {
      final int val = c;
      queue.execute(() -> results.add(val));
    }

    // Schedule block to notify when all tasks have completed
    CountDownLatch latch = new CountDownLatch(1);
    queue.execute(latch::countDown);

    latch.await(5, SECONDS);

    List<Integer> sortedResults = FluentIterable.from(results)
        .toSortedList(Ordering.natural());

    assertThat(results, is(sortedResults));
  }

  @Test
  public void testGroupWithSerialQueue() throws InterruptedException {

    DispatchQueue queue = new SerialDispatchQueue(DispatchQueues.HIGH);
    DispatchGroup group = new DispatchGroup();

    AtomicInteger otherFinished = new AtomicInteger(0);
    AtomicInteger groupFinished = new AtomicInteger(0);
    for(int c = 0; c < 50; ++c) {
      group.execute(queue, () -> {
        try {
          Thread.sleep(50);
        }
        catch(InterruptedException ignored) {
        }
        groupFinished.incrementAndGet();
      });
      queue.execute(otherFinished::incrementAndGet);
    }

    group.waitForCompletion(5, SECONDS);

    assertThat(groupFinished.get(), is(50));
  }

  @Test
  public void testGroupWithConcurrentQueue() throws InterruptedException {

    DispatchQueue queue = DispatchQueues.HIGH;
    DispatchGroup group = new DispatchGroup();

    AtomicInteger otherFinished = new AtomicInteger(0);
    AtomicInteger groupFinished = new AtomicInteger(0);
    for(int c = 0; c < 50; ++c) {
      group.execute(queue, () -> {
        try {
          Thread.sleep(50);
        }
        catch(InterruptedException ignored) {
        }
        groupFinished.incrementAndGet();
      });
      queue.execute(otherFinished::incrementAndGet);
    }

    group.waitForCompletion(5, SECONDS);

    assertThat(groupFinished.get(), is(50));
  }

  @Test
  public void testManualGroupWithSerialQueue() throws InterruptedException {

    DispatchQueue queue = new SerialDispatchQueue(DispatchQueues.HIGH);
    DispatchGroup group = new DispatchGroup();

    AtomicInteger otherFinished = new AtomicInteger(0);
    AtomicInteger groupFinished = new AtomicInteger(0);
    for(int c = 0; c < 100; ++c) {
      group.enter();
      queue.execute(() -> {
        try {
          Thread.sleep(10);
        }
        catch(InterruptedException ignored) {
        }
        groupFinished.incrementAndGet();
        group.leave();
      });
      queue.execute(otherFinished::incrementAndGet);
    }

    assertThat(group.waitForCompletion(5, SECONDS), is(true));
    assertThat(groupFinished.get(), is(100));
  }

  @Test
  public void testManualGroupWithConcurrentQueue() throws InterruptedException {

    DispatchQueue queue = DispatchQueues.HIGH;
    DispatchGroup group = new DispatchGroup();

    AtomicInteger otherFinished = new AtomicInteger(0);
    AtomicInteger groupFinished = new AtomicInteger(0);
    for(int c = 0; c < 200; ++c) {
      group.enter();
      queue.execute(() -> {
        try {
          Thread.sleep(50);
        }
        catch(InterruptedException ignored) {
        }
        groupFinished.incrementAndGet();
        group.leave();
      });
      queue.execute(otherFinished::incrementAndGet);
    }

    assertThat(group.waitForCompletion(5, SECONDS), is(true));
    assertThat(groupFinished.get(), is(200));
  }

  @Test
  public void testGroupWithSerialQueueAndNotify() throws InterruptedException {

    DispatchQueue queue = new SerialDispatchQueue(DispatchQueues.HIGH);

    DispatchGroup group = new DispatchGroup();

    AtomicInteger otherFinished = new AtomicInteger(0);
    AtomicInteger groupFinished = new AtomicInteger(0);
    for(int c = 0; c < 200; ++c) {
      group.execute(queue, () -> {
        try {
          Thread.sleep(10);
        }
        catch(InterruptedException ignored) {
        }
        groupFinished.incrementAndGet();
      });
      queue.execute(otherFinished::incrementAndGet);
    }

    CountDownLatch latch = new CountDownLatch(1);
    group.setNotification(queue, latch::countDown);

    assertThat(latch.await(5, SECONDS), is(true));
    assertThat(groupFinished.get(), is(200));
  }

  @Test
  public void testManualGroupWithSerialQueueAndNotify() throws InterruptedException {

    DispatchQueue queue = new SerialDispatchQueue(DispatchQueues.HIGH);

    DispatchGroup group = new DispatchGroup();

    AtomicInteger otherFinished = new AtomicInteger(0);
    AtomicInteger groupFinished = new AtomicInteger(0);
    for(int c = 0; c < 200; ++c) {
      group.enter();
      queue.execute(() -> {
        try {
          Thread.sleep(10);
        }
        catch(InterruptedException ignored) {
        }
        groupFinished.incrementAndGet();
        group.leave();
      });
      queue.execute(otherFinished::incrementAndGet);
    }

    CountDownLatch latch = new CountDownLatch(1);
    group.setNotification(queue, latch::countDown);

    assertThat(latch.await(5, SECONDS), is(true));
    assertThat(groupFinished.get(), is(200));
  }

  @Test
  public void testGroupWithConcurrentQueueAndNotify() throws InterruptedException {

    DispatchQueue queue = DispatchQueues.HIGH;

    DispatchGroup group = new DispatchGroup();

    AtomicInteger otherFinished = new AtomicInteger(0);
    AtomicInteger groupFinished = new AtomicInteger(0);
    for(int c = 0; c < 200; ++c) {
      group.execute(queue, () -> {
        try {
          Thread.sleep(50);
        }
        catch(InterruptedException ignored) {
        }
        groupFinished.incrementAndGet();
      });
      queue.execute(otherFinished::incrementAndGet);
    }

    CountDownLatch latch = new CountDownLatch(1);
    group.setNotification(queue, latch::countDown);

    assertThat(latch.await(5, SECONDS), is(true));
    assertThat(groupFinished.get(), is(200));
  }

  @Test
  public void testManualGroupWithConcurrentQueueAndNotify() throws InterruptedException {

    DispatchQueue queue = DispatchQueues.HIGH;

    DispatchGroup group = new DispatchGroup();

    AtomicInteger otherFinished = new AtomicInteger(0);
    AtomicInteger groupFinished = new AtomicInteger(0);
    for(int c = 0; c < 200; ++c) {
      group.enter();
      queue.execute(() -> {
        try {
          Thread.sleep(50);
        }
        catch(InterruptedException ignored) {
        }
        groupFinished.incrementAndGet();
        group.leave();
      });
      queue.execute(otherFinished::incrementAndGet);
    }

    CountDownLatch latch = new CountDownLatch(1);
    group.setNotification(queue, latch::countDown);

    assertThat(latch.await(5, SECONDS), is(true));
    assertThat(groupFinished.get(), is(200));
  }

  @Test
  public void testGroupNotificationsDontRepeatAutomatically() throws InterruptedException {

    DispatchQueue queue = DispatchQueues.HIGH;

    DispatchGroup group = new DispatchGroup();

    for(int c = 0; c < 50; ++c) {
      group.execute(queue, () -> {
        try {
          Thread.sleep(50);
        }
        catch(InterruptedException ignored) {
        }
      });
    }

    AtomicInteger notified = new AtomicInteger(0);
    CountDownLatch latch1 = new CountDownLatch(1);
    group.setNotification(queue, () -> {
      notified.incrementAndGet();
      latch1.countDown();
    });
    assertThat(latch1.await(5, SECONDS), is(true));

    CountDownLatch latch2 = new CountDownLatch(1);
    group.execute(queue, latch2::countDown);
    assertThat(latch2.await(5, SECONDS), is(true));

    assertThat(notified.get(), is(1));
  }

}
