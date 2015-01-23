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


import com.linkedin.data.template.SetMode;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.testutils.MockBatchEntityResponseFactory;
import com.linkedin.restli.client.testutils.MockBatchKVResponseFactory;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.MyCustomString;
import com.linkedin.restli.common.test.MyCustomStringRef;
import com.linkedin.restli.examples.greetings.api.Greeting;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
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

  private EntityResponse<Greeting> buildEntityResponse(Greeting recordTemplate, HttpStatus status, ErrorResponse errorResponse)
  {
    return new EntityResponse<Greeting>(Greeting.class).setEntity(recordTemplate, SetMode.IGNORE_NULL).
                                                        setStatus(status, SetMode.IGNORE_NULL).
                                                        setError(errorResponse, SetMode.IGNORE_NULL);
  }

  @DataProvider(name = "primitiveKey")
  public Object[][] primitiveKeyData()
  {
    Map<Long, Greeting> recordTemplates = new HashMap<Long, Greeting>();
    Map<Long, ErrorResponse> errorResponses = new HashMap<Long, ErrorResponse>();

    recordTemplates.put(1L, buildGreeting(1L));
    recordTemplates.put(2L, buildGreeting(2L));

    errorResponses.put(3L, new ErrorResponse().setMessage("3"));

    Map<Long, HttpStatus> statuses = new HashMap<Long, HttpStatus>();
    statuses.put(1L, HttpStatus.S_200_OK);
    statuses.put(2L, HttpStatus.S_200_OK);
    statuses.put(3L, HttpStatus.S_500_INTERNAL_SERVER_ERROR);

    Map<Long, EntityResponse<Greeting>> expectedResults = new HashMap<Long, EntityResponse<Greeting>>();
    expectedResults.put(1L, buildEntityResponse(recordTemplates.get(1L), HttpStatus.S_200_OK, null));
    expectedResults.put(2L, buildEntityResponse(recordTemplates.get(2L), HttpStatus.S_200_OK, null));
    expectedResults.put(3L, buildEntityResponse(null, HttpStatus.S_500_INTERNAL_SERVER_ERROR, errorResponses.get(3L)));

    return new Object[][]
    {
        {recordTemplates, statuses, errorResponses, expectedResults}
    };
  }

  @SuppressWarnings("unused")
  @Test(dataProvider = "primitiveKey")
  public void testPrimitiveKey(Map<Long, Greeting> recordTemplates,
                               Map<Long, HttpStatus> statuses,
                               Map<Long, ErrorResponse> errorResponses,
                               Map<Long, EntityResponse<Greeting>> expectedResults)
  {
    BatchKVResponse<Long, Greeting> response = MockBatchKVResponseFactory.createWithPrimitiveKey(Long.class,
                                                                                                 Greeting.class,
                                                                                                 recordTemplates,
                                                                                                 errorResponses);

    Assert.assertEquals(response.getResults(), recordTemplates);
    Assert.assertEquals(response.getErrors(), errorResponses);
  }

  @Test(dataProvider = "primitiveKey")
  public void testPrimitiveKeyEntityResponse(Map<Long, Greeting> recordTemplates,
                                             Map<Long, HttpStatus> statuses,
                                             Map<Long, ErrorResponse> errorResponses,
                                             Map<Long, EntityResponse<Greeting>> expectedResults)
  {
    BatchKVResponse<Long, EntityResponse<Greeting>> response =
        MockBatchEntityResponseFactory.createWithPrimitiveKey(Long.class,
                                                              Greeting.class,
                                                              recordTemplates,
                                                              statuses,
                                                              errorResponses);

    Assert.assertEquals(response.getResults(), expectedResults);
    Assert.assertEquals(response.getErrors(), errorResponses);
  }

  @DataProvider(name = "customPrimitiveTyperefKey")
  public Object[][] customPrimitiveTyperefKeyData()
  {
    MyCustomString m1 = new MyCustomString("1");
    MyCustomString m2 = new MyCustomString("2");
    MyCustomString m3 = new MyCustomString("3");

    Map<MyCustomString, Greeting> recordTemplates = new HashMap<MyCustomString, Greeting>();
    Map<MyCustomString, ErrorResponse> errorResponses = new HashMap<MyCustomString, ErrorResponse>();

    recordTemplates.put(m1, buildGreeting(1L));
    recordTemplates.put(m2, buildGreeting(2L));
    errorResponses.put(m3, new ErrorResponse().setMessage("3"));

    Map<MyCustomString, HttpStatus> statuses = new HashMap<MyCustomString, HttpStatus>();
    statuses.put(m1, HttpStatus.S_200_OK);
    statuses.put(m2, HttpStatus.S_200_OK);
    statuses.put(m3, HttpStatus.S_500_INTERNAL_SERVER_ERROR);

    Map<MyCustomString, EntityResponse<Greeting>> expectedResults = new HashMap<MyCustomString, EntityResponse<Greeting>>();
    expectedResults.put(m1, buildEntityResponse(recordTemplates.get(m1), HttpStatus.S_200_OK, null));
    expectedResults.put(m2, buildEntityResponse(recordTemplates.get(m2), HttpStatus.S_200_OK, null));
    expectedResults.put(m3, buildEntityResponse(null, HttpStatus.S_500_INTERNAL_SERVER_ERROR, errorResponses.get(m3)));

    return new Object[][]
    {
        {recordTemplates, statuses, errorResponses, expectedResults}
    };
  }

  @SuppressWarnings("unused")
  @Test(dataProvider = "customPrimitiveTyperefKey")
  public void testCustomPrimitiveTyperefKey(Map<MyCustomString, Greeting> recordTemplates,
                                            Map<MyCustomString, HttpStatus> statuses,
                                            Map<MyCustomString, ErrorResponse> errorResponses,
                                            Map<MyCustomString, EntityResponse<Greeting>> expectedResults)
  {
    BatchKVResponse<MyCustomString, Greeting> response = MockBatchKVResponseFactory.createWithCustomTyperefKey(
        MyCustomString.class,
        MyCustomStringRef.class,
        Greeting.class,
        recordTemplates,
        errorResponses);

    Assert.assertEquals(response.getResults(), recordTemplates);
    Assert.assertEquals(response.getErrors(), errorResponses);
  }

  @Test(dataProvider = "customPrimitiveTyperefKey")
  public void testCustomPrimitiveTyperefKeyEntityResponse(Map<MyCustomString, Greeting> recordTemplates,
                                                          Map<MyCustomString, HttpStatus> statuses,
                                                          Map<MyCustomString, ErrorResponse> errorResponses,
                                                          Map<MyCustomString, EntityResponse<Greeting>> expectedResults)
  {
    BatchKVResponse<MyCustomString, EntityResponse<Greeting>> response = MockBatchEntityResponseFactory.createWithCustomTyperefKey(
        MyCustomString.class,
        MyCustomStringRef.class,
        Greeting.class,
        recordTemplates,
        statuses,
        errorResponses);

    Assert.assertEquals(response.getResults(), expectedResults);
    Assert.assertEquals(response.getErrors(), errorResponses);
  }

  private CompoundKey buildCompoundKey(String part1, int part2)
  {
    return new CompoundKey().append("part1", part1).append("part2", part2);
  }

  @DataProvider(name = "compoundKey")
  public Object[][] compoundKeyData()
  {
    CompoundKey c1 = buildCompoundKey("c1", 1);
    CompoundKey c2 = buildCompoundKey("c2", 2);
    CompoundKey c3 = buildCompoundKey("c3", 3);

    Map<CompoundKey, Greeting> recordTemplates = new HashMap<CompoundKey, Greeting>();
    recordTemplates.put(c1, buildGreeting(1L));
    recordTemplates.put(c2, buildGreeting(2L));

    Map<CompoundKey, ErrorResponse> errorResponses = new HashMap<CompoundKey, ErrorResponse>();
    errorResponses.put(c3, new ErrorResponse().setMessage("3"));

    Map<CompoundKey, HttpStatus> statuses = new HashMap<CompoundKey, HttpStatus>();
    statuses.put(c1, HttpStatus.S_200_OK);
    statuses.put(c2, HttpStatus.S_200_OK);
    statuses.put(c3, HttpStatus.S_500_INTERNAL_SERVER_ERROR);

    Map<String, CompoundKey.TypeInfo> keyParts = new HashMap<String, CompoundKey.TypeInfo>();
    keyParts.put("part1", new CompoundKey.TypeInfo(String.class, String.class));
    keyParts.put("part2", new CompoundKey.TypeInfo(Integer.class, Integer.class));

    Map<CompoundKey, EntityResponse<Greeting>> expectedResults = new HashMap<CompoundKey, EntityResponse<Greeting>>();
    expectedResults.put(c1, buildEntityResponse(recordTemplates.get(c1), HttpStatus.S_200_OK, null));
    expectedResults.put(c2, buildEntityResponse(recordTemplates.get(c2), HttpStatus.S_200_OK, null));
    expectedResults.put(c3, buildEntityResponse(null, HttpStatus.S_500_INTERNAL_SERVER_ERROR, errorResponses.get(c3)));

    return new Object[][]
    {
        {keyParts, recordTemplates, statuses, errorResponses, expectedResults}
    };
  }

  @SuppressWarnings("unused")
  @Test(dataProvider = "compoundKey")
  public void testCompoundKey(Map<String, CompoundKey.TypeInfo> keyParts,
                              Map<CompoundKey, Greeting> recordTemplates,
                              Map<CompoundKey, HttpStatus> statuses,
                              Map<CompoundKey, ErrorResponse> errorResponses,
                              Map<CompoundKey, EntityResponse<Greeting>> expectedResults)
  {
    BatchKVResponse<CompoundKey, Greeting> response = MockBatchKVResponseFactory.createWithCompoundKey(CompoundKey.class,
                                                                                                       keyParts,
                                                                                                       Greeting.class,
                                                                                                       recordTemplates,
                                                                                                       errorResponses);

    Assert.assertEquals(response.getResults(), recordTemplates);
    Assert.assertEquals(response.getErrors(), errorResponses);
  }

  @Test(dataProvider = "compoundKey")
  public void testCompoundKeyEntityResponse(Map<String, CompoundKey.TypeInfo> keyParts,
                                            Map<CompoundKey, Greeting> recordTemplates,
                                            Map<CompoundKey, HttpStatus> statuses,
                                            Map<CompoundKey, ErrorResponse> errorResponses,
                                            Map<CompoundKey, EntityResponse<Greeting>> expectedResults)
  {
    BatchKVResponse<CompoundKey, EntityResponse<Greeting>> response =
        MockBatchEntityResponseFactory.createWithCompoundKey(CompoundKey.class,
                                                             keyParts,
                                                             Greeting.class,
                                                             recordTemplates,
                                                             statuses,
                                                             errorResponses);

    Assert.assertEquals(response.getResults(), expectedResults);
    Assert.assertEquals(response.getErrors(), errorResponses);
  }

  @DataProvider(name = "complexKey")
  public Object[][] complexKeyData()
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

    Map<ComplexResourceKey<Greeting, Greeting>, HttpStatus> statuses = new HashMap<ComplexResourceKey<Greeting, Greeting>, HttpStatus>();
    statuses.put(new ComplexResourceKey<Greeting, Greeting>(g1, g1), HttpStatus.S_200_OK);
    statuses.put(new ComplexResourceKey<Greeting, Greeting>(g2, g2), HttpStatus.S_200_OK);
    statuses.put(new ComplexResourceKey<Greeting, Greeting>(g3, g3), HttpStatus.S_500_INTERNAL_SERVER_ERROR);

    // Strip the parameters from complex keys in expected results and expected errors.

    Map<ComplexResourceKey<Greeting, Greeting>, Greeting> expectedRecordTemplates =
        new HashMap<ComplexResourceKey<Greeting, Greeting>, Greeting>();
    expectedRecordTemplates.put(new ComplexResourceKey<Greeting, Greeting>(g1, new Greeting()),
        recordTemplates.get(new ComplexResourceKey<Greeting, Greeting>(g1, g1)));
    expectedRecordTemplates.put(new ComplexResourceKey<Greeting, Greeting>(g2, new Greeting()),
        recordTemplates.get(new ComplexResourceKey<Greeting, Greeting>(g2, g2)));

    Map<ComplexResourceKey<Greeting, Greeting>, EntityResponse<Greeting>> expectedResults =
        new HashMap<ComplexResourceKey<Greeting, Greeting>, EntityResponse<Greeting>>();
    expectedResults.put(new ComplexResourceKey<Greeting, Greeting>(g1, new Greeting()),
        buildEntityResponse(recordTemplates.get(new ComplexResourceKey<Greeting, Greeting>(g1, g1)), HttpStatus.S_200_OK, null));
    expectedResults.put(new ComplexResourceKey<Greeting, Greeting>(g2, new Greeting()),
        buildEntityResponse(recordTemplates.get(new ComplexResourceKey<Greeting, Greeting>(g2, g2)), HttpStatus.S_200_OK, null));
    expectedResults.put(new ComplexResourceKey<Greeting, Greeting>(g3, new Greeting()),
        buildEntityResponse(null, HttpStatus.S_500_INTERNAL_SERVER_ERROR, errorResponses.get(new ComplexResourceKey<Greeting, Greeting>(g3, g3))));

    Map<ComplexResourceKey<Greeting, Greeting>, ErrorResponse> expectedErrors =
        new HashMap<ComplexResourceKey<Greeting, Greeting>, ErrorResponse>();
    expectedErrors.put(new ComplexResourceKey<Greeting, Greeting>(g3, new Greeting()),
        errorResponses.get(new ComplexResourceKey<Greeting, Greeting>(g3, g3)));

    return new Object[][]
    {
        {recordTemplates, statuses, errorResponses, expectedRecordTemplates, expectedResults, expectedErrors}
    };
  }

  @SuppressWarnings("unused")
  @Test(dataProvider = "complexKey")
  public void testComplexKey(Map<ComplexResourceKey<Greeting, Greeting>, Greeting> recordTemplates,
                             Map<ComplexResourceKey<Greeting, Greeting>, HttpStatus> statuses,
                             Map<ComplexResourceKey<Greeting, Greeting>, ErrorResponse> errorResponses,
                             Map<ComplexResourceKey<Greeting, Greeting>, Greeting> expectedRecordTemplates,
                             Map<ComplexResourceKey<Greeting, Greeting>, EntityResponse<Greeting>> expectedResults,
                             Map<ComplexResourceKey<Greeting, Greeting>, ErrorResponse> expectedErrors)
  {
    BatchKVResponse<ComplexResourceKey<Greeting, Greeting>, Greeting> response =
        MockBatchKVResponseFactory.createWithComplexKey(Greeting.class,
                                                        Greeting.class,
                                                        Greeting.class,
                                                        recordTemplates,
                                                        errorResponses);

    Assert.assertEquals(response.getResults(), expectedRecordTemplates);
    Assert.assertEquals(response.getErrors(), expectedErrors);
  }

  @SuppressWarnings("unused")
  @Test(dataProvider = "complexKey")
  public void testComplexKeyEntityResponse(Map<ComplexResourceKey<Greeting, Greeting>, Greeting> recordTemplates,
                                           Map<ComplexResourceKey<Greeting, Greeting>, HttpStatus> statuses,
                                           Map<ComplexResourceKey<Greeting, Greeting>, ErrorResponse> errorResponses,
                                           Map<ComplexResourceKey<Greeting, Greeting>, Greeting> expectedRecordTemplates,
                                           Map<ComplexResourceKey<Greeting, Greeting>, EntityResponse<Greeting>> expectedResults,
                                           Map<ComplexResourceKey<Greeting, Greeting>, ErrorResponse> expectedErrors)
  {
    BatchKVResponse<ComplexResourceKey<Greeting, Greeting>, EntityResponse<Greeting>> response =
        MockBatchEntityResponseFactory.createWithComplexKey(Greeting.class,
                                                            Greeting.class,
                                                            Greeting.class,
                                                            recordTemplates,
                                                            statuses,
                                                            errorResponses);

    Assert.assertEquals(response.getResults(), expectedResults);
    Assert.assertEquals(response.getErrors(), expectedErrors);
  }
}
