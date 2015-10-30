package com.linkedin.r2.message;

import com.linkedin.util.ArgumentUtil;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for request.
 *
 * @author Zhenkai Zhu
 */
public abstract class BaseRequest extends BaseMessage implements Request
{
  private final URI _uri;
  private final String _method;

  protected BaseRequest(Map<String, String> headers, List<String> cookies, URI uri, String method)
  {
    super(headers, cookies);
    ArgumentUtil.notNull(uri, "uri");
    ArgumentUtil.notNull(method, "method");
    _uri = uri;
    _method = method;
  }

  @Override
  public URI getURI()
  {
    return _uri;
  }

  @Override
  public String getMethod()
  {
    return _method;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
    {
      return true;
    }
    if (!(o instanceof BaseRequest))
    {
      return false;
    }
    if (!super.equals(o))
    {
      return false;
    }

    BaseRequest that = (BaseRequest) o;
    return _method.equals(that._method) && _uri.equals(that._uri);
  }

  @Override
  public int hashCode()
  {
    int result = super.hashCode();
    result = 31 * result + _uri.hashCode();
    result = 31 * result + _method.hashCode();
    return result;
  }

}
