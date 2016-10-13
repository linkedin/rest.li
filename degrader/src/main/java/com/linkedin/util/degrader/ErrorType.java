package com.linkedin.util.degrader;


/**
 * Contains the classes of error that we track during a request/response interaction
 * for the purpose of load balancing traffic.
 *
 * @author Oby Sumampouw (osumampouw@linkedin.com)
 */
public enum ErrorType
{
  /**
   * represents an error that occurs while trying to connect to a socket to a remote address/port
   */
  CONNECT_EXCEPTION,

  /**
   * represents an error that occurs when a client is doing I/O operation to a channel that is closed
   */
  CLOSED_CHANNEL_EXCEPTION,

  /**
   * represents other transport errors for example:
   * Connection timed out
   * Cannot send that many bytes over the wire
   * Socket timed out
   */
  REMOTE_INVOCATION_EXCEPTION,

  /**
   * represents an error condition where the client can't get a response from server within certain timeout period
   */
  TIMEOUT_EXCEPTION,

  /**
   * represents a server side error condition
   */
  SERVER_ERROR
}
