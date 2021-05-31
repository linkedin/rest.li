package com.linkedin.d2.balancer.strategies.degrader;

import com.linkedin.d2.balancer.event.EventEmitter;
import com.linkedin.d2.balancer.strategies.DelegatingRingFactory;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheckOperations;
import com.linkedin.util.clock.SettableClock;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.testng.annotations.Test;

import static com.linkedin.d2.balancer.util.TestHelper.assertSameElements;
import static com.linkedin.d2.balancer.util.TestHelper.concurrently;
import static com.linkedin.d2.balancer.util.TestHelper.getAll;
import static com.linkedin.d2.balancer.util.TestHelper.split;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class DegraderLoadBalancerStateTest
{
  private static final String SERVICE_NAME = "test";

  private static final List<PartitionDegraderLoadBalancerStateListener.Factory> DEGRADER_STATE_LISTENER_FACTORIES =
      Collections.emptyList();

  private static final long UPDATE_INTERVAL = 5000L;
  private static final boolean UPDATE_ONLY_AT_INTERVAL = true;
  private static final int POINT_PER_WEIGHT = 1;
  private static final String HASH_METHOD = null;
  private static final Map<String, Object> HASH_CONFIG = Collections.<String, Object>emptyMap();

  private static final double INITIAL_RECOVERY_LEVEL = 1.0D;
  private static final double RING_RAMP_FACTOR = 1.0D;
  private static final double HIGH_WATER_MARK = 1.0D;
  private static final double LOW_WATER_MARK = 1.0D;
  private static final double GLOBAL_STEP_UP = 1.0D;
  private static final double GLOBAL_STEP_DOWN = 1.0D;

  private static final long MIN_CALL_COUNT_HIGH_WATERMARK = 1L;
  private static final long MIN_CALL_COUNT_LOW_WATERMARK = 1L;
  private static final double HASH_RING_POINT_CLEAN_UP_RATE = 0.2D;
  private static final String CONSISTENT_HASH_ALGORITHM = null;
  private static final int NUM_PROBES = 21;
  private static final int POINTS_PER_HOST = 1;
  private static final double BOUNDED_LOAD_BALANCING_FACTOR = 1.25;
  private static final String PATH = null;
  private static final double QUARANTINE_MAX_PERCENT = 0.1D;
  private static final ScheduledExecutorService EXECUTOR_SERVICE = null;
  private static final HealthCheckOperations HEALTH_CHECK_OPERATIONS = null;
  private static final String HEALTH_CHECK_METHOD = null;
  private static final String HEALTH_CHECK_PATH = null;
  private static final long QUARANTINE_LATENCY = 100L;
  private static final EventEmitter EMITTER = null;
  private static final long LOW_EVENT_EMITTING_INTERVAL = 0;
  private static final long HIGH_EVENT_EMITTING_INTERVAL = 0;
  private static final String CLUSTER_NAME = "Unknown";

  /**
   * Resizing the array of partitions doesn't interfere with setting partition state.
   *
   * This test aims to reproduce a specific bug, which occurs when one thread sets a
   * partition state while another thread is in the middle of resizing the array of states.
   * To reproduce this, we inject a tricky Clock, which pauses execution of the latter
   * thread in the middle of resizing (when constructing the new partition state).
   *
   * This depends on DegraderLoadBalancerState to call the clock at least once to initialize
   * partition 1. If that changes, you'll have to change clock-related constants below.
   */
  @Test(groups = {"small", "back-end"})
  public void testConcurrentResizeAndSet()
      throws InterruptedException
  {
    final PauseClock clock = new PauseClock();
    DegraderLoadBalancerStrategyConfig config = new DegraderLoadBalancerStrategyConfig(
        UPDATE_INTERVAL,
        UPDATE_ONLY_AT_INTERVAL,
        POINT_PER_WEIGHT,
        HASH_METHOD,
        HASH_CONFIG,
        clock,
        INITIAL_RECOVERY_LEVEL,
        RING_RAMP_FACTOR,
        HIGH_WATER_MARK,
        LOW_WATER_MARK,
        GLOBAL_STEP_UP,
        GLOBAL_STEP_DOWN,
        MIN_CALL_COUNT_HIGH_WATERMARK,
        MIN_CALL_COUNT_LOW_WATERMARK,
        HASH_RING_POINT_CLEAN_UP_RATE,
        CONSISTENT_HASH_ALGORITHM,
        NUM_PROBES,
        POINTS_PER_HOST, BOUNDED_LOAD_BALANCING_FACTOR,
        PATH,
        QUARANTINE_MAX_PERCENT,
        EXECUTOR_SERVICE,
        HEALTH_CHECK_OPERATIONS,
        HEALTH_CHECK_METHOD,
        HEALTH_CHECK_PATH,
        QUARANTINE_LATENCY,
        EMITTER,
        LOW_EVENT_EMITTING_INTERVAL,
        HIGH_EVENT_EMITTING_INTERVAL,
        CLUSTER_NAME);
    final DegraderLoadBalancerState subject = new DegraderLoadBalancerStrategyV3(
        config, SERVICE_NAME, null, DEGRADER_STATE_LISTENER_FACTORIES).getState();
    Thread getPartition1 = new Thread()
    {
      @Override
      public void run()
      {
        subject.getPartitionState(1); // resize the array as a side-effect
      }
    };
    assertNotNull(subject.getPartitionState(0));
    final long clockCalled = clock._calls.get();
    assertTrue(clockCalled > 0, "clock not called"); // 1 partition initialized (so far)
    clock._paused = new CountDownLatch(1);
    clock._resume = new CountDownLatch(1);
    getPartition1.start();
    assertTrue(clock._paused.await(60, TimeUnit.SECONDS));
    // Now getPartition1 has started resizing the array.
    final PartitionDegraderLoadBalancerState newState = newPartitionState(0, 0);
    assertNotSame(subject.getPartitionState(0), newState);
    subject.setPartitionState(0, newState);
    assertSame(subject.getPartitionState(0), newState);
    clock._resume.countDown();
    getPartition1.join(60000);
    assertFalse(getPartition1.isAlive());
    // Now getPartition1 has finished resizing the array.
    assertSame(subject.getPartitionState(0), newState); // as before
    assertTrue(clock._calls.get() > clockCalled, "clock not called again"); // 2 partitions initialized
  }

  /**
   * A clock that can pause execution of calls to currentTimeMillis.
   */
  private static class PauseClock extends SettableClock
  {
    final AtomicLong _calls = new AtomicLong(0);
    CountDownLatch _paused = new CountDownLatch(0);
    CountDownLatch _resume = new CountDownLatch(0);

    @Override
    public long millis()
    {
      _calls.incrementAndGet();
      _paused.countDown();
      try
      {
        _resume.await();
      }
      catch (Exception e)
      {
        fail(e + "", e);
      }
      return super.millis();
    }
  }

  private static PartitionDegraderLoadBalancerState newPartitionState(long generationID, long lastUpdated)
  {
    return new PartitionDegraderLoadBalancerState(generationID, lastUpdated,
                                                  false, new DelegatingRingFactory<>(new DegraderLoadBalancerStrategyConfig(1L)),
                                                  Collections.<URI, Integer>emptyMap(),
                                                  PartitionDegraderLoadBalancerState.Strategy.LOAD_BALANCE,
                                                  0, 0, Collections.emptyMap(),
                                                  SERVICE_NAME, Collections.emptyMap(), 0, 0, 0,
                                                  Collections.emptyMap(), Collections.emptyMap(), null, 0);
  }

  private static List<PartitionDegraderLoadBalancerState> newPartitionStates(int numberOfPartitions)
  {
    List<PartitionDegraderLoadBalancerState> states = new ArrayList<PartitionDegraderLoadBalancerState>();
    for (int p = 0; p < numberOfPartitions; ++p)
      states.add(newPartitionState(p, p));
    return states;
  }

  /**
   * Concurrent calls to getPartitionState don't interfere with each other.
   * This test isn't repeatable, since the timing of the threads' execution is unpredictable.
   */
  @Test(groups = {"small", "back-end"})
  public void testConcurrentGets()
  {
    testConcurrentGets(8);
    testConcurrentGets(11);
  }

  private static void testConcurrentGets(int numberOfPartitions)
  {
    DegraderLoadBalancerState subject = DegraderLoadBalancerTest.getStrategy().getState();
    List<PartitionDegraderLoadBalancerState> a1 = concurrentGets(subject, numberOfPartitions);
    List<PartitionDegraderLoadBalancerState> a2 = concurrentGets(subject, (numberOfPartitions * 2) + 1);
    assertSameElements(a1, a2.subList(0, a1.size()));
  }

  /**
   * Call subject.getPartitionState(i) concurrently, for i in 0 .. numberOfPartitions-1.
   * Run several threads for each partition; make all the threads call subject concurrently.
   *
   * @return [subject.getPartitionState(0) .. subject.getPartitionState(numberOfPartitions - 1)]
   */
  private static List<PartitionDegraderLoadBalancerState> concurrentGets(DegraderLoadBalancerState subject,
                                                                         int numberOfPartitions)
  {
    int getsPerPartition = 3;
    List<Callable<PartitionDegraderLoadBalancerState>> reads
        = new ArrayList<Callable<PartitionDegraderLoadBalancerState>>();
    for (int g = 0; g < getsPerPartition; ++g)
      for (int p = 0; p < numberOfPartitions; ++p)
        reads.add(new GetPartitionState(subject, p));
    List<List<PartitionDegraderLoadBalancerState>> actual =
        split(getAll(concurrently(reads)), numberOfPartitions);
    assertEquals(actual.size(), getsPerPartition);
    List<PartitionDegraderLoadBalancerState> a0 = actual.get(0);
    assertEquals(a0.size(), numberOfPartitions);
    for (int a = 1; a < actual.size(); ++a)
      assertSameElements(actual.get(a), a0);
    return a0;
  }

  /**
   * Concurrent calls to getPartitionState and setPartitionState don't interfere with each other.
   * This test isn't repeatable, since the timing of the threads' execution is unpredictable.
   */
  @Test(groups = {"small", "back-end"})
  public void testConcurrentGetsAndSets()
  {
    testConcurrentGetsAndSets(8);
    testConcurrentGetsAndSets(11);
  }

  private static void testConcurrentGetsAndSets(int numberOfPartitions)
  {
    DegraderLoadBalancerState subject = DegraderLoadBalancerTest.getStrategy().getState();
    List<PartitionDegraderLoadBalancerState> newStates = newPartitionStates((numberOfPartitions * 2) + 1);
    List<PartitionDegraderLoadBalancerState> a1 = concurrentGetsAndSets(subject, newStates.subList(0, numberOfPartitions));
    List<PartitionDegraderLoadBalancerState> a2 = concurrentGetsAndSets(subject, newStates);
    assertSameElements(a1, a2.subList(0, a1.size()));
  }

  /**
   * Call subject.getPartitionState(i) and setPartitionState(i) concurrently, for i in 0 .. numberOfPartitions-1.
   * Run several threads for each partition; make all the threads call subject concurrently.
   *
   * @return [subject.getPartitionState(0) .. subject.getPartitionState(numberOfPartitions - 1)]
   */
  private static List<PartitionDegraderLoadBalancerState> concurrentGetsAndSets
    (DegraderLoadBalancerState subject, List<PartitionDegraderLoadBalancerState> newStates)
  {
    int numberOfPartitions = newStates.size();
    int getsPerPartition = 3;
    List<Callable<PartitionDegraderLoadBalancerState>> calls = new ArrayList<Callable<PartitionDegraderLoadBalancerState>>();
    for (int p = 0; p < numberOfPartitions; ++p)
      calls.add(new GetAndSetPartitionState(subject, p, newStates.get(p)));
    for (int g = 0; g < getsPerPartition; ++g)
      for (int p = 0; p < numberOfPartitions; ++p)
        calls.add(new GetPartitionState(subject, p));
    getAll(concurrently(calls));
    List<PartitionDegraderLoadBalancerState> actual = new ArrayList<PartitionDegraderLoadBalancerState>();
    for (int p = 0; p < numberOfPartitions; ++p)
      actual.add(subject.getPartitionState(p));
    assertSameElements(actual, newStates);
    return actual;
  }

  /**
   * Call DegraderLoadBalancerState.getPartitionState.
   */
  private static class GetPartitionState implements Callable<PartitionDegraderLoadBalancerState>
  {
    private final DegraderLoadBalancerState _state;
    private final int _partitionID;

    GetPartitionState(DegraderLoadBalancerState state, int partitionID)
    {
      _state = state;
      _partitionID = partitionID;
    }

    @Override
    public PartitionDegraderLoadBalancerState call()
        throws InterruptedException
    {
      return _state.getPartitionState(_partitionID);
    }
  }

  /**
   * Call DegraderLoadBalancerState.getPartitionState and then setPartitionState.
   */
  private static class GetAndSetPartitionState implements Callable<PartitionDegraderLoadBalancerState>
  {
    private final DegraderLoadBalancerState _state;
    private final int _partitionID;
    private final PartitionDegraderLoadBalancerState _newPartitionState;

    GetAndSetPartitionState(DegraderLoadBalancerState state,
                            int partitionID,
                            PartitionDegraderLoadBalancerState newPartitionState)
    {
      _state = state;
      _partitionID = partitionID;
      _newPartitionState = newPartitionState;
    }

    @Override
    public PartitionDegraderLoadBalancerState call()
    {
      try
      {
        return _state.getPartitionState(_partitionID);
      }
      finally
      {
        _state.setPartitionState(_partitionID, _newPartitionState);
      }
    }
  }
}
