package com.linkedin.d2.balancer.servers;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.properties.PropertyKeys;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;


/**
 * Tests {@link ZooKeeperAnnouncer}.
 */
public class TestZooKeeperAnnouncer
{
  private ZooKeeperAnnouncer _announcer;

  @Mock
  private ZooKeeperServer _server;
  @Mock
  private Callback<None> _callback;

  @BeforeMethod
  public void setUp()
  {
    MockitoAnnotations.initMocks(this);

    _announcer = new ZooKeeperAnnouncer(_server);
  }

  @Test
  public void testDoNotLoadBalance()
  {
    _announcer.doNotLoadBalance(_callback, true);

    verify(_server).addUriSpecificProperty(any(), any(), any(), any(), eq(PropertyKeys.DO_NOT_LOAD_BALANCE), eq(true), any());

    _announcer.doNotLoadBalance(_callback, false);

    verify(_server).addUriSpecificProperty(any(), any(), any(), any(), eq(PropertyKeys.DO_NOT_LOAD_BALANCE), eq(false), any());
  }
}
