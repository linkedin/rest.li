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

package com.linkedin.restli.tools.snapshot.check;

import com.linkedin.restli.tools.compatibility.CompatibilityInfoMap;
import com.linkedin.restli.tools.idlcheck.CompatibilityInfo;
import com.linkedin.restli.tools.idlcheck.CompatibilityLevel;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class TestRestliSnapshotCompatibilityChecker
{
  @Test
  public void testCompatibleRestSpecVsSnapshot()
  {
    final RestLiSnapshotCompatibilityChecker checker = new RestLiSnapshotCompatibilityChecker();
    final CompatibilityInfoMap infoMap = checker.checkRestSpecVsSnapshot(RESOURCES_DIR + FS + "idls" + FS + "twitter-statuses.restspec.json",
                                                                         RESOURCES_DIR + FS + "snapshots" + FS + "twitter-statuses.snapshot.json",
                                                                         CompatibilityLevel.EQUIVALENT);
    Assert.assertTrue(infoMap.isEquivalent());
  }

  @Test
  public void testIncompatibleRestSpecVsSnapshot()
  {
    final Collection<CompatibilityInfo> restSpecErrors = new HashSet<CompatibilityInfo>();
    final Collection<CompatibilityInfo> restSpecDiffs = new HashSet<CompatibilityInfo>();
    restSpecErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "identifier", "type"),
                                            CompatibilityInfo.Type.TYPE_INCOMPATIBLE, "int", "long"));
    restSpecDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "supports"),
                                            CompatibilityInfo.Type.SUPERSET, new HashSet<String>(Arrays.asList("create"))));
    restSpecDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "methods"),
                                            CompatibilityInfo.Type.SUPERSET, new HashSet<String>(Arrays.asList("create"))));

    final RestLiSnapshotCompatibilityChecker checker = new RestLiSnapshotCompatibilityChecker();
    final CompatibilityInfoMap incompatibleInfoMap = checker.checkRestSpecVsSnapshot(RESOURCES_DIR + FS + "idls" + FS + "twitter-statuses-incompatible.restspec.json",
                                                                                     RESOURCES_DIR + FS + "snapshots" + FS + "twitter-statuses.snapshot.json",
                                                                                     CompatibilityLevel.EQUIVALENT);
    Assert.assertTrue(incompatibleInfoMap.isModelEquivalent());

    final Collection<CompatibilityInfo> restSpecIncompatibles = incompatibleInfoMap.getRestSpecIncompatibles();
    final Collection<CompatibilityInfo> restSpecCompatibles = incompatibleInfoMap.getRestSpecCompatibles();

    for (CompatibilityInfo te : restSpecErrors)
    {
      Assert.assertTrue(restSpecIncompatibles.contains(te), "Reported restspec incompatibles should contain: " + te.toString());
      restSpecIncompatibles.remove(te);
    }
    for (CompatibilityInfo di : restSpecDiffs)
    {
      Assert.assertTrue(restSpecCompatibles.contains(di), "Reported restspec compatibles should contain: " + di.toString());
      restSpecCompatibles.remove(di);
    }

    Assert.assertTrue(restSpecIncompatibles.isEmpty());
    Assert.assertTrue(restSpecCompatibles.isEmpty());
  }

  @Test
  public void testFileNotFound()
  {
    final String nonExistentFilename1 = "NonExistentFile1";
    final String nonExistentFilename2 = "NonExistentFile2";
    final Collection<CompatibilityInfo> testIncompatibles = new HashSet<CompatibilityInfo>();
    final Collection<CompatibilityInfo> testCompatibles = new HashSet<CompatibilityInfo>();

    testIncompatibles.add(new CompatibilityInfo(Arrays.<Object>asList(""),
                                                CompatibilityInfo.Type.RESOURCE_MISSING,
                                                nonExistentFilename1));
    testCompatibles.add(new CompatibilityInfo(Arrays.<Object>asList(""),
                                              CompatibilityInfo.Type.RESOURCE_NEW,
                                              nonExistentFilename2));

    final RestLiSnapshotCompatibilityChecker checker = new RestLiSnapshotCompatibilityChecker();

    CompatibilityInfoMap infoMap = checker.check(nonExistentFilename1,
                                               nonExistentFilename2,
                                               CompatibilityLevel.BACKWARDS);
    Assert.assertFalse(infoMap.isCompatible(CompatibilityLevel.BACKWARDS));

    final Collection<CompatibilityInfo> incompatibles = new HashSet<CompatibilityInfo>(infoMap.getIncompatibles());
    final Collection<CompatibilityInfo> compatibles = new HashSet<CompatibilityInfo>(infoMap.getCompatibles());

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

  private static final String FS = File.separator;
  private static final String RESOURCES_DIR = "src" + FS + "test" + FS + "resources";
}
