/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.restli.common.util;

import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.transform.filter.request.MaskOperation;
import com.linkedin.restli.common.test.RecordTemplateWithPrimitiveKey;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.restli.common.util.ProjectionMaskApplier.*;


/**
 * Tests for {@link ProjectionMaskApplier}.
 *
 * @author Evan Williams
 */
public class TestProjectionMaskApplier
{
  @DataProvider
  public Object[][] provideBuildSchemaByProjectionData()
  {
    return new Object[][]
    {
        { buildProjectionMaskDataMap("id", "body"),
            new String[] { "id", "body" },
            new String[] { } },
        { buildProjectionMaskDataMap("id"),
            new String[] { "id" },
            new String[] { "body" } },
        { buildProjectionMaskDataMap("body"),
            new String[] { "body" },
            new String[] { "id" } },
    };
  }

  @Test(dataProvider = "provideBuildSchemaByProjectionData")
  public void testBuildSchemaByProjection(DataMap projectionMask, String[] expectedIncludedFields, String[] expectedExcludedFields)
  {
    DataSchema schema = DataTemplateUtil.getSchema(RecordTemplateWithPrimitiveKey.class);
    RecordDataSchema validatingSchema = (RecordDataSchema) buildSchemaByProjection(schema, projectionMask);

    for (String fieldName : expectedIncludedFields)
    {
      Assert.assertTrue(validatingSchema.contains(fieldName));
    }
    for (String fieldName : expectedExcludedFields)
    {
      Assert.assertFalse(validatingSchema.contains(fieldName));
    }
  }

  @Test
  public void testBuildSchemaByProjectionNonexistentFields()
  {
    RecordDataSchema schema = (RecordDataSchema) DataTemplateUtil.getSchema(RecordTemplateWithPrimitiveKey.class);
    DataMap projectionMask = buildProjectionMaskDataMap("id", "nonexistentFieldFooBar");

    try
    {
      buildSchemaByProjection(schema, projectionMask);
    }
    catch (InvalidProjectionException e)
    {
      Assert.assertEquals(e.getMessage(), "Projected field \"nonexistentFieldFooBar\" not present in schema \"" + schema.getFullName() + "\"");
      return;
    }

    Assert.fail("Building schema by projection with nonexistent fields should throw an InvalidProjectionException");
  }

  @Test
  public void testBuildSchemaByEmptyProjection()
  {
    DataSchema schema = DataTemplateUtil.getSchema(RecordTemplateWithPrimitiveKey.class);
    DataMap projectionMask = buildProjectionMaskDataMap();

    try
    {
      buildSchemaByProjection(schema, projectionMask);
    }
    catch (IllegalArgumentException e)
    {
      Assert.assertEquals(e.getMessage(), "Invalid projection masks.");
      return;
    }

    Assert.fail("Building schema by empty projection should throw an IllegalArgumentException");
  }

  /**
   * Create a projection mask {@link DataMap} from a list of field names.
   * @param fieldNames array of field names to include as positive entries in the mask
   * @return projection mask as a {@link DataMap}
   */
  private static DataMap buildProjectionMaskDataMap(String ... fieldNames)
  {
    Map<String, Object> map = new HashMap<>();
    for (String fieldName : fieldNames)
    {
      map.put(fieldName, MaskOperation.POSITIVE_MASK_OP.getRepresentation());
    }
    return new DataMap(map);
  }
}
