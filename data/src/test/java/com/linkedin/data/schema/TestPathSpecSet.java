package com.linkedin.data.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.collections.Lists;


/**
 * Unit tests for {@link PathSpecSet}.
 */
public class TestPathSpecSet {

  private static final PathSpec THREE_FIELD_MODEL_ALL_FIELDS = new PathSpec();
  private static final PathSpec THREE_FIELD_MODEL_FIELD1 = new PathSpec("field1");
  private static final PathSpec THREE_FIELD_MODEL_FIELD2 = new PathSpec("field2");
  private static final PathSpec THREE_FIELD_MODEL_FIELD3 = new PathSpec("record1");
  private static final PathSpec THREE_FIELD_MODEL_EMBEDDED_FIELD1 =
      new PathSpec(THREE_FIELD_MODEL_FIELD3.getPathComponents(), "embedded1");

  private static final PathSpec NESTED_MODELS_SIMPLE_RECORD = new PathSpec("simpleRecord");
  private static final PathSpec NESTED_MODELS_SIMPLE_RECORD_FIELD1 =
      new PathSpec(NESTED_MODELS_SIMPLE_RECORD.getPathComponents(), "intField");
  private static final PathSpec NESTED_MODELS_SIMPLE_RECORD_FIELD2 =
      new PathSpec(NESTED_MODELS_SIMPLE_RECORD.getPathComponents(), "byteField");
  private static final PathSpec NESTED_MODELS_ARRAY_ITEMS = new PathSpec("arrayOfRecords", "items");
  private static final PathSpec NESTED_MODELS_MAP_VALUES = new PathSpec("mapOfRecords", "values");

  @Test
  public void testEmpty() {
    PathSpecSet pathSpecSet = PathSpecSet.empty();
    Assert.assertEquals(pathSpecSet.getPathSpecs(), Collections.emptySet());
    Assert.assertFalse(pathSpecSet.isAllInclusive());
    Assert.assertTrue(pathSpecSet.isEmpty());
    Assert.assertEquals(pathSpecSet.toArray(), new PathSpec[0]);
  }

  @Test
  public void testAllInclusiveSet() {
    PathSpecSet pathSpecSet = PathSpecSet.allInclusive();
    Assert.assertEquals(pathSpecSet.getPathSpecs(), Collections.emptySet());
    Assert.assertTrue(pathSpecSet.isAllInclusive());
    Assert.assertFalse(pathSpecSet.isEmpty());
    Assert.assertNull(pathSpecSet.toArray());
  }

  @Test(dataProvider = "pathSpecCollections")
  public void testCreateFromPathSpecCollection(List<PathSpec> pathSpecs) {
    PathSpecSet pathSpecSet = PathSpecSet.of(pathSpecs);
    Assert.assertEquals(pathSpecSet.getPathSpecs(), new HashSet<>(pathSpecs));
    Assert.assertFalse(pathSpecSet.isEmpty());
    Assert.assertFalse(pathSpecSet.isAllInclusive());
  }

  @Test(dataProvider = "pathSpecCollections")
  public void testCreateFromPathSpecCollectionIsImmutable(List<PathSpec> pathSpecs) {
    List<PathSpec> pathSpecToMutate = new ArrayList<>(pathSpecs);
    PathSpecSet pathSpecSet = PathSpecSet.of(pathSpecToMutate);
    pathSpecToMutate.add(THREE_FIELD_MODEL_FIELD3);
    Assert.assertEquals(pathSpecSet.getPathSpecs(), new HashSet<>(pathSpecs));
  }

  @Test
  public void testCreateFromPathSpecVarArgs() {
    PathSpecSet pathSpecSet = PathSpecSet.of(THREE_FIELD_MODEL_FIELD1, THREE_FIELD_MODEL_FIELD2);
    Assert.assertEquals(pathSpecSet.getPathSpecs(),
        new HashSet<>(Arrays.asList(THREE_FIELD_MODEL_FIELD1, THREE_FIELD_MODEL_FIELD2)));
  }

  @Test
  public void testAssembleFromBuilder() {
    PathSpecSet pathSpecSet = PathSpecSet.newBuilder()
        .add(Lists.newArrayList("record1"))
        .add(THREE_FIELD_MODEL_FIELD1)
        .add(PathSpecSet.of(THREE_FIELD_MODEL_FIELD2))
        .build();

    Assert.assertEquals(pathSpecSet.getPathSpecs(),
        new HashSet<>(Arrays.asList(THREE_FIELD_MODEL_FIELD1, THREE_FIELD_MODEL_FIELD2, THREE_FIELD_MODEL_FIELD3)));
  }

  @Test(dataProvider = "buildersWithAllInclusive")
  public void testAssembleFromBuilderWithAllInclusiveSetOverridingAllValues(PathSpecSet.Builder builder) {
    Assert.assertFalse(builder.isEmpty());
    PathSpecSet pathSpecSet = builder.build();
    Assert.assertEquals(pathSpecSet, PathSpecSet.allInclusive());
    Assert.assertTrue(pathSpecSet.isAllInclusive());
    Assert.assertEquals(pathSpecSet.getPathSpecs(), new HashSet<>());
  }

