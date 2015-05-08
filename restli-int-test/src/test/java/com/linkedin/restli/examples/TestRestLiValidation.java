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
import com.linkedin.restli.common.BatchCreateIdResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.CreateStatus;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.GreetingMap;
import com.linkedin.restli.examples.greetings.api.MyItemArray;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.api.ValidationDemo;
import com.linkedin.restli.examples.greetings.api.myEnum;
import com.linkedin.restli.examples.greetings.api.myItem;
import com.linkedin.restli.examples.greetings.api.myRecord;
import com.linkedin.restli.examples.greetings.client.AutoValidationDemosBuilders;
import com.linkedin.restli.examples.greetings.client.AutoValidationDemosRequestBuilders;
import com.linkedin.restli.examples.greetings.client.ValidationDemosBuilders;
import com.linkedin.restli.examples.greetings.client.ValidationDemosRequestBuilders;
import com.linkedin.restli.server.validation.RestLiInputValidationFilter;
import com.linkedin.restli.server.validation.RestLiOutputValidationFilter;
import com.linkedin.restli.test.util.PatchBuilder;
import com.linkedin.restli.test.util.RootBuilderWrapper;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    super.init(Arrays.asList(new RestLiInputValidationFilter()), Arrays.asList(new RestLiOutputValidationFilter()));
    _restClientAuto = getClient();
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @DataProvider
  private static Object[][] manualBuilders()
  {
    return new Object[][]
        {
            {new ValidationDemosBuilders()},
            {new ValidationDemosRequestBuilders()}
        };
  }

  @DataProvider
  private static Object[][] autoBuilders()
  {
    return new Object[][]
        {
            {new AutoValidationDemosBuilders()},
            {new AutoValidationDemosRequestBuilders()}
        };
  }

  private Object[][] manualClientsAndBuilders()
  {
    return new Object[][] {
        {_restClientManual, new ValidationDemosBuilders()},
        {_restClientManual, new ValidationDemosRequestBuilders()}
    };
  }

  private Object[][] autoClientsAndBuilders()
  {
    return new Object[][] {
        {_restClientAuto, new AutoValidationDemosBuilders()},
        {_restClientAuto, new AutoValidationDemosRequestBuilders()}
    };
  }

  private Object[][] clientsAndBuilders()
  {
    return new Object[][] {
        {_restClientManual, new ValidationDemosBuilders()},
        {_restClientManual, new ValidationDemosRequestBuilders()},
        {_restClientAuto, new AutoValidationDemosBuilders()},
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
            {new ValidationDemo().setStringA("aaa"), "/stringA :: ReadOnly field present in a create request"},
            {new ValidationDemo().setIntA(1234), "/intA :: ReadOnly field present in a create request"},
            {new ValidationDemo().setUnionFieldWithInlineRecord(unionField), "/UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo1 :: ReadOnly field present in a create request"},
            {new ValidationDemo().setArrayWithInlineRecord(myItems), "/ArrayWithInlineRecord/0/bar1 :: ReadOnly field present in a create request"},
            {new ValidationDemo().setValidationDemoNext(new ValidationDemo().setStringB("stringB")), "/validationDemoNext/stringB :: ReadOnly field present in a create request"},
            {new ValidationDemo().setValidationDemoNext(new ValidationDemo().setUnionFieldWithInlineRecord(unionField)), "/validationDemoNext/UnionFieldWithInlineRecord :: ReadOnly field present in a create request"},
            // A field that is CreateOnly and required has to be present in a create request
            {new ValidationDemo(), "/stringB :: field is required but not found and has no default value"},
            {new ValidationDemo().setStringB("bbb"), "/UnionFieldWithInlineRecord :: field is required but not found and has no default value"},
            // Required fields without Rest.li data annotations should be present in a create request
            {new ValidationDemo().setArrayWithInlineRecord(myItems), "/ArrayWithInlineRecord/0/bar2 :: field is required but not found and has no default value"},
            {new ValidationDemo().setMapWithTyperefs(greetingMap), "/MapWithTyperefs/key1/id :: field is required but not found and has no default value"},
            {new ValidationDemo().setValidationDemoNext(new ValidationDemo()), "/validationDemoNext/stringA :: field is required but not found and has no default value"},
            {new ValidationDemo(), "/UnionFieldWithInlineRecord :: field is required but not found and has no default value"}
        };
  }

  @DataProvider
  public Object[][] provideCreateFailureData()
  {
    return wrapFailureCases(createFailures(), clientsAndBuilders());
  }

  @Test(dataProvider = "provideCreateFailureData")
  public void testCreateFailure(RestClient restClient, Object builder, ValidationDemo validationDemo, String errorMessage) throws RemoteInvocationException
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
  public void testBatchCreateManualFailure(List<ValidationDemo> validationDemos, List<String> errorMessages) throws RemoteInvocationException
  {
    Response<CollectionResponse<CreateStatus>> response = _restClientManual.sendRequest(new RootBuilderWrapper<Integer, ValidationDemo>(new ValidationDemosBuilders()).batchCreate().inputs(validationDemos).build()).getResponse();
    List<CreateStatus> results = response.getEntity().getElements();
    int i = 0;
    for (CreateStatus result : results)
    {
      Assert.assertEquals((int) result.getStatus(), HttpStatus.S_422_UNPROCESSABLE_ENTITY.getCode());
      Assert.assertTrue(result.getError().getMessage().contains(errorMessages.get(i++)));
    }
    response = _restClientManual.sendRequest(new RootBuilderWrapper<Integer, ValidationDemo>(new ValidationDemosRequestBuilders()).batchCreate().inputs(validationDemos).build()).getResponse();
    @SuppressWarnings("unchecked")
    List<CreateIdStatus<Integer>> results2 = ((BatchCreateIdResponse<Integer>) (Object) response.getEntity()).getElements();
    i = 0;
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
  public void testBatchCreateAutoFailure(RestClient restClient, Object builder, List<ValidationDemo> validationDemos, List<String> errorMessages) throws RemoteInvocationException
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
    return wrapSuccessCases(createSuccessData(), clientsAndBuilders());
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

  public static String[][] partialUpdateFailures()
  {
    return new String[][]
        {
            // Cannot set ReadOnly or CreateOnly fields in a partial update request
            {"{\"patch\": {\"$set\": {\"stringA\": \"abc\"}}}",
                "ERROR :: /stringA :: ReadOnly field present in a partial_update request\n"},
            {"{\"patch\": {\"$set\": {\"intA\": 123}}}",
                "ERROR :: /intA :: ReadOnly field present in a partial_update request\n"},
            {"{\"patch\": {\"UnionFieldWithInlineRecord\": {\"com.linkedin.restli.examples.greetings.api.myRecord\": {\"$set\": {\"foo1\": 1234}}}}}",
                "ERROR :: /UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo1 :: ReadOnly field present in a partial_update request\n"},
            {"{\"patch\": {\"$set\": {\"ArrayWithInlineRecord\": [{\"bar1\": \"bbb\"}]}}}",
                "ERROR :: /ArrayWithInlineRecord/0/bar1 :: ReadOnly field present in a partial_update request\n"},
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
            {"{\"patch\": {\"$delete\": [\"stringA\"]}}",
                "ERROR :: /stringA :: delete operation on a ReadOnly field is forbidden\n"},
            {"{\"patch\": {\"$delete\": [\"intA\"]}}",
                "ERROR :: /intA :: delete operation on a ReadOnly field is forbidden\n"},
            {"{\"patch\": {\"UnionFieldWithInlineRecord\": {\"com.linkedin.restli.examples.greetings.api.myRecord\": {\"$delete\": [\"foo1\"]}}}}",
                "ERROR :: /UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo1 :: delete operation on a ReadOnly field is forbidden\n"},
            {"{\"patch\": {\"validationDemoNext\": {\"$delete\": [\"stringB\"]}}}",
                "ERROR :: /validationDemoNext/stringB :: delete operation on a ReadOnly field is forbidden\n"},
            {"{\"patch\": {\"validationDemoNext\": {\"$delete\": [\"UnionFieldWithInlineRecord\"]}}}}",
                "ERROR :: /validationDemoNext/UnionFieldWithInlineRecord :: delete operation on a ReadOnly field is forbidden\n"},
            {"{\"patch\": {\"$delete\": [\"stringB\"]}}",
                "ERROR :: /stringB :: delete operation on a CreateOnly field is forbidden\n"},
            {"{\"patch\": {\"$delete\": [\"intB\"]}}",
                "ERROR :: /intB :: delete operation on a CreateOnly field is forbidden\n"},
            {"{\"patch\": {\"UnionFieldWithInlineRecord\": {\"com.linkedin.restli.examples.greetings.api.myRecord\": {\"$delete\": [\"foo2\"]}}}}",
                "ERROR :: /UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo2 :: delete operation on a CreateOnly field is forbidden\n"},
            {"{\"patch\": {\"MapWithTyperefs\": {\"key1\": {\"$delete\": [\"id\"]}}}}",
                "ERROR :: /MapWithTyperefs/id :: delete operation on a CreateOnly field is forbidden\n"}
        };
  }

  @DataProvider
  public Object[][] providePartialUpdateFailureData()
  {
    return wrapFailureCases(partialUpdateFailures(), clientsAndBuilders());
  }

  @Test(dataProvider = "providePartialUpdateFailureData")
  public void testPartialUpdateFailure(RestClient restClient, Object builder, String patch, String errorMessage) throws RemoteInvocationException, DataProcessingException
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
                "\"$set\": {\"ArrayWithInlineRecord\": [{\"bar2\": \"bbb\"}]}," +
                "\"MapWithTyperefs\": {\"key1\": {\"$set\": {\"tone\": \"SINCERE\"}}}," +
                "\"validationDemoNext\": {\"$set\": {\"stringA\": \"AAA\"}}}}",
            // Okay to set a field (validationDemoNext) containing a ReadOnly field (validationDemoNext/stringB), as long as the ReadOnly field is not specified
            "{\"patch\": {\"$set\": {\"validationDemoNext\": {\"stringA\": \"some value\"}}}}",
            // Okay to set a field (MapWithTyperefs/key1) containing a CreateOnly field (MapWithTyperefs/key1/id), as long as the CreateOnly field is not specified
            "{\"patch\": {\"MapWithTyperefs\": {\"key1\": {\"$set\": {\"message\": \"some message\", \"tone\": \"SINCERE\"}}}}}",
            // Okay to delete fields containing a ReadOnly field
            "{\"patch\": {\"$delete\": [\"ArrayWithInlineRecord\", \"UnionFieldWithInlineRecord\"]}}",
            // Okay to delete field containing a CreateOnly field
            "{\"patch\": {\"MapWithTyperefs\": {\"$delete\": [\"key1\"]}}}"
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
  public void testBatchPartialUpdateManual(RestClient restClient, Object builder, Map<Integer, PatchRequest<ValidationDemo>> inputs, Map<Integer, String> errorMessages) throws RemoteInvocationException
  {
    Request<BatchKVResponse<Integer, UpdateStatus>> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder).batchPartialUpdate().patchInputs(inputs).build();
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
  public void testBatchPartialUpdate(RestClient restClient, Object builder, Map<Integer, PatchRequest<ValidationDemo>> inputs, Map<Integer, String> errorMessages) throws RemoteInvocationException
  {
    try
    {
      Request<BatchKVResponse<Integer, UpdateStatus>> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder).batchPartialUpdate().patchInputs(inputs).build();
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
            // Required fields should be present in an update request
            {new ValidationDemo().setArrayWithInlineRecord(myItems), "/ArrayWithInlineRecord/0/bar2 :: field is required but not found and has no default value"},
            {new ValidationDemo().setMapWithTyperefs(greetingMap), "/MapWithTyperefs/key1/message :: field is required but not found and has no default value"},
            {new ValidationDemo().setValidationDemoNext(new ValidationDemo()), "/validationDemoNext/stringA :: field is required but not found and has no default value"},
            {new ValidationDemo(), "/UnionFieldWithInlineRecord :: field is required but not found and has no default value"},
            // Data schema annotations such as strlen are validated
            {new ValidationDemo().setStringA("012345678901234"), "/stringA :: length of \"012345678901234\" is out of range 1...10"}
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
  public void testUpdateFailure(RestClient restClient, Object builder, ValidationDemo validationDemo, String errorMessage) throws RemoteInvocationException
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
            // All required fields have to be present, regardless of ReadOnly or CreateOnly annotations
            validationDemo1,
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

  @Test(dataProvider = "manualBuilders")
  public void testGet(Object builder) throws RemoteInvocationException
  {
    Request<ValidationDemo> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder).get().id(1).build();
    Response<ValidationDemo> response = _restClientManual.sendRequest(request).getResponse();
    Assert.assertEquals(response.getStatus(), HttpStatus.S_200_OK.getCode());
  }

  @Test
  public void testBatchGet() throws RemoteInvocationException
  {
    BatchGetRequest<ValidationDemo> request = new ValidationDemosBuilders().batchGet().ids(1, 2, 3).build();
    Response<BatchResponse<ValidationDemo>> response = _restClientManual.sendRequest(request).getResponse();
    Assert.assertEquals(response.getStatus(), HttpStatus.S_200_OK.getCode());

    BatchGetEntityRequest<Integer, ValidationDemo> request2 = new ValidationDemosRequestBuilders().batchGet().ids(1, 2, 3).build();
    Response<BatchKVResponse<Integer, EntityResponse<ValidationDemo>>> response2 = _restClientManual.sendRequest(request2).getResponse();
    Assert.assertEquals(response2.getStatus(), HttpStatus.S_200_OK.getCode());
  }

  @Test(dataProvider = "manualBuilders")
  public void testGetAll(Object builder) throws RemoteInvocationException
  {
    Request<CollectionResponse<ValidationDemo>> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder).getAll().build();
    Response<CollectionResponse<ValidationDemo>> response = _restClientManual.sendRequest(request).getResponse();
    Assert.assertEquals(response.getStatus(), HttpStatus.S_200_OK.getCode());
  }

  @Test(dataProvider = "manualBuilders")
  public void testFinder(Object builder) throws RemoteInvocationException
  {
    Request<CollectionResponse<ValidationDemo>> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder).findBy("search").setQueryParam("intA", 1234).build();
    Response<CollectionResponse<ValidationDemo>> response = _restClientManual.sendRequest(request).getResponse();
    Assert.assertEquals(response.getStatus(), HttpStatus.S_200_OK.getCode());
  }

  @Test(dataProvider = "autoBuilders")
  public void testGetAuto(Object builder) throws RemoteInvocationException
  {
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
    final String errorMessage = ", ERROR :: /UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo1 :: field is required but not found and has no default value\n";
    try
    {
      BatchGetRequest<ValidationDemo> request = new AutoValidationDemosBuilders().batchGet().ids(ids).build();
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
    try
    {
      BatchGetEntityRequest<Integer, ValidationDemo> request2 = new AutoValidationDemosRequestBuilders().batchGet().ids(ids).build();
      _restClientAuto.sendRequest(request2).getResponse();
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

  @Test(dataProvider = "autoBuilders")
  public void testGetAllAuto(Object builder) throws RemoteInvocationException
  {
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

  @Test(dataProvider = "autoBuilders")
  public void testFinderAuto(Object builder) throws RemoteInvocationException
  {
    try
    {
      Request<CollectionResponse<ValidationDemo>> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder).findBy("search").setQueryParam("intA", 1234).build();
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

  // Tests for output validation filter handling exceptions from the resource
  @Test(dataProvider = "autoBuilders")
  public void testGetAutoWithException(Object builder) throws RemoteInvocationException
  {
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

    BatchGetRequest<ValidationDemo> request = new AutoValidationDemosBuilders().batchGet().ids(0, 1).build();
    Response<BatchResponse<ValidationDemo>> response = _restClientAuto.sendRequest(request).getResponse();
    Assert.assertEquals(response.getStatus(), HttpStatus.S_200_OK.getCode());
    Assert.assertEquals((int) response.getEntity().getErrors().get("0").getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode());
    Assert.assertEquals(response.getEntity().getResults().get("1"), expectedResult);
    BatchGetEntityRequest<Integer, ValidationDemo> request2 = new AutoValidationDemosRequestBuilders().batchGet().ids(0, 1).build();
    Response<BatchKVResponse<Integer, EntityResponse<ValidationDemo>>> response2 =_restClientAuto.sendRequest(request2).getResponse();
    Assert.assertEquals(response2.getStatus(), HttpStatus.S_200_OK.getCode());
    Assert.assertEquals((int) response2.getEntity().getErrors().get(0).getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode());
    Assert.assertEquals(response2.getEntity().getResults().get(1).getEntity(), expectedResult);
  }

  @Test(dataProvider = "autoBuilders")
  public void testFinderWithException(Object builder) throws RemoteInvocationException
  {
    try
    {
      Request<CollectionResponse<ValidationDemo>> request = new RootBuilderWrapper<Integer, ValidationDemo>(builder).findBy("search").setQueryParam("intA", 0).build();
      _restClientAuto.sendRequest(request).getResponse();
      Assert.fail("Expected RestLiResponseException");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode());
    }
  }
}
