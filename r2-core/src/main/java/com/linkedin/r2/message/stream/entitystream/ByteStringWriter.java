package com.linkedin.r2.message.stream.entitystream;

import com.linkedin.data.ByteString;
import com.linkedin.util.ArgumentUtil;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A writer that produce content based on the ByteString body
 */
public class ByteStringWriter implements Writer
{
  private final ByteString _content;
  private final AtomicBoolean _done;
  private WriteHandle _wh;

  public ByteStringWriter(ByteString content)
  {
    ArgumentUtil.notNull(content, "content");
    _content = content;
    _done = new AtomicBoolean(false);
  }

  @Override
  public void onInit(WriteHandle wh)
  {
    _wh = wh;
  }

  @Override
  public void onWritePossible()
  {
    while(_wh.remaining() > 0)
    {
      if (_done.compareAndSet(false, true))
      {
        _wh.write(_content);
      }
      else
      {
        _wh.done();
        break;
      }
    }
  }

  @Override
  public void onAbort(Throwable ex)
  {
    // do nothing
  }
}
