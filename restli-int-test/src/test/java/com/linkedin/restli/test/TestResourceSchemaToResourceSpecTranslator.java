/*
   Copyright (c) 2012 LinkedIn Corp.

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
package com.linkedin.restli.test;


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.FixedDataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.SchemaParserFactory;
import com.linkedin.data.schema.UnionDataSchema;
import com.linkedin.data.schema.resolver.FileDataSchemaResolver;
import com.linkedin.data.template.AbstractArrayTemplate;
import com.linkedin.data.template.AbstractMapTemplate;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.DirectArrayTemplate;
import com.linkedin.data.template.DirectMapTemplate;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.FieldDef;
import com.linkedin.data.template.FixedTemplate;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.TemplateOutputCastException;
import com.linkedin.data.template.UnionTemplate;
import com.linkedin.restli.common.ComplexKeySpec;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.common.util.ResourceSchemaToResourceSpecTranslator;
import com.linkedin.restli.common.util.ResourceSchemaToResourceSpecTranslator.ClassBindingResolver;
import com.linkedin.restli.common.util.RichResourceSchema;
import com.linkedin.restli.examples.custom.types.CustomLong;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.ActionsBuilders;
import com.linkedin.restli.examples.greetings.client.AssociationsBuilders;
import com.linkedin.restli.examples.greetings.client.ComplexArrayBuilders;
import com.linkedin.restli.examples.greetings.client.ComplexKeysBuilders;
import com.linkedin.restli.examples.greetings.client.CustomTypesBuilders;
import com.linkedin.restli.examples.greetings.client.FindersBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsAuthBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsBuilders;
import com.linkedin.restli.examples.greetings.client.SubgreetingsBuilders;
import com.linkedin.restli.examples.groups.client.ContactsBuilders;
import com.linkedin.restli.examples.groups.client.GroupMembershipsBuilders;
import com.linkedin.restli.examples.groups.client.GroupMembershipsComplexBuilders;
import com.linkedin.restli.examples.groups.client.GroupsBuilders;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.restspec.ActionSchema;
import com.linkedin.restli.restspec.ActionSchemaArray;
import com.linkedin.restli.restspec.FinderSchema;
import com.linkedin.restli.restspec.FinderSchemaArray;
import com.linkedin.restli.restspec.ParameterSchema;
import com.linkedin.restli.restspec.ParameterSchemaArray;
import com.linkedin.restli.restspec.ResourceSchema;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Validates the ResourceSchemaToResourceSpecTranslator by comparing the ResourceSpec it generates dynamically
 * against the ones generated statically by the java client builder generator.
 */
public class TestResourceSchemaToResourceSpecTranslator
{
  private static final String FS = File.separator;

  private String idlDir;

  DataSchemaResolver resolver;
  ResourceSchemaToResourceSpecTranslator translator;

  @BeforeClass
  public void initClass() throws Exception
  {
    String projectDir = System.getProperty("test.projectDir");
    idlDir = projectDir + FS + ".." + FS + "restli-int-test-api" + FS + "src" + FS + "main" + FS + "idl";
    String pdscDir = projectDir +  FS + ".." + FS + "restli-int-test-api" + FS + "src" + FS + "main" + FS + "pegasus";
    resolver = new FileDataSchemaResolver(SchemaParserFactory.instance(), pdscDir);

    translator = new ResourceSchemaToResourceSpecTranslator(resolver, new ExampleGeneratorClassBindingResolver());
  }

