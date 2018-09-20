package com.linkedin.restli.server.config;

/**
 * Special exception class for resource method level configuration parsing error.
 *
 * @author mnchen
 */
class ResourceMethodConfigParsingException extends Exception
{
  private static final long serialVersionUID = 1L;

  public ResourceMethodConfigParsingException(String message)
  {
    super(message);
  }

  public ResourceMethodConfigParsingException(Exception e)
  {
    super(e);
  }
}
