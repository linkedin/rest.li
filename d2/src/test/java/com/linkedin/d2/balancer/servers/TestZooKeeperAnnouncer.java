package com.linkedin.d2.balancer.servers;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.servers.ReadinessStatusManager.AnnouncerStatus;
import com.linkedin.d2.balancer.servers.ReadinessStatusManager.AnnouncerStatus.AnnouncementStatus;
import com.linkedin.d2.balancer.servers.ZooKeeperAnnouncer.ActionOnWeightBreach;

import com.linkedin.d2.discovery.event.LogOnlyServiceDiscoveryEventEmitter;
import com.linkedin.r2.util.NamedThreadFactory;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.d2.balancer.servers.ReadinessStatusManager.AnnouncerStatus.AnnouncementStatus.*;
import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


/**
 * Tests {@link ZooKeeperAnnouncer}.
 */
public class TestZooKeeperAnnouncer
{
  private static final Map<Integer, PartitionData> MAX_WEIGHT_BREACH_PARTITION_DATA =
      Collections.singletonMap(0, new PartitionData(1000));
  private static final Map<Integer, PartitionData> DECIMAL_PLACES_BREACH_PARTITION_DATA =
      Collections.singletonMap(0, new PartitionData(5.345));
  private static final Map<Integer, PartitionData> MAX_WEIGHT_AND_DECIMAL_PLACES_BREACH_PARTITION_DATA =
      Collections.singletonMap(0, new PartitionData(10.89));
  private static final Map<Integer, PartitionData> VALID_PARTITION_DATA =
      Collections.singletonMap(0, new PartitionData(2.3));

  private static final Exception DUMMY_EXCEPTION =  new RuntimeException("dummy error");

  @Test
  public void testSetDoNotLoadBalance()
  {
    ZooKeeperAnnouncerFixture fixture = new ZooKeeperAnnouncerFixture();
    ZooKeeperAnnouncer announcer = fixture._announcer;
    Callback<None> callback = fixture.getCallback();
    announcer.setDoNotLoadBalance(callback, true);

    verify(fixture._server).addUriSpecificProperty(any(), any(), any(), any(), eq(PropertyKeys.DO_NOT_LOAD_BALANCE), eq(true), any());

    announcer.setDoNotLoadBalance(callback, false);

    verify(fixture._server).addUriSpecificProperty(any(), any(), any(), any(), eq(PropertyKeys.DO_NOT_LOAD_BALANCE), eq(false), any());
  }

