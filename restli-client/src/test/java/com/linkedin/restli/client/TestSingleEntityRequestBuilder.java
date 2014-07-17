/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.client;


import com.linkedin.restli.client.test.TestRecord;
import com.linkedin.restli.common.ComplexResourceKey;

import org.testng.Assert;
import org.testng.annotations.Test;


public class TestSingleEntityRequestBuilder
{
  @Test()
  public void testIdReadOnliness()
  {
    SingleEntityRequestBuilder<Object, TestRecord, ?> builder = new DummySingleEntityRequestBuilder();
    ComplexResourceKey<TestRecord, TestRecord> originalKey =
        new ComplexResourceKey<TestRecord, TestRecord>(
            new TestRecord(), new TestRecord());
    builder.id(originalKey);

    Assert.assertNotSame(builder.buildReadOnlyId(), originalKey);
    originalKey.makeReadOnly();
    Assert.assertSame(builder.buildReadOnlyId(), originalKey);
  }

  @Test()
  public void testInputReadOnliness()
  {
    SingleEntityRequestBuilder<Object, TestRecord, ?> builder = new DummySingleEntityRequestBuilder();
    TestRecord originalInput = new TestRecord();
    builder.input(originalInput);

    Assert.assertNotSame(builder.buildReadOnlyInput(), originalInput);
    originalInput.data().makeReadOnly();
    Assert.assertSame(builder.buildReadOnlyInput(), originalInput);
  }

  private static class DummySingleEntityRequestBuilder
      extends SingleEntityRequestBuilder<Object, TestRecord, Request<Object>>
  {
    public DummySingleEntityRequestBuilder()
    {
      super(null, TestRecord.class, null, RestliRequestOptions.DEFAULT_OPTIONS);
    }

    @Override
    public Request<Object> build()
    {
      return null;
    }
  }
}
