package com.linkedin.d2.xds;

import io.grpc.EquivalentAddressGroup;
import io.grpc.ManagedChannel;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import io.grpc.NameResolverRegistry;
import io.grpc.Server;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


/**
 * Demonstrates that the xDS channel created by {@link XdsChannelFactory} hangs indefinitely when
 * the first resolved address accepts TCP but is slow to complete the HTTP/2 handshake (simulating
 * a slow TLS handshake on an INDIS server). Even when a second healthy server is available,
 * pick_first never tries it.
 *
 * This reproduces the production failure where the INDIS xDS channel got stuck for 12+ seconds
 * on a slow server while a healthy server was available.
 *
 * The test uses {@link XdsChannelFactory} to create the channel the same way production does,
 * with a custom NameResolver injected via the global registry to control address ordering.
 */
public class PickFirstSlowConnectionTest
{
  private static final String SLOW_TEST_SCHEME = "slowtest";

  private Server _healthyServer;
  private ServerSocket _slowServerSocket;
  private Thread _slowServerThread;
  private ManagedChannel _channel;
  private NameResolverProvider _testResolverProvider;

  @AfterMethod
  public void tearDown() throws Exception
  {
    if (_channel != null)
    {
      _channel.shutdownNow();
      _channel.awaitTermination(5, TimeUnit.SECONDS);
    }
    if (_healthyServer != null)
    {
      _healthyServer.shutdownNow();
      _healthyServer.awaitTermination(5, TimeUnit.SECONDS);
    }
    if (_slowServerSocket != null && !_slowServerSocket.isClosed())
    {
      _slowServerSocket.close();
    }
    if (_slowServerThread != null)
    {
      _slowServerThread.interrupt();
      _slowServerThread.join(5000);
    }
    if (_testResolverProvider != null)
    {
      NameResolverRegistry.getDefaultRegistry().deregister(_testResolverProvider);
    }
  }

  /**
   * Start a raw TCP server that accepts connections but never completes the HTTP/2 handshake.
   * This simulates an INDIS server that is reachable (TCP SYN-ACK) but slow to TLS-handshake.
   */
  private SlowServer startSlowServer() throws Exception
  {
    ServerSocket serverSocket = new ServerSocket(0);
    AtomicBoolean accepted = new AtomicBoolean(false);

    Thread thread = new Thread(() ->
    {
      try
      {
        while (!Thread.currentThread().isInterrupted())
        {
          Socket conn = serverSocket.accept();
          accepted.set(true);
          // Hold connection open, never send HTTP/2 preface or TLS ServerHello.
          // gRPC's pick_first sees TCP connected, so it won't try the next address.
          Thread.sleep(60_000);
          conn.close();
        }
      }
      catch (Exception e)
      {
        // Expected on teardown
      }
    }, "slow-server-acceptor");
    thread.setDaemon(true);
    thread.start();

    return new SlowServer(serverSocket, thread, accepted);
  }

  /**
   * Start a real gRPC server that responds to health checks immediately.
   */
  private Server startHealthyServer() throws Exception
  {
    return NettyServerBuilder.forPort(0)
        .addService(new HealthGrpc.HealthImplBase()
        {
          @Override
          public void check(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver)
          {
            responseObserver.onNext(
                HealthCheckResponse.newBuilder()
                    .setStatus(HealthCheckResponse.ServingStatus.SERVING)
                    .build());
            responseObserver.onCompleted();
          }
        })
        .build()
        .start();
  }

  /**
   * Register a custom NameResolver that returns the given addresses in order.
   * This lets us control which address pick_first tries first, while still using
   * {@link XdsChannelFactory} to build the channel exactly as production does.
   */
  private void registerTestNameResolver(InetSocketAddress... addresses)
  {
    _testResolverProvider = new NameResolverProvider()
    {
      @Override
      public NameResolver newNameResolver(URI targetUri, NameResolver.Args args)
      {
        if (!SLOW_TEST_SCHEME.equals(targetUri.getScheme()))
        {
          return null;
        }
        return new NameResolver()
        {
          @Override
          public String getServiceAuthority()
          {
            return targetUri.getAuthority() != null ? targetUri.getAuthority() : "localhost";
          }

          @Override
          public void start(Listener2 listener)
          {
            ResolutionResult.Builder resultBuilder = ResolutionResult.newBuilder();
            for (InetSocketAddress addr : addresses)
            {
              resultBuilder.setAddresses(
                  Arrays.asList(new EquivalentAddressGroup(addr)));
            }
            // Provide all addresses as separate EquivalentAddressGroups so pick_first
            // treats them as ordered alternatives.
            listener.onResult(ResolutionResult.newBuilder()
                .setAddresses(Arrays.stream(addresses)
                    .map(addr -> new EquivalentAddressGroup(Collections.singletonList(addr)))
                    .collect(java.util.stream.Collectors.toList()))
                .build());
          }

          @Override
          public void shutdown() { }
        };
      }

      @Override
      protected boolean isAvailable()
      {
        return true;
      }

      @Override
      protected int priority()
      {
        return 10; // higher than default DNS resolver (5)
      }

      @Override
      public String getDefaultScheme()
      {
        return SLOW_TEST_SCHEME;
      }
    };
    NameResolverRegistry.getDefaultRegistry().register(_testResolverProvider);
  }

