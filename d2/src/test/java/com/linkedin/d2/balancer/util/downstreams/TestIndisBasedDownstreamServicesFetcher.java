package com.linkedin.d2.balancer.util.downstreams;

import com.linkedin.common.callback.SuccessCallback;
import com.linkedin.d2.xds.XdsClient;
import indis.XdsD2;
import io.grpc.Status;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.linkedin.d2.xds.TestXdsClientImpl.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link IndisBasedDownstreamServicesFetcher}.
 */
public class TestIndisBasedDownstreamServicesFetcher
{
  private static final String APP_NAME = "TestApp";
  private static final String APP_INSTANCE = "prod-ltx1";
  private static final String CLIENT_SCOPE = "global";
  private static final String RESOURCE_NAME = APP_NAME + ":" + APP_INSTANCE + ":" + CLIENT_SCOPE;
  private static final Duration TIMEOUT = Duration.ofMillis(100);
  private static final List<String> SERVICE_LIST = Arrays.asList("ServiceA", "ServiceB", "ServiceC");

  @Mock
  private XdsClient _xdsClient;
  @Mock
  private DownstreamServicesFetcher _delegate;

  private ScheduledExecutorService _executorService;
  private IndisBasedDownstreamServicesFetcher _fetcher;

  @BeforeMethod
  public void setUp()
  {
    MockitoAnnotations.initMocks(this);
    _executorService = Executors.newScheduledThreadPool(1);
    _fetcher = new IndisBasedDownstreamServicesFetcher(_xdsClient, TIMEOUT, _executorService, _delegate);
  }

  @AfterMethod
  public void tearDown()
  {
    _executorService.shutdownNow();
  }

  @Test
  public void testSuccessfulFetchFromIndis()
  {
    // Setup
    XdsD2.D2CalleeServices calleeServices = XdsD2.D2CalleeServices.newBuilder()
        .addAllServices(SERVICE_LIST)
        .build();
    XdsClient.D2CalleesUpdate update = createCalleesUpdate(calleeServices);

    ArgumentCaptor<XdsClient.D2CalleesResourceWatcher> watcherCaptor =
        ArgumentCaptor.forClass(XdsClient.D2CalleesResourceWatcher.class);

    AtomicReference<List<String>> result = new AtomicReference<>();

    // Execute
    _fetcher.getServiceNames(APP_NAME, APP_INSTANCE, CLIENT_SCOPE, result::set);

    verify(_xdsClient).watchXdsResource(eq(RESOURCE_NAME), watcherCaptor.capture());

    // Simulate successful response from INDIS
    watcherCaptor.getValue().onChanged(update);

    // Verify
    Assert.assertEquals(result.get(), SERVICE_LIST);
    verify(_delegate, never()).getServiceNames(anyString(), anyString(), anyString(), any());
  }

  @Test
  public void testTimeoutFallbackToDelegate() throws InterruptedException
  {
    // Setup
    ArgumentCaptor<XdsClient.D2CalleesResourceWatcher> watcherCaptor =
        ArgumentCaptor.forClass(XdsClient.D2CalleesResourceWatcher.class);

    AtomicReference<List<String>> result = new AtomicReference<>();

    doAnswer(invocation -> {
      SuccessCallback<List<String>> callback = (SuccessCallback<List<String>>) invocation.getArguments()[3];
      callback.onSuccess(SERVICE_LIST);
      return null;
    }).when(_delegate).getServiceNames(eq(APP_NAME), eq(APP_INSTANCE), eq(CLIENT_SCOPE), any());

    CountDownLatch latch = new CountDownLatch(1);

    // Execute
    _fetcher.getServiceNames(APP_NAME, APP_INSTANCE, CLIENT_SCOPE, services -> {
      result.set(services);
      latch.countDown();
    });

    verify(_xdsClient).watchXdsResource(eq(RESOURCE_NAME), watcherCaptor.capture());

    // Wait for timeout to trigger
    Assert.assertTrue(latch.await(200, TimeUnit.MILLISECONDS));

    // Verify delegate was called
    verify(_delegate).getServiceNames(eq(APP_NAME), eq(APP_INSTANCE), eq(CLIENT_SCOPE), any());
    Assert.assertEquals(result.get(), SERVICE_LIST);
  }

