package com.linkedin.d2.xds;

import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.ManagedChannel;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import io.grpc.NameResolverRegistry;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


/**
 * Demonstrates that the xDS channel created by {@link XdsChannelFactory} hangs indefinitely when
 * the first resolved address accepts TCP but is slow to complete the HTTP/2 handshake (simulating
 * a slow TLS handshake on an INDIS server). Even when a second healthy server is available,
 * pick_first never tries it.
 *
 * <p>The test uses {@link XdsChannelFactory} to create the channel the same way production does,
 * with a custom {@link NameResolver} injected via the global {@link NameResolverRegistry} to
 * control address ordering.
 *
 * <p><b>IMPORTANT:</b> This test must run in its own JVM (via the {@code grpcSlowChannelTest}
 * Gradle task) because gRPC 1.68's {@code RetryingNameResolver} has a bug where shared global
 * state from other test classes causes NPEs with custom NameResolvers. See {@code d2/build.gradle}
 * for the task configuration.
 */
public class PickFirstSlowConnectionTest
{
  /**
   * Unique scheme per test method to avoid interference from prior test invocations
   * in the same JVM that may have cached resolver references in gRPC's global state.
   */
  private static final AtomicLong SCHEME_COUNTER = new AtomicLong(0);

  private Server _healthyServer;
  private ServerSocket _slowServerSocket;
  private Thread _slowServerThread;
  private ManagedChannel _channel;
  private NameResolverProvider _testResolverProvider;

  @AfterMethod(alwaysRun = true)
  public void tearDown() throws Exception
  {
    // Deregister the resolver provider BEFORE shutting down the channel to avoid a race
    // where channel shutdown triggers resolver lookups on a deregistered provider.
    if (_testResolverProvider != null)
    {
      NameResolverRegistry.getDefaultRegistry().deregister(_testResolverProvider);
      _testResolverProvider = null;
    }
    if (_channel != null)
    {
      _channel.shutdownNow();
      _channel.awaitTermination(5, TimeUnit.SECONDS);
      _channel = null;
    }
    if (_healthyServer != null)
    {
      _healthyServer.shutdownNow();
      _healthyServer.awaitTermination(5, TimeUnit.SECONDS);
      _healthyServer = null;
    }
    if (_slowServerSocket != null && !_slowServerSocket.isClosed())
    {
      _slowServerSocket.close();
    }
    _slowServerSocket = null;
    if (_slowServerThread != null)
    {
      _slowServerThread.interrupt();
      _slowServerThread.join(5000);
    }
    _slowServerThread = null;
  }

  /**
   * Start a raw TCP server that accepts connections but never completes the HTTP/2 handshake.
   * This simulates an INDIS server that is reachable at TCP level (SYN-ACK) but slow to
   * complete the TLS handshake. The socket stays open so gRPC won't get ECONNREFUSED or
   * ECONNRESET — it will just wait forever for the HTTP/2 connection preface.
   */
  private SlowServer startSlowServer() throws Exception
  {
    ServerSocket serverSocket = new ServerSocket(0); // bind to random available port
    AtomicBoolean accepted = new AtomicBoolean(false);

    Thread thread = new Thread(() ->
    {
      try
      {
        while (!Thread.currentThread().isInterrupted())
        {
          Socket conn = serverSocket.accept();
          accepted.set(true);
          // Hold the connection open but never send anything — simulates slow TLS handshake.
          // gRPC's pick_first sees TCP connected, so it won't try the next address.
          Thread.sleep(60_000);
          conn.close();
        }
      }
      catch (Exception e)
      {
        // Expected on teardown when ServerSocket is closed
      }
    }, "slow-server-acceptor");
    thread.setDaemon(true);
    thread.start();

    return new SlowServer(serverSocket, thread, accepted);
  }