  @DataProvider(name = "validatePartitionDataDataProvider")
  public Object[][] getValidatePartitionDataDataProvider()
  {
    return new Object[][] {
        {
          // no weight rules
          null, null, MAX_WEIGHT_BREACH_PARTITION_DATA, MAX_WEIGHT_BREACH_PARTITION_DATA, null, 0, 0
        },
        {
          // negative weight throws
          null, null, Collections.singletonMap(0, new PartitionData(-1.0)), null,
            new IllegalArgumentException("Weight -1.0 in Partition 0 is negative. Please correct it."), 0, 0
        },
        {
          // valid weight
          "3.0", null, VALID_PARTITION_DATA, VALID_PARTITION_DATA, null, 0, 0
        },
        {
          // no action default to IGNORE, which won't correct the value BUT will increment the counts
          "10.0", null, MAX_WEIGHT_BREACH_PARTITION_DATA, MAX_WEIGHT_BREACH_PARTITION_DATA, null, 1, 0
        },
        {
          // warn action won't correct the value
          "10.0", ZooKeeperAnnouncer.ActionOnWeightBreach.WARN, MAX_WEIGHT_BREACH_PARTITION_DATA,
            MAX_WEIGHT_BREACH_PARTITION_DATA, null, 1, 0
        },
        {
          // max weight breach, correct the value
          "10.0", ZooKeeperAnnouncer.ActionOnWeightBreach.RECTIFY, MAX_WEIGHT_BREACH_PARTITION_DATA,
            Collections.singletonMap(0, new PartitionData(10)), null, 1, 0
        },
        {
          // decimal places breach, correct the value
          "10.0", ZooKeeperAnnouncer.ActionOnWeightBreach.RECTIFY, DECIMAL_PLACES_BREACH_PARTITION_DATA,
            Collections.singletonMap(0, new PartitionData(5.3)), null, 0, 1
        },
        {
          // max weight and decimal places breach, correct the value
          "10.0", ZooKeeperAnnouncer.ActionOnWeightBreach.RECTIFY, MAX_WEIGHT_AND_DECIMAL_PLACES_BREACH_PARTITION_DATA,
            Collections.singletonMap(0, new PartitionData(10)), null, 1, 0
        },
        {
          // throw action throws for max weight breach
          "10.0", ZooKeeperAnnouncer.ActionOnWeightBreach.THROW, MAX_WEIGHT_BREACH_PARTITION_DATA, null,
            new IllegalArgumentException("[ACTION NEEDED] Weight 1000.0 in Partition 0 is greater than the max weight "
                + "allowed: 10.0. Please correct the weight. It will be force-capped to the max weight in the future."),
            1, 0
        },
        {
          // throw action does not throw for decimal places breach
          "10.0", ZooKeeperAnnouncer.ActionOnWeightBreach.THROW, DECIMAL_PLACES_BREACH_PARTITION_DATA,
            DECIMAL_PLACES_BREACH_PARTITION_DATA, null, 0, 1
        }
    };
  }
  @Test(dataProvider = "validatePartitionDataDataProvider")
  public void testValidatePartitionData(String maxWeight, ZooKeeperAnnouncer.ActionOnWeightBreach action,
      Map<Integer, PartitionData> input, Map<Integer, PartitionData> expected, Exception expectedException,
      int expectedMaxWeightBreachedCount, int expectedWeightDecimalPlacesBreachedCount)
  {
    ZooKeeperAnnouncer announcer = new ZooKeeperAnnouncerFixture(
        maxWeight == null ? null : new BigDecimal(maxWeight), action)._announcer;

    if (expectedException != null)
    {
      try
      {
        announcer.validatePartitionData(input);
        fail("Expected exception not thrown");
      }
      catch (Exception ex)
      {
        assertTrue(ex instanceof IllegalArgumentException);
        assertEquals(expectedException.getMessage(), ex.getMessage());
      }
    }
    else
    {
      assertEquals(expected, announcer.validatePartitionData(input));
    }
    assertEquals(expectedMaxWeightBreachedCount, announcer.getMaxWeightBreachedCount());
    assertEquals(expectedWeightDecimalPlacesBreachedCount, announcer.getWeightDecimalPlacesBreachedCount());
  }

