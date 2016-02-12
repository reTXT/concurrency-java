package io.retxt.promise.functions;

/**
 * A functional interface that consumes and supplies a single argument.
 * <p>
 * Created by kdubb on 2/8/16.
 */
@FunctionalInterface
public interface Filter<C, P> {

  P filter(C value) throws Throwable;

}
