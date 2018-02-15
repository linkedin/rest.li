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

package com.linkedin.reactivestreams;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.entitystream.CollectingReader;
import com.linkedin.entitystream.EntityStream;
import com.linkedin.entitystream.EntityStreams;
import com.linkedin.entitystream.SingletonWriter;
import com.linkedin.entitystream.Writer;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


public class TestSingletonWriter
{
  @Test
  public void testWrite()
      throws ExecutionException, InterruptedException
  {
    String singleton = "singleton";
    Writer<Object> singltonWriter = new SingletonWriter<>(singleton);
    EntityStream<Object> singletonStream = EntityStreams.newEntityStream(singltonWriter);
    FutureCallback<Set<?>> singletonFuture = new FutureCallback<>();
    singletonStream.setReader(new CollectingReader<>(Collectors.toSet(), singletonFuture));

    Assert.assertEquals(singletonFuture.get(), Collections.singleton(singleton));
  }
}
