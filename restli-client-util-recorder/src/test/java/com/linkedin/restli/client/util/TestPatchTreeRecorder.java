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

package com.linkedin.restli.client.util;


import com.linkedin.data.ByteString;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.SetMode;
import com.linkedin.data.template.TestCustom;
import com.linkedin.data.transform.patch.request.PatchCreator;
import com.linkedin.data.transform.patch.request.PatchTree;
import com.linkedin.data.transform.patch.request.RemoveFieldOp;
import com.linkedin.data.transform.patch.request.SetFieldOp;
import com.linkedin.pegasus.generator.test.CustomPointRecord;
import com.linkedin.restli.client.util.test.FooEnum;
import com.linkedin.restli.client.util.test.FooRecordTemplate;
import com.linkedin.restli.client.util.test.PatchTreeTestModel;

import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author jflorencio
 */
public class TestPatchTreeRecorder
{
  @Test
  public void testEmptyPatch()
  {
    PatchTreeRecorder<PatchTreeTestModel> pc = makeOne();
    Assert.assertEquals(pc.generatePatchTree().getDataMap(), new DataMap());
  }

  @Test
  public void testMethodsInheritedFromObjectOnProxy()
  {
    PatchTreeRecorder<PatchTreeTestModel> pc = makeOne();
    PatchTreeTestModel testModel = pc.getRecordingProxy();

    Assert.assertEquals(testModel.hashCode(), testModel.hashCode());
    Assert.assertNotNull(testModel.toString());
    Assert.assertTrue(testModel.equals(testModel));
    Assert.assertFalse(testModel.equals(new PatchTreeTestModel()));
  }

  @Test
  public void testSimpleSet()
  {
    PatchTreeRecorder<PatchTreeTestModel> pc = makeOne();
    PatchTreeTestModel testModel = pc.getRecordingProxy();

    testModel.setFooOptional(10l);
    testModel.setFooRequired(20l);

    Assert.assertEquals(pc.generatePatchTree().getDataMap(),
                        diffEmpty(new PatchTreeTestModel().setFooOptional(10l).setFooRequired(20l)));
  }

  @Test
  public void testSetCoerceEnum()
  {
    PatchTreeRecorder<PatchTreeTestModel> pc = makeOne();
    pc.getRecordingProxy().setFooEnum(FooEnum.A);

    Assert.assertEquals(pc.generatePatchTree().getDataMap(),
                        diffEmpty(new PatchTreeTestModel().setFooEnum(FooEnum.A)));
  }

  @Test
  public void testSetCoerceTypeRef()
  {
    PatchTreeRecorder<CustomPointRecord> pc = new PatchTreeRecorder<CustomPointRecord>(CustomPointRecord.class);
    pc.getRecordingProxy().setCustomPoint(new TestCustom.CustomPoint(1, 2));

    Assert.assertEquals(pc.generatePatchTree().getDataMap(),
                        diffEmpty(new CustomPointRecord().setCustomPoint(new TestCustom.CustomPoint(1, 2))));
  }

  @Test
  public void testSetRecordTemplate()
  {
    PatchTreeRecorder<PatchTreeTestModel> pc = makeOne();
    pc.getRecordingProxy().setFooRecordTemplate(new FooRecordTemplate().setBar(20));

    PatchTreeTestModel expectModel = new PatchTreeTestModel().
                            setFooRecordTemplate(new FooRecordTemplate().setBar(20));

    Assert.assertEquals(pc.generatePatchTree().getDataMap(),
                        diffEmpty(expectModel));
  }

  @Test
  public void testSetBytes()
  {
    PatchTreeRecorder<PatchTreeTestModel> pc = makeOne();
    pc.getRecordingProxy().setFooByteString(ByteString.copyString("foo", "UTF-8"));

    Assert.assertEquals(pc.generatePatchTree().getDataMap(),
                        diffEmpty(new PatchTreeTestModel().setFooByteString(ByteString.copyString("foo", "UTF-8"))));
  }

