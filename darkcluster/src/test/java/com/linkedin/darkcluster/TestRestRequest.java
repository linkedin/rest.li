package com.linkedin.darkcluster;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;

public class TestRestRequest implements RestRequest
{
  @Override
  public ByteString getEntity()
  {
    return null;
  }

  @Override
  public RestRequestBuilder builder()
  {
    return null;
  }

  @Override
  public String getMethod()
  {
    return null;
  }

  @Override
  public URI getURI()
  {
    return null;
  }

  @Override
  public String getHeader(String name)
  {
    return null;
  }

  @Override
  public List<String> getHeaderValues(String name)
  {
    return null;
  }

  @Override
  public List<String> getCookies()
  {
    return null;
  }

  @Override
  public Map<String, String> getHeaders()
  {
    return null;
  }
}
