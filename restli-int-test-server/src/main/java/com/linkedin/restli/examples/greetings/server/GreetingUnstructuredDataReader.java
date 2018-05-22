/*
   Copyright (c) 2018 LinkedIn Corp.

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
package com.linkedin.restli.examples.greetings.server;

import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;
import com.linkedin.entitystream.ReadHandle;
import com.linkedin.entitystream.Reader;
import com.linkedin.restli.server.CreateResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * A {@link Reader} implementation that uses a {@link ByteBuffer} to collect the entities in a stream and build a
 * {@link CreateResponse} object.
 */
public class GreetingUnstructuredDataReader implements Reader<ByteString>
{
  private ReadHandle _rh;
  private ByteArrayOutputStream _dataStorage = new ByteArrayOutputStream();
  private Callback<CreateResponse> _callback;

  public GreetingUnstructuredDataReader(Callback<CreateResponse> cb)
  {
    _callback = cb;
  }

  @Override
  public void onInit(ReadHandle rh)
  {
    _rh = rh;
    rh.request(1);
  }

  @Override
  public void onDataAvailable(ByteString data)
  {
    try
    {
      _dataStorage.write(data.copyBytes());
    }
    catch (IOException e)
    {
      // fail to write data in reader.
      _callback.onError(e);
    }
    _rh.request(1);
  }

  @Override
  public void onDone()
  {
    _callback.onSuccess(new CreateResponse(1));
  }

  @Override
  public void onError(Throwable e)
  {
    _callback.onError(e);
  }
}