  @Test
  public void testSimpleSetIgnoreNullSetMode()
  {
    PatchTreeRecorder<PatchTreeTestModel> pc = makeOne();
    pc.getRecordingProxy().setFooRequired(100).setFooOptional(null, SetMode.IGNORE_NULL);
    Assert.assertEquals(pc.generatePatchTree().getDataMap(), diffEmpty(new PatchTreeTestModel().setFooRequired(100)));
  }

  @Test
  public void testSimpleSetRemoveIfNullSetMode()
  {
    PatchTreeRecorder<PatchTreeTestModel> pc = makeOne();
    PatchTreeTestModel testModel = pc.getRecordingProxy();

    testModel.setFooOptional(null, SetMode.REMOVE_IF_NULL);

    // Augment the patch request with the removes
    PatchTree ptExpect = new PatchTree();
    ptExpect.addOperation(PatchTreeTestModel.fields().fooOptional(), new RemoveFieldOp());

    Assert.assertEquals(pc.generatePatchTree().getDataMap(), ptExpect.getDataMap());
  }

  @Test
  public void testSimpleSetRemoveOptionalIfNullWithValue()
  {
    PatchTreeRecorder<PatchTreeTestModel> pc = makeOne();
    PatchTreeTestModel testModel = pc.getRecordingProxy();

    testModel.setFooOptional(10l, SetMode.REMOVE_OPTIONAL_IF_NULL);
    Assert.assertEquals(pc.generatePatchTree().getDataMap(),
                        diffEmpty(new PatchTreeTestModel().setFooOptional(10l)));
  }