  @DataProvider(name = "testUpdateAnnouncerStatusForMarkUpDataProvider")
  public Object[][] testUpdateAnnouncerStatusForMarkUpDataProvider()
  {
    return new Object[][]
        {
            // Arguments:
            // initStatus - initial announcer's announcement status
            // isDarkWarmupEnabled - is dark warmup cluster enabled
            // isWarmUpMarkUpSuccess - is warmup cluster markup successful
            // isWarmUpMarkDownSuccess - is warmup cluster markdown successful
            // isRealMarkUpSuccess - is real cluster markup successful
            // expectedStatuses - expected announcer status sequence

            // Cases:
            // --- init status is de-announced ---
            // dark warmup enabled, all markup and markdown successful
            {DE_ANNOUNCED, true, true, true, true, Arrays.asList(ANNOUNCING, ANNOUNCED)},
            // dark warmup enabled, warmup cluster markup successful but markdown failed, real cluster markup successful
            {DE_ANNOUNCED, true, true, false, true, Arrays.asList(ANNOUNCING, ANNOUNCED)},
            // dark warmup enabled, warmup cluster markup failed, real cluster markup successful
            {DE_ANNOUNCED, true, false, true, true, Arrays.asList(ANNOUNCING, ANNOUNCED)},
            // dark warmup enabled, warmup cluster markup failed, real cluster markup failed
            {DE_ANNOUNCED, true, false, true, false, Collections.singletonList(ANNOUNCING)},
            // dark warmup disabled, real cluster markup successful
            {DE_ANNOUNCED, false, true, true, true, Arrays.asList(ANNOUNCING, ANNOUNCED)},
            // dark warmup disabled, real cluster markup failed
            {DE_ANNOUNCED, false, true, true, false, Collections.singletonList(ANNOUNCING)},

            // --- init status is announcing ---
            // dark warmup enabled, all markup and markdown successful
            {ANNOUNCING, true, true, true, true, Collections.singletonList(ANNOUNCED)},
            // dark warmup enabled, warmup cluster markup successful but markdown failed, real cluster markup successful
            {ANNOUNCING, true, true, false, true, Collections.singletonList(ANNOUNCED)},
            // dark warmup enabled, warmup cluster markup failed, real cluster markup successful
            {ANNOUNCING, true, false, true, true, Collections.singletonList(ANNOUNCED)},
            // dark warmup enabled, warmup cluster markup failed, real cluster markup failed
            {ANNOUNCING, true, false, true, false, Collections.emptyList()},
            // dark warmup disabled, real cluster markup successful
            {ANNOUNCING, false, true, true, true, Collections.singletonList(ANNOUNCED)},
            // dark warmup disabled, real cluster markup failed
            {ANNOUNCING, false, true, true, false, Collections.emptyList()},

            // --- other init statuses have the same behavior ---
            {ANNOUNCED, false, true, true, true, Arrays.asList(ANNOUNCING, ANNOUNCED)},
            {ANNOUNCED, false, true, true, false, Collections.singletonList(ANNOUNCING)},
            {DE_ANNOUNCING, false, true, true, true, Arrays.asList(ANNOUNCING, ANNOUNCED)},
            {DE_ANNOUNCING, false, true, true, false, Collections.singletonList(ANNOUNCING)}
        };
  }
  @Test(dataProvider = "testUpdateAnnouncerStatusForMarkUpDataProvider")
  public void testUpdateAnnouncerStatusForMarkUp(AnnouncementStatus initStatus, boolean isDarkWarmupEnabled,
      boolean isWarmUpMarkUpSuccess, boolean isWarmUpMarkDownSuccess, boolean isRealMarkUpSuccess,
      List<AnnouncementStatus> expectedStatuses)
  {
    ZooKeeperAnnouncerFixture fixture = new ZooKeeperAnnouncerFixture(isDarkWarmupEnabled, isWarmUpMarkUpSuccess,
        isWarmUpMarkDownSuccess, isRealMarkUpSuccess, initStatus);

    fixture._announcer.markUp(fixture.getCallback());

    fixture.waitForCallback(true);
    fixture.verifyAnnouncementStatusUpdates(expectedStatuses);
    fixture.shutdown();
  }

  @DataProvider(name = "testUpdateAnnouncerStatusForMarkDownDataProvider")
  public Object[][] testUpdateAnnouncerStatusForMarkDownDataProvider()
  {
    return new Object[][]{
        // Arguments:
        // initStatus - initial announcer's announcement status
        // isRealMarkDownSuccess - is real cluster markdown successful
        // expectedStatuses - expected announcer status sequence

        // Cases:
        // --- init status is announced ---
        // real cluster markdown successful
        {ANNOUNCED, true, Arrays.asList(DE_ANNOUNCING, DE_ANNOUNCED)},
        // real cluster markdown failed
        {ANNOUNCED, false, Collections.singletonList(DE_ANNOUNCING)},

        // --- init status is de-announcing ---
        // real cluster markdown successful
        {DE_ANNOUNCING, true, Collections.singletonList(DE_ANNOUNCED)},
        // real cluster markdown failed
        {DE_ANNOUNCING, false, Collections.emptyList()},

        // --- other init statuses have the same behavior ---
        {DE_ANNOUNCED, true, Arrays.asList(DE_ANNOUNCING, DE_ANNOUNCED)},
        {DE_ANNOUNCED, false, Collections.singletonList(DE_ANNOUNCING)},
        {ANNOUNCING, true, Arrays.asList(DE_ANNOUNCING, DE_ANNOUNCED)},
        {ANNOUNCING, false, Collections.singletonList(DE_ANNOUNCING)}
    };
  }
  @Test(dataProvider = "testUpdateAnnouncerStatusForMarkDownDataProvider")
  public void testUpdateAnnouncerStatusForMarkDown(AnnouncementStatus initStatus, boolean isRealClusterMarkdownSuccess,
      List<AnnouncementStatus> expectedStatuses)
  {
    ZooKeeperAnnouncerFixture fixture = new ZooKeeperAnnouncerFixture(isRealClusterMarkdownSuccess, initStatus);

    fixture._announcer.markDown(fixture.getCallback());

    fixture.waitForCallback(false);
    fixture.verifyAnnouncementStatusUpdates(expectedStatuses);
    fixture.shutdown();
  }

