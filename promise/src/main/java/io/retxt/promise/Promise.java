package io.retxt.promise;

import com.google.common.base.Optional;
import io.retxt.dispatch.ConcurrentDispatchQueue;
import io.retxt.dispatch.DispatchQueue;
import io.retxt.dispatch.DispatchQueues;
import io.retxt.promise.Seal.Discriminator;
import io.retxt.promise.functions.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.message.ObjectArrayMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Optional.fromNullable;



/**
 * A *promise* represents the future value of a task.
 * <p>
 * To obtain the value of a promise we call `then`.
 * <p>
 * Promises are chainable: `then` returns a promise, you can call `then` on
 * that promise, which returns a promise, you can call `then` on that
 * promise, et cetera.
 * <p>
 * Promises start in a pending state and *resolve* with a value to become
 * *fulfilled* or with an `ErrorType` to become rejected.
 *
 * @param <P>
 */
public class Promise<P> {

  enum ErrorPolicy {
    All,
    NoCancellation
  }



  private State<Resolution<P>> state;

  /**
   * Create a new pending promise.
   * <p>
   * This initializer is convenient when wrapping asynchronous systems that
   * use common patterns. For example:
   * <p>
   * <pre>
   * Promise<Image> fetchKitten() {
   *   return new Promise<>(resolve -> {
   *     KittenFetcher.fetchWithCompletionBlock(resolve)
   *   });
   * }
   * </pre>
   *
   * @see Promise(BiConsumer)
   */
  public Promise(Consumer<BiFunction<P, Throwable>> resolver) {
    this((Sealant<Function<Resolution<P>>>) resolve -> {
      resolver.consume((obj, err) -> {
        if(obj != null) {
          resolve.call(Resolution.fulfilled(obj));
        }
        else if(err != null) {
          resolve.call(Resolution.rejected(err));
        }
        else {
          resolve.call(Resolution.rejected(new IllegalStateException()));
        }
      });
    });
  }

  /**
   * Create a new pending promise.
   * <p>
   * Use this method when wrapping asynchronous systems that do *not* use promises so that they can be involved in
   * promise chains.
   * <p>
   * Don’t use this method if you already have promises! Instead, just return your promise!
   * <p>
   * The closure you pass is executed immediately on the calling thread.
   * <p>
   * <pre>
   * Promise<Image> fetchKitten() {
   *   return new Promise<>((fulfill, reject) -> {
   *     KittenFetcher.fetchWithCompletion((img, err) -> {
   *       if(err == null) {
   *         fulfill.call(img);
   *       }
   *       else {
   *         reject.call(err);
   *       }
   *     }
   *   });
   * }
   * </pre>
   *
   * @param resolvers The provided closure is called immediately. Inside, execute your asynchronous system,
   * calling fulfill if it suceeds and reject for any errors.
   */
  public Promise(BiConsumer<Function<P>, Function<Throwable>> resolvers) {
    AtomicReference<Function<Resolution<P>>> resolver = new AtomicReference<>();
    this.state = new UnsealedState<>(resolver);
    try {
      Function<P> fulfill = resolved -> resolver.get().call(Resolution.fulfilled(resolved));
      Function<Throwable> reject = error -> {
        if(isPending()) {
          resolver.get().call(Resolution.rejected(error));
        }
        else {
          LogManager.getLogger().warn("reject called on already rejected promise", error);
        }
      };
      resolvers.consume(fulfill, reject);
    }
    catch(Throwable t) {
      resolver.get().call(Resolution.rejected(t));
    }
  }

  public Promise(P value) {
    this.state = new SealedState<>(Resolution.fulfilled(value));
  }

  public Promise(Throwable value) {
    this.state = new SealedState<>(Resolution.rejected(value));
  }

  private <Q> Promise(Promise<Q> when, BiFunction<Resolution<Q>, Function<Resolution<P>>> body) {
    this((Sealant<Function<Resolution<P>>>) resolve -> when._pipe(resolution -> body.call(resolution, resolve)));
  }

  Promise(Sealant<Function<Resolution<P>>> sealant) {
    AtomicReference<Function<Resolution<P>>> resolver = new AtomicReference<>();
    this.state = new UnsealedState<>(resolver);
    try {
      sealant.seal(resolver.get());
    }
    catch(Throwable t) {
      resolver.get().call(Resolution.rejected(t));
    }
  }

  private void _pipe(Function<Resolution<P>> body) {
    state.get(seal -> {
      if(seal.isPending()) {
        seal.getPending().add(body);
      }
      else {
        body.call(seal.getResolved());
      }
    });
  }

