package com.linkedin.r2.message.stream.entitystream;

/**
 * Writer is the producer of data for an EntityStream {@link com.linkedin.r2.message.stream.entitystream.EntityStream}
 *
 * @author Zhenkai Zhu
 */
public interface Writer
{
  /**
   * This is called when a Reader is set for the EntityStream.
   *
   * @param wh the handle to write data to the EntityStream.
   */
  void onInit(final WriteHandle wh);

  /**
   * Invoked when it it possible to write data.
   *
   * This method will be invoked the first time as soon as data can be written to the WriteHandle.
   * Subsequent invocations will only occur if a call to {@link WriteHandle#remaining()} has returned 0
   * and it has since become possible to write data.
   */
  void onWritePossible();

  /**
   * Invoked when the entity stream is aborted.
   * Usually writer could do clean up to release any resource it has acquired.
   *
   * @param e the throwable that caused the entity stream to abort
   */
  void onAbort(Throwable e);
}
