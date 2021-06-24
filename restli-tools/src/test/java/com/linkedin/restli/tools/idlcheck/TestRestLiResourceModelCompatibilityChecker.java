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

package com.linkedin.restli.tools.idlcheck;

import com.linkedin.data.schema.generator.AbstractGenerator;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;


public class TestRestLiResourceModelCompatibilityChecker
{
  @Test
  public void testFileNotFound()
  {
    final String nonExistentFilename1 = "NonExistentFile1";
    final String nonExistentFilename2 = "NonExistentFile2";
    final Collection<CompatibilityInfo> testIncompatibles = new HashSet<>();
    final Collection<CompatibilityInfo> testCompatibles = new HashSet<>();

    testIncompatibles.add(new CompatibilityInfo(Arrays.<Object>asList(""),
                                                 CompatibilityInfo.Type.RESOURCE_MISSING,
                                                 nonExistentFilename1));
    testCompatibles.add(new CompatibilityInfo(Arrays.<Object>asList(""),
                                                 CompatibilityInfo.Type.RESOURCE_NEW,
                                                 nonExistentFilename2));

    final RestLiResourceModelCompatibilityChecker checker = new RestLiResourceModelCompatibilityChecker();
    checker.setResolverPath(System.getProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH));

    Assert.assertFalse(checker.check(nonExistentFilename1,
                                     nonExistentFilename2,
                                     CompatibilityLevel.BACKWARDS));

    final Collection<CompatibilityInfo> incompatibles = new HashSet<>(checker.getInfoMap().getIncompatibles());
    final Collection<CompatibilityInfo> compatibles = new HashSet<>(checker.getInfoMap().getCompatibles());

    for (CompatibilityInfo te : incompatibles)
    {
      Assert.assertTrue(testIncompatibles.contains(te), "Reported incompatibles should contain: " + te.toString());
      incompatibles.remove(te);
    }

    for (CompatibilityInfo di : compatibles)
    {
      Assert.assertTrue(testCompatibles.contains(di), "Reported compatibles should contain: " + di.toString());
      compatibles.remove(di);
    }
  }
}
