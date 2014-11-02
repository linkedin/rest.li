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

package com.linkedin.data.transform.patch;


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.transform.patch.request.PatchCreator;
import com.linkedin.data.transform.patch.request.PatchOpFactory;
import com.linkedin.data.transform.patch.request.PatchTree;

import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;

import static com.linkedin.data.TestUtil.asMap;
import static org.testng.Assert.assertEquals;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class TestPatchCreation
{
  @Test
  public void testExplicitPatchCreationSet()
  {
    PatchTree patch = new PatchTree();
    patch.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(42));
    patch.addOperation(new PathSpec("bar", "baz"), PatchOpFactory.setFieldOp("The quick brown fox"));

    //"{$set={foo=42}, bar={$set={baz=The quick brown fox}}}"
    final DataMap setBarMap = new DataMap();
    final DataMap bazMap = new DataMap();
    bazMap.put("baz", "The quick brown fox");
    final DataMap setMap = new DataMap();
    setMap.put(PatchConstants.SET_COMMAND, bazMap);
    setBarMap.put("bar", setMap);
    final DataMap fooMap = new DataMap();
    fooMap.put("foo", 42);
    setBarMap.put(PatchConstants.SET_COMMAND, fooMap);
    assertEquals(patch.getDataMap(), setBarMap, "PatchTree DataMap must be correct");
  }

  @Test
  public void testExplicitPatchCreationRemove()
  {
    PatchTree patch = new PatchTree();
    patch.addOperation(new PathSpec("foo"), PatchOpFactory.REMOVE_FIELD_OP);
    patch.addOperation(new PathSpec("bar", "baz"), PatchOpFactory.REMOVE_FIELD_OP);
    Assert.assertEquals(patch.toString(), "{bar={$delete=[baz]}, $delete=[foo]}");
  }

  @Test
  public void testExplicitPatchCreationMixed()
  {
    PatchTree patch = new PatchTree();
    patch.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(42));
    patch.addOperation(new PathSpec("bar", "baz"), PatchOpFactory.REMOVE_FIELD_OP);
    patch.addOperation(new PathSpec("qux"), PatchOpFactory.REMOVE_FIELD_OP);

    //"{$set={foo=42}, bar={$delete=[baz]}, $delete=[qux]}"
    final DataMap fooMap = new DataMap();
    fooMap.put("foo", 42);
    final DataMap deleteMap = new DataMap();
    deleteMap.put(PatchConstants.DELETE_COMMAND, new DataList(Arrays.asList("baz")));
    final DataMap setBarDeleteMap = new DataMap();
    setBarDeleteMap.put(PatchConstants.SET_COMMAND, fooMap);
    setBarDeleteMap.put("bar", deleteMap);
    setBarDeleteMap.put(PatchConstants.DELETE_COMMAND, new DataList(Arrays.asList("qux")));
    assertEquals(patch.getDataMap(), setBarDeleteMap, "PatchTree DataMap must be correct");
  }

  @Test
  public void testDiffPatchCreationSet() throws Exception
  {
    DataMap map = new DataMap();
    DataMap map2 = map.copy();
    map2.put("foo", 42);
    map2.put("bar", new DataMap());
    ((DataMap)map2.get("bar")).put("baz", "The quick brown fox");

    PatchTree patch = PatchCreator.diff(map, map2);

    //"{$set={foo=42, bar={baz=The quick brown fox}}}"
    final DataMap bazMap = new DataMap();
    bazMap.put("baz", "The quick brown fox");
    final DataMap fooBarMap = new DataMap();
    fooBarMap.put("foo", 42);
    fooBarMap.put("bar", bazMap);
    final DataMap setMap = new DataMap();
    setMap.put(PatchConstants.SET_COMMAND, fooBarMap);
    assertEquals(patch.getDataMap(), setMap, "PatchTree DataMap must be correct");
  }

  @Test
  public void testDiffPatchCreationRemove() throws Exception
  {
    DataMap map = new DataMap(asMap(
            "foo", 42,
            "bar", new DataMap(asMap(
                "baz", "The quick brown fox"
            ))
    ));
    DataMap map2 = map.copy();
    map2.remove("foo");
    ((DataMap)map2.get("bar")).remove("baz");

    PatchTree patch = PatchCreator.diff(map, map2);
    Assert.assertEquals(patch.toString(), "{bar={$delete=[baz]}, $delete=[foo]}");
  }

  @Test
  public void testDiffPatchCreationMixed() throws Exception
  {
    DataMap map = new DataMap(asMap(
            "qux", 42,
            "bar", new DataMap(asMap(
                "baz", "The quick brown fox"
            ))
    ));

    DataMap map2 = map.copy();
    map2.remove("foo");
    ((DataMap)map2.get("bar")).remove("baz");
    map2.put("foo", 42);
    map2.remove("qux");

    PatchTree patch = new PatchTree();
    patch.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(42));
    patch.addOperation(new PathSpec("bar", "baz"), PatchOpFactory.REMOVE_FIELD_OP);
    patch.addOperation(new PathSpec("qux"), PatchOpFactory.REMOVE_FIELD_OP);

    //"{$set={foo=42}, bar={$delete=[baz]}, $delete=[qux]}"
    final DataMap fooMap = new DataMap();
    fooMap.put("foo", 42);
    final DataMap deleteMap = new DataMap();
    deleteMap.put(PatchConstants.DELETE_COMMAND, new DataList(Arrays.asList("baz")));
    final DataMap setBarDeleteMap = new DataMap();
    setBarDeleteMap.put(PatchConstants.SET_COMMAND, fooMap);
    setBarDeleteMap.put("bar", deleteMap);
    setBarDeleteMap.put(PatchConstants.DELETE_COMMAND, new DataList(Arrays.asList("qux")));
    assertEquals(patch.getDataMap(), setBarDeleteMap, "PatchTree DataMap must be correct");
  }

  @Test
  public void testDiffPatchCreationIdentical() throws Exception
  {
    DataMap map = new DataMap(asMap(
            "qux", 42,
            "bar", new DataMap(asMap(
                "baz", "The quick brown fox"
            ))
    ));
    PatchTree patch = PatchCreator.diff(map, map);
    Assert.assertEquals(patch.toString(), "{}");
  }
}