  /**
   * Proves that the xDS channel (as created by {@link XdsChannelFactory}) hangs on a slow instance.
   *
   * Setup:
   * - Address 1 (slow): accepts TCP but never completes HTTP/2 handshake
   * - Address 2 (healthy): real gRPC server, responds immediately
   *
   * Uses {@link XdsChannelFactory} with no SSL (same as production when grpcSslContext is null),
   * and no custom LB policy (so it falls through to gRPC's default pick_first).
   *
   * Expected: pick_first connects to slow address, TCP succeeds, but HTTP/2 handshake never
   * completes. pick_first never tries the healthy address. The channel hangs until deadline.
   */
  @Test(timeOut = 15000)
  public void testXdsChannelHangsOnSlowInstance() throws Exception
  {
    // Start slow server (TCP accept, no HTTP/2)
    SlowServer slowServer = startSlowServer();
    _slowServerSocket = slowServer._serverSocket;
    _slowServerThread = slowServer._thread;
    int slowPort = _slowServerSocket.getLocalPort();

    // Start healthy server
    _healthyServer = startHealthyServer();
    int healthyPort = _healthyServer.getPort();

    // Register name resolver: slow address FIRST, healthy SECOND
    registerTestNameResolver(
        new InetSocketAddress("127.0.0.1", slowPort),
        new InetSocketAddress("127.0.0.1", healthyPort));

    // Create channel using XdsChannelFactory — the same code path as production.
    // In production: XdsChannelFactory(config.grpcSslContext, config.xdsServer, ...).createChannel()
    // Here: no SSL, no custom LB policy → uses plaintext + default pick_first
    XdsChannelFactory factory = new XdsChannelFactory(
        null,  // no SSL, same as when grpcSslContext is null
        SLOW_TEST_SCHEME + ":///indis-test-server",
        null,  // no custom LB policy → defaults to pick_first
        null   // no LB policy config
    );
    _channel = factory.createChannel();
    assertNotNull(_channel, "XdsChannelFactory should create a non-null channel");

    // Now do exactly what XdsClientValidator.validateHealthCheck() does:
    // a gRPC health check with a deadline — same as the pre-check that timed out in production.
    long startTime = System.currentTimeMillis();
    try
    {
      HealthCheckResponse response = HealthGrpc.newBlockingStub(_channel)
          .withDeadlineAfter(5, TimeUnit.SECONDS)
          .check(HealthCheckRequest.newBuilder().setService("").build());
      fail("Expected DEADLINE_EXCEEDED, but got response: " + response.getStatus());
    }
    catch (io.grpc.StatusRuntimeException e)
    {
      long elapsed = System.currentTimeMillis() - startTime;

      // Verify slow server accepted TCP (connection not refused)
      assertTrue(slowServer._accepted.get(),
          "Slow server should have accepted the TCP connection");

      // Verify we got DEADLINE_EXCEEDED — hung the full timeout, never tried healthy server
      assertEquals(e.getStatus().getCode(), io.grpc.Status.Code.DEADLINE_EXCEEDED,
          "Expected DEADLINE_EXCEEDED but got: " + e.getStatus());

      // Verify it waited close to the full deadline (didn't fail fast or failover)
      assertTrue(elapsed >= 4500,
          "Should have waited ~5s for deadline, but only waited " + elapsed + "ms. "
              + "If this is fast, pick_first may have tried the healthy server (unexpected).");

      System.out.println("=== TEST PROVES: XdsChannelFactory + pick_first hangs on slow instance ===");
      System.out.println("Slow server accepted TCP: " + slowServer._accepted.get());
      System.out.println("Healthy server was available but NEVER tried");
      System.out.println("Hung for " + elapsed + "ms until deadline (5s)");
      System.out.println("gRPC error: " + e.getStatus());
      System.out.println();
      System.out.println("In production, this is the exact scenario that caused the 12s hang:");
      System.out.println("  - XdsChannelFactory.createChannel() builds a NettyChannel with pick_first");
      System.out.println("  - DNS resolves to multiple INDIS IPs");
      System.out.println("  - pick_first picks one IP, TCP connects, TLS handshake hangs");
      System.out.println("  - pick_first never tries the next IP because TCP didn't fail");
      System.out.println("  - ADS stream readyTimeout fires after 2s, D2 falls back to empty file store");
      System.out.println("  - Venice can't discover venice-discovery service → job fails");
    }
  }

