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

import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.validation.ReadOnly;
import com.linkedin.restli.common.validation.RestLiDataValidator;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.server.CreateKVResponse;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.ReturnEntity;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.annotations.ValidatorParam;
import com.linkedin.restli.server.resources.KeyValueResource;

/**
 * Class for testing restli validation filter
 *
 * @author Boyang Chen
 */
@RestLiCollection(
    name = "validationCreateAndGet",
    namespace = "com.linkedin.restli.examples.greetings.client",
    keyName = "key"
)
@ReadOnly({"tone"})
public class ValidationCreateAndGetResource implements KeyValueResource<Long,Greeting>
{
  @ReturnEntity
  @RestMethod.Create
  public CreateKVResponse<Long, Greeting> create(Greeting entity, @ValidatorParam RestLiDataValidator validator)
  {
    Long id = 3L;
    entity.setId(id);
    ValidationResult result = validator.validate(entity);
    if (!result.isValid())
    {
      throw new RestLiServiceException(HttpStatus.S_422_UNPROCESSABLE_ENTITY, result.getMessages().toString());
    }
    return new CreateKVResponse<Long, Greeting>(id, entity);
  }
}
