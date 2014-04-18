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

/* $Id$ */
package test.r2.caprep;

import com.linkedin.r2.caprep.PassThroughFilter;
import com.linkedin.r2.caprep.ReplaceableFilter;
import com.linkedin.r2.filter.Filter;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.linkedin.r2.testutils.filter.BaseFilterTest;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class TestReplaceableFilter extends BaseFilterTest
{
  // TODO: Pseudo-test to get gradle working with TestNG
  @Test
  public void psuedoTest()
  {
    Assert.assertTrue(true);
  }

  @Override
  protected ReplaceableFilter getFilter()
  {
    return new ReplaceableFilter(new PassThroughFilter());
  }

  @Test
  public void testSetAndGet()
  {
    final ReplaceableFilter filter = getFilter();
    final Filter newFilter = new PassThroughFilter();

    Assert.assertTrue(!filter.getFilter().equals(newFilter));
    filter.setFilter(newFilter);
    Assert.assertEquals(newFilter, filter.getFilter());
  }
}
