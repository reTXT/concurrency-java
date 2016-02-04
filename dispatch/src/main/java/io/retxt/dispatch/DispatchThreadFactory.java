package io.retxt.dispatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;



/**
 * Dispatch thread factory
 */
class DispatchThreadFactory implements ThreadFactory {

  private static final Logger logger = LogManager.getLogger();

  private ThreadGroup group;
  private AtomicInteger threadNumber = new AtomicInteger(1);
  private String namePrefix;
  private int priority;

  DispatchThreadFactory(String prefix, int priority) {

    SecurityManager s = System.getSecurityManager();

    this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
    this.namePrefix = prefix + " #";
    this.priority = priority;
  }

  public Thread newThread(Runnable r) {

    Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);

    logger.trace("Starting thread {}", t.getName());

    if(t.isDaemon()) {
      t.setDaemon(false);
    }

    if(t.getPriority() != priority) {
      t.setPriority(priority);
    }

    return t;
  }

}
