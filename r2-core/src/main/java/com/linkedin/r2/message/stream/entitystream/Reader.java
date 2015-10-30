package com.linkedin.r2.message.stream.entitystream;

/**
 * Reader is an Observer {@link com.linkedin.r2.message.stream.entitystream.Observer} that has the extended ability to read data
 * and drive and control the data flow of an EntityStream {@link com.linkedin.r2.message.stream.entitystream.EntityStream}
 *
 * @author Zhenkai Zhu
 */
public interface Reader extends Observer
{
  /**
   * This is called when the reader is set to the EntityStream
   *
   * @param rh the ReadHandle {@link ReadHandle} provided to this reader.
   */
  void onInit(ReadHandle rh);

}
