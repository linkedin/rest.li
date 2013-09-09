package com.linkedin.restli.server;

import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;

/**
 * An adaptor interface that contains getters for selected resource method information that is safe to pass to InvokeAware user code.
 * The information contained in this class is expected to stay unchanged.
 * Any field that we see a potential for change should not be included.
 *
 * @author Zhenkai Zhu
 */
public interface RestLiMethodContext
{
  /**
   * @return Rest resource method, e.g. GET, BATCH_GET, etc.
   */
  public ResourceMethod getMethodType();

  public String getResourceName();

  public String getNamespace();

  public String getFinderName();

  public String getActionName();
}
