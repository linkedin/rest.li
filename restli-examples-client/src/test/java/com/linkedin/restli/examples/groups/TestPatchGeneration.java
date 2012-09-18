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

package com.linkedin.restli.examples.groups;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.transform.DataMapProcessor;
import com.linkedin.data.transform.filter.request.MaskCreator;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.data.transform.patch.Patch;
import com.linkedin.data.transform.patch.request.PatchCreator;
import com.linkedin.data.transform.patch.request.PatchOpFactory;
import com.linkedin.data.transform.patch.request.PatchTree;
import com.linkedin.restli.TestConstants;
import com.linkedin.restli.examples.groups.api.Group;
import com.linkedin.restli.examples.groups.api.Location;
import com.linkedin.restli.internal.common.URIMaskUtil;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class TestPatchGeneration
{

  @Test
  public void testFieldPaths()
  {
    PathSpec f = Group.fields().location().latitude();
    assertEquals(f.toString(), "/location/latitude");

    assertEquals(Group.fields().badge().toString(), "/badge");
    assertEquals(Location.fields().latitude().toString(), "/latitude");

    Group.Fields groupFields = Group.fields();
    List<PathSpec> fields = Arrays.asList(groupFields.id(), groupFields.location().latitude(),
                                       groupFields.location().longitude(), groupFields.name());
    assertEquals(fields.toString(), "[/id, /location/latitude, /location/longitude, /name]");
  }

  @Test
  public void testSimplePositiveMask() throws Exception
  {
    List<PathSpec> fields = Arrays.asList(Group.fields().name(), Group.fields().description(), Group.fields().id());
    MaskTree mask = MaskCreator.createPositiveMask(fields);
    assertEquals(mask.toString(), "{id=1, description=1, name=1}");
    assertEquals(URIMaskUtil.encodeMaskForURI(mask), "id,description,name");
  }

  @Test
  public void testNestedPositiveMask() throws Exception
  {
    List<PathSpec> fields = Arrays.asList(Group.fields().id(), Group.fields().location().latitude(), Group.fields().location().longitude(), Group.fields().name());
    MaskTree mask = MaskCreator.createPositiveMask(fields);
    assertEquals(mask.toString(), "{id=1, location={longitude=1, latitude=1}, name=1}");
    assertEquals(URIMaskUtil.encodeMaskForURI(mask), "id,location:(longitude,latitude),name");
  }


  @Test
  public void testNegativeMask() throws Exception
  {
    MaskTree mask = MaskCreator.createNegativeMask(Group.fields().badge(), Group.fields().id(),
                                                   Group.fields().owner().id());
    assertEquals(mask.toString(),"{id=0, owner={id=0}, badge=0}");
    assertEquals(URIMaskUtil.encodeMaskForURI(mask), "-id,owner:(-id),-badge");
  }

  @Test
  public void testExplicitUpdate()
  {
    PatchTree explicitUpdateSpec = new PatchTree();
    explicitUpdateSpec.addOperation(Group.fields().id(), PatchOpFactory.setFieldOp(42));
    explicitUpdateSpec.addOperation(Group.fields().name(), PatchOpFactory.setFieldOp("Foo"));
    explicitUpdateSpec.addOperation(Group.fields().description(), PatchOpFactory.REMOVE_FIELD_OP);
    assertEquals(explicitUpdateSpec.toString(), "{$set={id=42, name=Foo}, $delete=[description]}");
  }

  @Test
  public void testDiffFromNull() throws Exception
  {
    Group g1 = new Group();
    Group g2 = new Group(g1.data().copy());
    g2.setId(42);
    g2.setName("Some Group");
    PatchTree update = PatchCreator.diff(g1, g2);
    assertEquals(update.toString(), "{$set={id=42, name=Some Group}}");
  }

  @Test
  public void testDiffFromNullNested() throws Exception
  {
    Group g1 = new Group();
    Group g2 = new Group(g1.data().copy());
    Location loc = new Location();
    loc.setLatitude(42.0f);
    loc.setLongitude(17.0f);
    g2.setLocation(loc);
    PatchTree update = PatchCreator.diff(g1, g2);
    assertEquals(update.toString(), "{$set={location={longitude=17.0, latitude=42.0}}}");
  }

  @Test
  public void testDiffFromOverwrittenNested() throws Exception
  {
    Group g1 = new Group();
    Location loc1 = new Location();
    loc1.setLatitude(0.0f);
    loc1.setLongitude(0.0f);
    g1.setLocation(loc1);

    Group g2 = new Group(g1.data().copy());
    Location loc2 = new Location();
    loc2.setLatitude(42.0f);
    loc2.setLongitude(17.0f);
    g2.setLocation(loc2);
    PatchTree update = PatchCreator.diff(g1, g2);
    assertEquals(update.toString(), "{location={$set={longitude=17.0, latitude=42.0}}}");
  }

  @Test
  void testDiffFromNonNull() throws Exception
  {
    Group g1 = new Group();
    g1.setId(17);
    g1.setDescription("Some description");

    Group g2 = new Group(g1.data().copy());
    g2.setId(42);
    g2.setName("Some Group");
    PatchTree update = PatchCreator.diff(g1, g2);
    assertEquals(update.toString(), "{$set={id=42, name=Some Group}}");
  }

  @Test
  void testDiffRemove() throws Exception
  {
    Group g1 = new Group();
    g1.setId(17);
    g1.setDescription("Some description");
    Group g2 = new Group(g1.data().copy());
    g2.removeDescription();
    PatchTree update = PatchCreator.diff(g1, g2);
    assertEquals(update.toString(), "{$delete=[description]}");
  }

  @Test
  void testRoundtripDeleteField() throws Exception
  {
    Group g1 = new Group();
    g1.setId(17);
    g1.setDescription("Some description");
    Group g2 = new Group(g1.data().copy());
    g2.removeDescription();
    PatchTree update = PatchCreator.diff(g1, g2);

    assertEquals(update.toString(), "{$delete=[description]}");
    assertFalse(g1.equals(g2));

    DataMapProcessor processor = new DataMapProcessor(new Patch(), update.getDataMap(), g1.data());
    processor.run(false);

    assertEquals(g1, g2);
  }

  @Test
  void testRoundtripAddFields() throws Exception
  {
    Group g1 = new Group();
    g1.setId(17);
    g1.setDescription("Some description");

    Group g2 = new Group(g1.data().copy());
    g2.setId(42);
    g2.setName("Some Group");
    PatchTree update = PatchCreator.diff(g1, g2);
    assertEquals(update.toString(), "{$set={id=42, name=Some Group}}");
    assertFalse(g1.equals(g2));

    DataMapProcessor processor = new DataMapProcessor(new Patch(), update.getDataMap(), g1.data());
    processor.run(false);

    assertEquals(g1, g2);
  }

  @Test
  void testRoundtripAddEscapedField() throws Exception
  {
    Group g1 = new Group();
    g1.setId(17);
    g1.setDescription("Some description");

    Group g2 = new Group(g1.data().copy());
    g2.data().put("$foo", "value");
    PatchTree update = PatchCreator.diff(g1, g2);
    assertEquals(update.toString(), "{$set={$foo=value}}");
    assertFalse(g1.equals(g2));

    DataMapProcessor processor = new DataMapProcessor(new Patch(), update.getDataMap(), g1.data());
    processor.run(false);

    assertEquals(g1, g2);
  }

  @Test(groups = {TestConstants.TESTNG_GROUP_KNOWN_ISSUE})
  void testRoundtripModifyEscapedField() throws Exception
  {
    Group g1 = new Group();
    g1.data().put("$foo", new DataMap());

    Group g2 = new Group(g1.data().copy());
    ((DataMap)g2.data().get("$foo")).put("bar", 42);
    PatchTree update = PatchCreator.diff(g1, g2);
    assertEquals(update.toString(), "{$$foo={$set={bar=42}}}");
    assertFalse(g1.equals(g2));

    DataMapProcessor processor = new DataMapProcessor(new Patch(), update.getDataMap(), g1.data());
    processor.run(false);

    assertEquals(g1, g2);
  }
}
