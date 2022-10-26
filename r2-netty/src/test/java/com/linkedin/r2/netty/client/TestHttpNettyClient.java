package com.linkedin.r2.netty.client;

import com.linkedin.common.callback.Callbacks;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.bridge.client.TransportCallbackAdapter;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.http.client.AsyncPool;
import com.linkedin.r2.transport.http.client.common.ChannelPoolManager;
import com.linkedin.r2.transport.http.common.HttpProtocolVersion;
import com.linkedin.util.clock.SystemClock;
import io.netty.channel.EventLoopGroup;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;


public class TestHttpNettyClient {
  @Test
  void testMultithreadedDnsResolution() throws URISyntaxException, InterruptedException {
    ChannelPoolManager channelPoolManager = mock(ChannelPoolManager.class);
    when(channelPoolManager.getPoolForAddress(any())).thenReturn(mock(AsyncPool.class));

    HttpNettyClient httpNettyClient =
        new HttpNettyClient(mock(EventLoopGroup.class), mock(ScheduledExecutorService.class),
            mock(ExecutorService.class), channelPoolManager, channelPoolManager,
            HttpProtocolVersion.HTTP_2, SystemClock.instance(), 1000, 1000, 1000, null);

    RestRequest request = new RestRequestBuilder(new URI("http://localhost/")).build();
    RequestContext requestContext = new RequestContext();
    TransportCallback<RestResponse> transportCallback = new TransportCallbackAdapter<>(Callbacks.empty());
    Map<String, String> wireAttrs = new HashMap<>();

    Runnable runnable = () -> {
      for (int i = 0; i < 1000000; i++) {
        httpNettyClient.restRequest(request, requestContext, wireAttrs, transportCallback);
      }
    };

    Thread threadA = new Thread(runnable);
    Thread threadB = new Thread(runnable);

    Arrays.asList(threadA, threadB).forEach(Thread::start);

    threadA.join();
    threadB.join();

    assertEquals(httpNettyClient.getDnsResolutionErrors(), 0);
  }
}