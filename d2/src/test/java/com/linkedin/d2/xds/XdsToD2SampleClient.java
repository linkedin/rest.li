package com.linkedin.d2.xds;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.D2Client;
import com.linkedin.d2.balancer.D2ClientBuilder;
import com.linkedin.d2.balancer.dualread.DualReadModeProvider;
import com.linkedin.d2.balancer.dualread.DualReadStateManager;
import com.linkedin.d2.discovery.event.ServiceDiscoveryEventEmitter;
import com.linkedin.d2.xds.balancer.XdsLoadBalancerWithFacilitiesFactory;
import com.linkedin.d2.xds.util.SslContextUtil;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;


/**
 * WS2 local end-to-end harness for the INDIS observer-cluster subscription.
 *
 * <p>Unlike a hand-built {@link XdsToD2PropertiesAdaptor}, this drives the <b>real production path</b>: it
 * builds a {@link D2Client} via {@link D2ClientBuilder} with the shipping {@link XdsLoadBalancerWithFacilitiesFactory}
 * and the {@code subscribeToIndisObserverCluster} config flag turned on, then starts it. That exercises the full
 * plumbing under test — {@code D2ClientConfig} -> {@code D2ClientBuilder} -> {@code XdsLoadBalancerWithFacilitiesFactory}
 * -> {@code XdsToD2PropertiesAdaptor.setSubscribeToObserverCluster} -> the xDS subscription — against a live observer.
 *
 * <p>Success looks like these lines in the logs (DEBUG for {@code com.linkedin.d2.xds}):
 * <pre>
 *   Subscribing to D2_URI_MAP resource: /d2/uris/IndisRegistryObserver
 *   Received initial data for D2_URI_MAP /d2/uris/IndisRegistryObserver. Set state to FETCHED.
 * </pre>
 * See OBSERVER_LOCAL_TEST.md.
 */
