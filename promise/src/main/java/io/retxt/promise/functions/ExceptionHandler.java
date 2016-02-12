package io.retxt.promise.functions;

/**
 * Created by kdubb on 2/8/16.
 */
@FunctionalInterface
public interface ExceptionHandler {

  void handle(Throwable error);

}
