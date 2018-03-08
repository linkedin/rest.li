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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;


/**
 * A {@link Collector} of {@link ByteString}. It concatenates the bytes.
 */
public class ChunkedByteStringCollector implements Collector<ByteString, ChunkedByteStringCollector.ResultContainer, ChunkedByteStringCollector.Result>
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

  static class ResultContainer
  {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    int chunkCount = 0;
  }

  public static class Result
  {
    public byte[] data;
    public int chunkCount;

    Result(byte[] data, int chunkCount)
    {
      this.data = data;
      this.chunkCount = chunkCount;
    }
  }
}
