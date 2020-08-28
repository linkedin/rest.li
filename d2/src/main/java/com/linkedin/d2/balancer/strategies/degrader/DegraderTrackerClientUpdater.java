package com.linkedin.d2.balancer.strategies.degrader;

import com.linkedin.d2.balancer.clients.DegraderTrackerClient;
import com.linkedin.util.degrader.DegraderControl;

/**
 * This is a helper class to record the changes to tracker client during the update of partition state
 * without actually mutating the tracker client. The changes are applied to the tracker client only
 * when update is explicitly called. This allows us to eliminate the side-effects during the update of
 * the partition state.
 *
 * Note that because the recorded changes are not flushed to tracker client until update() is called,
 * TrackerClientUpdater.getMaxDropRate() may be different from tracker client's degraderControl.getMaxDropRate().
 * Hence TrackerClientUpdater.getMaxDropRate() should be used during the state update as some calculation
 * depends on the new maxDropRate that has not yet written into tracker client.
 *
 * For overrideDropRate and overrideMinCallCount, the new values are not used in the state update.
 */
public class DegraderTrackerClientUpdater
{
  private final DegraderTrackerClient _trackerClient;
  private final int _partitionId;
  private double _overrideDropRate;
  private double _maxDropRate;
  private int _overrideMinCallCount;

  DegraderTrackerClientUpdater(DegraderTrackerClient trackerClient, int partitionId)
  {
    _trackerClient = trackerClient;
    _partitionId = partitionId;
    DegraderControl degraderControl = _trackerClient.getDegraderControl(_partitionId);
    _overrideDropRate = degraderControl.getOverrideDropRate();
    _overrideMinCallCount = degraderControl.getOverrideMinCallCount();
    _maxDropRate = degraderControl.getMaxDropRate();
  }

  public DegraderTrackerClient getTrackerClient()
  {
    return _trackerClient;
  }

  // should be used if the new max drop rate needs to be read
  double getMaxDropRate()
  {
    return _maxDropRate;
  }

  void setOverrideDropRate(double overrideDropRate)
  {
    _overrideDropRate = overrideDropRate;
  }

  void setMaxDropRate(double maxDropRate)
  {
    _maxDropRate = maxDropRate;
  }

  void setOverrideMinCallCount(int overrideMinCallCount)
  {
    _overrideMinCallCount = overrideMinCallCount;
  }

  void update()
  {
    DegraderControl degraderControl = _trackerClient.getDegraderControl(_partitionId);
    degraderControl.setOverrideDropRate(_overrideDropRate);
    degraderControl.setMaxDropRate(_maxDropRate);
    degraderControl.setOverrideMinCallCount(_overrideMinCallCount);
  }

  @Override
  public String toString()
  {
    return _trackerClient.toString();
  }
}
