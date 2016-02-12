import io.retxt.dispatch.DispatchQueues;
import io.retxt.dispatch.ManualDispatchQueue;
import io.retxt.promise.Promise;
import io.retxt.promise.Promises;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;



/**
 * Created by kdubb on 2/9/16.
 */
public class PromiseTest {

  static Thread testThread;
  static Thread mainThread;

  @BeforeClass
  public static void setupThreads() {

    testThread = Thread.currentThread();

    mainThread = new Thread() {

      @Override
      public void run() {
        ((ManualDispatchQueue) DispatchQueues.MAIN).drain();
      }
    };
    mainThread.start();
  }

  @Test
  public void testChaining() throws Throwable {

    List<Integer> values = new ArrayList<>();

    Promise<Integer> p = new Promise<Integer>((val, error) -> {
      DispatchQueues.HIGH.executeAfter(100, MILLISECONDS, () -> {
        values.add(1);
        val.call(2);
      });
    }).then(val -> {
      values.add(val);
    }).filter(val -> {
      values.add(val + 1);
      return Integer.toString(val + 2);
    }).pipe(value -> {
      values.add(Integer.valueOf(value));
      return Promises.delay(50L, TimeUnit.MILLISECONDS, 5);
    }).always(value -> {
      values.add((Integer) value);
    });

    assertThat(Promises.await(p, 1, SECONDS), is(5));
    assertThat(values, is(asList(1, 2, 3, 4, 5)));
  }

  @Test
  public void testThreadLocation() throws Throwable {

    List<Integer> values = new ArrayList<>();

    Promise<Integer> p = new Promise<Integer>((val, error) -> {
      assertThat(Thread.currentThread(), is(testThread));
      values.add(1);
      val.call(100);
    }).thenInBackground(value -> {
      assertThat(Thread.currentThread(), is(not(mainThread)));
      values.add(2);
    }).then(value -> {
      assertThat(Thread.currentThread(), is(mainThread));
      values.add(3);
    }).filterInBackground(value -> {
      assertThat(Thread.currentThread(), is(not(mainThread)));
      values.add(4);
      return value;
    }).filter(value -> {
      assertThat(Thread.currentThread(), is(mainThread));
      values.add(5);
      return value;
    }).pipeInBackground(value -> {
      assertThat(Thread.currentThread(), is(not(mainThread)));
      values.add(6);
      return Promises.fulfilled(value);
    }).pipe(value -> {
      assertThat(Thread.currentThread(), is(mainThread));
      values.add(7);
      return Promises.fulfilled(value);
    }).always(DispatchQueues.LOW, value -> {
      assertThat(Thread.currentThread(), is(not(mainThread)));
      values.add(8);
    }).always(value -> {
      assertThat(Thread.currentThread(), is(mainThread));
      values.add(9);
    });

    assertThat(Promises.await(p, 1, SECONDS), is(100));
    assertThat(values, is(asList(1, 2, 3, 4, 5, 6, 7, 8, 9)));
  }

  @Test
  public void testDispatch() {

    Promises.dispatch(() -> {
      Thread.sleep(10000); // Long slow calculation
      return "1";
    });
  }

}
