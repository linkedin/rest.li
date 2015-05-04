/*
   Copyright (c) 2015 LinkedIn Corp.

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


import com.linkedin.restli.client.test.TestRecord;
import com.linkedin.restli.common.PatchRequest;

import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Soojung Ha
 */
public class TestPatchRequestRecorder
{
  @Test
  public void testPatchGenerateAndPatchRequestRecorderGenerateIdenticalPatches()
      throws CloneNotSupportedException
  {
    TestRecord t1 = new TestRecord();
    TestRecord t2 = new TestRecord(t1.data().copy());
    t2.setId(1L);
    t2.setMessage("Foo Bar Baz");
    PatchRequest<TestRecord> patchFromGenerator = PatchGenerator.diff(t1, t2);

    PatchRequestRecorder<TestRecord> patchRecorder = new PatchRequestRecorder<TestRecord>(TestRecord.class);
    patchRecorder.getRecordingProxy().setId(1L).setMessage("Foo Bar Baz");
    PatchRequest<TestRecord> patchFromRecorder = patchRecorder.generatePatchRequest();

    Assert.assertEquals(patchFromRecorder.getPatchDocument(), patchFromGenerator.getPatchDocument());
  }
}