public class XdsToD2SampleClient
{
  public static void main(String[] args) throws Exception
  {
    Options options = new Options();

    Option hostNameOption = new Option("hostName", true, "The node identifier for the xds client node");
    hostNameOption.setRequired(false);
    options.addOption(hostNameOption);

    Option xdsServerOption = new Option("xds", true, "xDS server address");
    xdsServerOption.setRequired(false);
    options.addOption(xdsServerOption);

    Option keyStoreFilePathOption = new Option("keyStoreFilePath", true, "keyStoreFilePath for TLS");
    keyStoreFilePathOption.setRequired(false);
    options.addOption(keyStoreFilePathOption);

    Option keyStorePasswordOption = new Option("keyStorePassword", true, "keyStorePassword for TLS");
    keyStorePasswordOption.setRequired(false);
    options.addOption(keyStorePasswordOption);

    Option keyStoreTypeOption = new Option("keyStoreType", true, "keyStoreType for TLS");
    keyStoreTypeOption.setRequired(false);
    options.addOption(keyStoreTypeOption);

    Option trustStoreFilePathOption = new Option("trustStoreFilePath", true, "trustStoreFilePath for TLS");
    trustStoreFilePathOption.setRequired(false);
    options.addOption(trustStoreFilePathOption);

    Option trustStorePasswordOption = new Option("trustStorePassword", true, "trustStorePassword for TLS");
    trustStorePasswordOption.setRequired(false);
    options.addOption(trustStorePasswordOption);

    CommandLineParser parser = new GnuParser();
    CommandLine cmd = parser.parse(options, args);

    String xdsServer = cmd.getOptionValue(xdsServerOption.getOpt(), "localhost:32123");
    String hostName = cmd.getOptionValue(hostNameOption.getOpt(), "observer-local-test-client");

    String keyStoreFilePath = cmd.getOptionValue(keyStoreFilePathOption.getOpt());
    String keyStorePassword = cmd.getOptionValue(keyStorePasswordOption.getOpt());
    String keyStoreType = cmd.getOptionValue(keyStoreTypeOption.getOpt());
    String trustStoreFilePath = cmd.getOptionValue(trustStoreFilePathOption.getOpt());
    String trustStorePassword = cmd.getOptionValue(trustStorePasswordOption.getOpt());

    SslContext sslContext = null;
    if (keyStoreFilePath != null && keyStorePassword != null && keyStoreType != null
        && trustStoreFilePath != null && trustStorePassword != null)
    {
      sslContext = SslContextUtil.buildClientSslContext(
          new File(keyStoreFilePath), keyStorePassword, keyStoreType, new File(trustStoreFilePath), trustStorePassword
      );
    }

    DualReadStateManager dualReadStateManager = new DualReadStateManager(
        () -> DualReadModeProvider.DualReadMode.DUAL_READ,
        Executors.newSingleThreadScheduledExecutor(), true);

    // Build through the real production path with the config flag flipped on. warmUp is disabled because we only
    // need the load balancer to start (which triggers the observer subscription); we are not making D2 requests.
    D2Client client = new D2ClientBuilder()
        .setXdsServer(xdsServer)
        .setHostName(hostName)
        .setGrpcSslContext(sslContext)
        .setDualReadStateManager(dualReadStateManager)
        .setServiceDiscoveryEventEmitter(new LoggingSdEventEmitter())
        .setLoadBalancerWithFacilitiesFactory(new XdsLoadBalancerWithFacilitiesFactory())
        .setSubscribeToIndisObserverCluster(true) // the config flag under test
        .setWarmUp(false)
        .build();

    CountDownLatch started = new CountDownLatch(1);
    Throwable[] startError = {null};
    client.start(new Callback<None>()
    {
      @Override
      public void onSuccess(None result)
      {
        System.out.println("[observer-test] D2 client started via D2ClientBuilder "
            + "(subscribeToIndisObserverCluster=true, warmUp=false)");
        started.countDown();
      }

      @Override
      public void onError(Throwable e)
      {
        startError[0] = e;
        System.out.println("[observer-test] D2 client start FAILED: " + e);
        started.countDown();
      }
    });
    started.await();
    if (startError[0] != null)
    {
      startError[0].printStackTrace();
      return;
    }

    System.out.println("[observer-test] started through full builder path; watch for 'Subscribing to' + "
        + "'Received initial data ... Set state to FETCHED' for all three observer resources: "
        + "NODE /d2/services/indisRegistryObserver, NODE /d2/clusters/IndisRegistryObserver, and "
        + "D2_URI_MAP /d2/uris/IndisRegistryObserver. From " + xdsServer);

    while (true)
    {
      Thread.sleep(5000);
    }
  }

  /**
   * Minimal {@link ServiceDiscoveryEventEmitter} that logs received service-discovery updates to stdout,
   * so the local observer-subscription test can show the observer's endpoints arriving over xDS.
   */
  private static class LoggingSdEventEmitter implements ServiceDiscoveryEventEmitter
  {
    @Override
    public void emitSDStatusInitialRequestEvent(String cluster, boolean isNextGen, long duration, boolean succeeded)
    {
      System.out.println("[observer-test] initial request for cluster '" + cluster + "' succeeded=" + succeeded
          + " (" + duration + "ms)");
    }

    @Override
    public void emitSDStatusUpdateReceiptEvent(String cluster, String host, int port, StatusUpdateActionType actionType,
        boolean isNextGen, String serviceRegistry, String serviceRegistryKey, String serviceRegistryValue,
        Integer serviceRegistryVersion, String tracingId, long timestamp)
    {
      System.out.println("[observer-test] endpoint " + actionType + ": cluster='" + cluster + "' " + host + ":" + port);
    }

    @Override
    public void emitSDStatusActiveUpdateIntentEvent(List<String> clustersClaimed, StatusUpdateActionType actionType,
        boolean isNextGen, String tracingId, long timestamp)
    {
    }

    @Override
    public void emitSDStatusWriteEvent(String cluster, String host, int port, StatusUpdateActionType actionType,
        String serviceRegistry, String serviceRegistryKey, String serviceRegistryValue, Integer serviceRegistryVersion,
        String tracingId, boolean succeeded, long timestamp)
    {
    }
  }
}
