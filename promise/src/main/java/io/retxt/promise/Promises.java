package io.retxt.promise;

import io.retxt.dispatch.DispatchQueue;
import io.retxt.dispatch.DispatchQueues;
import io.retxt.promise.functions.Function;
import io.retxt.promise.functions.Sealant;
import io.retxt.promise.functions.Supplier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;



/**
 * Created by kdubb on 2/10/16.
 */
public class Promises {

  public static <P> Promise<P> fulfilled(P value) {
    return new Promise<>(value);
  }

  public static <P> Promise<P> rejected(Throwable value) {
    return new Promise<>(value);
  }

  public static <P> Promise<P> delay(long timeout, TimeUnit timeUnit, Supplier<P> body) {
    return new Promise<>((Sealant<Function<Resolution<P>>>) resolver -> {
      DispatchQueues.HIGH.dispatchAfter(timeout, timeUnit, () -> {
        try {
          resolver.call(Resolution.fulfilled(body.supply()));
        }
        catch(Throwable t) {
          resolver.call(Resolution.rejected(t));
        }
      });
    });
  }

  public static <P> Promise<P> delay(long timeout, TimeUnit timeUnit, P value) {
    return new Promise<>((Sealant<Function<Resolution<P>>>) resolver -> {
      DispatchQueues.HIGH.dispatchAfter(timeout, timeUnit, () -> {
        resolver.call(Resolution.fulfilled(value));
      });
    });
  }

  public static <P> Promise<P> dispatch(DispatchQueue on, Supplier<P> body) {
    return new Promise<>((Sealant<Function<Resolution<P>>>) resolver -> {
      on.dispatch(() -> {
        try {
          resolver.call(Resolution.fulfilled(body.supply()));
        }
        catch(Throwable t) {
          resolver.call(Resolution.rejected(t));
        }
      });
    });
  }

  public static <P> Promise<P> dispatch(Supplier<P> body) {
    return dispatch(DispatchQueues.MEDIUM, body);
  }

  public static <P> Future<P> asFuture(Promise<P> source) {
    CompletableFuture<P> future = new CompletableFuture<>();
    source.then(future::complete);
    source.error(future::completeExceptionally);
    return future;
  }

}