  /**
   * Control test: same setup but with healthy server FIRST. Proves the test infrastructure works
   * and the issue is purely about pick_first address ordering.
   */
  @Test(timeOut = 15000)
  public void testXdsChannelSucceedsWhenHealthyIsFirst() throws Exception
  {
    SlowServer slowServer = startSlowServer();
    _slowServerSocket = slowServer._serverSocket;
    _slowServerThread = slowServer._thread;
    int slowPort = _slowServerSocket.getLocalPort();

    _healthyServer = startHealthyServer();
    int healthyPort = _healthyServer.getPort();

    // Healthy address FIRST this time
    registerTestNameResolver(
        new InetSocketAddress("127.0.0.1", healthyPort),
        new InetSocketAddress("127.0.0.1", slowPort));

    XdsChannelFactory factory = new XdsChannelFactory(
        null, SLOW_TEST_SCHEME + ":///indis-test-server", null, null);
    _channel = factory.createChannel();

    long startTime = System.currentTimeMillis();
    HealthCheckResponse response = HealthGrpc.newBlockingStub(_channel)
        .withDeadlineAfter(5, TimeUnit.SECONDS)
        .check(HealthCheckRequest.newBuilder().setService("").build());
    long elapsed = System.currentTimeMillis() - startTime;

    assertEquals(response.getStatus(), HealthCheckResponse.ServingStatus.SERVING);
    assertTrue(elapsed < 2000,
        "Healthy-first should complete quickly, but took " + elapsed + "ms");

    System.out.println("=== CONTROL: healthy first completes in " + elapsed + "ms ===");
  }

  /**
   * Tests the same scenario but with the {@link IPv6AwarePickFirstLoadBalancer} policy —
   * the actual policy used in production when xdsChannelLoadBalancingPolicy is configured.
   * This shuffles addresses but still delegates to pick_first, so the same hang occurs
   * whenever the slow address is shuffled to the front.
   */
  @Test(timeOut = 15000, invocationCount = 3) // run 3 times — shuffle may or may not put slow first
  public void testXdsChannelWithIPv6AwarePickFirstPolicy() throws Exception
  {
    SlowServer slowServer = startSlowServer();
    _slowServerSocket = slowServer._serverSocket;
    _slowServerThread = slowServer._thread;
    int slowPort = _slowServerSocket.getLocalPort();

    _healthyServer = startHealthyServer();
    int healthyPort = _healthyServer.getPort();

    // Register both addresses — IPv6AwarePickFirst will shuffle them
    registerTestNameResolver(
        new InetSocketAddress("127.0.0.1", slowPort),
        new InetSocketAddress("127.0.0.1", healthyPort));

    // Use the actual production LB policy
    // Force the static initializer to register the policy
    String policyName = IPv6AwarePickFirstLoadBalancer.POLICY_NAME;

    XdsChannelFactory factory = new XdsChannelFactory(
        null,
        SLOW_TEST_SCHEME + ":///indis-test-server",
        policyName,  // actual production LB policy
        null
    );
    _channel = factory.createChannel();

    long startTime = System.currentTimeMillis();
    try
    {
      HealthCheckResponse response = HealthGrpc.newBlockingStub(_channel)
          .withDeadlineAfter(5, TimeUnit.SECONDS)
          .check(HealthCheckRequest.newBuilder().setService("").build());
      long elapsed = System.currentTimeMillis() - startTime;
      // If we got here, shuffle put healthy first — that's fine, still proves the point
      System.out.println("=== IPv6AwarePickFirst: shuffle put healthy first, completed in "
          + elapsed + "ms ===");
    }
    catch (io.grpc.StatusRuntimeException e)
    {
      long elapsed = System.currentTimeMillis() - startTime;
      if (e.getStatus().getCode() == io.grpc.Status.Code.DEADLINE_EXCEEDED)
      {
        System.out.println("=== IPv6AwarePickFirst: shuffle put slow first, HUNG for "
            + elapsed + "ms ===");
        System.out.println("This proves the production LB policy has the same vulnerability.");
      }
      else
      {
        fail("Unexpected error: " + e.getStatus());
      }
    }
  }

  private static class SlowServer
  {
    final ServerSocket _serverSocket;
    final Thread _thread;
    final AtomicBoolean _accepted;

    SlowServer(ServerSocket serverSocket, Thread thread, AtomicBoolean accepted)
    {
      _serverSocket = serverSocket;
      _thread = thread;
      _accepted = accepted;
    }
  }
}