  static class ZooKeeperAnnouncerFixture
  {
    @Mock
    private ZooKeeperServer _server;
    @Mock
    private ReadinessStatusManager _readinessStatusManager;

    private ZooKeeperAnnouncer _announcer;
    private final ScheduledExecutorService _executorService;
    private final AnnouncerStatus _status;
    private final boolean _isDarkWarmupEnabled;
    private final boolean _isDarkWarmupMarkupSuccess;
    private ArgumentCaptor<AnnouncementStatus> _announcementStatusCaptor =
        ArgumentCaptor.forClass(AnnouncementStatus.class);
    private CountDownLatch _callbackLatch = new CountDownLatch(1);

    private static final String REAL_CLUSTER = "RealCluster";
    private static final String DARK_CLUSTER = "DarkCluster";
    private static final int WARMUP_DURATION = 1; // in seconds
    private static final int CALLBACK_TIMEOUT_MS = 200;

    public ZooKeeperAnnouncerFixture()
    {
      this(false, false, false, true, true,
          null, ActionOnWeightBreach.IGNORE, DE_ANNOUNCED);
    }

    public ZooKeeperAnnouncerFixture(BigDecimal maxWeight, ActionOnWeightBreach actionOnWeightBreach)
    {
      this(false, false, false, true, true,
          maxWeight, actionOnWeightBreach, DE_ANNOUNCED);
    }

    public ZooKeeperAnnouncerFixture(boolean isDarkWarmupEnabled, boolean isDarkWarmupMarkupSuccess, boolean isDarkWarmupMarkdownSuccess,
    boolean isRealClusterMarkupSuccess, AnnouncementStatus initStatus)
    {
      this(isDarkWarmupEnabled, isDarkWarmupMarkupSuccess, isDarkWarmupMarkdownSuccess, isRealClusterMarkupSuccess,
          true, null, ActionOnWeightBreach.IGNORE, initStatus);
    }

    public ZooKeeperAnnouncerFixture(boolean isRealClusterMarkdownSuccess, AnnouncementStatus initStatus)
    {
      this(false, false, false, true, isRealClusterMarkdownSuccess,
          null, ActionOnWeightBreach.IGNORE, initStatus);
    }

