package io.retxt.promise;

import io.retxt.dispatch.DispatchQueue;
import io.retxt.dispatch.DispatchQueues;
import io.retxt.promise.functions.Function;
import io.retxt.promise.functions.Sealant;
import io.retxt.promise.functions.Supplier;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;



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

  @SuppressWarnings("unchecked")
  public static <P> P await(Promise<P> promise) throws Throwable {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Object> value = new AtomicReference<>();
    promise.always(val -> {
      value.set(val);
      latch.countDown();
    });
    latch.await();
    if(value.get() instanceof Throwable) {
      throw (Throwable) value.get();
    }
    return (P) value.get();
  }

  @SuppressWarnings("unchecked")
  public static <P> P await(Promise<P> promise, long timeout, TimeUnit timeUnit) throws Throwable {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Object> value = new AtomicReference<>();
    promise.always(val -> {
      value.set(val);
      latch.countDown();
    });
    if(!latch.await(timeout, timeUnit)) {
      throw new CancellationException();
    }
    if(value.get() instanceof Throwable) {
      throw (Throwable) value.get();
    }
    return (P) value.get();
  }

}