  @Test
  public void testEmptyBuilder() {
    Assert.assertTrue(PathSpecSet.newBuilder().isEmpty());
    Assert.assertTrue(PathSpecSet.newBuilder().add().isEmpty());
  }

  @Test
  public void testBuilderNotEmpty() {
    Assert.assertFalse(PathSpecSet.newBuilder().add(THREE_FIELD_MODEL_FIELD1).isEmpty());
  }

  @Test(dataProvider = "copyWithScopeProvider")
  public void testCopyWithScope(PathSpecSet input, PathSpec parent, PathSpecSet expected) {
    Assert.assertEquals(input.copyWithScope(parent), expected);
  }

  @Test(dataProvider = "copyAndRemovePrefixProvider")
  public void testCopyAndRemovePrefix(PathSpecSet input, PathSpec prefix, PathSpecSet expected) {
    Assert.assertEquals(input.copyAndRemovePrefix(prefix), expected);
  }

  @Test
  public void testToString() {
    Assert.assertEquals(PathSpecSet.of(THREE_FIELD_MODEL_FIELD1).toString(), "PathSpecSet{/field1}");
    Assert.assertEquals(PathSpecSet.allInclusive().toString(), "PathSpecSet{all inclusive}");
  }

  @Test
  public void testHashCode() {
    PathSpecSet pss1 = PathSpecSet.of(THREE_FIELD_MODEL_FIELD1, THREE_FIELD_MODEL_FIELD2);
    PathSpecSet pss2 = PathSpecSet.of(THREE_FIELD_MODEL_FIELD1, THREE_FIELD_MODEL_FIELD2);
    Assert.assertEquals(pss1.hashCode(), pss2.hashCode());
  }

  @Test
  public void testContainsAllInclusiveSet() {
    PathSpecSet allIncliveSet = PathSpecSet.allInclusive();

    Assert.assertTrue(allIncliveSet.contains(THREE_FIELD_MODEL_ALL_FIELDS));
    Assert.assertTrue(allIncliveSet.contains(THREE_FIELD_MODEL_FIELD3));
    Assert.assertTrue(allIncliveSet.contains(THREE_FIELD_MODEL_EMBEDDED_FIELD1));
  }

  @Test
  public void testContainsEmptyProjection() {
    PathSpecSet empty = PathSpecSet.empty();

    Assert.assertFalse(empty.contains(THREE_FIELD_MODEL_ALL_FIELDS));
    Assert.assertFalse(empty.contains(THREE_FIELD_MODEL_FIELD3));
    Assert.assertFalse(empty.contains(THREE_FIELD_MODEL_EMBEDDED_FIELD1));
  }

  @Test
  public void testContainsExactAndChild() {
    PathSpecSet pss1 = PathSpecSet.of(THREE_FIELD_MODEL_FIELD3);
    Assert.assertFalse(pss1.contains(THREE_FIELD_MODEL_ALL_FIELDS));
    Assert.assertTrue(pss1.contains(THREE_FIELD_MODEL_FIELD3));
    Assert.assertTrue(pss1.contains(THREE_FIELD_MODEL_EMBEDDED_FIELD1));
    Assert.assertFalse(pss1.contains(THREE_FIELD_MODEL_FIELD2));

    PathSpecSet pss2 = PathSpecSet.of(THREE_FIELD_MODEL_EMBEDDED_FIELD1);
    Assert.assertFalse(pss1.contains(THREE_FIELD_MODEL_ALL_FIELDS));
    Assert.assertFalse(pss2.contains(THREE_FIELD_MODEL_FIELD3));
    Assert.assertTrue(pss2.contains(THREE_FIELD_MODEL_EMBEDDED_FIELD1));
    Assert.assertTrue(pss2.contains(new PathSpec(THREE_FIELD_MODEL_EMBEDDED_FIELD1.getPathComponents(), "foo")));
    Assert.assertFalse(pss1.contains(THREE_FIELD_MODEL_FIELD2));

    PathSpecSet pss3 = PathSpecSet.of(THREE_FIELD_MODEL_ALL_FIELDS);
    Assert.assertTrue(pss3.contains(THREE_FIELD_MODEL_ALL_FIELDS));
    Assert.assertTrue(pss3.contains(THREE_FIELD_MODEL_FIELD3));
    Assert.assertTrue(pss3.contains(THREE_FIELD_MODEL_EMBEDDED_FIELD1));
  }

