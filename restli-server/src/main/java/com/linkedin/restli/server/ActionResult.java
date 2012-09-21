package com.linkedin.restli.server;


import com.linkedin.restli.common.HttpStatus;


/**
 * @author Keren Jin
 */
public class ActionResult<V>
{
  private final V _value;
  private final HttpStatus _status;

  public ActionResult(V value)
  {
    _value = value;
    _status = HttpStatus.S_200_OK;
  }

  public ActionResult(V value, HttpStatus status)
  {
    _value = value;
    _status = status;
  }

  public ActionResult(HttpStatus status)
  {
    _value = null;
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
