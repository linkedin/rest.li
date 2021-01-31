/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.examples;

import com.linkedin.data.DataMap;
import com.linkedin.data.element.DataElement;
import com.linkedin.data.message.Message;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.schema.validator.AbstractValidator;
import com.linkedin.data.schema.validator.Validator;
import com.linkedin.data.schema.validator.ValidatorContext;
import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.BatchGetEntityRequest;
import com.linkedin.restli.client.BatchGetRequest;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.response.CreateResponse;
import com.linkedin.restli.common.BatchCollectionResponse;
import com.linkedin.restli.common.BatchCreateIdResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.CreateStatus;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.IdEntityResponse;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.common.validation.RestLiDataValidator;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.GreetingMap;
import com.linkedin.restli.examples.greetings.api.MyItemArray;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.api.ValidationDemo;
import com.linkedin.restli.examples.greetings.api.ValidationDemoCriteria;
import com.linkedin.restli.examples.greetings.api.myEnum;
import com.linkedin.restli.examples.greetings.api.myItem;
import com.linkedin.restli.examples.greetings.api.myRecord;
import com.linkedin.restli.examples.greetings.client.AutoValidationDemosRequestBuilders;
import com.linkedin.restli.examples.greetings.client.ValidationDemosRequestBuilders;
import com.linkedin.restli.server.validation.RestLiValidationFilter;
import com.linkedin.restli.test.util.PatchBuilder;
import com.linkedin.restli.test.util.RootBuilderWrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Integration tests for Rest.li data validation.
 * Sends requests to ValidationDemoResource and AutomaticValidationDemoResource,
 * and checks that valid requests are accepted and invalid ones are not.
 *
 * For validating data from the server, the "validationDemos" resource manually validates
 * outgoing data and fixes it, so the client will get a regular response.
 * However, the "autoValidationDemos" resource uses a filter to validate outgoing data,
 * so the client will get an error response.
 *
 * @author Soojung Ha
 */
public class TestRestLiValidation extends RestLiIntegrationTest
{
  private RestClient _restClientManual;
  private RestClient _restClientAuto;

  @BeforeClass
  public void initClass() throws Exception
  {
    super.init();
    _restClientManual = getClient();
    super.init(Arrays.asList(new RestLiValidationFilter()));
    _restClientAuto = getClient();
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  private Object[][] manualClientsAndBuilders()
  {
    return new Object[][] {
        {_restClientManual, new ValidationDemosRequestBuilders()}
    };
  }

  private Object[][] autoClientsAndBuilders()
  {
    return new Object[][] {
        {_restClientAuto, new AutoValidationDemosRequestBuilders()}
    };
  }

  private Object[][] clientsAndBuilders()
  {
    return new Object[][] {
        {_restClientManual, new ValidationDemosRequestBuilders()},
        {_restClientAuto, new AutoValidationDemosRequestBuilders()}
    };
  }

  private static Object[][] wrapFailureCases(Object[][] failureCases, Object[][] clientsAndBuilders)
  {
    Object[][] result = new Object[clientsAndBuilders.length * failureCases.length][4];
    for (int i = 0; i < failureCases.length; i++)
    {
      for (int j = 0; j < clientsAndBuilders.length; j++)
      {
        result[clientsAndBuilders.length * i + j][0] = clientsAndBuilders[j][0];
        result[clientsAndBuilders.length * i + j][1] = clientsAndBuilders[j][1];
        result[clientsAndBuilders.length * i + j][2] = failureCases[i][0];
        result[clientsAndBuilders.length * i + j][3] = failureCases[i][1];
      }
    }
    return result;
  }

  private static Object[][] wrapSuccessCases(Object[] successCases, Object[][] clientsAndBuilders)
  {
    Object[][] result = new Object[clientsAndBuilders.length * successCases.length][3];
    for (int i = 0; i < successCases.length; i++)
    {
      for (int j = 0; j < clientsAndBuilders.length; j++)
      {
        result[clientsAndBuilders.length * i + j][0] = clientsAndBuilders[j][0];
        result[clientsAndBuilders.length * i + j][1] = clientsAndBuilders[j][1];
        result[clientsAndBuilders.length * i + j][2] = successCases[i];
      }
    }
    return result;
  }

  public static Object[][] createFailures()
  {
    ValidationDemo.UnionFieldWithInlineRecord unionField = new ValidationDemo.UnionFieldWithInlineRecord();
    unionField.setMyRecord(new myRecord().setFoo1(111));
    MyItemArray myItems = new MyItemArray();
    myItems.add(new myItem().setBar1("bar1"));
    GreetingMap greetingMap = new GreetingMap();
    greetingMap.put("key1", new Greeting());
    return new Object[][]
        {
            // ReadOnly fields should not be specified in a create request, whether they are required or optional
            {new ValidationDemo().setStringA("aaa"),
                "/stringA :: ReadOnly field present in a create request"},
            {new ValidationDemo().setIntA(1234),
                "/intA :: ReadOnly field present in a create request"},
            {new ValidationDemo().setUnionFieldWithInlineRecord(unionField),
                "/UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo1 :: ReadOnly field present in a create request"},
            {new ValidationDemo().setArrayWithInlineRecord(myItems),
                "/ArrayWithInlineRecord/0/bar1 :: ReadOnly field present in a create request"},
            {new ValidationDemo().setValidationDemoNext(new ValidationDemo().setStringB("stringB")),
                "/validationDemoNext/stringB :: ReadOnly field present in a create request"},
            {new ValidationDemo().setValidationDemoNext(new ValidationDemo().setUnionFieldWithInlineRecord(unionField)),
                "/validationDemoNext/UnionFieldWithInlineRecord :: ReadOnly field present in a create request"},
            // A field that is CreateOnly and required has to be present in a create request
            {new ValidationDemo(),
                "/stringB :: field is required but not found and has no default value"},
            {new ValidationDemo().setStringB("bbb"),
                "/UnionFieldWithInlineRecord :: field is required but not found and has no default value"},
            // Required fields without Rest.li data annotations should be present in a create request
            {new ValidationDemo().setArrayWithInlineRecord(myItems),
                "/ArrayWithInlineRecord/0/bar2 :: field is required but not found and has no default value"},
            {new ValidationDemo().setMapWithTyperefs(greetingMap),
                "/MapWithTyperefs/key1/id :: field is required but not found and has no default value"},
            {new ValidationDemo().setValidationDemoNext(new ValidationDemo()),
                "/validationDemoNext/stringA :: field is required but not found and has no default value"},
            {new ValidationDemo(),
                "/UnionFieldWithInlineRecord :: field is required but not found and has no default value"}
        };
  }

  @DataProvider
  public Object[][] provideCreateFailureData()
  {
    return wrapFailureCases(createFailures(), clientsAndBuilders());
  }

  @Test(dataProvider = "provideCreateFailureData")
  public void testCreateFailure(RestClient restClient, Object builder, ValidationDemo validationDemo,
                                String errorMessage) throws RemoteInvocationException
  {
    try
    {
      restClient.sendRequest(new RootBuilderWrapper<Integer, ValidationDemo>(builder).create().input(validationDemo).build()).getResponse();
      Assert.fail("Expected RestLiResponseException");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_422_UNPROCESSABLE_ENTITY.getCode());
      Assert.assertTrue(e.getServiceErrorMessage().contains(errorMessage));
    }
  }

