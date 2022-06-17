package com.linkedin.d2.balancer.clients;

import com.linkedin.common.callback.Callback;
import com.linkedin.d2.balancer.D2Client;
import com.linkedin.d2.balancer.LoadBalancerWithFacilities;
import com.linkedin.d2.balancer.clusterfailout.FailoutConfig;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.util.ClusterInfoProvider;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import java.net.URI;
import java.net.URISyntaxException;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;


public class FailoutClientTest {
  public static final String CLUSTER_NAME = "Foo";
  public static final String REQUEST_URI = "d2://foo";
  public static final String REDIRECTED_URI = "d2://foo-baz";
  private FailoutRedirectStrategy _redirectStrategy;
  private LoadBalancerWithFacilities _loadBalancerWithFacilities;
  private D2Client _d2Client;

  private FailoutClient _failoutClient;

  @BeforeMethod
  public void setup() throws URISyntaxException {
    _redirectStrategy = mock(FailoutRedirectStrategy.class);
    when(_redirectStrategy.redirect(any(), any())).thenReturn(new URI(REDIRECTED_URI));

    _loadBalancerWithFacilities = mock(LoadBalancerWithFacilities.class);
    doAnswer(invocation -> {
      Callback callback = (Callback) invocation.getArguments()[1];
      ServiceProperties mockProperties = mock(ServiceProperties.class);
      when(mockProperties.getClusterName()).thenReturn(CLUSTER_NAME);
      callback.onSuccess(mockProperties);
      return null;
    }).when(_loadBalancerWithFacilities).getLoadBalancedServiceProperties(anyString(), any());

    _d2Client = mock(D2Client.class);
    doAnswer(invocation -> {
      Callback callback = (Callback) invocation.getArguments()[2];
      callback.onSuccess(null);
      return null;
    }).when(_d2Client).restRequest(any(), any(), any());

    doAnswer(invocation -> {
      Callback callback = (Callback) invocation.getArguments()[2];
      callback.onSuccess(null);
      return null;
    }).when(_d2Client).streamRequest(any(), any(), any());

    _failoutClient = new FailoutClient(_d2Client, _loadBalancerWithFacilities, _redirectStrategy);
  }

  @Test
  public void testRestRequestLoadBalancerError() throws URISyntaxException {
    doAnswer(invocation -> {
      Callback callback = (Callback) invocation.getArguments()[1];
      callback.onError(new RuntimeException());
      return null;
    }).when(_loadBalancerWithFacilities).getLoadBalancedServiceProperties(anyString(), any());
    sendAndVerifyRestRequest();
  }

  @Test
  public void testRestNoFailout() throws URISyntaxException {
    setupRedirectStrategy(false);

    sendAndVerifyRestRequest();
  }

  @Test
  public void testRestWithFailout() throws URISyntaxException, InterruptedException {
    setupRedirectStrategy(true);

    sendAndVerifyRestRequest();

    ArgumentCaptor<RestRequest> requestArgumentCaptor = ArgumentCaptor.forClass(RestRequest.class);
    verify(_d2Client, times(1)).restRequest(requestArgumentCaptor.capture(), any(), any());
    assertEquals(requestArgumentCaptor.getValue().getURI().toString(), REDIRECTED_URI);
  }

  @Test
  public void testStreamRequestLoadBalancerError() throws URISyntaxException {
    doAnswer(invocation -> {
      Callback callback = (Callback) invocation.getArguments()[1];
      callback.onError(new RuntimeException());
      return null;
    }).when(_loadBalancerWithFacilities).getLoadBalancedServiceProperties(anyString(), any());
    sendAndVerifyStreamRequest();
  }

  @Test
  public void testStreamNoFailout() throws URISyntaxException {
    setupRedirectStrategy(false);

    sendAndVerifyStreamRequest();
  }

  @Test
  public void testStreamWithFailout() throws URISyntaxException, InterruptedException {
    setupRedirectStrategy(true);

    sendAndVerifyStreamRequest();

    ArgumentCaptor<StreamRequest> requestArgumentCaptor = ArgumentCaptor.forClass(StreamRequest.class);
    verify(_d2Client, times(1)).streamRequest(requestArgumentCaptor.capture(), any(), any());
    assertEquals(requestArgumentCaptor.getValue().getURI().toString(), REDIRECTED_URI);
  }

  private void sendAndVerifyRestRequest() throws URISyntaxException {
    Callback callback = mock(Callback.class);
    _failoutClient.restRequest(new RestRequestBuilder(new URI(REQUEST_URI)).build(), callback);
    verify(callback, times(1)).onSuccess(any());
  }

  private void sendAndVerifyStreamRequest() throws URISyntaxException {
    Callback callback = mock(Callback.class);
    _failoutClient.streamRequest(new StreamRequestBuilder(new URI(REQUEST_URI)).build(EntityStreams.emptyStream()),
        callback);
    verify(callback, times(1)).onSuccess(any());
  }

  private void setupRedirectStrategy(boolean isFailedout) {
    FailoutConfig mockConfig = mock(FailoutConfig.class);
    when(mockConfig.isFailedOut()).thenReturn(isFailedout);
    ClusterInfoProvider mockProvider = mock(ClusterInfoProvider.class);
    when(mockProvider.getFailoutConfig(anyString())).thenReturn(mockConfig);
    when(_loadBalancerWithFacilities.getClusterInfoProvider()).thenReturn(mockProvider);
  }
}
