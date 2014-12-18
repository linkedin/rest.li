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


import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.restli.client.test.TestRecord;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.ResourceSpecImpl;

import java.util.Collections;
import java.util.EnumSet;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestRequest
{
  @Test
  public void testToSecureString()
  {
    final ResourceSpec spec = new ResourceSpecImpl(
        EnumSet.allOf(ResourceMethod.class),
        Collections.<String, DynamicRecordMetadata> emptyMap(),
        Collections.<String, DynamicRecordMetadata> emptyMap(),
        Long.class,
        null,
        null,
        TestRecord.class,
        Collections.<String, Class<?>> emptyMap());
    GetRequestBuilder<Long, TestRecord> builder = new GetRequestBuilder<Long, TestRecord>(
        "abc",
        TestRecord.class,
        spec,
        RestliRequestOptions.DEFAULT_OPTIONS);

    Request<TestRecord> request = builder.id(5L).build();

    Assert.assertEquals(
        request.toSecureString(),
        "com.linkedin.restli.client.GetRequest{_method=get, _baseUriTemplate=abc, _methodName=null, _requestOptions={_protocolVersionOption: USE_LATEST_IF_AVAILABLE}}");
  }
}
