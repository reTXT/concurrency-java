package io.retxt.promise.functions;

/**
 * Created by kdubb on 2/9/16.
 */
@FunctionalInterface
public interface Function<T> {

  void call(T value);

}
