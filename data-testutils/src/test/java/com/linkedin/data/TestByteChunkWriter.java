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

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.entitystream.CollectingReader;
import com.linkedin.entitystream.EntityStream;
import com.linkedin.entitystream.EntityStreams;
import com.linkedin.entitystream.Writer;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;


public class TestByteChunkWriter
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
    Writer<ByteString> writer = new ByteChunkWriter(DATA, chunkSize);
    EntityStream<ByteString> stream = EntityStreams.newEntityStream(writer);
    FutureCallback<Result> bytesFuture = new FutureCallback<>();
    stream.setReader(new CollectingReader<>(new ByteArrayCollector(), bytesFuture));
    Result result = bytesFuture.get();

    Assert.assertEquals(result.data, DATA);
    Assert.assertEquals(result.chunkCount, (DATA.length - 1) / chunkSize + 1);
  }

  private static class ByteArrayCollector implements Collector<ByteString, ResultContainer, Result>
  {

    @Override
    public Supplier<ResultContainer> supplier()
    {
      return ResultContainer::new;
    }

    @Override
    public BiConsumer<ResultContainer, ByteString> accumulator()
    {
      return this::accumulate;
    }

    private void accumulate(ResultContainer tmpResult, ByteString data)
    {
      try
      {
        tmpResult.os.write(data.copyBytes());
        tmpResult.chunkCount++;
      }
      catch (IOException e)
      {
        throw new RuntimeException(e);
      }
    }

    @Override
    public BinaryOperator<ResultContainer> combiner()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public Function<ResultContainer, Result> finisher()
    {
      return tmpResult -> new Result(tmpResult.os.toByteArray(), tmpResult.chunkCount);
    }

    @Override
    public Set<Characteristics> characteristics()
    {
      return Collections.emptySet();
    }
  }

  private static class ResultContainer
  {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    int chunkCount = 0;
  }

  private static class Result
  {
    byte[] data;
    int chunkCount;

    Result(byte[] data, int chunkCount)
    {
      this.data = data;
      this.chunkCount = chunkCount;
    }
  }
}
