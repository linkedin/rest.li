package com.linkedin.d2.xds;

import com.linkedin.d2.balancer.dualread.DualReadLoadBalancerJmx;
import com.linkedin.d2.balancer.dualread.DualReadModeProvider;
import com.linkedin.d2.balancer.dualread.DualReadStateManager;
import com.linkedin.d2.jmx.D2ClientJmxManager;
import com.linkedin.d2.jmx.JmxManager;
import com.linkedin.d2.xds.util.SslContextUtil;
import com.linkedin.util.clock.SystemClock;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import java.io.File;
import java.util.concurrent.Executors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;


public class XdsToD2SampleClient
{
  public static void main(String[] args) throws Exception
  {
    Options options = new Options();

    Option hostNameOption = new Option("hostName", true, "The node identifier for the xds client node");
    hostNameOption.setRequired(false);
    options.addOption(hostNameOption);

    Option nodeClusterOption =
        new Option("nodeCluster", true, "The local service cluster name where xds client is running");
    nodeClusterOption.setRequired(false);
    options.addOption(nodeClusterOption);

    Option xdsServerOption = new Option("xds", true, "xDS server address");
    xdsServerOption.setRequired(false);
    options.addOption(xdsServerOption);

    Option serviceNameOption = new Option("service", true, "Service name to discover");
    serviceNameOption.setRequired(false);
    options.addOption(serviceNameOption);

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

    Node node = Node.DEFAULT_NODE;
    if (cmd.hasOption("hostName") && cmd.hasOption("nodeCluster"))
    {
      node = new Node(cmd.getOptionValue("hostName"), cmd.getOptionValue("nodeCluster"), "gRPC", null);
    }

    String xdsServer = cmd.getOptionValue("xds", "localhost:32123");
    String serviceName = cmd.getOptionValue("service", "tokiBackendGrpc");

    String keyStoreFilePath = cmd.getOptionValue("keyStoreFilePath");
    String keyStorePassword = cmd.getOptionValue("keyStorePassword");
    String keyStoreType = cmd.getOptionValue("keyStoreType");
    String trustStoreFilePath = cmd.getOptionValue("trustStoreFilePath");
    String trustStorePassword = cmd.getOptionValue("trustStorePassword");

    SslContext sslContext = null;

    if (keyStoreFilePath != null && keyStorePassword != null && keyStoreType != null
        && trustStoreFilePath != null && trustStorePassword != null) {
      sslContext = SslContextUtil.buildClientSslContext(
          new File(keyStoreFilePath), keyStorePassword, keyStoreType, new File(trustStoreFilePath), trustStorePassword
      );
    }

    XdsChannelFactory xdsChannelFactory = new XdsChannelFactory(sslContext, xdsServer);
    XdsClient xdsClient = new XdsClientImpl(node, xdsChannelFactory.createChannel(),
        Executors.newSingleThreadScheduledExecutor(), XdsClientImpl.DEFAULT_READY_TIMEOUT_MILLIS);

    DualReadStateManager dualReadStateManager = new DualReadStateManager(
        () -> DualReadModeProvider.DualReadMode.DUAL_READ,
        Executors.newSingleThreadScheduledExecutor());

    XdsToD2PropertiesAdaptor adaptor = new XdsToD2PropertiesAdaptor(xdsClient, dualReadStateManager, null);
    adaptor.listenToService(serviceName);
    adaptor.listenToCluster("TokiBackendGrpc");

    while (true)
    {
    }
  }
}
