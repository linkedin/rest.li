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

package com.linkedin.restli.common.testutils;


import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.restli.common.ActionResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.test.RecordTemplateWithDefaultValue;

import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Keren Jin
 */
public class TestMockActionResponseFactory
{
  @Test
  public void testInference()
  {
    final RecordTemplateWithDefaultValue record = new RecordTemplateWithDefaultValue();
    record.setId(42L);
    record.setMessage("Lorem ipsum");

    final ActionResponse<RecordTemplateWithDefaultValue> response = MockActionResponseFactory.create(RecordTemplateWithDefaultValue.class, record);

    Assert.assertEquals(response.getValue(), record);

    final RecordDataSchema schema = response.schema();
    Assert.assertEquals(schema.getName(), ActionResponse.class.getSimpleName());
    Assert.assertEquals(schema.getField(ActionResponse.VALUE_NAME).getType(), DataTemplateUtil.getSchema(RecordTemplateWithDefaultValue.class));
  }

  @Test
  public void testDynamicSchema()
  {
    final RecordTemplateWithDefaultValue record = new RecordTemplateWithDefaultValue();
    record.setId(42L);
    record.setMessage("Lorem ipsum");

    final CollectionResponse<RecordTemplateWithDefaultValue> collectionResponse = new CollectionResponse<>(RecordTemplateWithDefaultValue.class);
    collectionResponse.getElements().add(record);
    @SuppressWarnings("unchecked")
    final ActionResponse<CollectionResponse<RecordTemplateWithDefaultValue>> response =
      (ActionResponse<CollectionResponse<RecordTemplateWithDefaultValue>>) (Object)
          MockActionResponseFactory.create(CollectionResponse.class, collectionResponse.schema(), collectionResponse);

    Assert.assertEquals(response.getValue(), collectionResponse);

    final RecordDataSchema schema = response.schema();
    Assert.assertEquals(schema.getName(), ActionResponse.class.getSimpleName());
    Assert.assertEquals(schema.getField(ActionResponse.VALUE_NAME).getType(), collectionResponse.schema());
  }
}
