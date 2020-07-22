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


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.transform.DataComplexProcessor;
import com.linkedin.data.transform.filter.request.MaskCreator;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.data.transform.patch.Patch;
import com.linkedin.data.transform.patch.PatchConstants;
import com.linkedin.data.transform.patch.request.PatchCreator;
import com.linkedin.data.transform.patch.request.PatchOpFactory;
import com.linkedin.data.transform.patch.request.PatchTree;
import com.linkedin.restli.TestConstants;
import com.linkedin.restli.examples.groups.api.Group;
import com.linkedin.restli.examples.groups.api.Location;
import com.linkedin.restli.internal.common.URIMaskUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;


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

    //"{id=1, description=1, name=1}"
    final DataMap idDescriptionNameMap = new DataMap();
    idDescriptionNameMap.put("id", 1);
    idDescriptionNameMap.put("description", 1);
    idDescriptionNameMap.put("name", 1);
    assertEquals(mask.getDataMap(), idDescriptionNameMap, "The MaskTree DataMap should be correct");

    //The ordering might be different but the URI should look something like:
    //"id,description,name";
    final String actualEncodedMaskURI = URIMaskUtil.encodeMaskForURI(mask);
    final Set<String> maskURISet = new HashSet<String>(Arrays.asList(actualEncodedMaskURI.split(",")));
    final Set<String> expectedURISet = new HashSet<String>();
    expectedURISet.add("id");
    expectedURISet.add("description");
    expectedURISet.add("name");
    Assert.assertEquals(maskURISet, expectedURISet, "The encoded mask should be correct");
  }

  @Test
  public void testNestedPositiveMask() throws Exception
  {
    List<PathSpec> fields = Arrays.asList(Group.fields().id(), Group.fields().location().latitude(), Group.fields().location().longitude(), Group.fields().name());
    MaskTree mask = MaskCreator.createPositiveMask(fields);

    //"{id=1, location={longitude=1, latitude=1}, name=1}"
    final DataMap idLocationNameMap = new DataMap();
    idLocationNameMap.put("id", 1);
    idLocationNameMap.put("name", 1);
    final DataMap longLatMap = new DataMap();
    longLatMap.put("longitude", 1);
    longLatMap.put("latitude", 1);
    idLocationNameMap.put("location", longLatMap);
    Assert.assertEquals(mask.getDataMap(), idLocationNameMap, "The MaskTree DataMap should match");

    //The ordering might be different but the URI should look something like:
    //"id,location:(longitude,latitude),name";
    final String actualEncodedMaskURI = URIMaskUtil.encodeMaskForURI(mask);
    //We convert back into a MaskTree so we can compare DataMaps because the URI could be in any order
    final MaskTree generatedMaskTree = URIMaskUtil.decodeMaskUriFormat(actualEncodedMaskURI);
    Assert.assertEquals(generatedMaskTree.getDataMap(), idLocationNameMap, "The actual encoded Mask URI should be correct");
  }

  @Test
  public void testNegativeMask() throws Exception
  {
    MaskTree mask = MaskCreator.createNegativeMask(Group.fields().badge(), Group.fields().id(),
                                                   Group.fields().owner().id());

    //"{id=0, owner={id=0}, badge=0}"
    final DataMap idOwnerBadgeMap = new DataMap();
    idOwnerBadgeMap.put("id", 0);
    idOwnerBadgeMap.put("badge", 0);
    final DataMap idMap = new DataMap();
    idMap.put("id", 0);
    idOwnerBadgeMap.put("owner", idMap);
    Assert.assertEquals(mask.getDataMap(), idOwnerBadgeMap, "The MaskTree DataMap should match");

    //The ordering might be different but the URI should look something like:
    //"-id,owner:(-id),-badge";
    final String actualEncodedMaskURI = URIMaskUtil.encodeMaskForURI(mask);
    final Set<String> maskURISet = new HashSet<String>(Arrays.asList(actualEncodedMaskURI.split(",")));
    final Set<String> expectedURISet = new HashSet<String>();
    expectedURISet.add("-id");
    expectedURISet.add("owner:(-id)");
    expectedURISet.add("-badge");
    Assert.assertEquals(maskURISet, expectedURISet, "The encoded mask should be correct");
  }

  @Test
  public void testExplicitUpdate()
  {
    PatchTree explicitUpdateSpec = new PatchTree();
    explicitUpdateSpec.addOperation(Group.fields().id(), PatchOpFactory.setFieldOp(42));
    explicitUpdateSpec.addOperation(Group.fields().name(), PatchOpFactory.setFieldOp("Foo"));
    explicitUpdateSpec.addOperation(Group.fields().description(), PatchOpFactory.REMOVE_FIELD_OP);

    //"{$set={id=42, name=Foo}, $delete=[description]}"
    final DataMap setDeleteMap = new DataMap();
    final DataMap idNameMap = new DataMap();
    idNameMap.put("id", 42);
    idNameMap.put("name", "Foo");
    setDeleteMap.put(PatchConstants.SET_COMMAND, idNameMap);
    setDeleteMap.put(PatchConstants.DELETE_COMMAND, new DataList(Arrays.asList("description")));
    assertEquals(explicitUpdateSpec.getDataMap(), setDeleteMap, "PatchTree DataMap should be correct");
  }

  @Test
  public void testDiffFromNull() throws Exception
  {
    Group g1 = new Group();
    Group g2 = new Group(g1.data().copy());
    g2.setId(42);
    g2.setName("Some Group");
    PatchTree update = PatchCreator.diff(g1, g2);

    //"{$set={id=42, name=Some Group}}"
    final DataMap internalSetMap = new DataMap();
    internalSetMap.put("id", 42);
    internalSetMap.put("name", "Some Group");
    final DataMap setMap = new DataMap();
    setMap.put(PatchConstants.SET_COMMAND, internalSetMap);
    assertEquals(update.getDataMap(), setMap, "PatchTree DataMap should be correct");
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

    //"{$set={location={longitude=17.0, latitude=42.0}}}"
    final DataMap setMap = new DataMap();
    final DataMap latLongMap = new DataMap();
    latLongMap.put("longitude", 17.0f);
    latLongMap.put("latitude", 42.0f);
    final DataMap locationMap = new DataMap();
    locationMap.put("location", latLongMap);
    setMap.put(PatchConstants.SET_COMMAND, locationMap);
    assertEquals(update.getDataMap(), setMap, "PatchTree DataMap should be correct");
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

    //"{location={$set={longitude=17.0, latitude=42.0}}}"
    final DataMap setMap = new DataMap();
    final DataMap longLatMap = new DataMap();
    longLatMap.put("longitude", 17.0f);
    longLatMap.put("latitude", 42.0f);
    setMap.put(PatchConstants.SET_COMMAND, longLatMap);
    final DataMap locationMap = new DataMap();
    locationMap.put("location", setMap);
    Assert.assertEquals(update.getDataMap(), locationMap, "PatchTree DataMap should be correct");
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

    //"{$set={id=42, name=Some Group}}"
    final DataMap internalSetMap = new DataMap();
    internalSetMap.put("id", 42);
    internalSetMap.put("name", "Some Group");
    final DataMap setMap = new DataMap();
    setMap.put(PatchConstants.SET_COMMAND, internalSetMap);
    assertEquals(update.getDataMap(), setMap, "PatchTree DataMap should be correct");
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

    //"{$delete=[description]}"
    final DataMap deleteMap = new DataMap();
    final DataList descriptionList = new DataList();
    descriptionList.add("description");
    deleteMap.put(PatchConstants.DELETE_COMMAND, descriptionList);
    assertEquals(update.getDataMap(), deleteMap, "PatchTree DataMap should be correct");
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

    //"{$delete=[description]}"
    final DataMap deleteMap = new DataMap();
    final DataList descriptionList = new DataList();
    descriptionList.add("description");
    deleteMap.put(PatchConstants.DELETE_COMMAND, descriptionList);
    assertEquals(update.getDataMap(), deleteMap, "PatchTree DataMap should be correct");

    assertFalse(g1.equals(g2));

    DataComplexProcessor processor = new DataComplexProcessor(new Patch(), update.getDataMap(), g1.data());
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

    //"{$set={id=42, name=Some Group}}"
    final DataMap setMap = new DataMap();
    final DataMap idNameMap = new DataMap();
    idNameMap.put("id", 42);
    idNameMap.put("name", "Some Group");
    setMap.put(PatchConstants.SET_COMMAND, idNameMap);
    assertEquals(update.getDataMap(), setMap, "PatchTree DataMap should be correct");

    assertFalse(g1.equals(g2));

    DataComplexProcessor processor = new DataComplexProcessor(new Patch(), update.getDataMap(), g1.data());
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

    //"{$set={$foo=value}}"
    final DataMap setMap = new DataMap();
    final DataMap fooMap = new DataMap();
    fooMap.put("$foo", "value");
    setMap.put(PatchConstants.SET_COMMAND, fooMap);
    assertEquals(update.getDataMap(), setMap, "PatchTree DataMap should be correct");

    assertFalse(g1.equals(g2));

    DataComplexProcessor processor = new DataComplexProcessor(new Patch(), update.getDataMap(), g1.data());
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

    //"{$$foo={$set={bar=42}}}"
    final DataMap setMap = new DataMap();
    final DataMap barMap = new DataMap();
    barMap.put("bar", 42);
    setMap.put(PatchConstants.SET_COMMAND, barMap);
    final DataMap fooMap = new DataMap();
    fooMap.put("$$foo", setMap);
    assertEquals(update.getDataMap(), fooMap, "PatchTree DataMap must be correct");

    assertFalse(g1.equals(g2));

    DataComplexProcessor processor = new DataComplexProcessor(new Patch(), update.getDataMap(), g1.data());
    processor.run(false);

    assertEquals(g1, g2);
  }
}