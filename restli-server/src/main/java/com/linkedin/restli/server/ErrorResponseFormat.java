package com.linkedin.restli.server;


import java.util.EnumSet;


/**
 * Provides settings for how error responses are formatted.
 *
 * jbetz@linkedin.com
 */
public enum ErrorResponseFormat
{

  /**
   * All available error information is included in responses, including server side stack trace.
   */
  FULL(EnumSet.allOf(ErrorResponsePart.class)),

  /**
   * Only the error message and explicitly provided error details or service error code are included in responses.
   */
  MESSAGE_AND_DETAILS(EnumSet.of(ErrorResponsePart.MESSAGE, ErrorResponsePart.DETAILS)),

  /**
   * Only the error message.
   */
  MESSAGE_ONLY(EnumSet.of(ErrorResponsePart.MESSAGE)),

  /**
   * Clients only get back a HTTP Status code and RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE header, nothing else.
   */
  MINIMAL(EnumSet.noneOf(ErrorResponsePart.class));

  private static enum ErrorResponsePart
  {
    HEADERS,
    STATUS_CODE_IN_BODY,
    STACKTRACE,
    MESSAGE,
    SERVICE_ERROR_CODE,
    DETAILS
  }

  private final EnumSet<ErrorResponsePart> _errorPartsToShow;

  ErrorResponseFormat(EnumSet<ErrorResponsePart> errorPartsToShow)
  {
    _errorPartsToShow = errorPartsToShow;
  }

  public static ErrorResponseFormat defaultFormat()
  {
    return FULL;
  }

  public boolean showHeaders()
  {
    return _errorPartsToShow.contains(ErrorResponsePart.HEADERS);
  }

  public boolean showStatusCodeInBody()
  {
    return _errorPartsToShow.contains(ErrorResponsePart.STATUS_CODE_IN_BODY);
  }

  public boolean showStacktrace()
  {
    return _errorPartsToShow.contains(ErrorResponsePart.STACKTRACE);
  }

  public boolean showMessage()
  {
    return _errorPartsToShow.contains(ErrorResponsePart.MESSAGE);
  }

  public boolean showServiceErrorCode()
  {
    return _errorPartsToShow.contains(ErrorResponsePart.SERVICE_ERROR_CODE);
  }

  public boolean showDetails()
  {
    return _errorPartsToShow.contains(ErrorResponsePart.DETAILS);
  }
}
