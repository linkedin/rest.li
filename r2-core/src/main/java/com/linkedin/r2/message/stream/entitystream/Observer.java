package com.linkedin.r2.message.stream.entitystream;

import com.linkedin.data.ByteString;

/**
 * Observer passively observes the data flow of an EntityStream {@link com.linkedin.r2.message.stream.entitystream.EntityStream}
 * i.e. observer cannot drive the flow of data in the EntityStream
 *
 * @author Zhenkai Zhu
 */
public interface Observer
{
  /**
   * This is called when a new chunk of data is written to the stream by the writer.
   * @param data data written by the writer
   */
  void onDataAvailable(ByteString data);

  /**
   * This is called when the writer finished writing.
   */
  void onDone();

  /**
   * This is called when an error has happened.
   * @param e the cause of the error
   */
  void onError(Throwable e);
}