  @Test
  public void testSimpleSetRemoveOptionalIfNullOnOptionalFieldPass()
  {
    PatchTreeRecorder<PatchTreeTestModel> pc = makeOne();
    PatchTreeTestModel restCommonTestModel = pc.getRecordingProxy();

    restCommonTestModel.setFooOptional(null, SetMode.REMOVE_OPTIONAL_IF_NULL);
    // Augment the patch request with the removes
    PatchTree ptExpect = new PatchTree();
    ptExpect.addOperation(PatchTreeTestModel.fields().fooOptional(), new RemoveFieldOp());

    Assert.assertEquals(pc.generatePatchTree().getDataMap(),
                        ptExpect.getDataMap());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSimpleSetRemoveOptionalIfNullOnRequiredFieldFail()
  {
    makeOne().getRecordingProxy().setFooRequired(null, SetMode.REMOVE_OPTIONAL_IF_NULL);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testSimpleSetDisallowNull()
  {
    makeOne().getRecordingProxy().setFooRequired(null, SetMode.DISALLOW_NULL);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testSimpleSetDisallowNullDefault()
  {
    makeOne().getRecordingProxy().setFooRequired(null);
  }

  @Test
  public void testFluentSet()
  {
    PatchTreeRecorder<PatchTreeTestModel> pc = makeOne();
    PatchTreeTestModel testModel = pc.getRecordingProxy();

    testModel.setFooOptional(100).setFooEnum(FooEnum.B);

    Assert.assertEquals(pc.generatePatchTree().getDataMap(),
                        diffEmpty(new PatchTreeTestModel().setFooOptional(100).setFooEnum(FooEnum.B)));
  }

  @Test
  public void testFluentSetWithRemove()
  {
    PatchTreeRecorder<PatchTreeTestModel> pc = makeOne();
    PatchTreeTestModel testModel = pc.getRecordingProxy();

    PatchTreeTestModel.FooUnion fooUnion = new PatchTreeTestModel.FooUnion();
    fooUnion.setInt(10);
    testModel.setFooRequired(100).setFooUnion(fooUnion).setFooRecordTemplate(null, SetMode.REMOVE_IF_NULL).removeFooOptional();

    PatchTree ptExpect = PatchCreator.diff(new DataMap(),
                                           new PatchTreeTestModel().setFooRequired(100).setFooUnion(fooUnion).data());
    // Augment the patch request with the removes
    ptExpect.addOperation(PatchTreeTestModel.fields().fooRecordTemplate(), new RemoveFieldOp());
    ptExpect.addOperation(PatchTreeTestModel.fields().fooOptional(), new RemoveFieldOp());

    Assert.assertEquals(pc.generatePatchTree().getDataMap(), ptExpect.getDataMap());
  }

  @Test
  public void testComplexDeepSetAndRemoves()
  {
    PatchTreeRecorder<PatchTreeTestModel> pc = makeOne();
    PatchTreeTestModel testModel = pc.getRecordingProxy();

    PatchTreeTestModel.FooUnion fooUnion = new PatchTreeTestModel.FooUnion();
    fooUnion.setInt(10);
    testModel.setFooRequired(100).setFooUnion(fooUnion).setFooOptional(null, SetMode.REMOVE_IF_NULL);
    testModel.getFooRecordTemplate().setBar(9001l);
    // GetMode should be irrelevant
    testModel.getFooRecordTemplate(GetMode.DEFAULT).setBaz(null, SetMode.REMOVE_IF_NULL);

    PatchTree ptExpect = PatchCreator.diff(new DataMap(),
                                           new PatchTreeTestModel().setFooRequired(100).setFooUnion(fooUnion).data());

    // Augment the patch request with the removes in the same order so we get the same patch request.
    ptExpect.addOperation(PatchTreeTestModel.fields().fooOptional(), new RemoveFieldOp());
    ptExpect.addOperation(PatchTreeTestModel.fields().fooRecordTemplate().bar(), new SetFieldOp(9001l));
    ptExpect.addOperation(PatchTreeTestModel.fields().fooRecordTemplate().baz(), new RemoveFieldOp());

    Assert.assertEquals(pc.generatePatchTree().getDataMap(),
                        ptExpect.getDataMap());
  }

  @Test
  public void testGeneratingPatchTwiceIsEqual()
  {
    PatchTreeRecorder<PatchTreeTestModel> pc = makeOne();
    pc.getRecordingProxy().setFooOptional(10l);

    Assert.assertEquals(pc.generatePatchTree().getDataMap(),
                        pc.generatePatchTree().getDataMap());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testPropagateIntoUnsupportedComplexType()
  {
    PatchTreeRecorder<PatchTreeTestModel> pc = makeOne();
    pc.getRecordingProxy().getFooUnion();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testPropagateIntoUnsupportedSimpleType()
  {
    PatchTreeRecorder<PatchTreeTestModel> pc = makeOne();
    pc.getRecordingProxy().getFooRequired();
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testUnsupportedMethod()
  {
    makeOne().getRecordingProxy().schema();
  }

  @Test
  public void testPatchGeneratesDeepCopiesOfInternalState()
  {
    PatchTreeRecorder<PatchTreeTestModel> pc = makeOne();
    PatchTreeTestModel restCommonTestModel = pc.getRecordingProxy();

    restCommonTestModel.setFooRecordTemplate(new FooRecordTemplate().setBar(10l));
    PatchTree pt1 = pc.generatePatchTree();

    restCommonTestModel.setFooRecordTemplate(new FooRecordTemplate().setBar(20l));
    PatchTree pt2 = pc.generatePatchTree();

    Assert.assertNotEquals(pt1.getDataMap(), pt2.getDataMap());
    Assert.assertEquals(pt1.getDataMap(),
                        diffEmpty(new PatchTreeTestModel().setFooRecordTemplate(new FooRecordTemplate().setBar(10l))));
    Assert.assertEquals(pt2.getDataMap(),
                        diffEmpty(new PatchTreeTestModel().setFooRecordTemplate(new FooRecordTemplate().setBar(20l))));
  }

  private PatchTreeRecorder<PatchTreeTestModel> makeOne()
  {
    return new PatchTreeRecorder<PatchTreeTestModel>(PatchTreeTestModel.class);
  }

  private <T extends RecordTemplate> DataMap diffEmpty(T recordTemplate)
  {
    return PatchCreator.diff(new DataMap(), recordTemplate.data()).getDataMap();
  }
}
