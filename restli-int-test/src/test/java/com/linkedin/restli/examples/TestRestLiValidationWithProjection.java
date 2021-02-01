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

import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.common.BatchCreateIdEntityResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.IdEntityResponse;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.api.ValidationDemo;
import com.linkedin.restli.examples.greetings.api.myEnum;
import com.linkedin.restli.examples.greetings.client.ActionsRequestBuilders;
import com.linkedin.restli.examples.greetings.client.AutoValidationWithProjectionRequestBuilders;
import com.linkedin.restli.server.validation.RestLiValidationFilter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Integration tests for Rest.li data validation with conjunction of rest.li projection.
 *
 * @author jnchen
 */
public class TestRestLiValidationWithProjection extends RestLiIntegrationTest
{
  public static final String EXPECTED_VALIDATION_DEMO_FAILURE_MESSAGE =
      "ERROR :: /validationDemoNext/UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo1 :: field is required but not found and has no default value\n"
          + "ERROR :: /validationDemoNext/stringA :: length of \"invalid, length is larger than the max\" is out of range 1...10\n"
          + "ERROR :: /validationDemoNext/ArrayWithInlineRecord/0/bar2 :: field is required but not found and has no default value\n"
          + "ERROR :: /validationDemoNext/MapWithTyperefs/foo/message :: field is required but not found and has no default value\n"
          + "ERROR :: /validationDemoNext/MapWithTyperefs/foo/tone :: field is required but not found and has no default value\n"
          + "ERROR :: /validationDemoNext/stringB :: field is required but not found and has no default value\n"
          + "ERROR :: /includedA :: length of \"invalid, length is larger than the max\" is out of range 1...10\n"
          + "ERROR :: /UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo1 :: field is required but not found and has no default value\n"
          + "ERROR :: /ArrayWithInlineRecord/0/bar2 :: field is required but not found and has no default value\n"
          + "ERROR :: /MapWithTyperefs/foo/message :: field is required but not found and has no default value\n"
          + "ERROR :: /MapWithTyperefs/foo/tone :: field is required but not found and has no default value\n"
          + "ERROR :: /stringA :: field is required but not found and has no default value\n";
  private RestClient _restClientAuto;

