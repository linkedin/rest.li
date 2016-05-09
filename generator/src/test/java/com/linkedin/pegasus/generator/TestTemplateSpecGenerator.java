package com.linkedin.pegasus.generator;


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.DataSchemaUtil;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import com.linkedin.pegasus.generator.spec.RecordTemplateSpec;
import com.linkedin.pegasus.generator.spec.UnionTemplateSpec;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author Keren Jin
 */
public class TestTemplateSpecGenerator
{
  private static final String INPUT_SCHEMA_NAME = "testSchema";
  private static final String CUSTOM_TYPE_NAME_1 = "JavaType1";
  private static final String CUSTOM_TYPE_NAME_2 = "JavaType2";
  private static final TyperefDataSchema CUSTOM_TYPE_1;
  private static final TyperefDataSchema CUSTOM_TYPE_2;

  static
  {
    CUSTOM_TYPE_1 = new TyperefDataSchema(new Name("customType_1"));
    CUSTOM_TYPE_1.setReferencedType(DataSchemaUtil.classToPrimitiveDataSchema(String.class));
    CUSTOM_TYPE_1.setProperties(Collections.singletonMap("java", new DataMap(Collections.singletonMap("class", CUSTOM_TYPE_NAME_1))));
    CUSTOM_TYPE_2 = new TyperefDataSchema(new Name("customType_2"));
    CUSTOM_TYPE_2.setReferencedType(DataSchemaUtil.classToPrimitiveDataSchema(int.class));
    CUSTOM_TYPE_2.setProperties(Collections.singletonMap("java", new DataMap(Collections.singletonMap("class", CUSTOM_TYPE_NAME_2))));
  }

  private AtomicInteger _uniqueNumberGenerator;
  @Mock
  private DataSchemaResolver _resolver;
  @Mock
  private DataSchemaLocation _location;

  @BeforeMethod
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    _uniqueNumberGenerator = new AtomicInteger();
    Mockito.when(_resolver.nameToDataSchemaLocations()).thenReturn(Collections.singletonMap(INPUT_SCHEMA_NAME, _location));
  }


  @Test(dataProvider = "customTypeDataForRecord")
  public void testCustomInfoForRecordFields(final List<DataSchema> customTypedSchemas)
  {
    final List<RecordDataSchema.Field> fields = customTypedSchemas.stream()
        .map(RecordDataSchema.Field::new)
        .peek(field -> field.setName("field_" + _uniqueNumberGenerator.getAndIncrement(), null))
        .collect(Collectors.toList());
    final RecordDataSchema record = new RecordDataSchema(new Name(INPUT_SCHEMA_NAME), RecordDataSchema.RecordType.RECORD);
    record.setFields(fields, null);

    final TemplateSpecGenerator generator = new TemplateSpecGenerator(_resolver);
    final RecordTemplateSpec spec = (RecordTemplateSpec) generator.generate(record, _location);

    for (int i = 0; i < customTypedSchemas.size(); ++i)
    {
      Assert.assertNotNull(spec.getFields().get(i).getCustomInfo());
      Assert.assertEquals(spec.getFields().get(i).getCustomInfo().getCustomClass().getClassName(),
                          ((Map<String, String>) customTypedSchemas.get(i).getProperties().get("java")).get("class"));
    }
  }

  @Test(dataProvider = "customTypeDataForUnion")
  public void testCustomInfoForUnionMembers(final List<DataSchema> customTypedSchemas)
  {
    final UnionDataSchema union = new UnionDataSchema();
    union.setTypes(customTypedSchemas, null);
    final TyperefDataSchema typeref = new TyperefDataSchema(new Name(INPUT_SCHEMA_NAME));
    typeref.setReferencedType(union);

    final TemplateSpecGenerator generator = new TemplateSpecGenerator(_resolver);
    final UnionTemplateSpec spec = (UnionTemplateSpec) generator.generate(typeref, _location);

    for (int i = 0; i < customTypedSchemas.size(); ++i)
    {
      Assert.assertNotNull(spec.getMembers().get(i).getCustomInfo());
      Assert.assertEquals(spec.getMembers().get(i).getCustomInfo().getCustomClass().getClassName(),
                          ((Map<String, String>) customTypedSchemas.get(i).getProperties().get("java")).get("class"));
    }
  }

  @DataProvider
  private Object[][] customTypeDataForRecord()
  {
    return new Object[][] {
        {Arrays.asList(CUSTOM_TYPE_1, CUSTOM_TYPE_1)},
        {Arrays.asList(CUSTOM_TYPE_1, CUSTOM_TYPE_2)},
    };
  }

  @DataProvider
  private Object[][] customTypeDataForUnion()
  {
    return new Object[][] {
        // since union does not allow same type for multiple members, we can only test the different type case
        {Arrays.asList(CUSTOM_TYPE_1, CUSTOM_TYPE_2)},
    };
  }
}
