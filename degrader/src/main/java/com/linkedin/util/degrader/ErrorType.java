package com.linkedin.util.degrader;


/**
 * Contains the class of errors that we track during a request/response interaction
 *
 * @author Oby Sumampouw (osumampouw@linkedin.com)
 */
public enum ErrorType
{
  /**
   * Represents http client error. Usually with 4xx error status code
   */
  HTTP_400_ERROR,

  /**
   * Represents http server error. Usually with 5xx error status code
   */
  HTTP_500_ERROR,

  /**
   * Represents an error that is caused by RemoteInvocationException when calling another server
   */
  REMOTE_INVOCATION_ERROR,

  /**
   *  Represents an error that is caused by REST exception whose status code is not 4xx or 5xx
   */
  OTHER_REST_EXCEPTION_ERROR
}
