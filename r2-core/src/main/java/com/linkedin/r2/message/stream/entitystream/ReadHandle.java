package com.linkedin.r2.message.stream.entitystream;

/**
 * This is the handle to read data from an EntityStream {@link com.linkedin.r2.message.stream.entitystream.EntityStream}
 *
 * @author Zhenkai Zhu
 */
public interface ReadHandle
{
  /**
   * This method signals the writer of the EntityStream that it can write more data.
   *
   * @param n the additional number of data chunks that the writer is permitted to write
   * @throws java.lang.IllegalArgumentException if n is not positive
   */
  void request(int n);

  /**
   * This method cancels the stream
   */
  void cancel();
}
