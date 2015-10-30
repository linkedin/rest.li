package com.linkedin.r2.message.stream.entitystream;

import com.linkedin.data.ByteString;


/**
 * A reader to cancel an unstarted {@link com.linkedin.r2.message.stream.entitystream.EntityStream} and
 * abort its underlying {@link com.linkedin.r2.message.stream.entitystream.Writer}.
 *
 * @author Ang Xu
 */
public final class CancelingReader implements Reader
{
  @Override
  public void onInit(ReadHandle rh)
  {
    rh.cancel();
  }

  @Override
  public void onDataAvailable(ByteString data)
  {
  }

  @Override
  public void onDone()
  {
  }

  @Override
  public void onError(Throwable e)
  {
  }
}
