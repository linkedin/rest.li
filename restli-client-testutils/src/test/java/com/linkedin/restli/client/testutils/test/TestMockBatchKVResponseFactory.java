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

package com.linkedin.restli.client.testutils.test;


import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.testutils.MockBatchKVResponseFactory;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.MyCustomString;
import com.linkedin.restli.common.test.MyCustomStringRef;
import com.linkedin.restli.examples.greetings.api.Greeting;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestMockBatchKVResponseFactory
{
  private Greeting buildGreeting(Long id)
  {
    return new Greeting().setId(id).setMessage(id + "");
  }

  @Test
  public void testPrimitiveKey()
  {
    Map<Long, Greeting> recordTemplates = new HashMap<Long, Greeting>();
    Map<Long, ErrorResponse> errorResponses = new HashMap<Long, ErrorResponse>();

    recordTemplates.put(1L, buildGreeting(1L));
    recordTemplates.put(2L, buildGreeting(2L));

    errorResponses.put(3L, new ErrorResponse().setMessage("3"));

    BatchKVResponse<Long, Greeting> response = MockBatchKVResponseFactory.createWithPrimitiveKey(Long.class,
                                                                                                 Greeting.class,
                                                                                                 recordTemplates,
                                                                                                 errorResponses);

    Assert.assertEquals(response.getResults(), recordTemplates);
    Assert.assertEquals(response.getErrors(), errorResponses);
  }

  @Test
  public void testCustomPrimitiveTyperefKey()
  {
    MyCustomString m1 = new MyCustomString("1");
    MyCustomString m2 = new MyCustomString("2");
    MyCustomString m3 = new MyCustomString("3");

    Map<MyCustomString, Greeting> recordTemplates = new HashMap<MyCustomString, Greeting>();
    Map<MyCustomString, ErrorResponse> errorResponses = new HashMap<MyCustomString, ErrorResponse>();

    recordTemplates.put(m1, buildGreeting(1L));
    recordTemplates.put(m2, buildGreeting(2L));
    errorResponses.put(m3, new ErrorResponse().setMessage("3"));

    BatchKVResponse<MyCustomString, Greeting> response = MockBatchKVResponseFactory.createWithCustomTyperefKey(
        MyCustomString.class,
        MyCustomStringRef.class,
        Greeting.class,
        recordTemplates,
        errorResponses);

    Assert.assertEquals(response.getResults(), recordTemplates);
    Assert.assertEquals(response.getErrors(), errorResponses);
  }

  private CompoundKey buildCompoundKey(String part1, int part2)
  {
    return new CompoundKey().append("part1", part1).append("part2", part2);
  }

  @Test
  public void testCompoundKey()
  {
    CompoundKey c1 = buildCompoundKey("c1", 1);
    CompoundKey c2 = buildCompoundKey("c2", 2);
    CompoundKey c3 = buildCompoundKey("c3", 3);

    Map<CompoundKey, Greeting> recordTemplates = new HashMap<CompoundKey, Greeting>();
    recordTemplates.put(c1, buildGreeting(1L));
    recordTemplates.put(c2, buildGreeting(2L));

    Map<CompoundKey, ErrorResponse> errorResponses = new HashMap<CompoundKey, ErrorResponse>();
    errorResponses.put(c3, new ErrorResponse().setMessage("3"));

    Map<String, CompoundKey.TypeInfo> keyParts = new HashMap<String, CompoundKey.TypeInfo>();
    keyParts.put("part1", new CompoundKey.TypeInfo(String.class, String.class));
    keyParts.put("part2", new CompoundKey.TypeInfo(Integer.class, Integer.class));

    BatchKVResponse<CompoundKey, Greeting> response = MockBatchKVResponseFactory.createWithCompoundKey(CompoundKey.class,
                                                                                                       keyParts,
                                                                                                       Greeting.class,
                                                                                                       recordTemplates,
                                                                                                       errorResponses);

    Assert.assertEquals(response.getResults(), recordTemplates);
    Assert.assertEquals(response.getErrors(), errorResponses);
  }

  @Test
  public void testComplexKey()
  {
    Map<ComplexResourceKey<Greeting, Greeting>, Greeting> recordTemplates =
        new HashMap<ComplexResourceKey<Greeting, Greeting>, Greeting>();
    Map<ComplexResourceKey<Greeting, Greeting>, ErrorResponse> errorResponses =
        new HashMap<ComplexResourceKey<Greeting, Greeting>, ErrorResponse>();

    Greeting g1 = buildGreeting(1L);
    Greeting g2 = buildGreeting(2L);
    Greeting g3 = buildGreeting(3L);

    recordTemplates.put(new ComplexResourceKey<Greeting, Greeting>(g1, g1), g1);
    recordTemplates.put(new ComplexResourceKey<Greeting, Greeting>(g2, g2), g2);

    errorResponses.put(new ComplexResourceKey<Greeting, Greeting>(g3, g3), new ErrorResponse().setMessage("3"));

    BatchKVResponse<ComplexResourceKey, Greeting> response =
        MockBatchKVResponseFactory.createWithComplexKey(Greeting.class,
                                                        Greeting.class,
                                                        Greeting.class,
                                                        recordTemplates,
                                                        errorResponses);

    Map<ComplexResourceKey, Greeting> storedResults = new HashMap<ComplexResourceKey, Greeting>();
    for (Map.Entry<ComplexResourceKey<Greeting, Greeting>, Greeting> entry: recordTemplates.entrySet())
    {
      storedResults.put(new ComplexResourceKey<Greeting, Greeting>(entry.getKey().getKey(),
                                                                   new Greeting()), entry.getValue());
    }

    Map<ComplexResourceKey, ErrorResponse> storedErrorResponses = new HashMap<ComplexResourceKey, ErrorResponse>();
    storedErrorResponses.put(new ComplexResourceKey<Greeting, Greeting>(g3, new Greeting()),
                             new ErrorResponse().setMessage("3"));

    Assert.assertEquals(response.getResults(), storedResults);
    Assert.assertEquals(response.getErrors(), storedErrorResponses);
  }
}
