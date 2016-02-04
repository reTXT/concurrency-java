package io.retxt.test.dispatch;

import io.retxt.dispatch.DispatchQueues;
import io.retxt.dispatch.LooperQueue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;



/**
 * Unit tests for AndroidPlatformQueues
 * <p>
 * Created by kdubb on 2/4/16.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AndroidPlatformQueuesTest {

  @Test
  public void testQueuesLoaded() {
    assertThat(DispatchQueues.MAIN, is(instanceOf(LooperQueue.class)));
    assertThat(DispatchQueues.UI, is(instanceOf(LooperQueue.class)));
  }

}
