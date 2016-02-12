package io.retxt.promise.functions;

import io.retxt.promise.Promise;



/**
 * A functional interface that consumes and supplies a single promise.
 * <p>
 * Created by kdubb on 2/8/16.
 */
@FunctionalInterface
public interface Pipe<C, P> {

  Promise<P> pipe(C value) throws Throwable;

}
