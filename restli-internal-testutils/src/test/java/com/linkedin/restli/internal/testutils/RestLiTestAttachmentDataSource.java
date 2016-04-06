/*
   Copyright (c) 2016 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.linkedin.restli.internal.testutils;


import com.linkedin.data.ByteString;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.restli.common.attachments.RestLiAttachmentDataSourceWriter;
import com.linkedin.util.ArgumentUtil;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Test utility class to represent a simple attachment for rest.li attachment streaming.
 *
 * To verify that functional progression happens during streaming, the provided payload is suggested to be at least
 * SUGGESTED_MINIMUM_PAYLOAD_SIZE bytes.
 *
 * Subsequent write operations will write WRITE_CHUNK_SIZE bytes at a time.
 *
 * @author Karim Vidhani
 */
public class RestLiTestAttachmentDataSource implements RestLiAttachmentDataSourceWriter
{
  //We suggest 2000 bytes greater then the the default chunk size that R2 uses.
  public static final int SUGGESTED_MINIMUM_PAYLOAD_SIZE = R2Constants.DEFAULT_DATA_CHUNK_SIZE + 2000;
  public static final int WRITE_CHUNK_SIZE = 1000;
  private static final Random random = new Random();

  private final String _attachmentId;
  private final ByteString _payload;
  private final AtomicBoolean _done;
  private WriteHandle _wh;
  private RestLiTestAttachmentDataSourceIterator _parentDataSourceIterator;
  private boolean _aborted = false;
  private int _offset;

  public RestLiTestAttachmentDataSource(final String attachmentID, final ByteString payload)
  {
    _attachmentId = attachmentID;
    _payload = payload;
    _offset = 0;
    ArgumentUtil.notNull(_payload, "_payload");
    _done = new AtomicBoolean(false);
  }

  public static RestLiTestAttachmentDataSource createWithRandomPayload(final String attachmentId)
  {
    return new RestLiTestAttachmentDataSource(attachmentId, generateRandomByteString(SUGGESTED_MINIMUM_PAYLOAD_SIZE));
  }

  @Override
  public String getAttachmentID()
  {
    return _attachmentId;
  }

  @Override
  public void onInit(WriteHandle wh)
  {
    _wh = wh;
  }

  @Override
  public void onWritePossible()
  {
    //Note that a paylaod represented by ByteString.empty() will directly go to _wh.done() which is acceptable.
    while (_wh.remaining() > 0)
    {
      if (_offset == _payload.length())
      {
        _done.set(true);
        _wh.done();
        if (_parentDataSourceIterator != null)
        {
          _parentDataSourceIterator.moveToNextDataSource();
        }
        break;
      }
      else
      {
        final int remaining = _payload.length() - _offset;
        final int amountToWrite;
        if (WRITE_CHUNK_SIZE > remaining)
        {
          amountToWrite = remaining;
        }
        else
        {
          amountToWrite = WRITE_CHUNK_SIZE;
        }
        _wh.write(_payload.slice(_offset, amountToWrite));
        _offset += amountToWrite;
      }
    }
  }

  @Override
  public void onAbort(Throwable e)
  {
    _aborted = true;
  }

  public ByteString getPayload()
  {
    return _payload;
  }

  public boolean finished()
  {
    return _done.get();
  }

  public void setParentDataSourceIterator(final RestLiTestAttachmentDataSourceIterator parentDataSourceIterator)
  {
    _parentDataSourceIterator = parentDataSourceIterator;
  }

  public boolean dataSourceAborted()
  {
    return _aborted;
  }

  public static ByteString generateRandomByteString(final int size)
  {
    final byte[] bytes = new byte[size];
    random.nextBytes(bytes);
    return ByteString.copy(bytes);
  }
}