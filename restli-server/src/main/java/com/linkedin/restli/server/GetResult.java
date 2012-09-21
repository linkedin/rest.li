package com.linkedin.restli.server;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.HttpStatus;


/**
 * @author Keren Jin
 */
public class GetResult<V extends RecordTemplate>
{
  private final V _value;
  private final HttpStatus _status;

  public GetResult(V value)
  {
    _value = value;
    _status = HttpStatus.S_200_OK;
  }

  public GetResult(V value, HttpStatus status)
  {
    _value = value;
    _status = status;
  }

  public V getValue()
  {
    return _value;
  }

  public HttpStatus getStatus()
  {
    return _status;
  }
}
