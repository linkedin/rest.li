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

package com.linkedin.restli.server.test;


import com.linkedin.data.DataMap;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.restli.internal.server.util.ArgumentUtils;

import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class TestParseUtils
{

  @Test
  public void testMaskParse() throws Exception
  {
    //"{}"
    checkProjection("", new DataMap());

    final DataMap fooMap = new DataMap();
    fooMap.put("foo", 1);
    //"{foo=1}"
    checkProjection("foo", fooMap);

    final DataMap fooBarMap = new DataMap();
    fooBarMap.put("foo", 1);
    fooBarMap.put("bar", 1);
    //"{foo=1, bar=1}"
    checkProjection("foo,bar", fooBarMap);

    final DataMap fooBarSpaceMap = new DataMap();
    fooBarMap.put("foo", 1);
    fooBarMap.put("bar", 1);
    //"{foo=1, bar=1}"
    checkProjection("foo, bar", fooBarMap);

    final DataMap fooMapWithBar = new DataMap();
    final DataMap barMap = new DataMap();
    barMap.put("bar", 1);
    fooMapWithBar.put("foo", barMap);
    //"{foo={bar=1}}"
    checkProjection("foo:(bar)", fooMapWithBar);

    final DataMap foobazMapWithBar = new DataMap();
    foobazMapWithBar.put("baz", 1);
    foobazMapWithBar.put("foo", barMap);
    //"{baz=1, foo={bar=1}}"
    checkProjection("foo:(bar),baz", foobazMapWithBar);

    final DataMap foobazMapWithBarAndQux = new DataMap();
    final DataMap quxMap = new DataMap();
    quxMap.put("qux", 1);
    foobazMapWithBarAndQux.put("baz", quxMap);
    foobazMapWithBarAndQux.put("foo", barMap);
    //"{baz={qux=1}, foo={bar=1}}"
    checkProjection("foo:(bar),baz:(qux)", foobazMapWithBarAndQux);
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

  private void checkProjection(final String input, final DataMap expected) throws Exception
  {
    MaskTree mask = ArgumentUtils.parseProjectionParameter(input);
    Assert.assertEquals(mask.getDataMap(), expected);
  }

  private void expectException(String input)
  {
    try
    {
      ArgumentUtils.parseProjectionParameter(input);
      Assert.fail("Expected exception");
    }
    catch (Exception e)
    {

    }
  }
}
