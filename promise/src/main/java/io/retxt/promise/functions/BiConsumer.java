package io.retxt.promise.functions;

/**
 * An interface for lambdas that consumes a single argument.
 * <p>
 * Created by kdubb on 2/8/16.
 */
@FunctionalInterface
public interface BiConsumer<C, D> {

  void consume(C value1, D value2) throws Throwable;

}