  /**
   * Start a real gRPC server that responds to health checks immediately.
   * This represents a healthy INDIS server that pick_first should fail over to
   * (but doesn't, because of the bug).
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
   * Register a custom {@link NameResolverProvider} that returns the given addresses in order.
   * This lets us control which address pick_first tries first, while still using
   * {@link XdsChannelFactory} to build the channel exactly as production does.
   *
   * <p>Each call generates a unique scheme name (xdstest1, xdstest2, ...) to avoid collisions
   * with other test methods or prior invocations in the same JVM.
   *
   * @param addresses the addresses to return from DNS resolution, in order
   * @return the unique scheme name to use when constructing the target URI
   */
  private String registerTestNameResolver(InetSocketAddress... addresses)
  {
    String scheme = "xdstest" + SCHEME_COUNTER.incrementAndGet();
    _testResolverProvider = new NameResolverProvider()
    {
      @Override
      public NameResolver newNameResolver(URI targetUri, NameResolver.Args args)
      {
        // Only handle our specific test scheme — return null for everything else
        // so other resolvers (e.g., dns) are unaffected.
        if (!scheme.equals(targetUri.getScheme()))
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
            // Provide each address as a separate EquivalentAddressGroup so pick_first
            // treats them as ordered alternatives (tries first, then second, etc.)
            List<EquivalentAddressGroup> groups = Stream.of(addresses)
                .map(addr -> new EquivalentAddressGroup(Collections.singletonList(addr)))
                .collect(Collectors.toList());
            listener.onResult(ResolutionResult.newBuilder()
                .setAddresses(groups)
                .setAttributes(Attributes.EMPTY)
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
        return 10; // higher than default DNS resolver (priority 5)
      }

      @Override
      public String getDefaultScheme()
      {
        return scheme;
      }
    };
    NameResolverRegistry.getDefaultRegistry().register(_testResolverProvider);
    return scheme;
  }

  /**
   * Proves that the xDS channel (as created by {@link XdsChannelFactory}) hangs on a slow instance.
   *
   * <p>Setup:
   * <ul>
   *   <li>Address 1 (slow): accepts TCP connections but never completes HTTP/2 handshake</li>
   *   <li>Address 2 (healthy): real gRPC server that responds to health checks immediately</li>
   * </ul>
   *
   * <p>Uses {@link XdsChannelFactory} with no SSL (same as production when grpcSslContext is null)
   * and no custom LB policy (falls through to gRPC's default pick_first).
   *
   * <p>Expected behavior if pick_first were resilient: after some timeout, it should try address 2.
   * <br>Actual behavior: pick_first stays stuck on address 1 indefinitely because TCP connected
   * successfully — it never moves to address 2. The health check hangs until its deadline expires.
   */
  @Test(timeOut = 15000) // test-level timeout to prevent infinite hang
  public void testXdsChannelHangsOnSlowInstance() throws Exception
  {
    // --- Set up servers ---
    SlowServer slowServer = startSlowServer();
    _slowServerSocket = slowServer._serverSocket;
    _slowServerThread = slowServer._thread;

    _healthyServer = startHealthyServer();

    // Register name resolver: slow address FIRST, healthy SECOND
    String scheme = registerTestNameResolver(
        new InetSocketAddress("127.0.0.1", _slowServerSocket.getLocalPort()),
        new InetSocketAddress("127.0.0.1", _healthyServer.getPort()));

    // Create channel using XdsChannelFactory — the same code path as production.
    // In production: XdsChannelFactory(config.grpcSslContext, config.xdsServer, ...).createChannel()
    // Here: no SSL, no custom LB policy → uses plaintext + default pick_first
    XdsChannelFactory factory = new XdsChannelFactory(
        null,  // no SSL, same as when grpcSslContext is null
        scheme + ":///indis-test-server",
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

      // Verify the slow server DID accept the TCP connection (connection not refused)
      assertTrue(slowServer._accepted.get(),
          "Slow server should have accepted the TCP connection");

      // Verify we got DEADLINE_EXCEEDED — hung the full timeout, never tried healthy server
      assertEquals(e.getStatus().getCode(), Status.Code.DEADLINE_EXCEEDED,
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
    }
  }

  /**
   * Control test: same setup but with the healthy server FIRST. Proves the test infrastructure
   * works correctly and the issue is purely about pick_first address ordering — when the healthy
   * address is first, the channel connects immediately.
   */
  @Test(timeOut = 15000)
  public void testXdsChannelSucceedsWhenHealthyIsFirst() throws Exception
  {
    SlowServer slowServer = startSlowServer();
    _slowServerSocket = slowServer._serverSocket;
    _slowServerThread = slowServer._thread;

    _healthyServer = startHealthyServer();

    // Healthy address FIRST, slow address SECOND
    String scheme = registerTestNameResolver(
        new InetSocketAddress("127.0.0.1", _healthyServer.getPort()),
        new InetSocketAddress("127.0.0.1", _slowServerSocket.getLocalPort()));

    XdsChannelFactory factory = new XdsChannelFactory(null, scheme + ":///indis-test-server", null, null);
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
    System.out.println("This confirms the issue is purely about address ordering in pick_first.");
  }

  /**
   * Tests the same scenario but with the {@link IPv6AwarePickFirstLoadBalancer} policy — the
   * actual policy used in production when {@code xdsChannelLoadBalancingPolicy} is configured.
   *
   * <p>The IPv6AwarePickFirst policy shuffles IPv4 and IPv6 addresses separately while preserving
   * their interleaving, then delegates to pick_first. Since both test addresses are IPv4, the
   * shuffle may put either address first — a coin flip. This test accepts both outcomes:
   * <ul>
   *   <li>If slow is shuffled first: DEADLINE_EXCEEDED after 5s (proves the vulnerability)</li>
   *   <li>If healthy is shuffled first: completes quickly (still valid, shuffle happened to help)</li>
   * </ul>
   */
  @Test(timeOut = 15000)
  public void testXdsChannelWithIPv6AwarePickFirstSlowFirst() throws Exception
  {
    SlowServer slowServer = startSlowServer();
    _slowServerSocket = slowServer._serverSocket;
    _slowServerThread = slowServer._thread;

    _healthyServer = startHealthyServer();

    // Both addresses provided — IPv6AwarePickFirst will shuffle them
    String scheme = registerTestNameResolver(
        new InetSocketAddress("127.0.0.1", _slowServerSocket.getLocalPort()),
        new InetSocketAddress("127.0.0.1", _healthyServer.getPort()));

    // Use the actual production LB policy.
    // Referencing POLICY_NAME forces the static initializer to register the policy.
    String policyName = IPv6AwarePickFirstLoadBalancer.POLICY_NAME;

    XdsChannelFactory factory = new XdsChannelFactory(
        null,
        scheme + ":///indis-test-server",
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
      // Shuffle put healthy first — that's fine, test is still valid
      System.out.println("=== IPv6AwarePickFirst: shuffle put healthy first, completed in "
          + elapsed + "ms ===");
    }
    catch (io.grpc.StatusRuntimeException e)
    {
      long elapsed = System.currentTimeMillis() - startTime;
      assertEquals(e.getStatus().getCode(), Status.Code.DEADLINE_EXCEEDED,
          "Expected DEADLINE_EXCEEDED but got: " + e.getStatus());
      System.out.println("=== IPv6AwarePickFirst: slow first, HUNG for " + elapsed + "ms ===");
      System.out.println("This proves the production LB policy has the same vulnerability.");
    }
  }

  /**
   * Bundles the slow server's resources for clean setup/teardown.
   */
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