    public ZooKeeperAnnouncerFixture(boolean isDarkWarmupEnabled, boolean isDarkWarmupMarkupSuccess, boolean isDarkWarmupMarkdownSuccess,
        boolean isRealClusterMarkupSuccess, boolean isRealClusterMarkdownSuccess, BigDecimal maxWeight,
        ActionOnWeightBreach actionOnWeightBreach, AnnouncementStatus initStatus)
    {
      MockitoAnnotations.initMocks(this);
      doNothing().when(_readinessStatusManager).registerAnnouncerStatus(any());
      doNothing().when(_readinessStatusManager).onAnnouncerStatusUpdated();

      _executorService = isDarkWarmupEnabled ? Executors.newSingleThreadScheduledExecutor(
          new NamedThreadFactory("ZooKeeperAnnouncerFixtureExecutor")) : null;
      _status = spy(new AnnouncerStatus(true, initStatus));
      doCallRealMethod().when(_status).setAnnouncementStatus(_announcementStatusCaptor.capture());
      _isDarkWarmupEnabled = isDarkWarmupEnabled;
      _isDarkWarmupMarkupSuccess = isDarkWarmupMarkupSuccess;

      _announcer = new ZooKeeperAnnouncer(_server, true, isDarkWarmupEnabled, DARK_CLUSTER, WARMUP_DURATION,
          _executorService, new LogOnlyServiceDiscoveryEventEmitter(), maxWeight, actionOnWeightBreach, _status,
          _readinessStatusManager);
      _announcer.setCluster(REAL_CLUSTER);
      _announcer.setPartitionData(VALID_PARTITION_DATA);
      _announcer.setUriSpecificProperties(Collections.emptyMap());

      if (isDarkWarmupEnabled)
      {
        mockInvokeClusterCallback(DARK_CLUSTER, true, isDarkWarmupMarkupSuccess);
        mockInvokeClusterCallback(DARK_CLUSTER, false, isDarkWarmupMarkdownSuccess);
      }
      mockInvokeClusterCallback(REAL_CLUSTER, true, isRealClusterMarkupSuccess);
      mockInvokeClusterCallback(REAL_CLUSTER, false, isRealClusterMarkdownSuccess);
    }

    private void mockInvokeClusterCallback(String cluster, boolean isMarkUp, boolean isSuccess)
    {
      if (isMarkUp)
      {
        doAnswer(invoc -> invokeCallbackHelper(invoc, 4, isSuccess))
            .when(_server).markUp(eq(cluster), any(), any(), any(), any());
      }
      else
      {
        doAnswer(invoc -> invokeCallbackHelper(invoc, 2, isSuccess))
            .when(_server).markDown(eq(cluster), any(), any());
      }
    }

    public void shutdown() {
      if (_executorService != null) {
        _executorService.shutdownNow();
      }
    }

    public Callback<None> getCallback()
    {
      return new Callback<None>()
      {
        @Override
        public void onError(Throwable e)
        {
          _callbackLatch.countDown();
        }

        @Override
        public void onSuccess(None result)
        {
          _callbackLatch.countDown();
        }
      };
    }

    public void waitForCallback(boolean isMarkup)
    {
      try
      {
        // for markup, if dark warmup is enabled and succeeded, wait for the warmup duration + callback timeout,
        // otherwise just wait for the callback timeout
        if(!_callbackLatch.await(isMarkup && _isDarkWarmupEnabled && _isDarkWarmupMarkupSuccess
            ? WARMUP_DURATION * 1000 + CALLBACK_TIMEOUT_MS : CALLBACK_TIMEOUT_MS, MILLISECONDS))
        {
          fail("Timed out waiting for callback");
        }
      }
      catch (InterruptedException e)
      {
        fail("Test interrupted while waiting for markup callback");
        Thread.currentThread().interrupt();
      }
      finally {
        _callbackLatch = new CountDownLatch(1); // reset for next usage
      }
    }

    public void verifyAnnouncementStatusUpdates(List<AnnouncementStatus> expectedStatuses)
    {
      assertEquals(expectedStatuses, _announcementStatusCaptor.getAllValues());
      verify(_readinessStatusManager, times(_announcementStatusCaptor.getAllValues().size()))
          .onAnnouncerStatusUpdated();
      reset(_status); // reset the spy to clear the invocation history
    }

    @SuppressWarnings("unchecked")
    private static Object invokeCallbackHelper(InvocationOnMock invoc, int callbackIdx, boolean isSuccess)
    {
      Callback<None> cb = (Callback<None>) invoc.getArguments()[callbackIdx];
      if (isSuccess)
      {
        cb.onSuccess(None.none());
      }
      else
      {
        cb.onError(DUMMY_EXCEPTION);
      }
      return null;
    }
  }
}