  @DataProvider
  public static Object[][] batchCreateFailureData()
  {
    List<ValidationDemo> validationDemos = new ArrayList<ValidationDemo>();
    List<String> errorMessages = new ArrayList<String>();
    Object[][] cases = createFailures();
    for (int i = 0; i < cases.length; i++)
    {
      validationDemos.add((ValidationDemo) cases[i][0]);
      errorMessages.add(((String) cases[i][1]).replaceFirst("create", "batch_create"));
    }
    return new Object[][]{
        {validationDemos, errorMessages}
    };
  }

  @Test(dataProvider = "batchCreateFailureData")
  public void testBatchCreateManualFailure(List<ValidationDemo> validationDemos, List<String> errorMessages)
      throws RemoteInvocationException
  {
    Response<CollectionResponse<CreateStatus>> response = _restClientManual.sendRequest(new RootBuilderWrapper<Integer, ValidationDemo>(
        new ValidationDemosRequestBuilders()).batchCreate().inputs(validationDemos).build()).getResponse();
    @SuppressWarnings("unchecked")
    List<CreateIdStatus<Integer>> results2 = ((BatchCreateIdResponse<Integer>) (Object) response.getEntity()).getElements();
    int i = 0;
    for (CreateIdStatus<Integer> result : results2)
    {
      Assert.assertEquals((int) result.getStatus(), HttpStatus.S_422_UNPROCESSABLE_ENTITY.getCode());
      Assert.assertTrue(result.getError().getMessage().contains(errorMessages.get(i++)));
    }
  }

  @DataProvider
  private Object[][] provideBatchCreateAutoFailureData()
  {
    return wrapFailureCases(batchCreateFailureData(), autoClientsAndBuilders());
  }

