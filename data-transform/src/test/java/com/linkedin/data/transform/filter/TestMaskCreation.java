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

/**
 * $Id: $
 */

package com.linkedin.data.transform.filter;


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.transform.filter.request.MaskCreator;
import com.linkedin.data.transform.filter.request.MaskOperation;
import com.linkedin.data.transform.filter.request.MaskTree;

import java.io.IOException;

import java.nio.file.Path;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

import static com.linkedin.data.TestUtil.dataMapFromString;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class TestMaskCreation
{
  @Test
  public void testPositiveMaskSingleField()
  {
    MaskTree mask = MaskCreator.createPositiveMask(new PathSpec("foo"));
    Assert.assertEquals(mask.toString(), "{foo=1}");
  }

  @Test
  public void testPositiveMaskMultipleFields()
  {
    MaskTree mask = MaskCreator.createPositiveMask(new PathSpec("foo"), new PathSpec("bar"));

    //"{foo=1, bar=1}"
    final DataMap fooBarMap = new DataMap();
    fooBarMap.put("foo", MaskOperation.POSITIVE_MASK_OP.getRepresentation());
    fooBarMap.put("bar", MaskOperation.POSITIVE_MASK_OP.getRepresentation());
    Assert.assertEquals(mask.getDataMap(), fooBarMap, "The MaskTree DataMap should match");
  }

  @Test
  public void testPositiveMaskNestedFields()
  {
    MaskTree mask = MaskCreator.createPositiveMask(new PathSpec("foo", "bar"), new PathSpec("bar", "baz"), new PathSpec("qux"));

    //"{foo={bar=1}, bar={baz=1}, qux=1}"
    final DataMap fooBarQuxMap = new DataMap();
    fooBarQuxMap.put("qux", MaskOperation.POSITIVE_MASK_OP.getRepresentation());
    final DataMap barMap = new DataMap();
    barMap.put("baz", MaskOperation.POSITIVE_MASK_OP.getRepresentation());
    final DataMap fooMap = new DataMap();
    fooMap.put("bar", MaskOperation.POSITIVE_MASK_OP.getRepresentation());
    fooBarQuxMap.put("foo", fooMap);
    fooBarQuxMap.put("bar", barMap);
    Assert.assertEquals(mask.getDataMap(), fooBarQuxMap, "The MaskTree DataMap should match");
  }

  @Test
  public void testPositiveMaskWithArrayRange()
  {
    PathSpec parentPath = new PathSpec("parent");
    PathSpec childPath = new PathSpec(parentPath.getPathComponents(), "child");
    childPath.setAttribute(PathSpec.ATTR_ARRAY_START, 10);
    childPath.setAttribute(PathSpec.ATTR_ARRAY_COUNT, 5);
    PathSpec grandChildPath = new PathSpec(childPath.getPathComponents(), "grandChild");
    MaskTree mask = MaskCreator.createPositiveMask(childPath, grandChildPath);

    // {parent={child={$start=10, grandChild=1, $count=5}}}
    DataMap childMap = new DataMap();
    childMap.put(FilterConstants.START, 10);
    childMap.put(FilterConstants.COUNT, 5);
    childMap.put("grandChild", MaskOperation.POSITIVE_MASK_OP.getRepresentation());
    DataMap parentMap = new DataMap();
    parentMap.put("child", childMap);
    DataMap expectedMaskMap = new DataMap();
    expectedMaskMap.put("parent", parentMap);

    Assert.assertEquals(mask.getDataMap(), expectedMaskMap);

    Map<PathSpec, MaskOperation> operations = mask.getOperations();
    Assert.assertEquals(operations.size(), 2);
    Assert.assertEquals(operations.get(childPath), MaskOperation.POSITIVE_MASK_OP);
    Assert.assertEquals(operations.get(grandChildPath), MaskOperation.POSITIVE_MASK_OP);
  }

  @Test
  public void testPositiveMaskWithDefaultArrayRangeValues()
  {
    PathSpec parentPath = new PathSpec("parent");
    PathSpec childPath = new PathSpec(parentPath.getPathComponents(), "child");
    // Use the default value for both start and count
    childPath.setAttribute(PathSpec.ATTR_ARRAY_START, 0);
    childPath.setAttribute(PathSpec.ATTR_ARRAY_COUNT, Integer.MAX_VALUE);
    PathSpec grandChildPath = new PathSpec(childPath.getPathComponents(), "grandChild");
    MaskTree mask = MaskCreator.createPositiveMask(childPath, grandChildPath);

    // Build the expected map with both start and count filtered out
    // {parent={child={grandChild=1}}}
    DataMap childMap = new DataMap();
    childMap.put("grandChild", MaskOperation.POSITIVE_MASK_OP.getRepresentation());
    DataMap parentMap = new DataMap();
    parentMap.put("child", childMap);
    DataMap expectedMaskMap = new DataMap();
    expectedMaskMap.put("parent", parentMap);

    Assert.assertEquals(mask.getDataMap(), expectedMaskMap);

    Map<PathSpec, MaskOperation> operations = mask.getOperations();
    Assert.assertEquals(operations.size(), 1);
    Assert.assertEquals(operations.get(grandChildPath), MaskOperation.POSITIVE_MASK_OP);
  }

  @Test
  public void testPositiveMaskWithArrayWildcardAndRange()
  {
    PathSpec parentPath = new PathSpec("parent");
    PathSpec childPath = new PathSpec(parentPath.getPathComponents(), "child");
    childPath.setAttribute(PathSpec.ATTR_ARRAY_START, 10);
    childPath.setAttribute(PathSpec.ATTR_ARRAY_COUNT, 5);
    PathSpec grandChildrenPath = new PathSpec(childPath.getPathComponents(), PathSpec.WILDCARD);
    PathSpec specificGrandChildPath = new PathSpec(childPath.getPathComponents(), "TheKid");
    // The pathspec 'specificGrandChildPath' should show up in the mask as we have the wildcard specified for grand children
    MaskTree mask = MaskCreator.createPositiveMask(childPath, grandChildrenPath, specificGrandChildPath);

    // {parent={child={$*=1, $start=10, $count=5}}}
    DataMap childMap = new DataMap();
    childMap.put(FilterConstants.START, 10);
    childMap.put(FilterConstants.COUNT, 5);
    childMap.put(FilterConstants.WILDCARD, MaskOperation.POSITIVE_MASK_OP.getRepresentation());
    DataMap parentMap = new DataMap();
    parentMap.put("child", childMap);
    DataMap expectedMaskMap = new DataMap();
    expectedMaskMap.put("parent", parentMap);

    Assert.assertEquals(mask.getDataMap(), expectedMaskMap);

    Map<PathSpec, MaskOperation> operations = mask.getOperations();
    Assert.assertEquals(operations.size(), 2);
    Assert.assertEquals(operations.get(childPath), MaskOperation.POSITIVE_MASK_OP);
    Assert.assertEquals(operations.get(grandChildrenPath), MaskOperation.POSITIVE_MASK_OP);
  }

  @Test
  public void testPositiveMaskWithRandomAttributes()
  {
    PathSpec parentPath = new PathSpec("parent");
    PathSpec childPath = new PathSpec(parentPath.getPathComponents(), "child");
    childPath.setAttribute("random", 10); // This shouldn't be in the generated MaskTree
    childPath.setAttribute(PathSpec.ATTR_ARRAY_COUNT, 5);
    MaskTree mask = MaskCreator.createPositiveMask(childPath);

    // {parent={child={$count=5}}}
    DataMap childMap = new DataMap();
    childMap.put(FilterConstants.COUNT, 5);
    DataMap parentMap = new DataMap();
    parentMap.put("child", childMap);
    DataMap expectedMaskMap = new DataMap();
    expectedMaskMap.put("parent", parentMap);

    Assert.assertEquals(mask.getDataMap(), expectedMaskMap);

    // Create a copy of the childPath without the random attribute as the generated mask won't include those attributes
    PathSpec childPathCopy = new PathSpec(childPath.getPathComponents().toArray(new String[0]));
    childPathCopy.setAttribute(PathSpec.ATTR_ARRAY_COUNT, 5);

    Map<PathSpec, MaskOperation> operations = mask.getOperations();
    Assert.assertEquals(operations.size(), 1);
    Assert.assertEquals(operations.get(childPathCopy), MaskOperation.POSITIVE_MASK_OP);
  }

  @Test
  public void testPositiveMaskWithFullArrayRangeValues()
  {
    PathSpec parentPath = new PathSpec("parent");

    // Build the array field's path with range (0 to 999)
    PathSpec arrayFirstHalfPath = new PathSpec(parentPath.getPathComponents(), "arrayField");
    arrayFirstHalfPath.setAttribute(PathSpec.ATTR_ARRAY_START, 0);
    arrayFirstHalfPath.setAttribute(PathSpec.ATTR_ARRAY_COUNT, 1000);

    // Build the array field's path with range (1000 to Integer.MAX_INT)
    PathSpec arraySecondHalfPath = new PathSpec(parentPath.getPathComponents(), "arrayField");
    arraySecondHalfPath.setAttribute(PathSpec.ATTR_ARRAY_START, 1000);
    arraySecondHalfPath.setAttribute(PathSpec.ATTR_ARRAY_COUNT, Integer.MAX_VALUE);

    MaskTree mask = MaskCreator.createPositiveMask(arrayFirstHalfPath, arraySecondHalfPath);

    // Build the expected map with both start and count filtered out
    // {parent={arrayField={$*=1}}}
    DataMap parentMap = new DataMap();
    DataMap arrayFieldMap = new DataMap();
    arrayFieldMap.put(FilterConstants.WILDCARD, MaskOperation.POSITIVE_MASK_OP.getRepresentation());
    parentMap.put("arrayField", arrayFieldMap);
    DataMap expectedMaskMap = new DataMap();
    expectedMaskMap.put("parent", parentMap);

    Assert.assertEquals(mask.getDataMap(), expectedMaskMap);
  }

  @Test
  public void testNegativeMaskSingleField()
  {
    MaskTree mask = MaskCreator.createNegativeMask(new PathSpec("foo"));
    Assert.assertEquals(mask.toString(), "{foo=0}");
  }

  @Test
  public void testNegativeMaskMultipleFields()
  {
    MaskTree mask = MaskCreator.createNegativeMask(new PathSpec("foo"), new PathSpec("bar"));

    //"{foo=0, bar=0}"
    final DataMap fooBarMap = new DataMap();
    fooBarMap.put("foo", MaskOperation.NEGATIVE_MASK_OP.getRepresentation());
    fooBarMap.put("bar", MaskOperation.NEGATIVE_MASK_OP.getRepresentation());
    Assert.assertEquals(mask.getDataMap(), fooBarMap, "The MaskTree DataMap should match");
  }

  @Test
  public void testNegativeMaskNestedFields()
  {
    MaskTree mask = MaskCreator.createNegativeMask(new PathSpec("foo", "bar"), new PathSpec("bar", "baz"),
                                                   new PathSpec("qux"));

    //"{foo={bar=0}, bar={baz=0}, qux=0}"
    final DataMap fooBarQuxMap = new DataMap();
    fooBarQuxMap.put("qux", MaskOperation.NEGATIVE_MASK_OP.getRepresentation());
    final DataMap barMap = new DataMap();
    barMap.put("baz", MaskOperation.NEGATIVE_MASK_OP.getRepresentation());
    final DataMap fooMap = new DataMap();
    fooMap.put("bar", MaskOperation.NEGATIVE_MASK_OP.getRepresentation());
    fooBarQuxMap.put("foo", fooMap);
    fooBarQuxMap.put("bar", barMap);
    Assert.assertEquals(mask.getDataMap(), fooBarQuxMap, "The MaskTree DataMap should match");
  }

  @Test
  public void testMixedMask()
  {
    MaskTree mask = new MaskTree();
    mask.addOperation(new PathSpec("foo", "bar"), MaskOperation.POSITIVE_MASK_OP);
    mask.addOperation(new PathSpec("baz", "qux"), MaskOperation.NEGATIVE_MASK_OP);

    //"{baz={qux=0}, foo={bar=1}}"
    final DataMap bazFooMap = new DataMap();
    final DataMap bazMap = new DataMap();
    final DataMap fooMap = new DataMap();
    bazMap.put("qux", MaskOperation.NEGATIVE_MASK_OP.getRepresentation());
    fooMap.put("bar", MaskOperation.POSITIVE_MASK_OP.getRepresentation());
    bazFooMap.put("baz", bazMap);
    bazFooMap.put("foo", fooMap);
    Assert.assertEquals(mask.getDataMap(), bazFooMap, "The MaskTree DataMap should match");
  }

  @Test
  public void testMaskWithWildcard()
  {
    MaskTree mask = new MaskTree();
    PathSpec wildcardSpec = new PathSpec("foo", PathSpec.WILDCARD, "bar");
    PathSpec asterixSpec = new PathSpec("foo", "*", "bar");
    Assert.assertFalse(wildcardSpec.equals(asterixSpec));

    mask.addOperation(wildcardSpec, MaskOperation.POSITIVE_MASK_OP);
    Assert.assertEquals(mask.toString(), "{foo={$*={bar=1}}}");
    Assert.assertEquals(mask.getOperations().get(wildcardSpec), MaskOperation.POSITIVE_MASK_OP);
    Assert.assertEquals(mask.getOperations().get(asterixSpec), null);

    mask = new MaskTree();
    mask.addOperation(asterixSpec, MaskOperation.POSITIVE_MASK_OP);
    Assert.assertEquals(mask.toString(), "{foo={*={bar=1}}}");
    Assert.assertEquals(mask.getOperations().get(asterixSpec), MaskOperation.POSITIVE_MASK_OP);
    Assert.assertEquals(mask.getOperations().get(wildcardSpec), null);

    mask = new MaskTree();
    mask.addOperation(asterixSpec, MaskOperation.POSITIVE_MASK_OP);
    mask.addOperation(wildcardSpec, MaskOperation.NEGATIVE_MASK_OP);
    Assert.assertEquals(mask.getOperations().get(wildcardSpec), MaskOperation.NEGATIVE_MASK_OP);
    Assert.assertEquals(mask.getOperations().get(asterixSpec), MaskOperation.POSITIVE_MASK_OP);
  }

  @Test
  public void testMaskWithDollarField()
  {
    MaskTree mask = new MaskTree();
    mask.addOperation(new PathSpec("$foo"), MaskOperation.POSITIVE_MASK_OP);
    Assert.assertEquals(mask.toString(), "{$$foo=1}");
    Assert.assertEquals(mask.getOperations().get(new PathSpec("$$foo")), null);
    Assert.assertEquals(mask.getOperations().get(new PathSpec("$foo")), MaskOperation.POSITIVE_MASK_OP);
  }

  /**
   * When positive mask for a PathSpec is composed with another positive mask, which is sub-PathSpec, then
   * the result is a positive mask with PathSpec only for the parent, because positive mask means
   * "select field and all it's children".
   */
  @Test
  public void testComposingPositiveSubmasks()
  {
    MaskTree mask = new MaskTree();
    mask.addOperation(new PathSpec("a", "b", "c"), MaskOperation.POSITIVE_MASK_OP);
    mask.addOperation(new PathSpec("a", "b"), MaskOperation.POSITIVE_MASK_OP);
    mask.addOperation(new PathSpec("a"), MaskOperation.POSITIVE_MASK_OP);
    Assert.assertEquals(mask.toString(), "{a=1}");
  }

  /**
   * When negative mask for a PathSpec is composed with another negative mask, which is sub-PathSpec, then
   * the result is a negative mask with PathSpec only for the parent, because negative mask means
   * "remove field and all it's children".
   */
  @Test
  public void testComposingNegativeSubmasks()
  {
    MaskTree mask = new MaskTree();
    mask.addOperation(new PathSpec("a", "b", "c"), MaskOperation.NEGATIVE_MASK_OP);
    mask.addOperation(new PathSpec("a", "b"), MaskOperation.NEGATIVE_MASK_OP);
    mask.addOperation(new PathSpec("a"), MaskOperation.NEGATIVE_MASK_OP);
    Assert.assertEquals(mask.toString(), "{a=0}");
  }

  /**
   * When positive mask for a PathSpec is composed with negative mask, which is sub-PathSpec, then
   * the result is a mask, which selects all fields in PathSpec using wildcard with negative mask
   * for sub-PathSpec. This is because negative mask has a higher priority then a positive mask.
   * @throws IOException
   */
  @Test
  public void testComposingPositiveMaskWithNegativeSubmasks() throws IOException
  {
    MaskTree mask = new MaskTree();
    mask.addOperation(new PathSpec("a", "b", "c"), MaskOperation.NEGATIVE_MASK_OP);
    mask.addOperation(new PathSpec("a"), MaskOperation.POSITIVE_MASK_OP);
    Assert.assertEquals(mask.toString(), dataMapFromString("{'a': {'$*': 1, 'b': {'c': 0}}}".replace('\'', '"')).toString());
  }

  /**
   * When negative mask for a PathSpec is composed with positive mask, which is sub-PathSpec, then
   * the result is a negative mask with PathSpec only for the parent, because negative mask has a
   * higher priority then a positive mask.
   * @throws IOException
   */
  @Test
  public void testComposingNegativeMaskWithPositiveSubmasks() throws IOException
  {
    MaskTree mask = new MaskTree();
    mask.addOperation(new PathSpec("a", "b", "c"), MaskOperation.POSITIVE_MASK_OP);
    mask.addOperation(new PathSpec("a"), MaskOperation.NEGATIVE_MASK_OP);
    Assert.assertEquals(mask.toString(), "{a=0}");
  }
}
