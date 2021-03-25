package com.linkedin.pegasus.generator.test;

import com.linkedin.data.schema.MaskMap;
import com.linkedin.data.schema.PathSpecSet;
import com.linkedin.data.transform.filter.request.MaskCreator;
import com.linkedin.data.transform.filter.request.MaskTree;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Tests the MaskMap builder APIs generated as part of data template classes.
 */
public class TestMaskMap
{
  @Test
  public void testEmptyMask()
  {
    MaskMap mask = RecordTest.createMask();
    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.empty());
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }

  @Test
  public void testPrimitiveFields()
  {
    MaskMap mask = RecordTest.createMask()
        .withBooleanField()
        .withDoubleField()
        .withEnumField()
        .withFloatField();

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        RecordTest.fields().booleanField(),
        RecordTest.fields().doubleField(),
        RecordTest.fields().enumField(),
        RecordTest.fields().floatField()
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }

  @Test
  public void testComplexFields()
  {
    MaskMap mask = RecordTest.createMask()
        .withRecordField()
        .withRecordInlineField();

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        RecordTest.fields().recordField(),
        RecordTest.fields().recordInlineField()
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }

  @Test
  public void testNestedFields()
  {
    MaskMap mask = RecordTest.createMask()
        .withRecordField(nestedMask -> nestedMask.withLocation().withOptionalLocation());

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        RecordTest.fields().recordField().location(),
        RecordTest.fields().recordField().optionalLocation()
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }

  @Test
  public void testArrayFieldDefaultProjection()
  {
    MaskMap mask = RecordTest.createMask()
        .withArrayField();

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        RecordTest.fields().arrayField()
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }

  @Test
  public void testArrayFieldRangeAttributes()
  {
    MaskMap mask = RecordTest.createMask()
        .withArrayField(10, 15);

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        RecordTest.fields().arrayField(10, 15)
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }

  @Test
  public void testArrayFieldNestedProjection()
  {
    MaskMap mask = ArrayTest.createMask()
        .withRecordArray(arrayMask -> arrayMask.withItems(RecordBar.ProjectionMask::withLocation));

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        ArrayTest.fields().recordArray().items().location()
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }

  @Test
  public void testArrayFieldNestedProjectionWithAttributes()
  {
    MaskMap mask = ArrayTest.createMask()
        .withRecordArray(arrayMask -> arrayMask.withItems(RecordBar.ProjectionMask::withLocation),
            5, 10);

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        ArrayTest.fields().recordArray(5, 10),
        ArrayTest.fields().recordArray().items().location()
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }

  @Test
  public void testMapFieldNestedProjection()
  {
    MaskMap mask = MapTest.createMask()
        .withRecordInlineMap(mapMask -> mapMask.withValues(RecordInMap.ProjectionMask::withF));

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        MapTest.fields().recordInlineMap().values().f()
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }

  @Test
  public void testMapFieldDefaultProjection()
  {
    MaskMap mask = MapTest.createMask()
        .withRecordInlineMap();

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        MapTest.fields().recordInlineMap()
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }

  @Test
  public void testUnionFieldDefaultProjection()
  {
    MaskMap mask = UnionTest.createMask()
        .withUnionWithAliases();

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        UnionTest.fields().unionWithAliases()
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }

  @Test
  public void testUnionFieldNestedProjection()
  {
    MaskMap mask = UnionTest.createMask()
        .withUnionWithAliases(unionMask -> unionMask.withMemAnotherInt()
            .withMemInt()
            .withMemBoolean()
            .withMemAnotherMap()
            .withMemMap());

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        UnionTest.fields().unionWithAliases().MemAnotherInt(),
        UnionTest.fields().unionWithAliases().MemInt(),
        UnionTest.fields().unionWithAliases().MemBoolean(),
        UnionTest.fields().unionWithAliases().MemAnotherMap(),
        UnionTest.fields().unionWithAliases().MemMap()
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }

  @Test
  public void testNonAliasedUnionFieldNestedProjection()
  {
    MaskMap mask = UnionTest.createMask()
        .withUnionWithoutNull(unionMask -> unionMask.withArray()
            .withBoolean()
            .withRecordBar(recordMask -> recordMask.withLocation())
            .withString()
            .withBytes());

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        UnionTest.fields().unionWithoutNull().Array(),
        UnionTest.fields().unionWithoutNull().Boolean(),
        UnionTest.fields().unionWithoutNull().RecordBar().location(),
        UnionTest.fields().unionWithoutNull().String(),
        UnionTest.fields().unionWithoutNull().Bytes()
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }

  // Tests the case where a partial mask is  created and updated later.
  @Test
  public void testReuseMaskSimpleFields()
  {
    RecordTest.ProjectionMask mask = RecordTest.createMask()
        .withBooleanField();

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        RecordTest.fields().booleanField()));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());

    // Now update the mask to add new fields.
    mask.withDoubleField();

    MaskTree tree2 = MaskCreator.createPositiveMask(PathSpecSet.of(
        RecordTest.fields().booleanField(),
        RecordTest.fields().doubleField()));
    Assert.assertEquals(mask.getDataMap(), tree2.getDataMap());
  }

  @Test
  public void testReuseMaskNestedRecord()
  {
    RecordTest.ProjectionMask mask = RecordTest.createMask()
        .withBooleanField()
        .withRecordField(nestedMask -> nestedMask.withLocation());

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        RecordTest.fields().booleanField(),
        RecordTest.fields().recordField().location()));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());

    // Now update the mask to add new fields.
    mask.withDoubleField()
        .withRecordField(nestedMask -> nestedMask.withOptionalLocation());

    MaskTree tree2 = MaskCreator.createPositiveMask(PathSpecSet.of(
        RecordTest.fields().booleanField(),
        RecordTest.fields().doubleField(),
        RecordTest.fields().recordField().location(),
        RecordTest.fields().recordField().optionalLocation()));
    Assert.assertEquals(mask.getDataMap(), tree2.getDataMap());
  }

  @Test
  public void testReuseMaskNestedRecordClearing()
  {
    RecordTest.ProjectionMask mask = RecordTest.createMask()
        .withRecordField(nestedMask -> nestedMask.withLocation());

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        RecordTest.fields().recordField().location()));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());

    // Clear the nested mask by projecting the entire field.
    mask.withRecordField();
    tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        RecordTest.fields().recordField()));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());

    // Now update the mask to add new fields.
    mask.withRecordField(nestedMask -> nestedMask.withOptionalLocation());

    tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        RecordTest.fields().recordField().optionalLocation()));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }

  @Test
  public void testReuseMaskNestedArray()
  {
    ArrayTest.ProjectionMask mask = ArrayTest.createMask()
        .withRecordArray(arrayMask -> arrayMask.withItems(RecordBar.ProjectionMask::withLocation));

    // Now update the mask to add new fields.
    mask.withRecordArray(arrayMask -> arrayMask.withItems(nestedMask -> nestedMask.withOptionalLocation()));

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        ArrayTest.fields().recordArray().items().location(),
        ArrayTest.fields().recordArray().items().optionalLocation()
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());

    // Reset the nested mask by projecting all
    mask.withRecordArray(0, 10);
    tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        ArrayTest.fields().recordArray(0, 10)
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());

    // Apply the nested mask again
    mask.withRecordArray(arrayMask -> arrayMask.withItems(nestedMask -> nestedMask.withOptionalLocation()));
    tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        ArrayTest.fields().recordArray().items().optionalLocation()
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }

  @Test
  public void testReuseUnionFieldNestedProjection()
  {
    UnionTest.ProjectionMask mask = UnionTest.createMask()
        .withUnionWithAliases(unionMask -> unionMask.withMemAnotherInt()
            .withMemAnotherMap()
            .withMemMap());

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        UnionTest.fields().unionWithAliases().MemAnotherInt(),
        UnionTest.fields().unionWithAliases().MemAnotherMap(),
        UnionTest.fields().unionWithAliases().MemMap()
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());

    mask.withUnionWithAliases(unionMask -> unionMask.withMemInt()
            .withMemBoolean());

    tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        UnionTest.fields().unionWithAliases().MemAnotherInt(),
        UnionTest.fields().unionWithAliases().MemInt(),
        UnionTest.fields().unionWithAliases().MemBoolean(),
        UnionTest.fields().unionWithAliases().MemAnotherMap(),
        UnionTest.fields().unionWithAliases().MemMap()
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }
}