  @Test(dataProvider = "provideBatchCreateAutoFailureData")
  public void testBatchCreateAutoFailure(RestClient restClient, Object builder, List<ValidationDemo> validationDemos,
                                         List<String> errorMessages) throws RemoteInvocationException
  {
    try
    {
      restClient.sendRequest(new RootBuilderWrapper<Integer, ValidationDemo>(builder).batchCreate().inputs(validationDemos).build()).getResponse();
      Assert.fail("Expected RestLiResponseException");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_422_UNPROCESSABLE_ENTITY.getCode());
      for (String message : errorMessages)
      {
        Assert.assertTrue(e.getServiceErrorMessage().contains(message));
      }
    }
  }

  private static Object[][] batchCreateAndGetFailures()
  {
    ValidationDemo.UnionFieldWithInlineRecord unionField = new ValidationDemo.UnionFieldWithInlineRecord();
    unionField.setMyEnum(myEnum.FOOFOO);
    return new Object[][]
        {
            {new ValidationDemo().setStringB("b1").setUnionFieldWithInlineRecord(unionField),
                "ERROR :: /UnionFieldWithInlineRecord :: field is required but not found and has no default value"},
            {new ValidationDemo().setStringB("b2").setUnionFieldWithInlineRecord(unionField),
                "ERROR :: /UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo1 " +
                    ":: field is required but not found and has no default value"}
        };
  }

  @DataProvider
  public static Object[][] batchCreateAndGetFailureData()
  {
    List<ValidationDemo> validationDemos = new ArrayList<ValidationDemo>();
    List<String> errorMessages = new ArrayList<String>();
    Object[][] cases = batchCreateAndGetFailures();
    for (int i = 0; i < cases.length; i++)
    {
      validationDemos.add((ValidationDemo) cases[i][0]);
      errorMessages.add(((String) cases[i][1]).replaceFirst("create", "batch_create"));
    }
    return new Object[][]
        {
            {validationDemos, errorMessages}
        };
  }

  @DataProvider
  private Object[][] provideBatchCreateAndGetAutoFailureData()
  {
    return wrapFailureCases(batchCreateAndGetFailureData(), autoClientsAndBuilders());
  }

  @Test(dataProvider = "provideBatchCreateAndGetAutoFailureData")
  public void testBatchCreateAndGetAutoFailure(RestClient restClient, Object builder, List<ValidationDemo> validationDemos,
                                               List<String> errorMessages) throws RemoteInvocationException
  {
    // Batch create succeeds, but batch get fails.
    try
    {
      restClient.sendRequest(new RootBuilderWrapper<Integer, ValidationDemo>(builder).batchCreate().inputs(validationDemos).build()).getResponse();
      Assert.fail("Expected RestLiResponseException");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
      for (String message : errorMessages)
      {
        Assert.assertTrue(e.getServiceErrorMessage().contains(message));
      }
    }
  }

  public static Object[] createSuccessData()
  {
    ValidationDemo.UnionFieldWithInlineRecord unionField1 = new ValidationDemo.UnionFieldWithInlineRecord();
    unionField1.setMyEnum(myEnum.FOOFOO);
    ValidationDemo validationDemo1 = new ValidationDemo().setStringB("some string").setUnionFieldWithInlineRecord(unionField1);
    ValidationDemo.UnionFieldWithInlineRecord unionField2 = new ValidationDemo.UnionFieldWithInlineRecord();
    unionField2.setMyRecord(new myRecord());
    MyItemArray myItems = new MyItemArray();
    myItems.add(new myItem().setBar2("bar2"));
    // ReadOnly fields can be missing even if they are required.
    ValidationDemo validationDemo2 = new ValidationDemo().setStringB("bbb")
        .setUnionFieldWithInlineRecord(unionField2).setArrayWithInlineRecord(myItems)
        .setValidationDemoNext(new ValidationDemo().setStringA("aaa"));
    return new Object[]{validationDemo1, validationDemo2};
  }

  @DataProvider
  public Object[][] provideCreateSuccessData()
  {
    return wrapSuccessCases(createSuccessData(), manualClientsAndBuilders());
  }

  @Test(dataProvider = "provideCreateSuccessData")
  @SuppressWarnings("unchecked")
  public void testCreateSuccess(RestClient restClient, Object builder, ValidationDemo validationDemo) throws RemoteInvocationException
  {
    Request<EmptyRecord> createRequest = new RootBuilderWrapper<Integer, ValidationDemo>(builder).create().input(validationDemo).build();
    Response<EmptyRecord> response = restClient.sendRequest(createRequest).getResponse();
    Assert.assertEquals(response.getStatus(), HttpStatus.S_201_CREATED.getCode());
    if (response.getEntity() instanceof CreateResponse)
    {
      Assert.assertEquals(((CreateResponse<Integer>)response.getEntity()).getId(), new Integer(1234));
    }
    else
    {
      Assert.assertEquals(((IdResponse<Integer>)(Object)response.getEntity()).getId(), new Integer(1234));
    }
  }

  @DataProvider
  public Object[][] provideCreateAndGetSuccessData()
  {
    return wrapSuccessCases(createSuccessData(), autoClientsAndBuilders());
  }

  @Test(dataProvider = "provideCreateAndGetSuccessData")
  @SuppressWarnings("unchecked")
  public void testCreateAndGetAutoSuccess(RestClient restClient, Object builder, ValidationDemo validationDemo) throws RemoteInvocationException
  {
    Request<IdEntityResponse<Integer, ValidationDemo>> createRequest = new RootBuilderWrapper<Integer, ValidationDemo>(builder)
        .createAndGet().input(validationDemo).build();
    Response<IdEntityResponse<Integer, ValidationDemo>> response = restClient.sendRequest(createRequest).getResponse();

    ValidationDemo.UnionFieldWithInlineRecord unionField = new ValidationDemo.UnionFieldWithInlineRecord();
    unionField.setMyEnum(myEnum.FOOFOO);
    ValidationDemo expected = new ValidationDemo().setStringA("a").setStringB("b").setUnionFieldWithInlineRecord(unionField);
    Assert.assertEquals(response.getStatus(), HttpStatus.S_201_CREATED.getCode());
    Assert.assertEquals(response.getEntity().getEntity(), expected);
  }

  public static Object[][] createAndGetFailureData()
  {
    ValidationDemo.UnionFieldWithInlineRecord unionField = new ValidationDemo.UnionFieldWithInlineRecord();
    unionField.setMyEnum(myEnum.BARBAR);
    ValidationDemo validationDemo = new ValidationDemo().setStringB("some string").setUnionFieldWithInlineRecord(unionField);
    return new Object[][]
        {{validationDemo, "ERROR :: /stringA :: field is required but not found and has no default value\n"}};
  }

  @DataProvider
  public Object[][] provideCreateAndGetFailureData()
  {
    return wrapFailureCases(createAndGetFailureData(), autoClientsAndBuilders());
  }

  @Test(dataProvider = "provideCreateAndGetFailureData")
  @SuppressWarnings("unchecked")
  public void testCreateAndGetAutoFailure(RestClient restClient, Object builder, ValidationDemo validationDemo,
                                          String errorMessage) throws RemoteInvocationException
  {
    // Create succeeds, but get fails.
    Request<IdEntityResponse<Integer, ValidationDemo>> createRequest = new RootBuilderWrapper<Integer, ValidationDemo>(builder)
        .createAndGet().input(validationDemo).build();
    try
    {
      restClient.sendRequest(createRequest).getResponse();
      Assert.fail("Expected RestLiResponseException");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
      Assert.assertEquals(e.getServiceErrorMessage(), errorMessage);
    }
  }

  public static String[][] partialUpdateFailures()
  {
    return new String[][]
        {
            // Required fields cannot be missing in a new record
            {"{\"patch\": {\"validationDemoNext\": {\"$set\": {\"MapWithTyperefs\": {\"key1\": {\"id\": 1234, \"message\": \"msg\"}}}}}}",
                "ERROR :: /validationDemoNext/MapWithTyperefs/key1/tone :: field is required but not found and has no default value\n"},
            {"{\"patch\": {\"validationDemoNext\": {\"MapWithTyperefs\": {\"$set\": {\"key1\": {\"id\": 1234, \"message\": \"msg\"}}}}}}",
                "ERROR :: /validationDemoNext/MapWithTyperefs/key1/tone :: field is required but not found and has no default value\n"},
            // Cannot delete required fields
            {"{\"patch\": {\"$delete\": [\"UnionFieldWithInlineRecord\"]}}",
                "ERROR :: /UnionFieldWithInlineRecord :: cannot delete a required field\n"},
            {"{\"patch\": {\"validationDemoNext\": {\"$delete\": [\"stringA\"]}}}",
                "ERROR :: /validationDemoNext/stringA :: cannot delete a required field\n"},
            {"{\"patch\": {\"MapWithTyperefs\": {\"key1\": {\"$delete\": [\"message\"]}}}}",
                "ERROR :: /MapWithTyperefs/key1/message :: cannot delete a required field\n"},
            // Cannot set ReadOnly or CreateOnly fields in a partial_update request (unless the field is the descendant of an array)
            {"{\"patch\": {\"$set\": {\"stringA\": \"abc\"}}}",
                "ERROR :: /stringA :: ReadOnly field present in a partial_update request\n"},
            {"{\"patch\": {\"$set\": {\"intA\": 123}}}",
                "ERROR :: /intA :: ReadOnly field present in a partial_update request\n"},
            {"{\"patch\": {\"UnionFieldWithInlineRecord\": {\"com.linkedin.restli.examples.greetings.api.myRecord\": {\"$set\": {\"foo1\": 1234}}}}}",
                "ERROR :: /UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo1 :: ReadOnly field present in a partial_update request\n"},
            {"{\"patch\": {\"$set\": {\"UnionFieldWithInlineRecord\": {\"com.linkedin.restli.examples.greetings.api.myRecord\": {\"foo1\": 1234}}}}}",
                "ERROR :: /UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo1 :: ReadOnly field present in a partial_update request\n"},
            {"{\"patch\": {\"validationDemoNext\": {\"$set\": {\"stringB\": \"abc\"}}}}",
                "ERROR :: /validationDemoNext/stringB :: ReadOnly field present in a partial_update request\n"},
            {"{\"patch\": {\"validationDemoNext\": {\"UnionFieldWithInlineRecord\": {\"$set\": {\"com.linkedin.restli.examples.greetings.api.myEnum\": \"FOOFOO\"}}}}}",
                "ERROR :: /validationDemoNext/UnionFieldWithInlineRecord :: ReadOnly field present in a partial_update request\n"},
            {"{\"patch\": {\"$set\": {\"stringB\": \"abc\"}}}",
                "ERROR :: /stringB :: CreateOnly field present in a partial_update request\n"},
            {"{\"patch\": {\"$set\": {\"intB\": 123}}}",
                "ERROR :: /intB :: CreateOnly field present in a partial_update request\n"},
            {"{\"patch\": {\"UnionFieldWithInlineRecord\": {\"com.linkedin.restli.examples.greetings.api.myRecord\": {\"$set\": {\"foo2\": 1234}}}}}",
                "ERROR :: /UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo2 :: CreateOnly field present in a partial_update request\n"},
            {"{\"patch\": {\"MapWithTyperefs\": {\"key1\": {\"$set\": {\"id\": 1234}}}}}",
                "ERROR :: /MapWithTyperefs/key1/id :: CreateOnly field present in a partial_update request\n"},
            // Cannot set child (descendant) of ReadOnly or CreateOnly fields in a partial update request
            {"{\"patch\": {\"validationDemoNext\": {\"UnionFieldWithInlineRecord\": {\"com.linkedin.restli.examples.greetings.api.myRecord\": {\"$set\": {\"foo1\": 1234}}}}}}",
                "ERROR :: /validationDemoNext/UnionFieldWithInlineRecord :: ReadOnly field present in a partial_update request\n"},
            // Cannot delete ReadOnly or CreateOnly fields in a partial update request
            {"{\"patch\": {\"$delete\": [\"intA\"]}}",
                "ERROR :: /intA :: cannot delete a ReadOnly field or its descendants\n"},
            {"{\"patch\": {\"$delete\": [\"intB\"]}}",
                "ERROR :: /intB :: cannot delete a CreateOnly field or its descendants\n"},
            {"{\"patch\": {\"UnionFieldWithInlineRecord\": {\"com.linkedin.restli.examples.greetings.api.myRecord\": {\"$delete\": [\"foo2\"]}}}}",
                "ERROR :: /UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo2 :: cannot delete a CreateOnly field or its descendants\n"},
            // Cannot delete child (descendant) of ReadOnly or CreateOnly fields in a partial update request
            {"{\"patch\": {\"validationDemoNext\": {\"UnionFieldWithInlineRecord\": {\"com.linkedin.restli.examples.greetings.api.myRecord\": {\"$delete\": [\"foo2\"]}}}}}",
                "ERROR :: /validationDemoNext/UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo2 :: cannot delete a ReadOnly field or its descendants\n"},
        };
  }

  @DataProvider
  public Object[][] providePartialUpdateFailureData()
  {
    return wrapFailureCases(partialUpdateFailures(), clientsAndBuilders());
  }

  @Test(dataProvider = "providePartialUpdateFailureData")
  public void testPartialUpdateFailure(RestClient restClient, Object builder, String patch, String errorMessage)
      throws RemoteInvocationException, DataProcessingException
  {
    PatchRequest<ValidationDemo> patchRequest = PatchBuilder.buildPatchFromString(patch);
    Request<EmptyRecord> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder).partialUpdate().id(1).input(patchRequest).build();
    try
    {
      restClient.sendRequest(request).getResponse();
      Assert.fail("Expected RestLiResponseException");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_422_UNPROCESSABLE_ENTITY.getCode());
      Assert.assertEquals(e.getServiceErrorMessage(), errorMessage);
    }
  }

  public static String[] partialUpdateSuccesses()
  {
    return new String[]
        {
            // Partial updates are valid if they don't contain ReadOnly or CreateOnly fields
            "{\"patch\": {\"UnionFieldWithInlineRecord\": {\"$set\": {\"com.linkedin.restli.examples.greetings.api.myEnum\": \"FOOFOO\"}}," +
                "\"MapWithTyperefs\": {\"key1\": {\"$set\": {\"tone\": \"SINCERE\"}}}," +
                "\"validationDemoNext\": {\"$set\": {\"stringA\": \"AAA\"}}}}",
            // A field (validationDemoNext) containing a ReadOnly field (validationDemoNext/stringB) has to be partially set
            "{\"patch\": {\"validationDemoNext\": {\"$set\": {\"stringA\": \"some value\"}}}}",
            // A field (MapWithTyperefs/key1) containing a CreateOnly field (MapWithTyperefs/key1/id) has to be partially set
            "{\"patch\": {\"MapWithTyperefs\": {\"key1\": {\"$set\": {\"message\": \"some message\", \"tone\": \"SINCERE\"}}}}}",
            // Okay to set a field containing a ReadOnly field if the ReadOnly field is omitted
            "{\"patch\": {\"$set\": {\"ArrayWithInlineRecord\": [{\"bar2\": \"missing bar1\"}]}}}",
            "{\"patch\": {\"$set\": {\"UnionFieldWithInlineRecord\": {\"com.linkedin.restli.examples.greetings.api.myRecord\": {}}}}}",
            "{\"patch\": {\"$set\": {\"validationDemoNext\": {\"stringA\": \"no stringB\"}}}}",
            // Okay to delete a field containing a ReadOnly field
            "{\"patch\": {\"$delete\": [\"ArrayWithInlineRecord\"]}}",
            // Okay to delete a field containing a CreateOnly field
            "{\"patch\": {\"MapWithTyperefs\": {\"$delete\": [\"key1\"]}}}",
            // Okay to set a ReadOnly field if it's the descendant of an array
            "{\"patch\": {\"$set\": {\"ArrayWithInlineRecord\": [{\"bar1\": \"setting ReadOnly field\", \"bar2\": \"foo\"}]}}}",
            // Okay to set a CreateOnly field if it's the descendant of an array
            "{\"patch\": {\"$set\": {\"ArrayWithInlineRecord\": [{\"bar3\": \"setting CreateOnly field\", \"bar2\": \"foo\"}]}}}"
        };
  }

  @DataProvider
  public Object[][] providePartialUpdateSuccessData()
  {
    return wrapSuccessCases(partialUpdateSuccesses(), clientsAndBuilders());
  }

  @Test(dataProvider = "providePartialUpdateSuccessData")
  public void testPartialUpdateSuccess(RestClient restClient, Object builder, String patch) throws RemoteInvocationException
  {
    PatchRequest<ValidationDemo> patchRequest = PatchBuilder.buildPatchFromString(patch);
    Request<EmptyRecord> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder).partialUpdate().id(1).input(patchRequest).build();
    Response<EmptyRecord> response = restClient.sendRequest(request).getResponse();
    Assert.assertEquals(response.getStatus(), HttpStatus.S_204_NO_CONTENT.getCode());
  }

  @DataProvider
  public static Object[][] batchPartialUpdateData() throws DataProcessingException
  {
    String[][] failures = partialUpdateFailures();
    String[] successes = partialUpdateSuccesses();
    Map<Integer, PatchRequest<ValidationDemo>> inputs = new HashMap<Integer, PatchRequest<ValidationDemo>>();
    Map<Integer, String> errorMessages = new HashMap<Integer, String>();
    for (int i = 0; i < failures.length; i++)
    {
      inputs.put(i, PatchBuilder.<ValidationDemo>buildPatchFromString(failures[i][0]));
      errorMessages.put(i, failures[i][1].replace("partial_update", "batch_partial_update"));
    }
    for (int i = 0; i < successes.length; i++)
    {
      inputs.put(failures.length + i, PatchBuilder.<ValidationDemo>buildPatchFromString(successes[i]));
      errorMessages.put(failures.length + i, "");
    }
    return new Object[][]{{inputs, errorMessages}};
  }

  @DataProvider
  private Object[][] provideBatchPartialUpdateManualData() throws DataProcessingException
  {
    return wrapFailureCases(batchPartialUpdateData(), manualClientsAndBuilders());
  }

  @Test(dataProvider = "provideBatchPartialUpdateManualData")
  public void testBatchPartialUpdateManual(RestClient restClient, Object builder, Map<Integer, PatchRequest<ValidationDemo>> inputs,
                                           Map<Integer, String> errorMessages) throws RemoteInvocationException
  {
    Request<BatchKVResponse<Integer, UpdateStatus>> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder)
        .batchPartialUpdate().patchInputs(inputs).build();
    Response<BatchKVResponse<Integer, UpdateStatus>> response = restClient.sendRequest(request).getResponse();
    for (Map.Entry<Integer, UpdateStatus> entry : response.getEntity().getResults().entrySet())
    {
      String expected = errorMessages.get(entry.getKey());
      if (expected.isEmpty())
      {
        Assert.assertEquals((int) entry.getValue().getStatus(), HttpStatus.S_204_NO_CONTENT.getCode());
      }
      else
      {
        Assert.assertEquals((int) entry.getValue().getStatus(), HttpStatus.S_422_UNPROCESSABLE_ENTITY.getCode());
        Assert.assertEquals(entry.getValue().getError().getMessage(), errorMessages.get(entry.getKey()));
      }
    }
  }

  @DataProvider
  private Object[][] provideBatchPartialUpdateAutoData() throws DataProcessingException
  {
    return wrapFailureCases(batchPartialUpdateData(), autoClientsAndBuilders());
  }

  @Test(dataProvider = "provideBatchPartialUpdateAutoData")
  public void testBatchPartialUpdate(RestClient restClient, Object builder, Map<Integer, PatchRequest<ValidationDemo>> inputs,
                                     Map<Integer, String> errorMessages) throws RemoteInvocationException
  {
    try
    {
      Request<BatchKVResponse<Integer, UpdateStatus>> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder)
          .batchPartialUpdate().patchInputs(inputs).build();
      restClient.sendRequest(request).getResponse();
      Assert.fail("Expected RestLiResponseException");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_422_UNPROCESSABLE_ENTITY.getCode());
      for (String message : errorMessages.values())
      {
        Assert.assertTrue(e.getServiceErrorMessage().contains(message));
      }
    }
  }

  public static Object[][] updateFailures()
  {
    MyItemArray myItems = new MyItemArray();
    myItems.add(new myItem().setBar1("bar1"));
    GreetingMap greetingMap = new GreetingMap();
    greetingMap.put("key1", new Greeting());
    return new Object[][]
        {
            // Required fields even if marked createOnly should be present
            {new ValidationDemo(),
                 "/stringB :: field is required but not found and has no default value"},
            // Required fields should be present in an update request
            {new ValidationDemo().setArrayWithInlineRecord(myItems),
                "/ArrayWithInlineRecord/0/bar2 :: field is required but not found and has no default value"},
            {new ValidationDemo().setMapWithTyperefs(greetingMap),
                "/MapWithTyperefs/key1/message :: field is required but not found and has no default value"},
            {new ValidationDemo().setValidationDemoNext(new ValidationDemo()),
                "/validationDemoNext/stringA :: field is required but not found and has no default value"},
            {new ValidationDemo(),
                "/UnionFieldWithInlineRecord :: field is required but not found and has no default value"},
            // Data schema annotations such as strlen are validated
            {new ValidationDemo().setStringA("012345678901234"),
                "/stringA :: length of \"012345678901234\" is out of range 1...10"}
        };
  }

  @DataProvider
  public Object[][] provideUpdateFailureData()
  {
    return wrapFailureCases(updateFailures(), clientsAndBuilders());
  }

  // For update operations, only data schema annotations are validated.
  // Rest.li annotations such as ReadOnly and CreateOnly have no effect.
  @Test(dataProvider = "provideUpdateFailureData")
  public void testUpdateFailure(RestClient restClient, Object builder, ValidationDemo validationDemo,
                                String errorMessage) throws RemoteInvocationException
  {
    Request<EmptyRecord> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder).update().id(1).input(validationDemo).build();
    try
    {
      restClient.sendRequest(request).getResponse();
      Assert.fail("Expected RestLiResponseException");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_422_UNPROCESSABLE_ENTITY.getCode());
      Assert.assertTrue(e.getServiceErrorMessage().contains(errorMessage));
    }
  }

  /**
   * Required but read-only fields are optional. Required create-only fields must be present.
   */
  public static Object[] updateSuccesses()
  {
    ValidationDemo.UnionFieldWithInlineRecord unionField = new ValidationDemo.UnionFieldWithInlineRecord();
    unionField.setMyRecord(new myRecord().setFoo1(1));
    ValidationDemo validationDemo1 = new ValidationDemo().setStringA("aaa").setStringB("bbb").setUnionFieldWithInlineRecord(unionField);
    ValidationDemo.UnionFieldWithInlineRecord unionField2 = new ValidationDemo.UnionFieldWithInlineRecord();
    unionField2.setMyEnum(myEnum.BARBAR);
    MyItemArray array = new MyItemArray();
    array.add(new myItem().setBar1("BAR1").setBar2("BAR2"));
    array.add(new myItem().setBar1("BAR11").setBar2("BAR22"));
    GreetingMap map = new GreetingMap();
    map.put("key1", new Greeting().setId(1).setMessage("msg").setTone(Tone.FRIENDLY));
    return new Object[]
        {
            // All fields present.
            validationDemo1,
            // ReadOnly fields stringA, intA not present
            new ValidationDemo().setStringB("BBB").setUnionFieldWithInlineRecord(unionField2)
                    .setIntB(5432).setArrayWithInlineRecord(array).setMapWithTyperefs(map).setValidationDemoNext(validationDemo1),
            new ValidationDemo().setStringA("aaa").setStringB("bbb").setUnionFieldWithInlineRecord(unionField2)
                .setIntA(1234).setIntB(5678).setArrayWithInlineRecord(array).setMapWithTyperefs(map).setValidationDemoNext(validationDemo1)
        };
  }

  @DataProvider
  public Object[][] provideUpdateSuccessData()
  {
    return wrapSuccessCases(updateSuccesses(), clientsAndBuilders());
  }

  @Test(dataProvider = "provideUpdateSuccessData")
  public void testUpdateSuccess(RestClient restClient, Object builder, ValidationDemo validationDemo) throws RemoteInvocationException
  {
    Request<EmptyRecord> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder).update().id(1).input(validationDemo).build();
    Response<EmptyRecord> response = restClient.sendRequest(request).getResponse();
    Assert.assertEquals(response.getStatus(), HttpStatus.S_204_NO_CONTENT.getCode());
  }

  @Test
  public void testGet() throws RemoteInvocationException
  {
    Object builder = new ValidationDemosRequestBuilders();
    Request<ValidationDemo> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder).get().id(1).build();
    Response<ValidationDemo> response = _restClientManual.sendRequest(request).getResponse();
    Assert.assertEquals(response.getStatus(), HttpStatus.S_200_OK.getCode());
  }

  @Test
  public void testBatchGet() throws RemoteInvocationException
  {
    BatchGetEntityRequest<Integer, ValidationDemo> request = new ValidationDemosRequestBuilders().batchGet().ids(1, 2, 3).build();
    Response<BatchKVResponse<Integer, EntityResponse<ValidationDemo>>> response = _restClientManual.sendRequest(request).getResponse();
    Assert.assertEquals(response.getStatus(), HttpStatus.S_200_OK.getCode());
  }

  @Test
  public void testGetAll() throws RemoteInvocationException
  {
    Object builder = new ValidationDemosRequestBuilders();
    Request<CollectionResponse<ValidationDemo>> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder).getAll().build();
    Response<CollectionResponse<ValidationDemo>> response = _restClientManual.sendRequest(request).getResponse();
    Assert.assertEquals(response.getStatus(), HttpStatus.S_200_OK.getCode());
  }

  @Test
  public void testFinder() throws RemoteInvocationException
  {
    Object builder = new ValidationDemosRequestBuilders();
    Request<CollectionResponse<ValidationDemo>> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder)
        .findBy("search").setQueryParam("intA", 1234).build();
    Response<CollectionResponse<ValidationDemo>> response = _restClientManual.sendRequest(request).getResponse();
    Assert.assertEquals(response.getStatus(), HttpStatus.S_200_OK.getCode());
  }

  @Test
  public void testBatchFinder() throws RemoteInvocationException
  {
    Object builder = new ValidationDemosRequestBuilders();
    ValidationDemoCriteria c1 = new ValidationDemoCriteria().setIntA(1111).setStringB("hello");
    ValidationDemoCriteria c2 = new ValidationDemoCriteria().setIntA(1111).setStringB("world");

    Request<BatchCollectionResponse<ValidationDemo>> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder)
        .batchFindBy("searchValidationDemos").setQueryParam("criteria", Arrays.asList(c1, c2)).build();
    Response<BatchCollectionResponse<ValidationDemo>> response = _restClientManual.sendRequest(request).getResponse();

    Assert.assertEquals(response.getStatus(), HttpStatus.S_200_OK.getCode());
  }

  @Test
  public void testGetAuto() throws RemoteInvocationException
  {
    Object builder = new AutoValidationDemosRequestBuilders();

    try
    {
      Request<ValidationDemo> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder).get().id(1).build();
      _restClientAuto.sendRequest(request).getResponse();
      Assert.fail("Expected RestLiResponseException");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getServiceErrorMessage(), "ERROR :: /stringA :: length of \"stringA is readOnly\" is out of range 1...10\n" +
          "ERROR :: /stringB :: field is required but not found and has no default value\n");
    }
  }

  @Test
  public void testBatchGetAuto() throws RemoteInvocationException
  {
    final List<Integer> ids = Arrays.asList(11, 22, 33);
    final String errorMessage = ", ERROR :: /UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo1 " +
        ":: field is required but not found and has no default value\n";
    try
    {
      BatchGetEntityRequest<Integer, ValidationDemo> request = new AutoValidationDemosRequestBuilders().batchGet().ids(ids).build();
      _restClientAuto.sendRequest(request).getResponse();
      Assert.fail("Expected RestLiResponseException");
    }
    catch (RestLiResponseException e)
    {
      for (Integer id : ids)
      {
        Assert.assertTrue(e.getServiceErrorMessage().contains("Key: " + id.toString() + errorMessage));
      }
    }
  }

  @Test
  public void testGetAllAuto() throws RemoteInvocationException
  {
    Object builder = new AutoValidationDemosRequestBuilders();

    try
    {
      Request<CollectionResponse<ValidationDemo>> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder).getAll().build();
      _restClientAuto.sendRequest(request).getResponse();
      Assert.fail("Expected RestLiResponseException");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getServiceErrorMessage(), "ERROR :: /stringA :: length of \"This string is too long to pass validation.\" is out of range 1...10\n" +
          "ERROR :: /stringA :: length of \"This string is too long to pass validation.\" is out of range 1...10\n" +
          "ERROR :: /stringA :: length of \"This string is too long to pass validation.\" is out of range 1...10\n" +
          "ERROR :: /stringA :: length of \"This string is too long to pass validation.\" is out of range 1...10\n");
    }
  }

  @Test
  public void testFinderAuto() throws RemoteInvocationException
  {
    Object builder = new AutoValidationDemosRequestBuilders();

    try
    {
      Request<CollectionResponse<ValidationDemo>> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder)
          .findBy("search").setQueryParam("intA", 1234).build();
      _restClientAuto.sendRequest(request).getResponse();
      Assert.fail("Expected RestLiResponseException");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getServiceErrorMessage(), "ERROR :: /stringB :: field is required but not found and has no default value\n" +
          "ERROR :: /stringB :: field is required but not found and has no default value\n" +
          "ERROR :: /stringB :: field is required but not found and has no default value\n");
    }
  }

  @Test
  public void testBatchFinderAutoWithMissingField() throws RemoteInvocationException
  {
    Object builder = new AutoValidationDemosRequestBuilders();

    try
    {
      ValidationDemoCriteria c1 = new ValidationDemoCriteria().setIntA(1111).setStringB("hello");
      ValidationDemoCriteria c2 = new ValidationDemoCriteria().setIntA(4444).setStringB("world");

      Request<BatchCollectionResponse<ValidationDemo>> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder)
          .batchFindBy("searchValidationDemos").setQueryParam("criteria", Arrays.asList(c1, c2)).build();
      _restClientAuto.sendRequest(request).getResponse();
      Assert.fail("Expected RestLiResponseException");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getServiceErrorMessage(), "BatchCriteria: 0 Element: 0 ERROR :: /stringB :: field is required but not found and has no default value\n" +
          "BatchCriteria: 0 Element: 1 ERROR :: /stringB :: field is required but not found and has no default value\n" +
          "BatchCriteria: 0 Element: 2 ERROR :: /stringB :: field is required but not found and has no default value\n");
    }
  }

  @Test
  public void testBatchFinderAutoWithOverLengthField() throws RemoteInvocationException
  {
    Object builder = new AutoValidationDemosRequestBuilders();

    try
    {
      ValidationDemoCriteria c1 = new ValidationDemoCriteria().setIntA(2222).setStringB("hello");
      ValidationDemoCriteria c2 = new ValidationDemoCriteria().setIntA(4444).setStringB("world");

      Request<BatchCollectionResponse<ValidationDemo>> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder)
          .batchFindBy("searchValidationDemos").setQueryParam("criteria", Arrays.asList(c1, c2)).build();
      _restClientAuto.sendRequest(request).getResponse();
      Assert.fail("Expected RestLiResponseException");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getServiceErrorMessage(), "BatchCriteria: 0 Element: 0 ERROR :: /stringA :: length of \"longLengthValueA\" is out of range 1...10\n" +
          "BatchCriteria: 0 Element: 1 ERROR :: /stringA :: length of \"longLengthValueA\" is out of range 1...10\n" +
          "BatchCriteria: 0 Element: 2 ERROR :: /stringA :: length of \"longLengthValueA\" is out of range 1...10\n");
    }
  }

  @Test
  public void testBatchFinderAutoWithMultipleErrorFields() throws RemoteInvocationException
  {
    Object builder = new AutoValidationDemosRequestBuilders();

    try
    {
      ValidationDemoCriteria c1 = new ValidationDemoCriteria().setIntA(3333).setStringB("hello");
      ValidationDemoCriteria c2 = new ValidationDemoCriteria().setIntA(4444).setStringB("world");

      Request<BatchCollectionResponse<ValidationDemo>> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder)
          .batchFindBy("searchValidationDemos").setQueryParam("criteria", Arrays.asList(c1, c2)).build();
      _restClientAuto.sendRequest(request).getResponse();
      Assert.fail("Expected RestLiResponseException");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getServiceErrorMessage(), "BatchCriteria: 0 Element: 0 ERROR :: /stringA :: length of \"longLengthValueA\" is out of range 1...10\n" +
          "ERROR :: /stringB :: field is required but not found and has no default value\n" +
          "BatchCriteria: 0 Element: 1 ERROR :: /stringA :: length of \"longLengthValueA\" is out of range 1...10\n" +
          "ERROR :: /stringB :: field is required but not found and has no default value\n" +
          "BatchCriteria: 0 Element: 2 ERROR :: /stringA :: length of \"longLengthValueA\" is out of range 1...10\n" +
          "ERROR :: /stringB :: field is required but not found and has no default value\n");
    }
  }


  @Test
  public void testBatchFinderAutoWithErrorCriteriaResult() throws RemoteInvocationException
  {
    Object builder = new AutoValidationDemosRequestBuilders();

    ValidationDemoCriteria c1 = new ValidationDemoCriteria().setIntA(5555).setStringB("hello");
    ValidationDemoCriteria c2 = new ValidationDemoCriteria().setIntA(4444).setStringB("world");

    Request<BatchCollectionResponse<ValidationDemo>> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder)
        .batchFindBy("searchValidationDemos").setQueryParam("criteria", Arrays.asList(c1, c2)).build();
    _restClientAuto.sendRequest(request).getResponse();
    Response<BatchCollectionResponse<ValidationDemo>> response = _restClientAuto.sendRequest(request).getResponse();
    Assert.assertEquals(response.getStatus(), HttpStatus.S_200_OK.getCode());
  }


  // Tests for output validation filter handling exceptions from the resource
  @Test
  public void testGetAutoWithException() throws RemoteInvocationException
  {
    Object builder = new AutoValidationDemosRequestBuilders();

    try
    {
      Request<ValidationDemo> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder).get().id(0).build();
      _restClientAuto.sendRequest(request).getResponse();
      Assert.fail("Expected RestLiResponseException");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode());
    }
  }

  @Test
  public void testBatchGetAutoWithException() throws RemoteInvocationException
  {
    // The resource returns an error for id=0 but a normal result for id=1
    ValidationDemo.UnionFieldWithInlineRecord union = new ValidationDemo.UnionFieldWithInlineRecord();
    union.setMyRecord(new myRecord().setFoo1(100).setFoo2(200));
    ValidationDemo expectedResult = new ValidationDemo().setStringA("a").setStringB("b").setUnionFieldWithInlineRecord(union);

    BatchGetEntityRequest<Integer, ValidationDemo> request = new AutoValidationDemosRequestBuilders().batchGet().ids(0, 1).build();
    Response<BatchKVResponse<Integer, EntityResponse<ValidationDemo>>> response =_restClientAuto.sendRequest(request).getResponse();
    Assert.assertEquals(response.getStatus(), HttpStatus.S_200_OK.getCode());
    Assert.assertEquals((int) response.getEntity().getErrors().get(0).getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode());
    Assert.assertEquals(response.getEntity().getResults().get(1).getEntity(), expectedResult);
  }

  @Test
  public void testFinderWithException() throws RemoteInvocationException
  {
    Object builder = new AutoValidationDemosRequestBuilders();

    try
    {
      Request<CollectionResponse<ValidationDemo>> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder)
          .findBy("search").setQueryParam("intA", 0).build();
      _restClientAuto.sendRequest(request).getResponse();
      Assert.fail("Expected RestLiResponseException");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode());
    }
  }

  @Test
  public void testCustomValidatorMap()
  {
    // Provide Rest.li annotations manually since the validator is not called from the server or through generated request builders.
    Map<String, List<String>> annotations = new HashMap<>();
    annotations.put("createOnly", Arrays.asList("stringB", "intB", "UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo2", "MapWithTyperefs/*/id"));
    annotations.put("readOnly", Arrays.asList("stringA", "intA", "UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo1", "ArrayWithInlineRecord/*/bar1", "validationDemoNext/stringB", "validationDemoNext/UnionFieldWithInlineRecord"));

    // Invalid entity, because intB is not a multiple of seven.
    ValidationDemo.UnionFieldWithInlineRecord unionField1 = new ValidationDemo.UnionFieldWithInlineRecord();
    unionField1.setMyEnum(myEnum.FOOFOO);
    ValidationDemo entity = new ValidationDemo().setIntB(24).setStringB("some string").setUnionFieldWithInlineRecord(unionField1);

    // Validate without the class map
    RestLiDataValidator validator = new RestLiDataValidator(annotations, ValidationDemo.class, ResourceMethod.CREATE);
    ValidationResult result = validator.validateInput(entity);
    Assert.assertTrue(result.isValid());

    // Validate with the class map
    Map<String, Class<? extends Validator>> validatorClassMap = new HashMap<>();
    validatorClassMap.put("seven", SevenValidator.class);
    validator = new RestLiDataValidator(annotations, ValidationDemo.class, ResourceMethod.CREATE, validatorClassMap);
    result = validator.validateInput(entity);
    Assert.assertFalse(result.isValid());
    Assert.assertEquals(result.getMessages().size(), 1);
    for (Message message : result.getMessages())
    {
      Assert.assertTrue(message.toString().contains("24 is not a multiple of seven"));
    }
  }

  public static class SevenValidator extends AbstractValidator
  {
    public SevenValidator(DataMap config)
    {
      super(config);
    }

    @Override
    public void validate(ValidatorContext context)
    {
      DataElement element = context.dataElement();
      Integer value = (Integer) element.getValue();
      if (value % 7 != 0)
      {
        context.addResult(new Message(element.path(), "%d is not a multiple of seven", value));
      }
    }
  }
}