  @DataProvider(name="restspecsAndBuilders")
  public Object[][] restspecsAndBuilders() throws Exception
  {
    return new Object[][] {
      new Object[] {loadResourceSchema("com.linkedin.restli.examples.greetings.client.greetings.restspec.json"), null, GreetingsBuilders.class},
      new Object[] {loadResourceSchema("com.linkedin.restli.examples.greetings.client.complexArray.restspec.json"), null, ComplexArrayBuilders.class},
      new Object[] {loadResourceSchema("com.linkedin.restli.examples.greetings.client.complexKeys.restspec.json"), null, ComplexKeysBuilders.class},
      new Object[] {loadResourceSchema("com.linkedin.restli.examples.greetings.client.associations.restspec.json"), null, AssociationsBuilders.class},
      new Object[] {loadResourceSchema("com.linkedin.restli.examples.greetings.client.actions.restspec.json"), null, ActionsBuilders.class},
      new Object[] {loadResourceSchema("com.linkedin.restli.examples.greetings.client.customTypes.restspec.json"), null, CustomTypesBuilders.class},
      new Object[] {loadResourceSchema("com.linkedin.restli.examples.greetings.client.finders.restspec.json"), null, FindersBuilders.class},
      new Object[] {loadResourceSchema("com.linkedin.restli.examples.greetings.client.greeting.restspec.json"), null, GreetingBuilders.class},
      new Object[] {loadResourceSchema("com.linkedin.restli.examples.greetings.client.greeting.restspec.json"), "subgreetings", SubgreetingsBuilders.class },
      new Object[] {loadResourceSchema("com.linkedin.restli.examples.greetings.client.greetingsAuth.restspec.json"), null, GreetingsAuthBuilders.class },
      new Object[] {loadResourceSchema("com.linkedin.restli.examples.groups.client.groupMemberships.restspec.json"), null, GroupMembershipsBuilders.class },
      new Object[] {loadResourceSchema("com.linkedin.restli.examples.groups.client.groupMembershipsComplex.restspec.json"), null, GroupMembershipsComplexBuilders.class },
      new Object[] {loadResourceSchema("com.linkedin.restli.examples.groups.client.groups.restspec.json"), null, GroupsBuilders.class },
      new Object[] {loadResourceSchema("com.linkedin.restli.examples.groups.client.groups.restspec.json"), "contacts", ContactsBuilders.class }
    };
  }

  private RichResourceSchema loadResourceSchema(String restspecFile) throws Exception
  {
    ResourceSchema resourceSchema = DataMapUtils.read(new FileInputStream(idlDir + FS + restspecFile), ResourceSchema.class);
    return new RichResourceSchema(resourceSchema);
  }

  @Test(dataProvider = "restspecsAndBuilders")
  public void testTranslator(RichResourceSchema resourceSchema, String subresource, Class<?> resourceBuildersClass) throws Exception
  {
    if (subresource != null)
    {
      resourceSchema = resourceSchema.getSubresource(subresource);
    }
    ResourceSpec actualResourceSpec = translator.translate(resourceSchema.getResourceSchema());
    ResourceSpec expectedResourceSpec = getResourceSpecFromClientBuilders(resourceBuildersClass);

    compareResourceSpecs(actualResourceSpec, expectedResourceSpec, resourceSchema);
  }

  public void compareResourceSpecs(ResourceSpec actual, ResourceSpec expected, RichResourceSchema resourceSchema)
  {
    Assert.assertEquals(actual.getSupportedMethods(), expected.getSupportedMethods());
    compareTypes(actual.getKeyType(), expected.getKeyType());
    compareTypes(actual.getValueType(), expected.getValueType());
    compareComplexKey(actual.getComplexKeyType(), expected.getComplexKeyType());
    compareKeyParts(actual.getKeyParts(), expected.getKeyParts());

    ActionSchemaArray actions = resourceSchema.getActions();
    compareActions(actual, expected, actions);

    ActionSchemaArray entityActions = resourceSchema.getEntityActions();
    compareActions(actual, expected, entityActions);

    FinderSchemaArray finders = resourceSchema.getFinders();
    for (FinderSchema finder : finders)
    {
      compareParameters(actual.getRequestMetadata(finder.getName()), expected.getRequestMetadata(finder.getName()));
    }
  }

  private void compareActions(ResourceSpec actual, ResourceSpec expected, ActionSchemaArray actions)
  {
    for (ActionSchema action : actions)
    {
      ParameterSchemaArray parameters = action.getParameters();
      DynamicRecordMetadata actualRequest = actual.getRequestMetadata(action.getName());
      DynamicRecordMetadata expectedRequest = expected.getRequestMetadata(action.getName());
      if (expectedRequest != null)
      {
        Assert.assertNotNull(actualRequest);
        Assert.assertEquals(actualRequest.getRecordDataSchema(), expectedRequest.getRecordDataSchema());

        if (parameters != null)
        {
          for (ParameterSchema parameter : parameters)
          {
            compareFieldDef(actualRequest.getFieldDef(parameter.getName()), expectedRequest.getFieldDef(parameter.getName()));
          }
        }
      }
      else
      {
        Assert.assertNull(actualRequest);
      }
      compareParameters(actual.getActionResponseMetadata(action.getName()),
                        expected.getActionResponseMetadata(action.getName()));
    }
  }

