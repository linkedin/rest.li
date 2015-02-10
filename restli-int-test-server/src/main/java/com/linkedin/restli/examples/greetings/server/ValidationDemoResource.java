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

package com.linkedin.restli.examples.greetings.server;


import com.linkedin.data.message.Message;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.validation.CreateOnly;
import com.linkedin.restli.common.validation.ReadOnly;
import com.linkedin.restli.common.validation.RestLiDataValidator;
import com.linkedin.restli.examples.greetings.api.ValidationDemo;
import com.linkedin.restli.examples.greetings.api.myEnum;
import com.linkedin.restli.examples.greetings.api.myRecord;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.annotations.ValidatorParam;
import com.linkedin.restli.server.resources.KeyValueResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Free-form resource for testing Rest.li data validation.
 * This class shows how to validate data manually by injecting the validator as a resource method parameter.
 * Outgoing data that fails validation is corrected before it is sent to the client.
 *
 * @author Soojung Ha
 */
@RestLiCollection(name = "validationDemos", namespace = "com.linkedin.restli.examples.greetings.client")
@ReadOnly({"stringA", "intA", "UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo1",
           "ArrayWithInlineRecord/bar1", "validationDemoNext/stringB", "validationDemoNext/UnionFieldWithInlineRecord"})
@CreateOnly({"stringB", "intB", "UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo2",
             "MapWithTyperefs/id"})
public class ValidationDemoResource implements KeyValueResource<Integer, ValidationDemo>
{
  @RestMethod.Create
  public CreateResponse create(final ValidationDemo entity, @ValidatorParam RestLiDataValidator validator)
  {
    ValidationResult result = validator.validate(entity);
    if (!result.isValid())
    {
      throw new RestLiServiceException(HttpStatus.S_422_UNPROCESSABLE_ENTITY, result.getMessages().toString());
    }
    return new CreateResponse(1234);
  }

  @RestMethod.BatchCreate
  public BatchCreateResult<Integer, ValidationDemo> batchCreate(final BatchCreateRequest<Integer, ValidationDemo> entities,
                                                                @ValidatorParam RestLiDataValidator validator)
  {
    List<CreateResponse> results = new ArrayList<CreateResponse>();
    int id = 0;
    for (ValidationDemo entity : entities.getInput())
    {
      ValidationResult result = validator.validate(entity);
      if (result.isValid())
      {
        results.add(new CreateResponse(id));
        id++;
      }
      else
      {
        results.add(new CreateResponse(new RestLiServiceException(HttpStatus.S_422_UNPROCESSABLE_ENTITY, result.getMessages().toString())));
      }
    }
    return new BatchCreateResult<Integer, ValidationDemo>(results);
  }

