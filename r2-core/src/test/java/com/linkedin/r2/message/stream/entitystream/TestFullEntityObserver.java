/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.r2.message.stream.entitystream;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.data.ByteString;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Sean Sheng
 */
public class TestFullEntityObserver
{
  private static final long TIMEOUT = 5000;
  private static final TimeUnit UNIT = TimeUnit.MILLISECONDS;
  private static final ByteString CONTENT = ByteString.copy(new byte[8092]);

  @Test
  public void testSuccess() throws Exception
  {
    final Writer writer = new ByteStringWriter(CONTENT);
    final Reader reader = new DrainReader();
    final FutureCallback<ByteString> callback = new FutureCallback<>();
    final Observer observer = new FullEntityObserver(callback);
    final EntityStream entityStream = EntityStreams.newEntityStream(writer);
    entityStream.addObserver(observer);
    entityStream.setReader(reader);

    final ByteString content = callback.get(TIMEOUT, UNIT);
    Assert.assertSame(content, CONTENT);
  }

  @Test(expectedExceptions = ExecutionException.class)
  public void testError() throws Exception
  {
    final Writer writer = new ByteStringWriter(CONTENT);
    final Reader reader = new CancelingReader();
    final FutureCallback<ByteString> callback = new FutureCallback<>();
    final Observer observer = new FullEntityObserver(callback);
    final EntityStream entityStream = EntityStreams.newEntityStream(writer);
    entityStream.addObserver(observer);
    entityStream.setReader(reader);

    callback.get(TIMEOUT, UNIT);
  }
}
