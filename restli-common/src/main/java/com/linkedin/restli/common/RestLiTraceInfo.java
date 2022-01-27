package com.linkedin.restli.common;

import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;


/**
 * This class exposes RestLi trace data within the requestContext to aid in request tracing.
 */
public class RestLiTraceInfo {
  private final String _requestTarget;
  private final String _requestTemplate;
  private final String _requestOperation;

  public static void inject(RequestContext requestContext,
      String requestTarget, String requestPath, String requestOperation) {
    requestContext.putLocalAttr(R2Constants.RESTLI_TRACE_INFO, new RestLiTraceInfo(requestTarget, requestPath, requestOperation));
  }

  public static RestLiTraceInfo from(RequestContext requestContext) {
    return (RestLiTraceInfo) requestContext.getLocalAttr(R2Constants.RESTLI_TRACE_INFO);
  }

  private RestLiTraceInfo(String requestTarget, String requestTemplate, String requestOperation) {
    _requestTarget = requestTarget;
    _requestTemplate = requestTemplate;
    _requestOperation = requestOperation;
  }

  public String getRequestTarget() {
    return _requestTarget;
  }

  public String getRequestTemplate() {
    return _requestTemplate;
  }

  public String getRequestOperation() {
    return _requestOperation;
  }
}