  @Test
  public void testErrorFallbackToDelegate()
  {
    // Setup
    ArgumentCaptor<XdsClient.D2CalleesResourceWatcher> watcherCaptor =
        ArgumentCaptor.forClass(XdsClient.D2CalleesResourceWatcher.class);

    AtomicReference<List<String>> result = new AtomicReference<>();

    doAnswer(invocation -> {
      SuccessCallback<List<String>> callback = (SuccessCallback<List<String>>) invocation.getArguments()[3];
      callback.onSuccess(SERVICE_LIST);
      return null;
    }).when(_delegate).getServiceNames(eq(APP_NAME), eq(APP_INSTANCE), eq(CLIENT_SCOPE), any());

    // Execute
    _fetcher.getServiceNames(APP_NAME, APP_INSTANCE, CLIENT_SCOPE, result::set);

    verify(_xdsClient).watchXdsResource(eq(RESOURCE_NAME), watcherCaptor.capture());

    // Simulate error from INDIS
    watcherCaptor.getValue().onError(Status.UNAVAILABLE.withDescription("Connection lost"));

    // Verify
    verify(_delegate).getServiceNames(eq(APP_NAME), eq(APP_INSTANCE), eq(CLIENT_SCOPE), any());
    Assert.assertEquals(result.get(), SERVICE_LIST);
  }

  @Test
  public void testInvalidUpdateFallbackToDelegate()
  {
    // Setup - create an invalid update (null calleeServices)
    XdsClient.D2CalleesUpdate invalidUpdate = createCalleesUpdate(null);

    ArgumentCaptor<XdsClient.D2CalleesResourceWatcher> watcherCaptor =
        ArgumentCaptor.forClass(XdsClient.D2CalleesResourceWatcher.class);

    AtomicReference<List<String>> result = new AtomicReference<>();

    doAnswer(invocation -> {
      SuccessCallback<List<String>> callback = (SuccessCallback<List<String>>) invocation.getArguments()[3];
      callback.onSuccess(SERVICE_LIST);
      return null;
    }).when(_delegate).getServiceNames(eq(APP_NAME), eq(APP_INSTANCE), eq(CLIENT_SCOPE), any());

    // Execute
    _fetcher.getServiceNames(APP_NAME, APP_INSTANCE, CLIENT_SCOPE, result::set);

    verify(_xdsClient).watchXdsResource(eq(RESOURCE_NAME), watcherCaptor.capture());

    // Simulate invalid update from INDIS
    watcherCaptor.getValue().onChanged(invalidUpdate);

    // Verify
    verify(_delegate).getServiceNames(eq(APP_NAME), eq(APP_INSTANCE), eq(CLIENT_SCOPE), any());
    Assert.assertEquals(result.get(), SERVICE_LIST);
  }

  @Test
  public void testEmptyServiceListFromIndis()
  {
    // Setup
    XdsD2.D2CalleeServices emptyCalleeServices = XdsD2.D2CalleeServices.newBuilder().build();
    XdsClient.D2CalleesUpdate update = createCalleesUpdate(emptyCalleeServices);

    ArgumentCaptor<XdsClient.D2CalleesResourceWatcher> watcherCaptor =
        ArgumentCaptor.forClass(XdsClient.D2CalleesResourceWatcher.class);

    AtomicReference<List<String>> result = new AtomicReference<>();

    // Execute
    _fetcher.getServiceNames(APP_NAME, APP_INSTANCE, CLIENT_SCOPE, result::set);

    verify(_xdsClient).watchXdsResource(eq(RESOURCE_NAME), watcherCaptor.capture());

    // Simulate empty response from INDIS
    watcherCaptor.getValue().onChanged(update);

    // Verify
    Assert.assertEquals(result.get(), Collections.emptyList());
    verify(_delegate, never()).getServiceNames(anyString(), anyString(), anyString(), any());
  }

  @Test
  public void testOnChangedAfterTimeoutIsIgnored() throws InterruptedException
  {
    // Setup
    XdsD2.D2CalleeServices calleeServices = XdsD2.D2CalleeServices.newBuilder()
        .addAllServices(SERVICE_LIST)
        .build();
    XdsClient.D2CalleesUpdate update = createCalleesUpdate(calleeServices);

    ArgumentCaptor<XdsClient.D2CalleesResourceWatcher> watcherCaptor =
        ArgumentCaptor.forClass(XdsClient.D2CalleesResourceWatcher.class);

    AtomicReference<List<String>> result = new AtomicReference<>();
    List<String> delegateResponse = Arrays.asList("DelegateService1", "DelegateService2");

    doAnswer(invocation -> {
      SuccessCallback<List<String>> callback = (SuccessCallback<List<String>>) invocation.getArguments()[3];
      callback.onSuccess(delegateResponse);
      return null;
    }).when(_delegate).getServiceNames(eq(APP_NAME), eq(APP_INSTANCE), eq(CLIENT_SCOPE), any());

    CountDownLatch latch = new CountDownLatch(1);
    // Execute
    _fetcher.getServiceNames(APP_NAME, APP_INSTANCE, CLIENT_SCOPE, services -> {
      result.set(services);
      latch.countDown();
    });

    verify(_xdsClient).watchXdsResource(eq(RESOURCE_NAME), watcherCaptor.capture());

    // Wait for timeout
    Assert.assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
    Assert.assertEquals(result.get(), delegateResponse);

    // Try to call onChanged after timeout - should be ignored
    watcherCaptor.getValue().onChanged(update);

    // Verify callback was only called once (from delegate)
    Assert.assertEquals(result.get(), delegateResponse);
  }

