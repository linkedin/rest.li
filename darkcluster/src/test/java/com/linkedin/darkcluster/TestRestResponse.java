package com.linkedin.darkcluster;

import java.util.List;
import java.util.Map;

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;

public class TestRestResponse implements RestResponse
{
  @Override
  public ByteString getEntity()
  {
    return null;
  }

  @Override
  public RestResponseBuilder builder()
  {
    return null;
  }

  @Override
  public int getStatus()
  {
    return 0;
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