  public boolean isPending() {
    return !state.get().isPresent();
  }

  public boolean isResolved() {
    return state.get().isPresent();
  }

  public boolean isFulfilled() {
    return state.get().transform(Resolution::isFulfilled).or(false);
  }

  public boolean isRejected() {
    return state.get().transform(Resolution::isRejected).or(false);
  }

  public Promise<P> then(DispatchQueue on, Consumer<P> body) {
    return new Promise<>(this, (resolution, resolve) -> {
      if(resolution.isFulfilled()) {
        on.execute(() -> {
          try {
            body.consume(resolution.getFulfilled());
            resolve.call(Resolution.fulfilled(resolution.getFulfilled()));
          }
          catch(Throwable t) {
            resolve.call(Resolution.rejected(t));
          }
        });
      }
      else {
        resolve.call(Resolution.rejected(resolution.getRejected()));
      }
    });
  }

  public Promise<P> thenInBackground(Consumer<P> body) {
    return then(DispatchQueues.LOW, body);
  }

  public Promise<P> then(Consumer<P> body) {
    return then(DispatchQueues.MAIN, body);
  }

  public <Q> Promise<Q> filter(DispatchQueue on, Filter<P, Q> body) {
    return new Promise<>(this, (resolution, resolve) -> {
      if(resolution.isFulfilled()) {
        on.execute(() -> {
          try {
            resolve.call(Resolution.fulfilled(body.filter(resolution.getFulfilled())));
          }
          catch(Throwable t) {
            resolve.call(Resolution.rejected(t));
          }
        });
      }
      else {
        resolve.call(Resolution.rejected(resolution.getRejected()));
      }
    });
  }

  public <Q> Promise<Q> filterInBackground(Filter<P, Q> body) {
    return filter(DispatchQueues.LOW, body);
  }

  public <Q> Promise<Q> filter(Filter<P, Q> filter) {
    return filter(DispatchQueues.MAIN, filter);
  }

  public <Q> Promise<Q> pipe(DispatchQueue on, Pipe<P, Q> body) {
    return new Promise<>(this, (resolution, resolve) -> {
      if(resolution.isFulfilled()) {
        on.execute(() -> {
          try {
            Promise<Q> promise = body.pipe(resolution.getFulfilled());
            if(promise == this) {
              throw new IllegalStateException("Returned 'this'");
            }
            promise._pipe(resolve);
          }
          catch(Throwable t) {
            resolve.call(Resolution.rejected(t));
          }
        });
      }
      else {
        resolve.call(Resolution.rejected(resolution.getRejected()));
      }
    });
  }

  public <Q> Promise<Q> pipeInBackground(Pipe<P, Q> body) {
    return pipe(DispatchQueues.LOW, body);
  }

  public <Q> Promise<Q> pipe(Pipe<P, Q> body) {
    return pipe(DispatchQueues.MAIN, body);
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  public void error(ErrorPolicy policy, ExceptionHandler body) {
    _pipe(resolution -> {
      if(resolution.isRejected()) {
        if(policy == ErrorPolicy.All || !(resolution.getRejected() instanceof CancellationException)) {
          DispatchQueues.MAIN.execute(() -> body.handle(resolution.getRejected()));
        }
      }
    });
  }

  public void error(ExceptionHandler body) {
    error(ErrorPolicy.NoCancellation, body);
  }

  public Promise<P> recover(DispatchQueue on, Pipe<Throwable, P> body) {
    return new Promise<>(this, (resolution, resolve) -> {
      if(resolution.isFulfilled()) {
        resolve.call(resolution);
      }
      else {
        on.execute(() -> {
          try {
            Promise<P> promise = body.pipe(resolution.getRejected());
            if(promise != this) {
              throw new IllegalStateException("Returned 'this'");
            }
            promise._pipe(resolve);
          }
          catch(Throwable t) {
            resolve.call(Resolution.rejected(t));
          }
        });
      }
    });
  }

  public void recover(Pipe<Throwable, P> body) {
    recover(DispatchQueues.MAIN, body);
  }

  public Promise<P> recover(DispatchQueue on, Filter<Throwable, P> body) {
    return new Promise<>(this, (resolution, resolve) -> {
      if(resolution.isFulfilled()) {
        resolve.call(resolution);
      }
      else {
        on.execute(() -> {
          try {
            resolve.call(Resolution.fulfilled(body.filter(resolution.getRejected())));
          }
          catch(Throwable t) {
            resolve.call(Resolution.rejected(t));
          }
        });
      }
    });
  }

  public void recover(Filter<Throwable, P> body) {
    recover(DispatchQueues.MAIN, body);
  }

  public Promise<P> always(DispatchQueue on, Consumer<Object> body) {
    return new Promise<>(this, (resolution, resolve) -> {
      on.execute(() -> {
        try {
          @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
          Object value = resolution.isFulfilled() ? resolution.getFulfilled() : resolution.getRejected();
          body.consume(value);
          resolve.call(resolution);
        }
        catch(Throwable t) {
          resolve.call(Resolution.rejected(t));
        }
      });
    });
  }

  public Promise<P> always(Consumer<Object> body) {
    return always(DispatchQueues.MAIN, body);
  }

}



class Seal<P> {