  @DataProvider
  public static Object[][] buildersWithAllInclusive() {
    return new Object[][]{
        {
            PathSpecSet.newBuilder()
                .add(PathSpecSet.allInclusive())
                .add(Lists.newArrayList("field1"))
                .add(THREE_FIELD_MODEL_FIELD2)
                .add(PathSpecSet.of(THREE_FIELD_MODEL_FIELD3))
        },
        {
            PathSpecSet.newBuilder()
                .add(Lists.newArrayList("field1"))
                .add(THREE_FIELD_MODEL_FIELD2)
                .add(PathSpecSet.of(THREE_FIELD_MODEL_FIELD3))
                .add(PathSpecSet.allInclusive())
        },
        {
            PathSpecSet.newBuilder()
                .add(Lists.newArrayList("field1"))
                .add(THREE_FIELD_MODEL_FIELD2)
                .add(PathSpecSet.allInclusive())
                .add(PathSpecSet.of(THREE_FIELD_MODEL_FIELD3))
        }
    };
  }

  @DataProvider
  public static Object[][] pathSpecCollections() {
    return new Object[][] {
        {
          Collections.singletonList(THREE_FIELD_MODEL_FIELD1),
        },
        {

          Arrays.asList(THREE_FIELD_MODEL_FIELD1, THREE_FIELD_MODEL_FIELD2),
        }
    };
  }

  @DataProvider
  public static Object[][] copyWithScopeProvider() {
    return new Object[][] {
        {
            PathSpecSet.of(THREE_FIELD_MODEL_FIELD1),
            NESTED_MODELS_SIMPLE_RECORD,
            PathSpecSet.of(new PathSpec("simpleRecord", "field1"))
        },
        {
            PathSpecSet.newBuilder()
                .add(THREE_FIELD_MODEL_FIELD1)
                .add(THREE_FIELD_MODEL_FIELD2)
                .build(),
            NESTED_MODELS_SIMPLE_RECORD,
            PathSpecSet.newBuilder()
                .add(new PathSpec("simpleRecord", "field1"))
                .add(new PathSpec("simpleRecord", "field2"))
                .build()
        },
        {
            PathSpecSet.of(THREE_FIELD_MODEL_FIELD1),
            NESTED_MODELS_ARRAY_ITEMS,
            PathSpecSet.of(new PathSpec("arrayOfRecords", "items", "field1"))
        },
        {
            PathSpecSet.of(THREE_FIELD_MODEL_FIELD1),
            NESTED_MODELS_MAP_VALUES,
            PathSpecSet.of(new PathSpec("mapOfRecords", "values", "field1"))
        },
        {
            PathSpecSet.empty(),
            NESTED_MODELS_SIMPLE_RECORD,
            PathSpecSet.empty()
        },
        {
            PathSpecSet.allInclusive(),
            NESTED_MODELS_SIMPLE_RECORD,
            PathSpecSet.of(NESTED_MODELS_SIMPLE_RECORD)
        }
    };
  }

  @DataProvider
  public static Object[][] copyAndRemovePrefixProvider() {
    return new Object[][] {
        {
            PathSpecSet.empty(),
            NESTED_MODELS_SIMPLE_RECORD,
            PathSpecSet.empty()
        },
        {
            PathSpecSet.allInclusive(),
            NESTED_MODELS_SIMPLE_RECORD,
            PathSpecSet.allInclusive()
        },
        {
            PathSpecSet.of(NESTED_MODELS_SIMPLE_RECORD),
            NESTED_MODELS_SIMPLE_RECORD,
            PathSpecSet.allInclusive()
        },
        {
            PathSpecSet.of(NESTED_MODELS_SIMPLE_RECORD, NESTED_MODELS_SIMPLE_RECORD_FIELD1),
            NESTED_MODELS_SIMPLE_RECORD,
            PathSpecSet.allInclusive()
        },
        {
            PathSpecSet.of(
                new PathSpec("recordField"),
                new PathSpec("recordField", "nestedRecord", "nestedField2"),
                new PathSpec("intField")
            ),
            new PathSpec("recordField", "nestedRecord"),
            PathSpecSet.allInclusive()
        },
        {
            PathSpecSet.of(
                new PathSpec("recordField", "nestedRecord", "nestedField1"),
                new PathSpec("recordField", "nestedRecord", "nestedField2"),
                new PathSpec("intField")
            ),
            new PathSpec("recordField", "nestedRecord"),
            PathSpecSet.of(
                new PathSpec("nestedField1"),
                new PathSpec("nestedField2")
            )
        },
        {
            PathSpecSet.of(NESTED_MODELS_ARRAY_ITEMS),
            NESTED_MODELS_SIMPLE_RECORD,
            PathSpecSet.empty()
        },
        {
            PathSpecSet.of(NESTED_MODELS_SIMPLE_RECORD_FIELD1, NESTED_MODELS_SIMPLE_RECORD_FIELD2,
                NESTED_MODELS_ARRAY_ITEMS),
            NESTED_MODELS_SIMPLE_RECORD,
            PathSpecSet.of(new PathSpec("intField"), new PathSpec("byteField"))
        },
        {
            PathSpecSet.of(),
            NESTED_MODELS_ARRAY_ITEMS,
            PathSpecSet.empty()
        }
    };
  }
}