  @Test
  public void testOnErrorAfterSuccessIsIgnored()
  {
    // Setup
    XdsD2.D2CalleeServices calleeServices = XdsD2.D2CalleeServices.newBuilder()
        .addAllServices(SERVICE_LIST)
        .build();
    XdsClient.D2CalleesUpdate update = createCalleesUpdate(calleeServices);

    ArgumentCaptor<XdsClient.D2CalleesResourceWatcher> watcherCaptor =
        ArgumentCaptor.forClass(XdsClient.D2CalleesResourceWatcher.class);

    AtomicReference<List<String>> result = new AtomicReference<>();

    // Execute
    _fetcher.getServiceNames(APP_NAME, APP_INSTANCE, CLIENT_SCOPE, result::set);

    verify(_xdsClient).watchXdsResource(eq(RESOURCE_NAME), watcherCaptor.capture());

    // Simulate successful response
    watcherCaptor.getValue().onChanged(update);

    Assert.assertEquals(result.get(), SERVICE_LIST);

    // Try to call onError after success - should be ignored
    watcherCaptor.getValue().onError(Status.UNAVAILABLE);

    // Verify delegate was never called
    verify(_delegate, never()).getServiceNames(anyString(), anyString(), anyString(), any());
    Assert.assertEquals(result.get(), SERVICE_LIST);
  }

  @Test
  public void testReconnectBeforeCompletion()
  {
    // Setup
    ArgumentCaptor<XdsClient.D2CalleesResourceWatcher> watcherCaptor =
        ArgumentCaptor.forClass(XdsClient.D2CalleesResourceWatcher.class);

    XdsD2.D2CalleeServices calleeServices = XdsD2.D2CalleeServices.newBuilder()
        .addAllServices(SERVICE_LIST)
        .build();
    XdsClient.D2CalleesUpdate update = createCalleesUpdate(calleeServices);

    AtomicReference<List<String>> result = new AtomicReference<>();

    // Execute
    _fetcher.getServiceNames(APP_NAME, APP_INSTANCE, CLIENT_SCOPE, result::set);

    verify(_xdsClient).watchXdsResource(eq(RESOURCE_NAME), watcherCaptor.capture());

    // Simulate reconnect before completion - should not affect anything
    watcherCaptor.getValue().onReconnect();

    // Then simulate successful response
    watcherCaptor.getValue().onChanged(update);

    // Verify
    Assert.assertEquals(result.get(), SERVICE_LIST);
  }

  @Test
  public void testReconnectAfterCompletion()
  {
    // Setup
    ArgumentCaptor<XdsClient.D2CalleesResourceWatcher> watcherCaptor =
        ArgumentCaptor.forClass(XdsClient.D2CalleesResourceWatcher.class);

    XdsD2.D2CalleeServices calleeServices = XdsD2.D2CalleeServices.newBuilder()
        .addAllServices(SERVICE_LIST)
        .build();
    XdsClient.D2CalleesUpdate update = createCalleesUpdate(calleeServices);

    AtomicReference<List<String>> result = new AtomicReference<>();

    // Execute
    _fetcher.getServiceNames(APP_NAME, APP_INSTANCE, CLIENT_SCOPE, result::set);

    verify(_xdsClient).watchXdsResource(eq(RESOURCE_NAME), watcherCaptor.capture());

    // Simulate successful response
    watcherCaptor.getValue().onChanged(update);

    Assert.assertEquals(result.get(), SERVICE_LIST);

    // Simulate reconnect after completion - should be ignored
    watcherCaptor.getValue().onReconnect();

    // Verify result is unchanged
    Assert.assertEquals(result.get(), SERVICE_LIST);
  }

  @Test
  public void testDelegatePassthrough()
  {
    // Setup
    SuccessCallback<List<String>> callback = mock(SuccessCallback.class);

    // Execute
    _fetcher.getServiceNames(callback);

    // Verify
    verify(_delegate).getServiceNames(callback);
    verify(_xdsClient, never()).watchXdsResource(anyString(), any());
  }
}
