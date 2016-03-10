package io.retxt.dispatch;

/**
 * Created by kdubb on 3/10/16.
 */
@FunctionalInterface
public interface Block {

  void run() throws Exception;

}



class RunnableBlock implements Runnable {

  private Block block;

  public RunnableBlock(Block block) {
    this.block = block;
  }

  @Override
  public void run() {
    try {
      block.run();
    }
    catch(Throwable e) {
      Thread thread = Thread.currentThread();
      thread.getUncaughtExceptionHandler().uncaughtException(thread, e);
    }
  }

}