  enum Discriminator {
    Pending,
    Resolved
  }



  Discriminator discriminator;
  Object value;

  public Seal(Collection<Function<P>> pending) {
    discriminator = Discriminator.Pending;
    value = pending;
  }

  public Seal(P resolved) {
    setResolved(resolved);
  }

  public boolean isPending() {
    return discriminator == Discriminator.Pending;
  }

  @SuppressWarnings("unchecked")
  public Collection<Function<P>> getPending() {
    return (Collection<Function<P>>) value;
  }

  public boolean isResolved() {
    return discriminator == Discriminator.Resolved;
  }

  @SuppressWarnings("unchecked")
  public P getResolved() {
    return (P) value;
  }

  public void setResolved(P resolved) {
    discriminator = Discriminator.Resolved;
    value = resolved;
  }

}



class Resolution<P> {

  enum Discriminator {
    Fulfilled,
    Rejected
  }



  Discriminator discriminator;
  Object value;

  static <P> Resolution<P> fulfilled(P fulfilled) {
    Resolution<P> value = new Resolution<>();
    value.setFulfilled(fulfilled);
    return value;
  }

  static <P> Resolution<P> rejected(Throwable rejected) {
    Resolution<P> value = new Resolution<>();
    value.setRejected(rejected);
    return value;
  }

  public boolean isFulfilled() {
    return discriminator == Discriminator.Fulfilled;
  }

  @SuppressWarnings("unchecked")
  public P getFulfilled() {
    return (P) value;
  }

  public void setFulfilled(P resolved) {
    discriminator = Discriminator.Fulfilled;
    value = resolved;
  }

  public boolean isRejected() {
    return discriminator == Discriminator.Rejected;
  }

  public Throwable getRejected() {
    return (Throwable) value;
  }

  public void setRejected(Throwable rejected) {
    discriminator = Discriminator.Rejected;
    value = rejected;
  }

}



interface State<P> {

  Optional<P> get();

  void get(Function<Seal<P>> handler);

}



class UnsealedState<P> implements State<P> {

  private ConcurrentDispatchQueue sync = new ConcurrentDispatchQueue(Thread.NORM_PRIORITY);
  private Seal<P> seal;

  UnsealedState(AtomicReference<Function<P>> resolverResult) {
    seal = new Seal<>(new ArrayList<>());
    resolverResult.set(resolution -> {
      final AtomicReference<Collection<Function<P>>> handlers = new AtomicReference<>();
      sync.executeBarrierSync(() -> {
        if(seal.isPending()) {
          handlers.set(seal.getPending());
          seal.setResolved(resolution);
        }
      });
      if(handlers.get() != null) {
        for(Function<P> valueHandler : handlers.get()) {
          valueHandler.call(resolution);
        }
      }
    });
  }

  @Override
  public Optional<P> get() {
    final AtomicReference<P> result = new AtomicReference<>();
    sync.executeSync(() -> {
      if(seal.isResolved()) {
        result.set(seal.getResolved());
      }
    });
    return fromNullable(result.get());
  }

  @Override
  public void get(Function<Seal<P>> body) {
    AtomicBoolean sealed = new AtomicBoolean();
    sync.executeSync(() -> sealed.set(seal.isResolved()));
    if(!sealed.get()) {
      sync.executeBarrierSync(() -> {
        if(seal.isPending()) {
          body.call(seal);
        }
        else {
          sealed.set(true);
        }
      });
    }
    if(sealed.get()) {
      body.call(seal);
    }
  }

}



class SealedState<P> implements State<P> {

  private P resolution;

  public SealedState(P resolution) {
    this.resolution = resolution;
  }

  @Override
  public Optional<P> get() {
    return Optional.of(resolution);
  }

  @Override
  public void get(Function<Seal<P>> handler) {
    handler.call(new Seal<>(resolution));
  }

}
