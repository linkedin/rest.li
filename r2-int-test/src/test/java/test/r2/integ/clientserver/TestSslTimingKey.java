package test.r2.integ.clientserver;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.r2.message.timing.TimingContextUtil.TimingContext;
import com.linkedin.r2.message.timing.TimingKey;
import com.linkedin.r2.netty.handler.common.SslHandshakeTimingHandler;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.sample.echo.EchoService;
import com.linkedin.r2.sample.echo.rest.RestEchoClient;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import test.r2.integ.clientserver.providers.AbstractEchoServiceTest;
import test.r2.integ.clientserver.providers.ClientServerConfiguration;
import test.r2.integ.clientserver.providers.client.ClientProvider;
import test.r2.integ.clientserver.providers.server.ServerProvider;

/**
 * @author Nizar Mankulangara
 */
public class TestSslTimingKey extends AbstractEchoServiceTest
{
  @Factory(dataProvider = "allHttps", dataProviderClass = ClientServerConfiguration.class)
  public TestSslTimingKey(ClientProvider clientProvider, ServerProvider serverProvider, int port)
  {
    super(clientProvider, serverProvider, port);
  }

  @Test
  public void testSslTimingKey() throws Exception
  {
    if(isHttp2StreamBasedChannel())
      return;

    final EchoService client = new RestEchoClient( Bootstrap.createURI(_port, Bootstrap.getEchoURI(), true),createClient());

    final String msg = "This is a simple http echo message";
    final FutureCallback<String> callback = new FutureCallback<>();
    client.echo(msg, callback);
    Assert.assertEquals(callback.get(), msg);
    RequestContext  context = _clientCaptureFilter.getRequestContext();
    @SuppressWarnings("unchecked")
    Map<TimingKey, TimingContext> map=(Map<TimingKey, TimingContext>)context.getLocalAttr("timings");
    Assert.assertNotNull(map);
    Assert.assertTrue(map.containsKey(SslHandshakeTimingHandler.TIMING_KEY));
    TimingContext timingContext = map.get(SslHandshakeTimingHandler.TIMING_KEY);
    Assert.assertNotNull(timingContext);
  }
}
