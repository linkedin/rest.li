package com.linkedin.d2.balancer.servers;

public class NoOpReadinessStatusManager implements ReadinessStatusManager
{
  @Override
  public void registerAnnouncerStatus(AnnouncerStatus status)
  {
    // no-op
  }

  @Override
  public void onAnnouncerStatusUpdated()
  {
    // no-op
  }

  @Override
  public void addWatcher(ReadinessStatusWatcher watcher)
  {
    // no-op
  }
}