  @BeforeClass
  public void initClass() throws Exception
  {
    super.init();
    super.init(Arrays.asList(new RestLiValidationFilter()));
    _restClientAuto = getClient();
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @Test
  public void testNoProjection() throws RemoteInvocationException
  {
    Request<CollectionResponse<ValidationDemo>> request = new AutoValidationWithProjectionRequestBuilders()
        .findBySearchWithProjection()
        .build();

    try {
      _restClientAuto.sendRequest(request).getResponse();
    } catch (RestLiResponseException e) {
      Assert.assertEquals(e.getServiceErrorMessage(), EXPECTED_VALIDATION_DEMO_FAILURE_MESSAGE);
    }
  }

  @DataProvider
  private Object[][] provideProjectionWithValidFieldsBuilders()
  {
    List<PathSpec> spec = Arrays.asList(
        ValidationDemo.fields().stringB(),
        ValidationDemo.fields().includedB(),
        ValidationDemo.fields().UnionFieldWithInlineRecord().MyRecord().foo2(),
        // Add a wildcard for projecting the rest of the union members
        new PathSpec(ValidationDemo.fields().UnionFieldWithInlineRecord().getPathComponents(), PathSpec.WILDCARD),
        ValidationDemo.fields().ArrayWithInlineRecord().items().bar1(),
        ValidationDemo.fields().MapWithTyperefs().values().id(),
        ValidationDemo.fields().validationDemoNext().intB());

    AutoValidationWithProjectionRequestBuilders builders = new AutoValidationWithProjectionRequestBuilders();

    Request<CollectionResponse<ValidationDemo>> findRequest = builders
        .findBySearchWithProjection()
        .fields(spec.toArray(new PathSpec[spec.size()]))
        .build();

    Request<ValidationDemo> getRequest =
        builders.get().id(1).fields(spec.toArray(new PathSpec[spec.size()])).build();

    Request<ValidationDemo> getRequestWithArrayRange =
        builders.get().id(1).fields(ValidationDemo.fields().ArrayWithInlineRecord(10, 20)).build();

    Request<CollectionResponse<ValidationDemo>> getAllRequest =
        builders.getAll().fields(spec.toArray(new PathSpec[spec.size()])).build();

    // Valid input for CreateAndGet
    ValidationDemo.UnionFieldWithInlineRecord unionField = new ValidationDemo.UnionFieldWithInlineRecord();
    unionField.setMyEnum(myEnum.FOOFOO);
    ValidationDemo validDemo = new ValidationDemo().setStringB("b").setUnionFieldWithInlineRecord(unionField);

    Request<IdEntityResponse<Integer, ValidationDemo>> createAndGetRequest =
        builders.createAndGet().input(validDemo).fields(spec.toArray(new PathSpec[spec.size()])).build();

    Request<BatchCreateIdEntityResponse<Integer, ValidationDemo>> batchCreateAndGetRequest =
        builders.batchCreateAndGet().inputs(Arrays.asList(validDemo)).fields(spec.toArray(new PathSpec[spec.size()])).build();

    return new Object[][] {
        { findRequest, HttpStatus.S_200_OK },
        { getRequest, HttpStatus.S_200_OK },
        { getRequestWithArrayRange, HttpStatus.S_200_OK },
        { getAllRequest, HttpStatus.S_200_OK },
        { createAndGetRequest, HttpStatus.S_201_CREATED },
        { batchCreateAndGetRequest, HttpStatus.S_200_OK }
    };
  }

  @Test(dataProvider = "provideProjectionWithValidFieldsBuilders")
  public void testProjectionWithValidFields(Request<?> request, HttpStatus expectedStatus) throws RemoteInvocationException
  {
    Response<?> response = _restClientAuto.sendRequest(request).getResponse();
    Assert.assertEquals(response.getStatus(), expectedStatus.getCode());
  }

  @Test
  public void testProjectionWithInvalidFields() throws RemoteInvocationException
  {
    Request<CollectionResponse<ValidationDemo>> request =
        new AutoValidationWithProjectionRequestBuilders().findBySearchWithProjection()
            .fields(ValidationDemo.fields().stringA(), //invalid
                    ValidationDemo.fields().stringB(),
                    ValidationDemo.fields().includedA(), //invalid
                    ValidationDemo.fields().UnionFieldWithInlineRecord().MyRecord().foo1(), //invalid
                    ValidationDemo.fields().UnionFieldWithInlineRecord().MyRecord().foo2(),
                    ValidationDemo.fields().ArrayWithInlineRecord().items().bar1(),
                    ValidationDemo.fields().ArrayWithInlineRecord().items().bar2(), //invalid
                    ValidationDemo.fields().MapWithTyperefs().values().id(),
                    ValidationDemo.fields().MapWithTyperefs().values().tone(), //invalid
                    ValidationDemo.fields().validationDemoNext().stringA()) //invalid
            .build();
    try {
      _restClientAuto.sendRequest(request).getResponse();
    } catch (RestLiResponseException e) {
      Assert.assertEquals(e.getServiceErrorMessage(),
                          "ERROR :: /validationDemoNext/stringA :: length of \"invalid, length is larger than the max\" is out of range 1...10\n" +
                              "ERROR :: /includedA :: length of \"invalid, length is larger than the max\" is out of range 1...10\n" +
                              "ERROR :: /UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo1 :: field is required but not found and has no default value\n" +
                              "ERROR :: /ArrayWithInlineRecord/0/bar2 :: field is required but not found and has no default value\n" +
                              "ERROR :: /MapWithTyperefs/foo/tone :: field is required but not found and has no default value\n" +
                              "ERROR :: /stringA :: field is required but not found and has no default value\n");
    }
  }

  @DataProvider
  private Object[][] provideProjectionWithNonexistentFieldsData()
  {
    AutoValidationWithProjectionRequestBuilders builders = new AutoValidationWithProjectionRequestBuilders();

    Request<ValidationDemo> getRequest =
        builders.get().id(1).fields(new PathSpec("nonexistentFieldFooBar")).build();

    Request<CollectionResponse<ValidationDemo>> getAllRequest =
        builders.getAll().fields(new PathSpec("nonexistentFieldFooBar")).build();

    Request<CollectionResponse<ValidationDemo>> findRequest =
        builders.findBySearchWithProjection().fields(new PathSpec("nonexistentFieldFooBar")).build();

    return new Object[][]
    {
        { getRequest },
        { getAllRequest },
        { findRequest }
    };
  }

  @Test(dataProvider = "provideProjectionWithNonexistentFieldsData")
  public void testProjectionWithNonexistentFields(Request<?> request) throws RemoteInvocationException
  {
    RecordDataSchema schema = (RecordDataSchema) DataTemplateUtil.getSchema(ValidationDemo.class);
    try
    {
      _restClientAuto.sendRequest(request).getResponse();
      Assert.fail("Building schema by projection with nonexistent fields should return an HTTP 400 error");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode());
      Assert.assertEquals(e.getServiceErrorMessage(), "Projected field \"nonexistentFieldFooBar\" not present in schema \"" + schema.getFullName() + "\"");
    }
  }

  /**
   * Ensures that projections are ignored completely in the validating filter and do not result in any errors for
   * actions resource ACTION requests.
   *
   * This test is motivated by an NPE that occurred when a {@link com.linkedin.restli.server.annotations.RestLiActions}
   * resource was queried with an ACTION request containing a projection. In this case, since the resource has
   * no value class and since a previous implementation of {@link RestLiValidationFilter} relied on the value class
   * for all resource methods, an NPE would be thrown. Now, the value class will only be accessed for resource methods
   * that require validation on response.
   * @throws RemoteInvocationException from the client
   */
  @Test
  public void testActionsResourceIgnoreProjection() throws RemoteInvocationException
  {
    Message.Fields fields = Message.fields();

    Message message = new Message()
        .setId("ktvz")
        .setMessage("Cheesecake")
        .setTone(Tone.SINCERE);

    Request<Message> req = new ActionsRequestBuilders()
        .actionEchoMessage()
        .messageParam(message)
        .setParam(RestConstants.FIELDS_PARAM, new HashSet<>(Arrays.asList(fields.message(), fields.tone())))
        .build();

    Message result = _restClientAuto.sendRequest(req).getResponseEntity();

    Assert.assertEquals(result, message);
  }
}
