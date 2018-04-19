package test.r2.integ.helper;

import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import java.util.Map;

/**
 * @author Zhenkai Zhu
 */
public class LogEntityLengthFilter implements RestFilter
{
  private volatile int _reqEntityLen = 0;
  private volatile int _resEntityLen = 0;

  @Override
  public void onRestRequest(RestRequest req,
                            RequestContext requestContext,
                            Map<String, String> wireAttrs,
                            NextFilter<RestRequest, RestResponse> nextFilter)
  {
    _reqEntityLen = req.getEntity().length();
    nextFilter.onRequest(req, requestContext, wireAttrs);
  }

  @Override
  public void onRestResponse(RestResponse res,
                               RequestContext requestContext,
                               Map<String, String> wireAttrs,
                               NextFilter<RestRequest, RestResponse> nextFilter)
  {
    _resEntityLen = res.getEntity().length();
    nextFilter.onResponse(res, requestContext, wireAttrs);
  }

  @Override
  public void onRestError(Throwable ex,
                            RequestContext requestContext,
                            Map<String, String> wireAttrs,
                            NextFilter<RestRequest, RestResponse> nextFilter)
  {
    nextFilter.onError(ex, requestContext, wireAttrs);
  }

  public int getRequestEntityLength()
  {
    return _reqEntityLen;
  }

  public int getResponseEntityLength()
  {
    return _resEntityLen;
  }
}
