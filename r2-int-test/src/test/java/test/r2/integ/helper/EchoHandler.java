package test.r2.integ.helper;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.transport.common.RestRequestHandler;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */
public class EchoHandler implements RestRequestHandler
{
  @Override
  public void handleRequest(RestRequest request, RequestContext requestContext, final Callback<RestResponse> callback)
  {
    RestResponseBuilder builder = new RestResponseBuilder();
    callback.onSuccess(builder.setEntity(request.getEntity()).build());
  }
}
