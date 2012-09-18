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

package com.linkedin.restli.server.test;

import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.restli.internal.server.util.ArgumentUtils;

import org.testng.Assert;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class TestParseUtils
{

  @Test
  public void testMaskParse() throws Exception
  {
    checkProjection("", "{}");
    checkProjection("foo", "{foo=1}");
    checkProjection("foo,bar", "{foo=1, bar=1}");
    checkProjection("foo, bar", "{foo=1, bar=1}");

    checkProjection("foo:(bar)", "{foo={bar=1}}");
    checkProjection("foo:(bar),baz", "{baz=1, foo={bar=1}}");
    checkProjection("foo:(bar),baz:(qux)", "{baz={qux=1}, foo={bar=1}}");
  }

  @Test
  public void testMaskParseFailures()
  {
    expectException(":()");
    expectException(":(foo");
    expectException("foo)");
    expectException("foo:(bar");
    expectException("foo:bar");
    expectException("foo:(bar))");
  }

  private void checkProjection(String input, String expected) throws Exception
  {
    MaskTree mask = ArgumentUtils.parseProjectionParameter(input);
    assertEquals(mask.getDataMap().toString(), expected);
  }

  private void expectException(String input)
  {
    try
    {
      MaskTree mask = ArgumentUtils.parseProjectionParameter(input);
      Assert.fail("Expected exception");
    }
    catch (Exception e)
    {

    }
  }
}
