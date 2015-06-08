package com.linkedin.r2.transport.http.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.util.Cancellable;
import com.linkedin.r2.util.Timeout;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Ang Xu
 * @version $Revision: $
 */
public class TestChannelPoolStreamHandler
{
  @Test(dataProvider = "connectionClose")
  public void testConnectionClose(String headerName, List<String> headerValue)
  {
    EmbeddedChannel ch = getChannel();
    FakePool pool = new FakePool();
    ch.attr(ChannelPoolStreamHandler.CHANNEL_POOL_ATTR_KEY).set(pool);

    HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.ACCEPTED);
    HttpContent lastChunk = new DefaultLastHttpContent();
    response.headers().set(headerName, headerValue);
    ch.writeInbound(response);
    ch.writeInbound(lastChunk);

    Assert.assertTrue(pool.isDisposeCalled());
    Assert.assertFalse(pool.isPutCalled());
  }

  @Test(dataProvider = "connectionKeepAlive")
  public void testConnectionKeepAlive(String headerName, List<String> headerValue)
  {
    EmbeddedChannel ch = getChannel();
    FakePool pool = new FakePool();
    ch.attr(ChannelPoolStreamHandler.CHANNEL_POOL_ATTR_KEY).set(pool);

    HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.ACCEPTED);
    HttpContent lastChunk = new DefaultLastHttpContent();
    response.headers().set(headerName, headerValue);

    ch.writeInbound(response);
    ch.writeInbound(lastChunk);

    Assert.assertFalse(pool.isDisposeCalled());
    Assert.assertTrue(pool.isPutCalled());
  }

  private static EmbeddedChannel getChannel()
  {
    EmbeddedChannel ch =  new EmbeddedChannel(new RAPResponseDecoder(1000), new ChannelPoolStreamHandler());
    ch.attr(RAPResponseDecoder.TIMEOUT_ATTR_KEY).set(new Timeout<None>(Executors.newSingleThreadScheduledExecutor(), 1000, TimeUnit.MILLISECONDS, None.none()));
    return ch;
  }

  @DataProvider(name = "connectionClose")
  public Object[][] createTestData1()
  {
    return new Object[][]
    {
        {"Connection", Arrays.asList("close")},
        // The following two test cases cannot be supported because netty only checks the first header value for isKeepAlive()
//        {"connection", Arrays.asList("foo", "close", "bar")},
//        {"CONNECTION", Arrays.asList("Keep-Alive", "Close")}
    };
  }

  @DataProvider(name = "connectionKeepAlive")
  public Object[][] createTestData2()
  {
    return new Object[][]
        {
            {"Connection", Arrays.asList("Keep-Alive")},
            {"connection", Arrays.asList("keep-alive")},
            {"CONNECTION", Arrays.asList("foo", "bar")},
            {"foo", Arrays.asList("baz")}
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
