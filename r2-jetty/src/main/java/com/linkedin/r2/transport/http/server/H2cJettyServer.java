package com.linkedin.r2.transport.http.server;

import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;


/**
 * @author Sean Sheng
 * @version $Revision: $
 */
public class H2cJettyServer extends HttpJettyServer
{
  public H2cJettyServer(
      int port,
      String contextPath,
      int threadPoolSize,
      HttpDispatcher dispatcher,
      boolean restOverStream)
  {
    super(port, contextPath, threadPoolSize, dispatcher, restOverStream);
  }

  public H2cJettyServer(
      int port,
      String contextPath,
      int threadPoolSize,
      HttpDispatcher dispatcher,
      ServletType servletType,
      int asyncTimeout,
      boolean restOverStream)
  {
    super(port, contextPath, threadPoolSize, dispatcher, servletType, asyncTimeout, restOverStream);
  }

  @Override
  protected Connector[] getConnectors(Server server)
  {
    HttpConfiguration configuration = new HttpConfiguration();
    ServerConnector connector = new ServerConnector(
        server,
        new HttpConnectionFactory(configuration, HttpCompliance.RFC2616),
        new HTTP2CServerConnectionFactory(configuration));
    connector.setPort(_port);

    return new Connector[] { connector };
  }
}