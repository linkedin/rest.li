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

package com.linkedin.data.transform;


import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.transform.filter.request.MaskOperation;
import com.linkedin.data.transform.filter.request.MaskTree;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * DataList range mask and negative masks are not tested, since they are not supported by Rest.li.
 *
 * @author Keren Jin
 */
public class TestProjectionUtil
{
  @Test
  public void testNullFilter()
  {
    Assert.assertTrue(ProjectionUtil.isPathPresent(null, new PathSpec("foo")));
  }

  @Test
  public void testEmptyFilter()
  {
    final MaskTree filter = new MaskTree();

    Assert.assertFalse(ProjectionUtil.isPathPresent(filter, new PathSpec("foo")));
  }

  @Test
  public void testEmptyPath()
  {
    final MaskTree filter = new MaskTree();
    filter.addOperation(new PathSpec("foo"), MaskOperation.POSITIVE_MASK_OP);

    Assert.assertTrue(ProjectionUtil.isPathPresent(filter, new PathSpec()));
  }

  @Test
  public void testPositiveSinglePath()
  {
    final MaskTree filter = new MaskTree();
    filter.addOperation(new PathSpec("foo", "bar", "baz"), MaskOperation.POSITIVE_MASK_OP);

    // ancestor nodes are considered present if matched in path
    Assert.assertTrue(ProjectionUtil.isPathPresent(filter, new PathSpec("foo")));
    Assert.assertTrue(ProjectionUtil.isPathPresent(filter, new PathSpec("foo", "bar")));

    Assert.assertTrue(ProjectionUtil.isPathPresent(filter, new PathSpec("foo", "bar", "baz")));

    // all matched child nodes are considered present
    Assert.assertTrue(ProjectionUtil.isPathPresent(filter, new PathSpec("foo", "bar", "baz", "xyz")));
    Assert.assertTrue(ProjectionUtil.isPathPresent(filter, new PathSpec("foo", "bar", "baz", "abc", "xyz")));

    Assert.assertFalse(ProjectionUtil.isPathPresent(filter, new PathSpec("xyz")));
    Assert.assertFalse(ProjectionUtil.isPathPresent(filter, new PathSpec("foo", "baz")));
    Assert.assertFalse(ProjectionUtil.isPathPresent(filter, new PathSpec("foo", "xyz")));
    Assert.assertFalse(ProjectionUtil.isPathPresent(filter, new PathSpec("foo", "bar", "xyz")));
  }

  @Test
  public void testPositiveWithWildcardSinglePath()
  {
    final MaskTree filter = new MaskTree();
    filter.addOperation(new PathSpec("foo", PathSpec.WILDCARD, "baz"), MaskOperation.POSITIVE_MASK_OP);

    Assert.assertTrue(ProjectionUtil.isPathPresent(filter, new PathSpec("foo")));
    Assert.assertTrue(ProjectionUtil.isPathPresent(filter, new PathSpec("foo", "bar")));
    Assert.assertTrue(ProjectionUtil.isPathPresent(filter, new PathSpec("foo", "bar", "baz")));
    Assert.assertTrue(ProjectionUtil.isPathPresent(filter, new PathSpec("foo", "bar", "baz", "xyz")));
    Assert.assertTrue(ProjectionUtil.isPathPresent(filter, new PathSpec("foo", "bar", "baz", "abc", "xyz")));

    Assert.assertFalse(ProjectionUtil.isPathPresent(filter, new PathSpec("foo", "bar", "xyz")));
  }

  @Test
  public void testPositiveMultiPaths()
  {
    final MaskTree filter = new MaskTree();
    filter.addOperation(new PathSpec("foo", "bar", "baz"), MaskOperation.POSITIVE_MASK_OP);

    final Collection<PathSpec> positivePaths = new HashSet<PathSpec>(Arrays.asList(
      new PathSpec("foo"),
      new PathSpec("foo", "bar"),
      new PathSpec("foo", "bar", "baz"),
      new PathSpec("foo", "bar", "baz", "xyz"),
      new PathSpec("foo", "bar", "baz", "abc", "xyz")
    ));
    final Collection<PathSpec> negativePaths = new HashSet<PathSpec>(Arrays.asList(
      new PathSpec("xyz"),
      new PathSpec("foo", "baz"),
      new PathSpec("foo", "xyz"),
      new PathSpec("foo", "bar", "xyz")
    ));

    // test false positive
    final Set<PathSpec> positiveResult = ProjectionUtil.getPresentPaths(filter, new HashSet<PathSpec>(positivePaths));
    Assert.assertEquals(positiveResult, positivePaths);

    // test false negative
    final Set<PathSpec> negativeResult = ProjectionUtil.getPresentPaths(filter, new HashSet<PathSpec>(negativePaths));
    Assert.assertTrue(negativeResult.isEmpty());

    final Set<PathSpec> combinedPaths = new HashSet<PathSpec>(positivePaths);
    combinedPaths.addAll(negativePaths);

    // combine both to test internal ordering, overwrites, etc.
    final Set<PathSpec> combinedResult = ProjectionUtil.getPresentPaths(filter, combinedPaths);
    Assert.assertEquals(combinedResult, new HashSet<PathSpec>(positivePaths));

    for (PathSpec p : negativePaths)
    {
      Assert.assertFalse(combinedResult.contains(p));
    }
  }

  @Test
  public void testPositiveWithWildcardMultiPaths()
  {
    final MaskTree filter = new MaskTree();
    filter.addOperation(new PathSpec("foo", PathSpec.WILDCARD, "baz"), MaskOperation.POSITIVE_MASK_OP);

    final Collection<PathSpec> positivePaths = new HashSet<PathSpec>(Arrays.asList(
      new PathSpec("foo"),
      new PathSpec("foo", "bar"),
      new PathSpec("foo", "bar", "baz"),
      new PathSpec("foo", "bar", "baz", "xyz"),
      new PathSpec("foo", "bar", "baz", "abc", "xyz")
    ));
    final Collection<PathSpec> negativePaths = new HashSet<PathSpec>(Arrays.asList(
      new PathSpec("foo", "bar", "xyz")
    ));

    final Set<PathSpec> positiveResult = ProjectionUtil.getPresentPaths(filter, new HashSet<PathSpec>(positivePaths));
    Assert.assertEquals(positiveResult, positivePaths);

    final Set<PathSpec> negativeResult = ProjectionUtil.getPresentPaths(filter, new HashSet<PathSpec>(negativePaths));
    Assert.assertTrue(negativeResult.isEmpty());

    final Set<PathSpec> combinedPaths = new HashSet<PathSpec>(positivePaths);
    combinedPaths.addAll(negativePaths);

    final Set<PathSpec> combinedResult = ProjectionUtil.getPresentPaths(filter, combinedPaths);
    Assert.assertEquals(combinedResult, new HashSet<PathSpec>(positivePaths));

    for (PathSpec p : negativePaths)
    {
      Assert.assertFalse(combinedResult.contains(p));
    }
  }
}
