package com.linkedin.d2.xds.balancer;

import com.google.common.collect.ImmutableMap;
import com.linkedin.common.callback.Callback;
import com.linkedin.d2.xds.XdsClient;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.Test;

import static com.linkedin.d2.xds.TestXdsClientImpl.*;
import static java.lang.Thread.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


public class TestXdsDirectory
{
  /**
   * Simulate getting cluster and service names with multiple caller threads. Caller threads should be blocked until
   * onAllResourcesProcessed is called by a different thread. Caller threads should be re-blocked if new update comes
   * in, and unblocked again when onAllResourcesProcessed is called again.
   * New caller threads coming in while the data is not being updated should get the data immediately.
   */
  @Test(timeOut = 3000, invocationCount = 10)
  public void testGetClusterAndServiceNames() throws InterruptedException {
    int numCallers = 20;
    int halfCallers = numCallers / 2;
    XdsDirectoryFixture fixture = new XdsDirectoryFixture();
    XdsDirectory directory = fixture._xdsDirectory;
    Assert.assertNull(directory._watcher.get());
    directory.start();
    List<String> expectedClusterNames = Collections.singletonList(CLUSTER_NAME);
    List<String> expectedServiceNames = Collections.singletonList(SERVICE_NAME);
    fixture.runCallers(halfCallers, expectedClusterNames, expectedServiceNames);
    sleep(50); // sleep just a bit so the caller threads can add watcher
    XdsClient.WildcardD2ClusterOrServiceNameResourceWatcher watcher = Objects.requireNonNull(directory._watcher.get());

    // verified names are not updated, results are empty, which means all threads are waiting.
    Assert.assertTrue(directory._isUpdating.get());
    Assert.assertTrue(directory._serviceNames.isEmpty());
    Assert.assertTrue(directory._clusterNames.isEmpty());

    // update cluster and service names and mimic adding callers in the middle of updating
    watcher.onChanged(SERVICE_RESOURCE_NAME, SERVICE_NAME_DATA_UPDATE);
    watcher.onChanged(SERVICE_RESOURCE_NAME_2, SERVICE_NAME_DATA_UPDATE_2);
    fixture.runCallers(halfCallers, expectedClusterNames, expectedServiceNames);
    watcher.onChanged(CLUSTER_RESOURCE_NAME, CLUSTER_NAME_DATA_UPDATE);
    watcher.onRemoval(SERVICE_RESOURCE_NAME_2);

    // verify service names and cluster names are updated, but updating flag is true, and all threads are still waiting
    Assert.assertEquals(directory._clusterNames, Collections.singletonMap(CLUSTER_RESOURCE_NAME, CLUSTER_NAME));
    Assert.assertEquals(directory._serviceNames, Collections.singletonMap(SERVICE_RESOURCE_NAME, SERVICE_NAME));
    Assert.assertTrue(directory._isUpdating.get());
    Assert.assertEquals(fixture._latch.getCount(), numCallers);

    // finish updating by another thread to verify the lock can be released by a different thread. All callers should
    // be unblocked and the isUpdating flag is false.
    fixture.notifyComplete();
    Assert.assertFalse(directory._isUpdating.get());
    fixture.waitCallers();

    // new caller coming in while the data is not being updated should get the data immediately
    fixture.runCallers(1, null, expectedServiceNames);
    fixture.waitCallers();

    // adding new resource will trigger updating again, caller threads should be re-blocked, and new data shouldn't be
    // added to the results
    watcher.onChanged(SERVICE_RESOURCE_NAME_2, SERVICE_NAME_DATA_UPDATE_2);
    fixture.runCallers(1, null, Arrays.asList(SERVICE_NAME, SERVICE_NAME_2));
    Assert.assertTrue(directory._isUpdating.get());
    Assert.assertEquals(directory._serviceNames,
        ImmutableMap.of(SERVICE_RESOURCE_NAME, SERVICE_NAME, SERVICE_RESOURCE_NAME_2, SERVICE_NAME_2));
    Assert.assertEquals(fixture._latch.getCount(), 1);

    // finish updating again, new data should be added to the results
    fixture.notifyComplete();
    Assert.assertFalse(directory._isUpdating.get());
    fixture.waitCallers();
  }

  private static final class XdsDirectoryFixture
  {
    XdsDirectory _xdsDirectory;
    @Mock
    XdsClient _xdsClient;
    CountDownLatch _latch;
    ExecutorService _executor;

    public XdsDirectoryFixture()
    {
      MockitoAnnotations.initMocks(this);
      doNothing().when(_xdsClient).watchAllXdsResources(any());
      _xdsDirectory = new XdsDirectory(_xdsClient);
    }

    void runCallers(int num, List<String> expectedClusterResult, List<String> expectedServiceResult)
    {
      if (_executor == null || _executor.isShutdown() || _executor.isTerminated())
      {
        _executor = Executors.newFixedThreadPool(num);
        _latch = new CountDownLatch(num);
      }
      else
      {
        _latch = new CountDownLatch((int) (_latch.getCount() + num));
      }

      for (int i = 0; i < num; i++)
      {
        boolean isForServiceName = i % 2 == 0;
        _executor.execute(createCaller(isForServiceName,
            isForServiceName ? expectedServiceResult : expectedClusterResult));
      }
    }

    void waitCallers() throws InterruptedException {
      _executor.shutdown();
      if (!_latch.await(1000, java.util.concurrent.TimeUnit.MILLISECONDS))
      {
        Assert.fail("Timeout waiting for all callers to finish");
      }
    }

    CallerThread createCaller(boolean isForServiceNames, List<String> expectedResult)
    {
      return new CallerThread(isForServiceNames, expectedResult);
    }

    void notifyComplete()
    {
      Thread t = new Thread(() -> _xdsDirectory._watcher.get().onAllResourcesProcessed());

      t.start();

      try
      {
        t.join();
      }
      catch (InterruptedException e) {
        fail("Interrupted while waiting for onAllResourcesProcessed to be called");
      }
    }

    static boolean matchSortedLists(List<String> one, List<String> other)
    {
      if (one.size() != other.size())
      {
        return false;
      }
      return Objects.equals(one.stream().sorted().collect(Collectors.toList()),
          other.stream().sorted().collect(Collectors.toList()));
    }

    final class CallerThread implements Runnable
    {
      private final Callback<List<String>> _callback;
      private final boolean _isForServiceNames;

      public CallerThread(boolean isForServiceNames, List<String> expectedResult)
      {
        _callback = new Callback<List<String>>()
        {
          @Override
          public void onError(Throwable e)
          {
            Assert.fail("Unexpected error: " + e);
          }

          @Override
          public void onSuccess(List<String> result)
          {
            assertTrue(matchSortedLists(result, expectedResult));
            _latch.countDown();
          }
        };
        _isForServiceNames = isForServiceNames;
      }

      @Override
      public void run()
      {
        if (_isForServiceNames)
        {
          _xdsDirectory.getServiceNames(_callback);
        }
        else
        {
          _xdsDirectory.getClusterNames(_callback);
        }
      }
    }
  }
}
