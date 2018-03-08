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

package com.linkedin.data;

import com.linkedin.entitystream.CollectingReader;
import com.linkedin.entitystream.EntityStream;
import com.linkedin.entitystream.EntityStreams;
import com.linkedin.entitystream.Writer;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;


public class TestChunkedByteStringWriter
{
  private static final byte[] DATA = new byte[256];
  static
  {
    for (int i = 0; i < 256; i++)
    {
      DATA[i] = (byte) i;
    }
  }

  @DataProvider
  public Object[][] data()
  {
    return new Object[][]
        {
            {1},
            {127},
            {128},
            {255},
            {256},
            {512},
        };
  }

  @Test(dataProvider = "data")
  public void testWrite(int chunkSize)
      throws InterruptedException, ExecutionException, TimeoutException
  {
    Writer<ByteString> writer = new ChunkedByteStringWriter(DATA, chunkSize);
    EntityStream<ByteString> stream = EntityStreams.newEntityStream(writer);
    CollectingReader<ByteString, ?, ChunkedByteStringCollector.Result> reader = new CollectingReader<>(new ChunkedByteStringCollector());
    stream.setReader(reader);

    ChunkedByteStringCollector.Result result = reader.getResult().toCompletableFuture().get();
    Assert.assertEquals(result.data, DATA);
    Assert.assertEquals(result.chunkCount, (DATA.length - 1) / chunkSize + 1);
  }
}
