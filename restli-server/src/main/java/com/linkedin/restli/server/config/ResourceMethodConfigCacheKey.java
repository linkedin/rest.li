package com.linkedin.restli.server.config;

import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.config.ResourceMethodKeyParser.OperationContext;
import com.linkedin.restli.server.config.ResourceMethodKeyParser.RestResourceContext;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.BiFunction;


/**
 * Cache key for mapped resource method for an incoming rest.li request. We internally cache the resource method
 * level configuration to avoid precedence order resolution every time.
 *
 * @author mnchen
 */
class ResourceMethodConfigCacheKey
{
  // rest.li resource name
  private final String _resourceName;
  // rest.li resource method type
  private final ResourceMethod _opType;
  // action name or finder name, optional
  private final Optional<String> _opName;

  ResourceMethodConfigCacheKey(ResourceMethodDescriptor requestMethod)
  {
    _resourceName = getResourceName(requestMethod);
    _opType = requestMethod.getType();
    _opName = getOperationName(requestMethod);
  }

  private static String getResourceName(ResourceMethodDescriptor requestMethod)
  {
    ResourceModel currentModel = requestMethod.getResourceModel();
    StringBuffer resourceName = new StringBuffer(currentModel.getName());
    while ((currentModel = currentModel.getParentResourceModel()) != null)
    {
      resourceName.insert(0, ":" + currentModel.getName());
    }
    return resourceName.toString();
  }

  private static Optional<String> getOperationName(ResourceMethodDescriptor requestMethod)
  {
    if (requestMethod.getFinderName() != null)
    {
      return Optional.of(requestMethod.getFinderName());
    }
    else if (requestMethod.getActionName() != null)
    {
      return Optional.of(requestMethod.getActionName());
    }
    else if (requestMethod.getBatchFinderName() != null)
    {
      return Optional.of(requestMethod.getBatchFinderName());
    }
    else
    {
      return Optional.empty();
    }
  }

  public String getResourceName()
  {
    return _resourceName;
  }

  public ResourceMethod getOperationType()
  {
    return _opType;
  }

  public Optional<String> getOperationName()
  {
    return _opName;
  }

  @Override
  public String toString()
  {
    return "ResourceMethodConfigCacheKey{" +
            "resourceName='" + _resourceName + '\'' +
            ", opType=" + _opType +
            ", opName=" + _opName +
            '}';
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ResourceMethodConfigCacheKey that = (ResourceMethodConfigCacheKey) o;
    return Objects.equals(_resourceName, that._resourceName) &&
            _opType == that._opType &&
            Objects.equals(_opName, that._opName);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(_resourceName, _opType, _opName);
  }
}
