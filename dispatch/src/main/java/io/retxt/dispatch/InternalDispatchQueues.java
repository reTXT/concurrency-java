package io.retxt.dispatch;

import com.google.common.collect.Ordering;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Integer.parseInt;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.getProperty;



abstract class InternalDispatchQueue implements DispatchQueue {

  enum Priority {

    High(Thread.MAX_PRIORITY),
    Medium(Thread.NORM_PRIORITY),
    Low(Thread.MIN_PRIORITY);

    private final int threadPriority;

    Priority(int threadPriority) {
      this.threadPriority = threadPriority;
    }

    int getThreadPriority() {
      return threadPriority;
    }

  }



  static ScheduledExecutorService scheduledService =
      new ScheduledThreadPoolExecutor(1, new DispatchThreadFactory("DispatchQueue Scheduler", Thread.NORM_PRIORITY));

  static ThreadPoolExecutor executor = createExecutor();

  protected abstract void _dispatch(Block block);

  protected CountDownLatch _dispatchSync(Block block) {
    CountDownLatch completionLatch = new CountDownLatch(1);
    _dispatch(() -> {
      try {
        block.run();
      }
      finally {
        completionLatch.countDown();
      }
    });
    return completionLatch;
  }

  protected void _dispatchAfter(long time, TimeUnit timeUnit, Block block) {
    scheduledService.schedule(() -> this.dispatch(block), time, timeUnit);
  }

  @Override
  public void dispatch(Block block) {
    _dispatch(block);
  }

  @Override
  public void dispatchAfter(long time, TimeUnit timeUnit, Block block) {
    _dispatchAfter(time, timeUnit, block);
  }

  @Override
  public void dispatchSync(Block block) {
    while(true) {
      try {
        _dispatchSync(block).await();
        return;
      }
      catch(InterruptedException ignored) {
      }
    }
  }

  @Override
  public boolean dispatchSync(long timeout, TimeUnit timeUnit, Block block) {
    while(true) {
      try {
        return _dispatchSync(block).await(timeout, timeUnit);
      }
      catch(InterruptedException ignored) {
      }
    }
  }

  public static ThreadPoolExecutor createExecutor() {

    int minThreads = 1;
    int maxThreads =
        parseInt(getProperty("io.retxt.dispatch.globalQueues.maxThreads",
                             String.valueOf(getRuntime().availableProcessors() * 8)));

    ThreadFactory threadFactory = new DispatchThreadFactory("Dispatch Queue", Thread.NORM_PRIORITY);
    BacklogTransferQueue queue = new BacklogTransferQueue();
    return new ThreadPoolExecutor(minThreads, maxThreads,
                                  60, TimeUnit.SECONDS,
                                  queue,
                                  threadFactory,
                                  queue);
  }

  static class BacklogTransferQueue extends LinkedTransferQueue<Runnable> implements RejectedExecutionHandler {

    PriorityBlockingQueue<QueuedBlock> backlogQueue = new PriorityBlockingQueue<>(50, Ordering.natural());

    @Override
    public boolean offer(Runnable r) {
      return tryTransfer(r);
    }

    @Override
    public Runnable poll() {
      Runnable next = backlogQueue.poll();
      if(next != null) {
        return next;
      }
      return super.poll();
    }

    @Override
    public Runnable poll(long timeout, TimeUnit timeUnit) throws InterruptedException {
      Runnable next = backlogQueue.poll();
      if(next != null) {
        return next;
      }
      return super.poll(timeout, timeUnit);
    }

    @Override
    public Runnable take() throws InterruptedException {
      Runnable next = backlogQueue.poll();
      if(next != null) {
        return next;
      }
      return super.take();
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
      backlogQueue.add((QueuedBlock) r);
    }

  }



  /**
   * Priority/FIFO ordered Block wrapper.
   * <p>
   * Created by kdubb on 11/21/14.
   */
  static class QueuedBlock implements Runnable, Comparable<QueuedBlock> {

    interface Listener {

      void scheduled(QueuedBlock block);

      void enter(QueuedBlock block);

      void exit(QueuedBlock block);

    }



    private static AtomicLong currentSequence = new AtomicLong();

    protected Block block;

    protected Integer priority;
    protected Long sequence;
    protected Listener listener;

    QueuedBlock(Block block, int priority, Listener listener) {
      this.block = block;
      this.sequence = currentSequence.incrementAndGet();
      this.priority = priority;
      this.listener = listener;

      if(listener != null) {
        listener.scheduled(this);
      }
    }

    @Override
    public int compareTo(QueuedBlock another) {
      int res = priority.compareTo(another.priority);
      if(res != 0) {
        return res;
      }

      return sequence.compareTo(another.sequence);
    }

    @Override
    public void run() {

      if(listener != null) {
        listener.enter(this);
      }

      Thread thread = Thread.currentThread();

      thread.setPriority(priority);
      try {

        block.run();

      }
      catch(Throwable e) {

        thread.getUncaughtExceptionHandler().uncaughtException(thread, e);

      }
      finally {
        thread.setPriority(Thread.NORM_PRIORITY);
        if(listener != null) {
          listener.exit(this);
        }
      }

    }

  }

}



/**
 * Global (concurrent) dispatch queue
 * <p>
 * Executes blocks concurrently on a global priority based thread pool.
 * <p>
 * Created by kdubb on 11/21/14.
 */
class GlobalDispatchQueue extends InternalDispatchQueue implements DispatchQueue {

  private Priority priority;

  GlobalDispatchQueue(Priority priority) {
    this.priority = priority;
  }

  @Override
  public void _dispatch(Block block) {
    executor.execute(new QueuedBlock(block, priority.getThreadPriority(), null));
  }

}
