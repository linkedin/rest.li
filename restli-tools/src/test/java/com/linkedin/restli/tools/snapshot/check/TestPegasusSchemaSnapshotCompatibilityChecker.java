/*
 * Copyright (c) 2020 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.restli.tools.snapshot.check;

import com.linkedin.data.schema.compatibility.CompatibilityOptions;
import com.linkedin.restli.tools.compatibility.CompatibilityInfoMap;
import com.linkedin.restli.tools.idlcheck.CompatibilityInfo;
import com.linkedin.restli.tools.idlcheck.CompatibilityLevel;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestPegasusSchemaSnapshotCompatibilityChecker
{
  private final String FS = File.separator;
  private String testDir = System.getProperty("testDir", new File("src/test").getAbsolutePath());
  private String snapshotDir = testDir + FS + "pegasusSchemaSnapshot";

  @Test(dataProvider = "compatibleInputFiles")
  public void testCompatiblePegasusSchemaSnapshot(String prevSchema, String currSchema, CompatibilityLevel compatLevel, CompatibilityOptions.Mode mode)
  {
    PegasusSchemaSnapshotCompatibilityChecker checker = new PegasusSchemaSnapshotCompatibilityChecker();
    CompatibilityInfoMap infoMap = checker.checkPegasusSchemaCompatibility(snapshotDir + FS + prevSchema, snapshotDir + FS + currSchema,
        mode);
    Assert.assertTrue(infoMap.isModelCompatible(compatLevel));
  }

  @Test(dataProvider = "incompatibleInputFiles")
  public void testIncompatiblePegasusSchemaSnapshot(String prevSchema, String currSchema,
      Collection<CompatibilityInfo> expectedIncompatibilityErrors, Collection<CompatibilityInfo> expectedWireCompatibilityDiffs,
      Collection<CompatibilityInfo> expectedCompatibilityDiffs )
  {
    PegasusSchemaSnapshotCompatibilityChecker checker = new PegasusSchemaSnapshotCompatibilityChecker();
    CompatibilityInfoMap infoMap = checker.checkPegasusSchemaCompatibility(snapshotDir + FS + prevSchema, snapshotDir + FS + currSchema,
        CompatibilityOptions.Mode.DATA);
    Assert.assertFalse(infoMap.isModelCompatible(CompatibilityLevel.BACKWARDS));
    Assert.assertFalse(infoMap.isModelCompatible(CompatibilityLevel.EQUIVALENT));
    Assert.assertTrue(infoMap.isModelCompatible(CompatibilityLevel.IGNORE));

    final Collection<CompatibilityInfo> modelIncompatibles = infoMap.getModelIncompatibles();
    final Collection<CompatibilityInfo> modelWireCompatibles = infoMap.getModelWireCompatibles();
    final Collection<CompatibilityInfo> modelCompatibles = infoMap.getModelCompatibles();

    for (CompatibilityInfo error : expectedIncompatibilityErrors)
    {
      Assert.assertTrue(modelIncompatibles.contains(error), "Reported model incompatibles should contain: " + error.toString());
      modelIncompatibles.remove(error);
    }
    for (CompatibilityInfo diff : expectedWireCompatibilityDiffs)
    {
      Assert.assertTrue(modelWireCompatibles.contains(diff), "Reported model wireCompatibles should contain: " + diff.toString());
      modelWireCompatibles.remove(diff);
    }
    for (CompatibilityInfo diff : expectedCompatibilityDiffs)
    {
      Assert.assertTrue(modelCompatibles.contains(diff), "Reported model compatibles should contain: " + diff.toString());
      modelCompatibles.remove(diff);
    }

    Assert.assertTrue(modelIncompatibles.isEmpty());
    Assert.assertTrue(modelWireCompatibles.isEmpty());
    Assert.assertTrue(modelCompatibles.isEmpty());
  }

  @Test(dataProvider = "fileMapTestData")
  public void testCreateMapFromFiles(String inputDir, Map<String, String> expectedFileMap)
  {
    PegasusSchemaSnapshotCompatibilityChecker checker = new PegasusSchemaSnapshotCompatibilityChecker();
    Map<String,String> actualResult = checker.createMapFromFiles(inputDir);
    Assert.assertEquals(actualResult.size(), expectedFileMap.size());
    actualResult.forEach((fileName, path)-> {
      Assert.assertTrue(expectedFileMap.containsKey(fileName));
      Assert.assertEquals(actualResult.get(fileName), expectedFileMap.get(fileName));
    });
  }

  @Test(dataProvider = "matchingFilePairTestData")
  public void testGetMatchingPrevAndCurrSnapshotPairs(String preSnapshotDir, String currSnapshotDir, List<String> expectedFilePairList)
  {
    PegasusSchemaSnapshotCompatibilityChecker checker = new PegasusSchemaSnapshotCompatibilityChecker();
    List<String> actualFilePairList = checker.getMatchingPrevAndCurrSnapshotPairs(preSnapshotDir, currSnapshotDir);
    Assert.assertEquals(actualFilePairList.size(), expectedFilePairList.size());
    for (int i = 0; i < actualFilePairList.size(); i++)
    {
      Assert.assertEquals(actualFilePairList.get(0), expectedFilePairList.get(0));
    }
  }

  @DataProvider
  private Object[][] matchingFilePairTestData()
  {
    String prevSnapshotDir = snapshotDir + FS + "prevSnapshot";
    String currSnapshotDir = snapshotDir + FS + "currSnapshot";
    List<String> pairList = Arrays.asList(
        prevSnapshotDir + FS + "BirthInfo.pdl",
        currSnapshotDir + FS + "BirthInfo.pdl",
        "",
        currSnapshotDir + FS + "com.linkedin.test.BirthInfo.pdl",
        prevSnapshotDir + FS + "com.linkedin.test.Data.pdl",
        ""
    );
    return new Object[][]
        {
            {
                prevSnapshotDir,
                currSnapshotDir,
                pairList
            }
        };
  }

  @DataProvider
  private Object[][] fileMapTestData()
  {
    String inputDir = snapshotDir + FS + "currSnapshot";
    String fileName1 = "com.linkedin.test.BirthInfo.pdl";
    String fileName2 = "BirthInfo.pdl";
    Map<String, String> fileMap = new HashMap<>();
    fileMap.put(fileName1, inputDir + FS + fileName1);
    fileMap.put(fileName2, inputDir + FS + fileName2);

    return new Object[][]
        {
            {   inputDir,
                fileMap
            }
        };
  }

  @DataProvider
  private Object[][] compatibleInputFiles()
  {
    return new Object[][]
        {
            { "Date.pdl", "compatibleSchemaSnapshot/Date.pdl", CompatibilityLevel.EQUIVALENT, CompatibilityOptions.Mode.DATA },
            { "Foo.pdl", "compatibleSchemaSnapshot/Foo.pdl", CompatibilityLevel.BACKWARDS, CompatibilityOptions.Mode.EXTENSION },
            { "BirthInfo.pdl", "compatibleSchemaSnapshot/BirthInfo.pdl", CompatibilityLevel.WIRE_COMPATIBLE, CompatibilityOptions.Mode.DATA },
        };
  }

  @DataProvider
  private Object[][] incompatibleInputFiles()
  {
    final Collection<CompatibilityInfo> incompatibilityErrors = new HashSet<>();
    final Collection<CompatibilityInfo> compatibilityDiffs = new HashSet<>();
    incompatibilityErrors.add(new CompatibilityInfo(Arrays.<Object>asList("BirthInfo"),
        CompatibilityInfo.Type.TYPE_BREAKS_NEW_READER, "new record added required fields name"));
    incompatibilityErrors.add(new CompatibilityInfo(Arrays.<Object>asList("BirthInfo"),
        CompatibilityInfo.Type.TYPE_BREAKS_OLD_READER, "new record removed required fields year"));
    incompatibilityErrors.add(new CompatibilityInfo(Arrays.<Object>asList("BirthInfo", "day", "string"),
        CompatibilityInfo.Type.TYPE_BREAKS_NEW_AND_OLD_READERS, "schema type changed from int to string"));
    compatibilityDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("BirthInfo", "month", "long"),
        CompatibilityInfo.Type.TYPE_INFO, "numeric type promoted from int to long"));

    return new Object[][]
        {
            {   "BirthInfo.pdl",
                "incompatibleSchemaSnapshot/BirthInfo.pdl",
                incompatibilityErrors,
                new HashSet<>(),
                compatibilityDiffs
            },
            {   "BirthInfo.pdl",
                "compatibleSchemaSnapshot/BirthInfo.pdl", // This is combination is compatible with WIRE_COMPATIBLE, but not with BACKWARDS
                new HashSet<>(),
                new HashSet<>(Arrays.asList(new CompatibilityInfo(Arrays.<Object>asList("BirthInfo", "eyeColor", "Color", "symbols"),
                    CompatibilityInfo.Type.ENUM_VALUE_ADDED, "new enum added symbols GREEN"))),
                new HashSet<>()
            },
        };
  }
}
