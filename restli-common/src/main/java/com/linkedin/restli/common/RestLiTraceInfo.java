package com.linkedin.restli.common;

import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;


/**
 * This class exposes RestLi trace data within the requestContext to aid in request tracing.
 */
public class RestLiTraceInfo {
  private final String _requestTarget;
  private final String _requestOperation;
  private final String _baseUriTemplate;
  private final String _resourceMethodIdentifier;

  public static void inject(RequestContext requestContext, String requestTarget, String requestOperation,
      String baseUriTemplate, String resourceMethodIdentifier) {
    requestContext.putLocalAttr(R2Constants.RESTLI_TRACE_INFO,
        new RestLiTraceInfo(requestTarget, baseUriTemplate, resourceMethodIdentifier, requestOperation));
  }

  public static RestLiTraceInfo from(RequestContext requestContext) {
    return (RestLiTraceInfo) requestContext.getLocalAttr(R2Constants.RESTLI_TRACE_INFO);
  }

  private RestLiTraceInfo(String requestTarget, String baseUriTemplate, String resourceMethodIdentifier, String requestOperation) {
    _requestTarget = requestTarget;
    _requestOperation = requestOperation;
    _baseUriTemplate = baseUriTemplate;
    _resourceMethodIdentifier = resourceMethodIdentifier;
  }

  public String getRequestTarget() {
    return _requestTarget;
  }

  public String getRequestOperation() {
    return _requestOperation;
  }

  public String getBaseUriTemplate() {
    return _baseUriTemplate;
  }

  public String getResourceMethodIdentifier() {
    return _resourceMethodIdentifier;
  }
}
