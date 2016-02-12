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

  protected abstract void _execute(Runnable runnable);

  protected void _executeAfter(long time, TimeUnit timeUnit, Runnable task) {
    scheduledService.schedule(() -> this.execute(task), time, timeUnit);
  }

  @Override
  public void execute(Runnable runnable) {
    _execute(runnable);
  }

  @Override
  public void executeAfter(long time, TimeUnit timeUnit, Runnable task) {
    _executeAfter(time, timeUnit, task);
  }

  @Override
  public void executeSync(Runnable runnable) {
    while(true) {
      try {
        _executeSync(runnable).await();
        return;
      }
      catch(InterruptedException ignored) {
      }
    }
  }

  @Override
  public boolean executeSync(long timeout, TimeUnit timeUnit, Runnable runnable) {
    while(true) {
      try {
        return _executeSync(runnable).await(timeout, timeUnit);
      }
      catch(InterruptedException ignored) {
      }
    }
  }

  private CountDownLatch _executeSync(Runnable runnable) {
    CountDownLatch completionLatch = new CountDownLatch(1);
    _execute(() -> {
      try {
        runnable.run();
      }
      finally {
        completionLatch.countDown();
      }
    });
    return completionLatch;
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

    PriorityBlockingQueue<QueuedTask> backlogQueue = new PriorityBlockingQueue<>(50, Ordering.natural());

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
      backlogQueue.add((QueuedTask) r);
    }
  }



  /**
   * Priority/FIFO ordered Runnable wrapper.
   * <p>
   * Created by kdubb on 11/21/14.
   */
  static class QueuedTask implements Runnable, Comparable<QueuedTask> {

    interface Listener {

      void scheduled(QueuedTask task);

      void enter(QueuedTask task);

      void exit(QueuedTask task);

    }



    private static AtomicLong currentSequence = new AtomicLong();

    protected Runnable work;

    protected Integer priority;
    protected Long sequence;
    protected Listener listener;

    QueuedTask(Runnable work, int priority, Listener listener) {
      this.work = work;
      this.sequence = currentSequence.incrementAndGet();
      this.priority = priority;
      this.listener = listener;

      if(listener != null) {
        listener.scheduled(this);
      }
    }

    @Override
    public int compareTo(QueuedTask another) {
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

      Thread.currentThread().setPriority(priority);
      try {

        work.run();

      }
      finally {
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
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
 * Executes tasks concurrently on a global priority based thread pool.
 * <p>
 * Created by kdubb on 11/21/14.
 */
class GlobalDispatchQueue extends InternalDispatchQueue implements DispatchQueue {

  private Priority priority;

  GlobalDispatchQueue(Priority priority) {
    this.priority = priority;
  }

  @Override
  public void _execute(Runnable runnable) {
    executor.execute(new QueuedTask(runnable, priority.getThreadPriority(), null));
  }

}
