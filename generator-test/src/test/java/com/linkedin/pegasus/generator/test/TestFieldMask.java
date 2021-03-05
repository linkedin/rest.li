package com.linkedin.pegasus.generator.test;

import com.linkedin.data.schema.FieldMask;
import com.linkedin.data.schema.PathSpecSet;
import com.linkedin.data.transform.filter.request.MaskCreator;
import com.linkedin.data.transform.filter.request.MaskTree;
import java.util.function.Function;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Tests the FieldMask builder APIs generated as part of data tempalte classes.
 */
public class TestFieldMask
{
  @Test
  public void testEmptyMask()
  {
    FieldMask mask = RecordTest.createMask();
    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.empty());
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }

  @Test
  public void testPrimitiveFields()
  {
    FieldMask mask = RecordTest.createMask()
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
    FieldMask mask = RecordTest.createMask()
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
    FieldMask mask = RecordTest.createMask()
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
    FieldMask mask = RecordTest.createMask()
        .withArrayField();

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        RecordTest.fields().arrayField()
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }

  @Test
  public void testArrayFieldRangeAttributes()
  {
    FieldMask mask = RecordTest.createMask()
        .withArrayField(10, 15);

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        RecordTest.fields().arrayField(10, 15)
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }

  @Test
  public void testArrayFieldNestedProjection()
  {
    FieldMask mask = ArrayTest.createMask()
        .withRecordArray(arrayMask -> arrayMask.withItems(RecordBar.ProjectionMask::withLocation));

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        ArrayTest.fields().recordArray().items().location()
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }

  @Test
  public void testArrayFieldNestedProjectionWithAttributes()
  {
    FieldMask mask = ArrayTest.createMask()
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
    FieldMask mask = MapTest.createMask()
        .withRecordInlineMap(mapMask -> mapMask.withValues(RecordInMap.ProjectionMask::withF));

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        MapTest.fields().recordInlineMap().values().f()
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }

  @Test
  public void testMapFieldDefaultProjection()
  {
    FieldMask mask = MapTest.createMask()
        .withRecordInlineMap();

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        MapTest.fields().recordInlineMap()
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }

  @Test
  public void testUnionFieldDefaultProjection()
  {
    FieldMask mask = UnionTest.createMask()
        .withUnionWithAliases();

    MaskTree tree = MaskCreator.createPositiveMask(PathSpecSet.of(
        UnionTest.fields().unionWithAliases()
    ));
    Assert.assertEquals(mask.getDataMap(), tree.getDataMap());
  }

  @Test
  public void testUnionFieldNestedProjection()
  {
    FieldMask mask = UnionTest.createMask()
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
    FieldMask mask = UnionTest.createMask()
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
}