  public void compareParameters(DynamicRecordMetadata actualResponse, DynamicRecordMetadata expectedResponse)
  {
    if (expectedResponse != null)
    {
      Assert.assertNotNull(actualResponse);
      Assert.assertEquals(actualResponse.getRecordDataSchema(), expectedResponse.getRecordDataSchema());

      if (expectedResponse.getFieldDef("value") != null)
      {
        compareFieldDef(actualResponse.getFieldDef("value"), expectedResponse.getFieldDef("value"));
      }
    }
    else
    {
      Assert.assertNull(actualResponse);
    }
  }

  public void compareFieldDef(FieldDef<?> actual, FieldDef<?> expected)
  {
    Assert.assertEquals(actual.getDataSchema(), expected.getDataSchema());
    Assert.assertEquals(actual.getField(), expected.getField());
    Assert.assertEquals(actual.getName(), expected.getName());
    checkDataTemplatesAreOfSameTypeHierarchyBranch(actual.getType(), expected.getType());
    checkDataTemplatesAreOfSameTypeHierarchyBranch(actual.getDataClass(), expected.getDataClass());

  }

  public void compareTypes(TypeSpec actual, TypeSpec expected)
  {
    if (expected != null)
    {
      Assert.assertNotNull(actual);
      Assert.assertEquals(actual.getSchema(), expected.getSchema());
      checkDataTemplatesAreOfSameTypeHierarchyBranch(actual.getType(), expected.getType());
    }
  }

  private static final Class<?>[] DATA_TEMPLATE_TYPE_BRANCHES = new Class<?>[] {
      FixedTemplate.class,
      AbstractArrayTemplate.class,
      AbstractMapTemplate.class,
      RecordTemplate.class,
      AbstractMapTemplate.class,
      UnionTemplate.class
  };

  public void checkDataTemplatesAreOfSameTypeHierarchyBranch(Class<?> actual, Class<?> expected)
  {
    if (expected != null)
    {
      Assert.assertNotNull(actual);
      if (DataTemplate.class.isAssignableFrom(actual))
      {
        for (Class<?> templateType : DATA_TEMPLATE_TYPE_BRANCHES)
        {
          if (templateType.isAssignableFrom(expected))
          {
            Assert.assertTrue(templateType.isAssignableFrom(actual));
            return;
          }
        }
        throw new IllegalStateException("Unrecognized branch of DataTemplate type hierarchy: " + expected);
      }
      else
      {
        comparePrimitivesClasses(actual, expected);
      }
    }
  }

  public void comparePrimitivesClasses(Class<?> actual, Class<?> expected)
  {
    if (actual == expected)
    {
      return;
    }
    // the custom type coercers are not friendly to being inspected, so hard coding the only expectation we have for them
    else if (actual == Long.class && expected == CustomLong.class)
    {
      return;
    }
    Assert.fail("primitive types do not match.  actual: " + actual + " expected: " + expected);
  }

  public void compareKeyParts(Map<String, CompoundKey.TypeInfo> actual, Map<String, CompoundKey.TypeInfo> expected)
  {
    Assert.assertEquals(actual.keySet(), expected.keySet());
    for (Map.Entry<String, CompoundKey.TypeInfo> actualEntry : actual.entrySet())
    {
      String key = actualEntry.getKey();
      CompoundKey.TypeInfo actualValue = actualEntry.getValue();
      CompoundKey.TypeInfo expectedValue = expected.get(key);
      compareTypes(actualValue.getBinding(), expectedValue.getBinding());
      compareTypes(actualValue.getDeclared(), expectedValue.getDeclared());
    }
  }

  public void compareComplexKey(ComplexKeySpec actualComplexKeyType, ComplexKeySpec expectedComplexKeyType)
  {
    if (expectedComplexKeyType == null && actualComplexKeyType == null) return;
    Assert.assertNotNull(expectedComplexKeyType);
    Assert.assertNotNull(actualComplexKeyType);
    compareTypes(actualComplexKeyType.getKeyType(), expectedComplexKeyType.getKeyType());
    compareTypes(actualComplexKeyType.getParamsType(), expectedComplexKeyType.getParamsType());
  }

