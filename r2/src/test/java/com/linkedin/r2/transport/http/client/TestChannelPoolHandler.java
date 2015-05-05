package com.linkedin.r2.transport.http.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.util.Cancellable;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collection;

/**
 * @author Ang Xu
 * @version $Revision: $
 */
public class TestChannelPoolHandler
{
  @Test(dataProvider = "connectionClose")
  public void testConnectionClose(String headerName, String headerValue)
  {
    EmbeddedChannel ch = new EmbeddedChannel(new ChannelPoolHandler());
    FakePool pool = new FakePool();
    ch.attr(ChannelPoolHandler.CHANNEL_POOL_ATTR_KEY).set(pool);

    RestResponse response = new RestResponseBuilder().setHeader(headerName, headerValue).build();
    ch.writeInbound(response);

    Assert.assertTrue(pool.isDisposeCalled());
    Assert.assertFalse(pool.isPutCalled());
  }

  @Test(dataProvider = "connectionKeepAlive")
  public void testConnectionKeepAlive(String headerName, String headerValue)
  {
    EmbeddedChannel ch = new EmbeddedChannel(new ChannelPoolHandler());
    FakePool pool = new FakePool();
    ch.attr(ChannelPoolHandler.CHANNEL_POOL_ATTR_KEY).set(pool);

    RestResponse response = new RestResponseBuilder().setHeader(headerName, headerValue).build();
    ch.writeInbound(response);

    Assert.assertFalse(pool.isDisposeCalled());
    Assert.assertTrue(pool.isPutCalled());
  }

  @DataProvider(name = "connectionClose")
  public Object[][] createTestData1()
  {
    return new Object[][]
    {
        {"Connection", "close"},
        {"connection", "foo, close, bar"},
        {"CONNECTION", "Keep-Alive, Close"}
    };
  }

  @DataProvider(name = "connectionKeepAlive")
  public Object[][] createTestData2()
  {
    return new Object[][]
        {
            {"Connection", "Keep-Alive"},
            {"connection", "keep-alive"},
            {"CONNECTION", "foo, bar"},
            {"foo", "baz"}
        };
  }

  private static class FakePool implements AsyncPool<Channel>
  {
    private boolean _isPutCalled = false;
    private boolean _isDisposeCalled = false;

    public boolean isPutCalled()
    {
      return _isPutCalled;
    }

    public boolean isDisposeCalled()
    {
      return _isDisposeCalled;
    }

    @Override
    public String getName()
    {
      return null;
    }

    @Override
    public void start()
    {

    }

    @Override
    public void shutdown(Callback<None> callback)
    {

    }

    @Override
    public Collection<Callback<Channel>> cancelWaiters()
    {
      return null;
    }

    @Override
    public PoolStats getStats()
    {
      return null;
    }

    @Override
    public void dispose(Channel obj)
    {
      _isDisposeCalled = true;
    }

    @Override
    public void put(Channel obj)
    {
      _isPutCalled = true;
    }

    @Override
    public Cancellable get(Callback<Channel> callback)
    {
      return null;
    }
  }
}
