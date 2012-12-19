package com.linkedin.util.degrader;


/**
 * Contains the class of errors that we track for degrader purposes
 *
 * @author Oby Sumampouw (osumampouw@linkedin.com)
 */
public class ErrorConstants
{
  public static final String HTTP_400_ERRORS = "http400Errors";
  public static final String HTTP_500_ERRORS = "http500Errors";
  public static final String REMOTE_INVOCATION_ERROR = "remoteInvocationError";
  public static final String GENERAL_ERROR = "generalError";
}
