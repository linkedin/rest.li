package com.linkedin.d2.balancer.servers;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import org.apache.zookeeper.KeeperException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Chris Zhang
 */
public class ZooKeeperAnnouncerTest
{
  private ZooKeeperAnnouncer _zooKeeperAnnouncer;
  private ArgumentCaptor<Callback> _captor;

  @Mock
  private ZooKeeperServer _zooKeeperServer;

  @BeforeMethod
  public void setUp()
  {
    MockitoAnnotations.initMocks(this);

    _zooKeeperAnnouncer = new ZooKeeperAnnouncer(_zooKeeperServer);
    _captor = ArgumentCaptor.forClass(Callback.class);
  }

  @Test
  public void testMarkUpConnectionLoss()
  {
    _zooKeeperAnnouncer.markUp(new FutureCallback<>());
    verify(_zooKeeperServer, times(1)).markUp(any(), any(), any(), any(), _captor.capture());

    Callback callback = _captor.getValue();
    callback.onError(new KeeperException.ConnectionLossException());

    _zooKeeperAnnouncer.retry(new FutureCallback<>());
    verify(_zooKeeperServer, times(2)).markUp(any(), any(), any(), any(), any());
  }

  @Test
  public void testMarkDownConnectionLoss()
  {
    _zooKeeperAnnouncer.markDown(new FutureCallback<>());
    verify(_zooKeeperServer, times(1)).markDown(any(), any(), _captor.capture());

    Callback callback = _captor.getValue();
    callback.onError(new KeeperException.ConnectionLossException());

    _zooKeeperAnnouncer.retry(new FutureCallback<>());
    verify(_zooKeeperServer, times(2)).markDown(any(), any(), any());
  }

  @Test
  public void testMarkUpSessionExpired()
  {
    _zooKeeperAnnouncer.markUp(new FutureCallback<>());
    verify(_zooKeeperServer, times(1)).markUp(any(), any(), any(), any(), _captor.capture());

    Callback callback = _captor.getValue();
    callback.onError(new KeeperException.SessionExpiredException());

    _zooKeeperAnnouncer.retry(new FutureCallback<>());
    verify(_zooKeeperServer, times(2)).markUp(any(), any(), any(), any(), any());
  }

  @Test
  public void testMarkDownSessionExpired()
  {
    _zooKeeperAnnouncer.markDown(new FutureCallback<>());
    verify(_zooKeeperServer, times(1)).markDown(any(), any(), _captor.capture());

    Callback callback = _captor.getValue();
    callback.onError(new KeeperException.SessionExpiredException());

    _zooKeeperAnnouncer.retry(new FutureCallback<>());
    verify(_zooKeeperServer, times(2)).markDown(any(), any(), any());
  }
}
