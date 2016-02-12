package io.retxt.promise.functions;

/**
 * A functional interface that supplies a single argument.
 * <p>
 * Created by kdubb on 2/8/16.
 */
@FunctionalInterface
public interface Supplier<S> {

  S supply() throws Throwable;

}
