package com.linkedin.d2.balancer.servers;

import javax.annotation.Nonnull;


/**
 * This class manages the readiness status of the server.
 * Currently, it's based on D2 announcements' statuses as a preliminary design, can be extended in future for internal
 * components' readiness status.
 * See the <a href="preliminary design doc">https://shorturl.at/hmjz8</a> for details of determining the readiness status based on
 * D2 announcements. Treating D2 announcements as the app servers’ intent to serve traffic, the readiness status is
 * determined by the combination of D2 announcements' sent status.
 */
public interface ReadinessStatusManager
{
  void registerAnnouncerStatus(AnnouncerStatus status);

  void onAnnouncerStatusUpdated();

  void addWatcher(ReadinessStatusWatcher watcher);

  interface ReadinessStatusWatcher
  {
    void onReadinessStatusChanged(ReadinessStatus newStatus);
  }

  enum ReadinessStatus
  {
    /**
     * When the D2 clusters that are intended to be announced have sent announcements successfully.
     * (And if any D2 cluster is intended to be de-announced, the corresponding de-announcement is also sent successfully)
     */
    SERVING,
    /**
     * When all D2 clusters are intended to be de-announced and the de-announcements are sent successfully, or never
     * announced before.
     * OR: if some D2 clusters have isRequiredToServe set to true, any of them is de-announced.
     */
    NOT_SERVING,
    /**
     * There is a time gap between when a serving intent changes (either static config loaded at startup, or dynamic
     * markup/markdown API is called at runtime) and when the intent is fulfilled ---- (de-)announcement sent successfully.
     * During this gap, the intent is not fulfilled, and the readiness status is INCONSISTENT. Or, the (de-)announcements
     * failed to be sent, the readiness status is also INCONSISTENT.
     * This status can be deprecated in future when the readiness status is based on internal components' readiness
     * rather than D2 announcements' statuses.
     */
    INCONSISTENT
  }

  class AnnouncerStatus
  {
    // whether this announcer is to be announced and defined in static config
    private final boolean isToBeAnnouncedFromStaticConfig;

    private AnnouncementStatus announcementStatus;

    public AnnouncerStatus(boolean isToBeAnnouncedFromStaticConfig, @Nonnull AnnouncementStatus announcementStatus)
    {
      this.isToBeAnnouncedFromStaticConfig = isToBeAnnouncedFromStaticConfig;
      this.announcementStatus = announcementStatus;
    }

    public boolean isToBeAnnouncedFromStaticConfig()
    {
      return isToBeAnnouncedFromStaticConfig;
    }

    public boolean isAnnounced()
    {
      return announcementStatus == AnnouncementStatus.ANNOUNCED;
    }

    public boolean isDeAnnounced()
    {
      return announcementStatus == AnnouncementStatus.DE_ANNOUNCED;
    }

    public boolean isAnnouncing()
    {
      return announcementStatus == AnnouncementStatus.ANNOUNCING;
    }

    public boolean isDeAnnouncing()
    {
      return announcementStatus == AnnouncementStatus.DE_ANNOUNCING;
    }

    public AnnouncementStatus getAnnouncementStatus()
    {
      return announcementStatus;
    }

    public synchronized void setAnnouncementStatus(@Nonnull AnnouncementStatus announcementStatus)
    {
      this.announcementStatus = announcementStatus;
    }

    public boolean isEqual(AnnouncerStatus other)
    {
      return other != null && this.isToBeAnnouncedFromStaticConfig == other.isToBeAnnouncedFromStaticConfig
          && this.announcementStatus == other.announcementStatus;
    }

    public String toString()
    {
      return "AnnouncerStatus{isToBeAnnouncedFromStaticConfig=" + isToBeAnnouncedFromStaticConfig
          + ", announcementStatus=" + announcementStatus + "}";
    }

    public enum AnnouncementStatus
    {
      DE_ANNOUNCED, // de-announcement sent successfully
      DE_ANNOUNCING, // a de-announcement is to be or is being sent
      ANNOUNCING, // an announcement is to be or is being sent
      ANNOUNCED // announcement sent successfully
    }
  }
}
