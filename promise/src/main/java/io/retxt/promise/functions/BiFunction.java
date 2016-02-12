package io.retxt.promise.functions;

/**
 * Created by kdubb on 2/9/16.
 */
@FunctionalInterface
public interface BiFunction<T, U> {

  void call(T value1, U value2);

}