  @RestMethod.Update
  public UpdateResponse update(final Integer key, final ValidationDemo entity, @ValidatorParam RestLiDataValidator validator)
  {
    ValidationResult result = validator.validate(entity);
    if (!result.isValid())
    {
      throw new RestLiServiceException(HttpStatus.S_422_UNPROCESSABLE_ENTITY, result.getMessages().toString());
    }
    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  @RestMethod.BatchUpdate
  public BatchUpdateResult<Integer, ValidationDemo> batchUpdate(final BatchUpdateRequest<Integer, ValidationDemo> entities,
                                                                @ValidatorParam RestLiDataValidator validator)
  {
    Map<Integer, UpdateResponse> results = new HashMap<Integer, UpdateResponse>();
    Map<Integer, RestLiServiceException> errors = new HashMap<Integer, RestLiServiceException>();
    for (Map.Entry<Integer, ValidationDemo> entry : entities.getData().entrySet())
    {
      Integer key = entry.getKey();
      ValidationDemo entity = entry.getValue();
      ValidationResult result = validator.validate(entity);
      if (result.isValid())
      {
        results.put(key, new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
      }
      else
      {
        errors.put(key, new RestLiServiceException(HttpStatus.S_422_UNPROCESSABLE_ENTITY, result.getMessages().toString()));
      }
    }
    return new BatchUpdateResult<Integer, ValidationDemo>(results, errors);
  }

  @RestMethod.PartialUpdate
  public UpdateResponse update(final Integer key, final PatchRequest<ValidationDemo> patch,
                               @ValidatorParam RestLiDataValidator validator)
  {
    ValidationResult result = validator.validate(patch);
    if (!result.isValid())
    {
      for (Message message : result.getMessages())
      {
        System.out.println(message);
      }
      throw new RestLiServiceException(HttpStatus.S_422_UNPROCESSABLE_ENTITY, result.getMessages().toString());
    }
    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  @RestMethod.BatchPartialUpdate
  public BatchUpdateResult<Integer, ValidationDemo> batchUpdate(final BatchPatchRequest<Integer, ValidationDemo> entityUpdates,
                                                                @ValidatorParam RestLiDataValidator validator)
  {
    Map<Integer, UpdateResponse> results = new HashMap<Integer, UpdateResponse>();
    Map<Integer, RestLiServiceException> errors = new HashMap<Integer, RestLiServiceException>();
    for (Map.Entry<Integer, PatchRequest<ValidationDemo>> entry : entityUpdates.getData().entrySet())
    {
      Integer key = entry.getKey();
      PatchRequest<ValidationDemo> patch = entry.getValue();
      ValidationResult result = validator.validate(patch);
      if (result.isValid())
      {
        results.put(key, new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
      }
      else
      {
        errors.put(key, new RestLiServiceException(HttpStatus.S_422_UNPROCESSABLE_ENTITY, result.getMessages().toString()));
      }
    }
    return new BatchUpdateResult<Integer, ValidationDemo>(results, errors);
  }

  private void check(boolean condition)
  {
    if (!condition)
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    }
  }

  @RestMethod.Get
  public ValidationDemo get(final Integer key, @ValidatorParam RestLiDataValidator validator)
  {
    // Generate an entity that does not conform to the data schema
    ValidationDemo.UnionFieldWithInlineRecord union = new ValidationDemo.UnionFieldWithInlineRecord();
    union.setMyEnum(myEnum.BARBAR);
    ValidationDemo validationDemo = new ValidationDemo().setStringA("stringA is readOnly").setUnionFieldWithInlineRecord(union);

    // Validate the entity
    ValidationResult result = validator.validate(validationDemo);
    check(!result.isValid());
    String errorMessages = result.getMessages().toString();
    check(errorMessages.contains("/stringA :: length of \"stringA is readOnly\" is out of range 1...10"));
    check(errorMessages.contains("/stringB :: field is required but not found"));

    // Fix the entity
    validationDemo.setStringA("abcd").setStringB("stringB");

    // Validate the entity again
    result = validator.validate(validationDemo);
    check(result.isValid());

    return validationDemo;
  }

  @RestMethod.BatchGet
  public Map<Integer, ValidationDemo> batchGet(Set<Integer> ids, @ValidatorParam RestLiDataValidator validator)
  {
    Map<Integer, ValidationDemo> resultMap = new HashMap<Integer, ValidationDemo>();

    // Generate entities that are missing a required field
    for (Integer id : ids)
    {
      ValidationDemo.UnionFieldWithInlineRecord union = new ValidationDemo.UnionFieldWithInlineRecord();
      union.setMyRecord(new myRecord());
      ValidationDemo validationDemo = new ValidationDemo().setStringA("a").setStringB("b").setUnionFieldWithInlineRecord(union);
      resultMap.put(id, validationDemo);
    };

    // Validate outgoing data
    for (ValidationDemo entity : resultMap.values())
    {
      ValidationResult result = validator.validate(entity);
      check(!result.isValid());
      check(result.getMessages().toString().contains("/UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo1 :: field is required but not found"));
    }

    // Fix entities
    for (Integer id : ids)
    {
      resultMap.get(id).getUnionFieldWithInlineRecord().getMyRecord().setFoo1(1234);
    }

    // Validate again
    for (ValidationDemo entity : resultMap.values())
    {
      ValidationResult result = validator.validate(entity);
      check(result.isValid());
    }

    return resultMap;
  }

  @RestMethod.GetAll
  public List<ValidationDemo> getAll(@ValidatorParam RestLiDataValidator validator)
  {
    List<ValidationDemo> validationDemos = new ArrayList<ValidationDemo>();

    // Generate entities with stringA fields that are too long
    for (int i = 0; i < 10; i++)
    {
      ValidationDemo.UnionFieldWithInlineRecord union = new ValidationDemo.UnionFieldWithInlineRecord();
      union.setMyEnum(myEnum.FOOFOO);
      validationDemos.add(new ValidationDemo().setStringA("This string is too long to pass validation.")
                              .setStringB("stringB").setUnionFieldWithInlineRecord(union));
    }

    // Validate outgoing data
    for (ValidationDemo entity : validationDemos)
    {
      ValidationResult result = validator.validate(entity);
      check(!result.isValid());
      check(result.getMessages().toString().contains("/stringA :: length of \"This string is too long to pass validation.\" is out of range 1...10"));
    }

    // Fix entities
    for (ValidationDemo validationDemo : validationDemos)
    {
      validationDemo.setStringA("short str");
    }

    // Validate again
    for (ValidationDemo entity : validationDemos)
    {
      ValidationResult result = validator.validate(entity);
      check(result.isValid());
    }

    return validationDemos;
  }

  @Finder("search")
  public List<ValidationDemo> search(@QueryParam("intA") Integer intA, @ValidatorParam RestLiDataValidator validator)
  {
    List<ValidationDemo> validationDemos = new ArrayList<ValidationDemo>();

    // Generate entities that are missing stringB fields
    for (int i = 0; i < 3; i++)
    {
      ValidationDemo.UnionFieldWithInlineRecord union = new ValidationDemo.UnionFieldWithInlineRecord();
      union.setMyEnum(myEnum.FOOFOO);
      validationDemos.add(new ValidationDemo().setStringA("valueA").setIntA(intA).setUnionFieldWithInlineRecord(union));
    }

    // Validate outgoing data
    for (ValidationDemo entity : validationDemos)
    {
      ValidationResult result = validator.validate(entity);
      check(!result.isValid());
      check(result.getMessages().toString().contains("/stringB :: field is required but not found"));
    }

    // Fix entities
    for (ValidationDemo validationDemo : validationDemos)
    {
      validationDemo.setStringB("valueB");
    }

    // Validate again
    for (ValidationDemo entity : validationDemos)
    {
      ValidationResult result = validator.validate(entity);
      check(result.isValid());
    }

    return validationDemos;
  }
}