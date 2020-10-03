/*
 * Copyright (c) 2020 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.data.schema.compatibility;

import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.annotation.IdentitySchemaVisitor;
import com.linkedin.data.schema.annotation.PegasusSchemaAnnotationHandlerImpl;
import com.linkedin.data.schema.annotation.SchemaAnnotationHandler;
import com.linkedin.data.schema.annotation.SchemaAnnotationHandler.CompatibilityCheckContext;
import com.linkedin.data.schema.annotation.SchemaAnnotationHandler.AnnotationCompatibilityResult;
import com.linkedin.data.schema.annotation.SchemaVisitor;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestAnnotationCompatibilityChecker
{
  private final static String BAR_ANNOTATION_NAMESPACE = "bar";
  private final static String BAZ_ANNOTATION_NAMESPACE = "baz";
  private final static String ANNOTATION_FIELD_NAME = "foo";

  @Test(dataProvider = "annotationCompatibilityCheckTestData")
  public void testCheckCompatibility(String prevSchemaFile, String currSchemaFile, List<SchemaAnnotationHandler> handlers, List<AnnotationCompatibilityResult> expectedResults)
      throws IOException
  {
    DataSchema prevSchema = TestUtil.dataSchemaFromPdlInputStream(getClass().getResourceAsStream(prevSchemaFile));
    DataSchema currSchema = TestUtil.dataSchemaFromPdlInputStream(getClass().getResourceAsStream(currSchemaFile));
    List<AnnotationCompatibilityResult> results = AnnotationCompatibilityChecker
        .checkPegasusSchemaAnnotation(prevSchema, currSchema, handlers);
    Assert.assertEquals(results.size(), expectedResults.size());
    for (int i = 0; i < results.size(); i++)
    {
      Assert.assertEquals(results.get(i).getMessages().size(), expectedResults.get(i).getMessages().size());
      List<CompatibilityMessage> actualCompatibilityMessage = (List<CompatibilityMessage>) results.get(i).getMessages();
      List<CompatibilityMessage> expectCompatibilityMessage = (List<CompatibilityMessage>) expectedResults.get(i).getMessages();
      for (int j = 0; j < actualCompatibilityMessage.size(); j++)
      {
        Assert.assertEquals(actualCompatibilityMessage.get(j).toString(), expectCompatibilityMessage.get(j).toString());
      }
    }
  }

  @DataProvider
  private Object[][] annotationCompatibilityCheckTestData() throws IOException {
    // Set up expected result: both previous schema and current schema contain the same PathSpecs.
    CompatibilityCheckContext checkContext = generateAnnotationCheckContext(new PathSpec("TestSchema1/field1"));
    CompatibilityCheckContext checkContext1 = generateAnnotationCheckContext(new PathSpec("TestSchema1/field2"));

    AnnotationCompatibilityResult expectResultWithCompatibleChange1 = generateExpectResult(new CompatibilityMessage(checkContext.getPathSpecToSchema(),
      CompatibilityMessage.Impact.ANNOTATION_COMPATIBLE_CHANGE,
        "Updating annotation field \"%s\" value is backward compatible change", ANNOTATION_FIELD_NAME));
    AnnotationCompatibilityResult expectResultWithInCompatibleChange1 = generateExpectResult(new CompatibilityMessage(checkContext1.getPathSpecToSchema(),
        CompatibilityMessage.Impact.ANNOTATION_INCOMPATIBLE_CHANGE,
        "Deleting existed annotation \"%s\" is backward incompatible change", BAR_ANNOTATION_NAMESPACE));

    // Set up expected result: only previous schema contains the resolvedProperty with the same annotation namespace as SchemaAnnotationHandler
    CompatibilityCheckContext checkContext2 = generateAnnotationCheckContext(new PathSpec("TestSchema2/field1"));

    AnnotationCompatibilityResult expectResult2 = generateExpectResult(new CompatibilityMessage(checkContext2.getPathSpecToSchema(),
      CompatibilityMessage.Impact.ANNOTATION_INCOMPATIBLE_CHANGE,
        "Adding new annotation \"%s\" is backward compatible change", BAR_ANNOTATION_NAMESPACE));

    // Set up expected result: only current schema contains the resolvedProperty with the same annotation namespace as SchemaAnnotationHandler
    CompatibilityCheckContext checkContext3 = generateAnnotationCheckContext(new PathSpec("TestSchema3/field1"));
    AnnotationCompatibilityResult expectResult3 = generateExpectResult(new CompatibilityMessage(checkContext3.getPathSpecToSchema(),
        CompatibilityMessage.Impact.ANNOTATION_INCOMPATIBLE_CHANGE, "Deleting existed annotation \"%s\" is backward incompatible change", BAR_ANNOTATION_NAMESPACE));

    // Set up expected results: multiple handlers.
    CompatibilityCheckContext checkContext4 = generateAnnotationCheckContext(new PathSpec("TestSchema4/field1"));
    AnnotationCompatibilityResult barHandlerExpectResult = generateExpectResult(new CompatibilityMessage(checkContext4.getPathSpecToSchema(),
        CompatibilityMessage.Impact.ANNOTATION_INCOMPATIBLE_CHANGE,
        "Adding new annotation \"%s\" is backward compatible change", BAR_ANNOTATION_NAMESPACE));
    AnnotationCompatibilityResult bazHandlerExpectResult = generateExpectResult(new CompatibilityMessage(checkContext4.getPathSpecToSchema(),
        CompatibilityMessage.Impact.ANNOTATION_COMPATIBLE_CHANGE,
        "Updating annotation field \"%s\" value is backward compatible change", ANNOTATION_FIELD_NAME));

    return new Object[][]
        {
            {
                "previousSchema/TestSchema1.pdl",
                "currentSchema/TestSchema1.pdl",
                Collections.singletonList(generateSchemaAnnotationHandler(BAR_ANNOTATION_NAMESPACE)),
                Arrays.asList(expectResultWithCompatibleChange1, expectResultWithInCompatibleChange1)
            },
            {
                "previousSchema/TestSchema2.pdl",
                "currentSchema/TestSchema2.pdl",
                Collections.singletonList(generateSchemaAnnotationHandler(BAR_ANNOTATION_NAMESPACE)),
                Collections.singletonList(expectResult2)
            },
            {
                "previousSchema/TestSchema3.pdl",
                "currentSchema/TestSchema3.pdl",
                Collections.singletonList(generateSchemaAnnotationHandler(BAR_ANNOTATION_NAMESPACE)),
                Collections.singletonList(expectResult3)
            },
            {
                "previousSchema/TestSchema4.pdl",
                "currentSchema/TestSchema4.pdl",
                Arrays.asList(generateSchemaAnnotationHandler(BAR_ANNOTATION_NAMESPACE), generateSchemaAnnotationHandler(BAZ_ANNOTATION_NAMESPACE)),
                Arrays.asList(barHandlerExpectResult, bazHandlerExpectResult)
            },
        };
  }

  private SchemaAnnotationHandler generateSchemaAnnotationHandler(String annotationNamespace)
  {
    SchemaAnnotationHandler handler = new PegasusSchemaAnnotationHandlerImpl(annotationNamespace)
    {
      @Override
      public String getAnnotationNamespace()
      {
        return annotationNamespace;
      }

      @Override
      public boolean implementsCheckCompatibility()
      {
        return true;
      }

      @Override
      public SchemaVisitor getVisitor()
      {
        return new IdentitySchemaVisitor();
      }

      @Override
      public AnnotationCompatibilityResult checkCompatibility(Map<String,Object> prevResolvedProperties, Map<String, Object> currResolvedProperties,
          CompatibilityCheckContext prevContext, CompatibilityCheckContext currContext)
      {
        AnnotationCompatibilityResult result = new AnnotationCompatibilityResult();

        if (prevResolvedProperties.get(annotationNamespace) == null)
        {
          if (prevContext.getPathSpecToSchema() != null)
          {
            result.getMessages().add(new CompatibilityMessage(currContext.getPathSpecToSchema(),
                CompatibilityMessage.Impact.ANNOTATION_INCOMPATIBLE_CHANGE, "Adding new annotation \"%s\" is backward compatible change", annotationNamespace));
          }
        }
        else if (currResolvedProperties.get(annotationNamespace) == null)
        {
          if (currContext.getPathSpecToSchema() != null)
          {
            result.getMessages().add(new CompatibilityMessage(prevContext.getPathSpecToSchema(),
                CompatibilityMessage.Impact.ANNOTATION_INCOMPATIBLE_CHANGE, "Deleting existed annotation \"%s\" is backward incompatible change", annotationNamespace));
          }
        }
        else
        {
          DataMap prev = (DataMap) prevResolvedProperties.get(annotationNamespace);
          DataMap curr = (DataMap) currResolvedProperties.get(annotationNamespace);
          if (prev.containsKey(ANNOTATION_FIELD_NAME) && !curr.containsKey(ANNOTATION_FIELD_NAME))
          {
            result.getMessages().add(new CompatibilityMessage(prevContext.getPathSpecToSchema(),
                CompatibilityMessage.Impact.ANNOTATION_INCOMPATIBLE_CHANGE, "remove annotation field \"%s\" is backward incompatible change", ANNOTATION_FIELD_NAME));
          }
          if (prev.containsKey(ANNOTATION_FIELD_NAME) && curr.containsKey(ANNOTATION_FIELD_NAME))
          {
            if (prev.get(ANNOTATION_FIELD_NAME) != curr.get(ANNOTATION_FIELD_NAME))
            {
              result.getMessages().add(new CompatibilityMessage(prevContext.getPathSpecToSchema(),
                  CompatibilityMessage.Impact.ANNOTATION_COMPATIBLE_CHANGE, "Updating annotation field \"%s\" value is backward compatible change", ANNOTATION_FIELD_NAME));
            }
          }
        }
        return result;
      }
    };
    return handler;
  }

  private CompatibilityCheckContext generateAnnotationCheckContext(PathSpec pathSpec)
  {
    CompatibilityCheckContext context = new CompatibilityCheckContext();
    context.setPathSpecToSchema(pathSpec);
    return context;
  }

  private AnnotationCompatibilityResult generateExpectResult(CompatibilityMessage compatibilityMessage)
  {
    SchemaAnnotationHandler.AnnotationCompatibilityResult result = new SchemaAnnotationHandler.AnnotationCompatibilityResult();
    result.addMessage(compatibilityMessage);
    return result;
  }
}