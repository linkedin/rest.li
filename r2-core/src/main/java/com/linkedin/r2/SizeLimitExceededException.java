package com.linkedin.r2;

/**
 * Represents an error condition that exceeds certain size limit
 *
 * @author Ang Xu
 * @version $Revision: $
 */
public class SizeLimitExceededException extends Exception
{
  private static final long serialVersionUID = 1L;

  /**
   * Construct a new instance with specified message.
   *
   * @param message the message to be used for this exception.
   */
  public SizeLimitExceededException(String message)
  {
    super(message);
  }
}
