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
package com.linkedin.restli.server.util;

import static org.testng.Assert.assertEquals;

import java.io.IOException;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.restli.common.PatchRequest;

/**
 * @author jodzga
 */
public class TestPatchHelper
{
  private static final JacksonDataCodec codec = new JacksonDataCodec();

  @DataProvider(name="data")
  public Object[][] statusData()
  {
    return new Object[][]
      {
        //first element is pair (patch, projection), second element is expected patch
        { new String[] {"{}", "{}"} , "{}" },
        { new String[] {"{'$set': {'a': 'aaa'}}", "{}"} , "{}" },
        { new String[] {"{'$set': {'a': 'aaa'}}", "{'b': 1}"} , "{}" },
        { new String[] {"{'$set': {'a': 'aaa'}}", "{'a': 1}"} , "{'$set': {'a': 'aaa'}}" },
        { new String[] {"{'$set': {'a': 'aaa', 'b': 'bbb'}}", "{'a': 1}"} , "{'$set': {'a': 'aaa'}}" },
        { new String[] {"{'x': {'$set': {'a': 'aaa'}}}", "{'a': 1}"} , "{}" },
        { new String[] {"{'x': {'$set': {'a': 'aaa'}}}", "{'x': {'a': 1}}"} , "{'x': {'$set': {'a': 'aaa'}}}" },
        { new String[] {"{'$set': {'x': {'a': 'aaa'}}}", "{'x': {'a': 1}}"} , "{'$set': {'x': {'a': 'aaa'}}}" },
        { new String[] {"{'$set': {'x': {'a': 'aaa', 'b': 'bbb'}}}", "{'x': {'a': 1}}"} , "{'$set': {'x': {'a': 'aaa'}}}" },
        { new String[] {"{'$delete': ['a']}", "{}"} , "{}" },
        { new String[] {"{'$delete': ['a']}", "{'b': 1}"} , "{}" },
        { new String[] {"{'$delete': ['a']}", "{'a': 1}"} , "{'$delete': ['a']}" },
        { new String[] {"{'$delete': ['a', 'b']}", "{'a': 1}"} , "{'$delete': ['a']}" },
        { new String[] {"{'x': {'$delete': ['a']}}", "{'a': 1}"} , "{}" },
        { new String[] {"{'x': {'$delete': ['a']}}", "{'x': {'a': 1}}"} , "{'x': {'$delete': ['a']}}" },
        { new String[] {"{'$set': {'x': {'a': 'aaa', 'b': 'bbb'}}}", "{'x': {'a': 0}}"} , "{'$set': {'x': {'b': 'bbb'}}}" },
        { new String[] {"{'$set': {'x': {'a': 'aaa', 'b': 'bbb'}}, '$delete': ['y']}", "{'x': {'a': 0}}"} , "{'$set': {'x': {'b': 'bbb'}}, '$delete': ['y']}" },
        { new String[] {"{'$set': {'x': {'a': 'aaa', 'b': 'bbb'}}, '$delete': ['y']}", "{'x': {'a': 0}}"} , "{'$set': {'x': {'b': 'bbb'}}, '$delete': ['y']}" },
        { new String[] {"{'x': {'$set': {'a': 'aaa'}}, '$delete': ['y']}", "{'x': {'a': 1}}"} , "{'x': {'$set': {'a': 'aaa'}}}" },
        { new String[] {"{'x': {'$set': {'a': 'aaa'}}, '$delete': ['y']}", "{'x': {'a': 1}, 'z': 1}"} , "{'x': {'$set': {'a': 'aaa'}}}" },
        { new String[] {"{'x': {'$set': {'a': 'aaa'}}, '$delete': ['y']}", "{'y': 1}"} , "{'$delete': ['y']}" },
      };
  }

  @Test(dataProvider = "data")
  public void testProjectionOnPatch(String[] patchAndProjection, String expectedResult) throws IOException
  {
    DataMap patch = dataMapFromString(patchAndProjection[0].replace('\'', '"'));
    DataMap projection = dataMapFromString(patchAndProjection[1].replace('\'', '"'));
    DataMap expected = dataMapFromString(expectedResult.replace('\'', '"'));
    assertEquals(PatchHelper.applyProjection(PatchRequest.<RecordTemplate>createFromPatchDocument(patch), new MaskTree(projection)).getPatchDocument(), expected);
  }

  private DataMap dataMapFromString(String json) throws IOException
  {
    return codec.stringToMap(json);
  }

}
