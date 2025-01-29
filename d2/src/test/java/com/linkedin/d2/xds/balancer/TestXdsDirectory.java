package com.linkedin.d2.xds.balancer;

import com.google.common.collect.ImmutableMap;
import com.linkedin.common.callback.Callback;
import com.linkedin.d2.xds.XdsClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.Test;

import static com.linkedin.d2.xds.TestXdsClientImpl.*;
import static org.mockito.Mockito.*;


public class TestXdsDirectory
{
  /**
   * Simulate getting cluster and service names with multiple threads. Threads should be blocked until
   * onAllResourcesProcessed is called. They should be re-blocked if new update comes in, and unblocked again when
   * onAllResourcesProcessed is called. New threads coming in while the data is not being updated should get the data
   * immediately.
   */
  @Test(timeOut = 3000)
  public void testGetClusterAndServiceNames() throws InterruptedException {
    int numCallers = 20;
    int halfCallers = numCallers / 2;
    XdsDirectoryFixture fixture = new XdsDirectoryFixture();
    XdsDirectory directory = fixture._xdsDirectory;
    Assert.assertNull(directory._watcher.get());
    directory.start();
    ExecutorService executor = Executors.newFixedThreadPool(numCallers);
    runCallers(fixture, executor, halfCallers);
    XdsClient.WildcardD2ClusterOrServiceNameResourceWatcher watcher = Objects.requireNonNull(directory._watcher.get());

    // verified names are not updated, results are empty, which means all threads are waiting.
    Assert.assertTrue(directory._isUpdating.get());
    Assert.assertTrue(fixture._results.isEmpty());
    Assert.assertTrue(directory._serviceNames.isEmpty());
    Assert.assertTrue(directory._clusterNames.isEmpty());

    // update cluster and service names and mimic adding callers in the middle of updating
    watcher.onChanged(SERVICE_RESOURCE_NAME, SERVICE_NAME_DATA_UPDATE);
    watcher.onChanged(SERVICE_RESOURCE_NAME_2, SERVICE_NAME_DATA_UPDATE_2);
    runCallers(fixture, executor, halfCallers);
    watcher.onChanged(CLUSTER_RESOURCE_NAME, CLUSTER_NAME_DATA_UPDATE);
    watcher.onRemoval(SERVICE_RESOURCE_NAME_2);

    // verify service names and cluster names are updated, but updating flag is true, and threads are still waiting
    Assert.assertEquals(directory._clusterNames, Collections.singletonMap(CLUSTER_RESOURCE_NAME, CLUSTER_NAME));
    Assert.assertEquals(directory._serviceNames, Collections.singletonMap(SERVICE_RESOURCE_NAME, SERVICE_NAME));
    Assert.assertTrue(directory._isUpdating.get());
    Assert.assertTrue(fixture._results.isEmpty());

    // finish updating and unblock all callers
    watcher.onAllResourcesProcessed();
    executor.shutdown();
    Assert.assertTrue(executor.awaitTermination(1000, java.util.concurrent.TimeUnit.MILLISECONDS));

    // verify updating flag is false, and all threads are unblocked and added their results
    Assert.assertFalse(directory._isUpdating.get());
    Assert.assertEquals(fixture._results.size(), numCallers);
    long clusterMatchCount = fixture._results.stream()
        .filter(result -> Objects.equals(result, Collections.singletonList(CLUSTER_NAME))).count();
    Assert.assertEquals(clusterMatchCount, halfCallers);
    long serviceMatchCount = fixture._results.stream()
        .filter(result -> Objects.equals(result, Collections.singletonList(SERVICE_NAME))).count();
    Assert.assertEquals(serviceMatchCount, halfCallers);

    // new caller coming in while the data is not being updated should get the data immediately
    fixture.createCaller(true).run();
    Assert.assertEquals(fixture._results.size(), numCallers + 1);

    // adding new resource will trigger updating again, caller threads should be re-blocked, and new data shouldn't be
    // added to the results
    watcher.onChanged(SERVICE_RESOURCE_NAME_2, SERVICE_NAME_DATA_UPDATE_2);
    executor = Executors.newFixedThreadPool(1);
    runCallers(fixture, executor, 1);
    Assert.assertTrue(directory._isUpdating.get());
    Assert.assertEquals(directory._serviceNames,
        ImmutableMap.of(SERVICE_RESOURCE_NAME, SERVICE_NAME, SERVICE_RESOURCE_NAME_2, SERVICE_NAME_2));
    Assert.assertTrue(fixture._results.stream().noneMatch(result ->
        matchSortedLists(result, Arrays.asList(SERVICE_NAME, SERVICE_NAME_2))));

    // finish updating again, new data should be added to the results
    watcher.onAllResourcesProcessed();
    Assert.assertFalse(directory._isUpdating.get());
    executor.shutdown();
    Assert.assertTrue(executor.awaitTermination(1000, java.util.concurrent.TimeUnit.MILLISECONDS));
    Assert.assertEquals(1, fixture._results.stream()
        .filter(result -> matchSortedLists(result, Arrays.asList(SERVICE_NAME, SERVICE_NAME_2))).count());
  }

  private void runCallers(XdsDirectoryFixture fixture, ExecutorService executor, int num)
  {
    for (int i = 0; i < num; i++)
    {
      executor.execute(fixture.createCaller(i % 2 == 0));
    }
  }

  private boolean matchSortedLists(List<String> one, List<String> other)
  {
    if (one.size() != other.size())
    {
      return false;
    }
    return Objects.equals(one.stream().sorted().collect(Collectors.toList()),
        other.stream().sorted().collect(Collectors.toList()));
  }

  private static final class XdsDirectoryFixture
  {
    XdsDirectory _xdsDirectory;
    @Mock
    XdsClient _xdsClient;
    List<List<String>> _results = new ArrayList<>();;

    public XdsDirectoryFixture()
    {
      MockitoAnnotations.initMocks(this);
      doNothing().when(_xdsClient).watchAllXdsResources(any());
      _xdsDirectory = new XdsDirectory(_xdsClient);
    }

    CallerThread createCaller(boolean isForServiceNames)
    {
      return new CallerThread(isForServiceNames);
    }

    final class CallerThread implements Runnable
    {
      private final Callback<List<String>> _callback;
      private final boolean _isForServiceNames;

      public CallerThread(boolean isForServiceNames)
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
            _results.add(result);
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