  private ResourceSpec getResourceSpecFromClientBuilders(Class<?> type)
  {
    try
    {
      Field schemaField = type.getDeclaredField("_resourceSpec");
      schemaField.setAccessible(true);
      ResourceSpec resourceSpec = (ResourceSpec) schemaField.get(null);
      if (resourceSpec == null)
      {
        throw new IllegalArgumentException("Class missing _resourceSpec: " + type.getName());
      }

      return resourceSpec;
    }
    catch (IllegalAccessException e)
    {
      throw new IllegalArgumentException("Error evaluating _resourceSpec name for class: " + type.getName(), e);
    }
    catch (NoSuchFieldException e)
    {
      throw new IllegalArgumentException("Class missing _resourceSpec field: " + type.getName(), e);
    }
  }

  /* --------------------------------------------------------------------------------------------------------------
   * For testing, provide some bindings that use placeholders for all the data template types.
   */

  private static class ExampleGeneratorClassBindingResolver implements ClassBindingResolver
  {
    public Class<? extends DataTemplate> resolveTemplateClass(DataSchema schema)
    {
      switch(schema.getDereferencedType()) {
        case FIXED:
          return FixedTemplatePlaceholder.class;
        case ARRAY:
          return ArrayTemplatePlaceholder.class;
        case RECORD:
          return RecordTemplatePlaceholder.class;
        case MAP:
          return MapTemplatePlaceholder.class;
        case UNION:
          return UnionTemplatePlaceholder.class;
        case TYPEREF:
          throw new IllegalStateException("TYPEREF should not be returned for a dereferenced byte. schema: " + schema);
        default:
          throw new IllegalStateException("Unrecognized enum value: " + schema.getDereferencedType());
      }
    }

    public Class<? extends Enum> resolveEnumClass(EnumDataSchema enumDataSchema)
    {
      // placeholder for enum mapping, just hard coding the ones we care about for testing
      if(enumDataSchema.getName().equals(Tone.class.getSimpleName()))
      {
        return Tone.class;
      }
      else
      {
        throw new IllegalArgumentException("Unknown enum type.  All enums must be registered with resolveEnumClass for this test suite.");
      }
    }
  }

  private static class RecordTemplatePlaceholder extends RecordTemplate
  {
    public RecordTemplatePlaceholder(DataMap object)
        throws TemplateOutputCastException
    {
      super(object, null);
    }

    public RecordTemplatePlaceholder(DataMap object, RecordDataSchema schema)
        throws TemplateOutputCastException
    {
      super(object, schema);
    }
  }

  private static class UnionTemplatePlaceholder extends UnionTemplate
  {
    public UnionTemplatePlaceholder(Object object)
        throws TemplateOutputCastException
    {
      super(object, null);
    }

    public UnionTemplatePlaceholder(Object object, UnionDataSchema schema)
        throws TemplateOutputCastException
    {
      super(object, schema);
    }
  }

  private static class ArrayTemplatePlaceholder<E> extends DirectArrayTemplate<E>
  {
    public ArrayTemplatePlaceholder(DataList list)
    {
      super(list, null, null);
    }

    public ArrayTemplatePlaceholder(DataList list, ArrayDataSchema schema, Class<E> elementClass)
    {
      super(list, schema, elementClass);
    }
  }

  private static class MapTemplatePlaceholder<V> extends DirectMapTemplate<V>
  {
    public MapTemplatePlaceholder(DataMap map)
    {
      super(map, null, null);
    }

    public MapTemplatePlaceholder(DataMap map, MapDataSchema schema, Class<V> valueClass)
    {
      super(map, schema, valueClass);
    }
  }

  private static class FixedTemplatePlaceholder extends FixedTemplate
  {
    public FixedTemplatePlaceholder(Object object)
        throws TemplateOutputCastException
    {
      super(object, null);
    }

    public FixedTemplatePlaceholder(Object object, FixedDataSchema schema)
        throws TemplateOutputCastException
    {
      super(object, schema);
    }
  }
}
