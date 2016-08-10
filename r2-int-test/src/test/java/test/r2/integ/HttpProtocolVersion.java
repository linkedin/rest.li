package test.r2.integ;

import com.linkedin.r2.transport.http.client.HttpClientFactory;


/**
 * @author Sean Sheng
 * @version $Revision: $
 */
public class HttpProtocolVersion
{
  public static final String HTTP_1_1 = HttpClientFactory.HttpProtocolVersion.HTTP_1_1.name();
  public static final String HTTP_2 = HttpClientFactory.HttpProtocolVersion.HTTP_2.name();
}
