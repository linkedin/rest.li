package com.linkedin.r2.message.stream.entitystream;

import com.linkedin.data.ByteString;

/**
 * This is the handle to write data to an EntityStream.
 * This is not thread-safe.
 *
 * @author Zhenkai Zhu
 */
public interface WriteHandle
{
  /**
   * This writes data into the EntityStream.
   * This call may have no effect if the stream has been aborted
   * @param data the data chunk to be written
   * @throws java.lang.IllegalStateException if remaining capacity is 0, or done() or error() has been called
   * @throws java.lang.IllegalStateException if called after done() or error() has been called
   */
  void write(final ByteString data);

  /**
   * Signals that Writer has finished writing.
   * This call has no effect if the stream has been aborted or done() or error() has been called
   */
  void done();

  /**
   * Signals that the Writer has encountered an error.
   * This call has no effect if the stream has been aborted or done() or error() has been called
   * @param throwable the cause of the error.
   */
  void error(final Throwable throwable);

  /**
   * Returns the remaining capacity in number of data chunks
   *
   * Always returns 0 if the stream is aborted or finished with done() or error()
   *
   * @return the remaining capacity in number of data chunks
   */
  int remaining();
}
