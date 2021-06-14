package com.linkedin.restli.docgen;

import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;


public class RenderContext
{
  private final OutputStream _outputStream;
  private final Map<String, String> _headers;

  public RenderContext(OutputStream outputStream)
  {
    this(outputStream, Collections.emptyMap());
  }

  public RenderContext(OutputStream outputStream, Map<String, String> headers)
  {
    _outputStream = outputStream;
    _headers = headers;
  }

  public OutputStream getOutputStream()
  {
    return _outputStream;
  }

  public Map<String, String> getHeaders()
  {
    return _headers;
  }
}
