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

import static com.linkedin.data.TestUtil.dataMapFromString;

import java.io.IOException;

import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.transform.filter.request.MaskCreator;
import com.linkedin.data.transform.filter.request.MaskOperation;
import com.linkedin.data.transform.filter.request.MaskTree;
import org.testng.Assert;
import org.testng.annotations.Test;

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
    Assert.assertEquals(mask.toString(), "{foo=1, bar=1}");
  }

  @Test
  public void testPositiveMaskNestedFields()
  {
    MaskTree mask = MaskCreator.createPositiveMask(new PathSpec("foo", "bar"), new PathSpec("bar", "baz"), new PathSpec("qux"));
    Assert.assertEquals(mask.toString(), "{foo={bar=1}, bar={baz=1}, qux=1}");
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
    Assert.assertEquals(mask.toString(), "{foo=0, bar=0}");
  }

  @Test
  public void testNegativeMaskNestedFields()
  {
    MaskTree mask = MaskCreator.createNegativeMask(new PathSpec("foo", "bar"), new PathSpec("bar", "baz"),
                                                   new PathSpec("qux"));
    Assert.assertEquals(mask.toString(), "{foo={bar=0}, bar={baz=0}, qux=0}");
  }

  @Test
  public void testMixedMask()
  {
    MaskTree mask = new MaskTree();
    mask.addOperation(new PathSpec("foo", "bar"), MaskOperation.POSITIVE_MASK_OP);
    mask.addOperation(new PathSpec("baz", "qux"), MaskOperation.NEGATIVE_MASK_OP);
    Assert.assertEquals(mask.toString(), "{baz={qux=0}, foo={bar=1}}");
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
