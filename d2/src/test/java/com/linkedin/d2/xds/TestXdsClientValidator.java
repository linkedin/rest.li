/*
   Copyright (c) 2023 LinkedIn Corp.

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

package com.linkedin.d2.xds;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Test class for XdsClientValidator
 */
public class TestXdsClientValidator
{
  @DataProvider(name = "provideJavaVersionTestData")
  public Object[][] provideJavaVersionTestData()
  {
    return new Object[][]
        {
            // Java 8 tests
            {"1.8.0_172", "1.8.0_172", true},
            {"1.8.0_282", "1.8.0_172", true},
            {"1.8.0_172", "1.8.0_282", false},
            {"1.8.0_282-msft", "1.8.0_172", true},
            {"1.8.0_172-msft", "1.8.0_282", false},
            {"1.8.0_282-msft", "1.8.0_282-msft", true},
            {"1.8.0_283-msft", "1.8.0_282-msft", true},
            {"1.8.0_281-msft", "1.8.0_282-msft", false},

            // Java 9+ tests
            {"9.0.1", "1.8.0_282", true},
            {"11.0.1", "1.8.0_282", true},
            {"17.0.1", "1.8.0_282", true},
            {"21.0.1", "1.8.0_282", true},

            // Edge cases
            {"1.8.0_172", "1.8.0_172", true},
            {"1.8.0_172", "1.8.0_171", true},
            {"1.8.0_171", "1.8.0_172", false},

            // Invalid formats
            {"1.8.0", "1.8.0_282", false},
            {"1.7.0_80", "1.8.0_282", false},
            {"invalid", "1.8.0_282", false},
            {"1.8.0_282", "invalid", false},

            // Null/empty cases
            {null, "1.8.0_282", false},
            {"", "1.8.0_282", false},
            {"1.8.0_282", null, false},
            {"1.8.0_282", "", false},
        };
  }

  @Test(dataProvider = "provideJavaVersionTestData")
  public void testMeetsMinimumVersion(String currentVersion, String minVersion, boolean expectedResult)
  {
    boolean result = XdsClientValidator.meetsMinimumVersion(currentVersion, minVersion);
    assertEquals(result, expectedResult,
                 "Failed for currentVersion: " + currentVersion + ", minVersion: " + minVersion);
  }

  @Test
  public void testMeetsMinimumVersionWithRealSystemProperty()
  {
    // Test with actual system property
    String realJavaVersion = System.getProperty("java.version");
    assertNotNull(realJavaVersion, "java.version system property should not be null");

    // Test that real version passes with a very low minimum requirement
    boolean result = XdsClientValidator.meetsMinimumVersion(realJavaVersion, "1.0.0");
    assertTrue(result, "Real Java version should pass with very low minimum requirement");

    // Test that real version fails with a very high minimum requirement
    result = XdsClientValidator.meetsMinimumVersion(realJavaVersion, "99.0.0");
    assertFalse(result, "Real Java version should fail with very high minimum requirement");
  }

  @Test
  public void testMeetsMinimumVersionWithSpecialCases()
  {
    // Test with whitespace trimming
    assertTrue(XdsClientValidator.meetsMinimumVersion("  1.8.0_282-msft  ", "1.8.0_282-msft"));
    assertTrue(XdsClientValidator.meetsMinimumVersion("1.8.0_282-msft", "  1.8.0_282-msft  "));

    // Test with very long vendor suffixes
    assertTrue(XdsClientValidator.meetsMinimumVersion("1.8.0_282-very-long-vendor-suffix", "1.8.0_282-msft"));
    assertTrue(XdsClientValidator.meetsMinimumVersion("1.8.0_283-very-long-vendor-suffix", "1.8.0_282-msft"));

    // Test with multiple dashes in vendor suffix
    assertTrue(XdsClientValidator.meetsMinimumVersion("1.8.0_282-msft-preview", "1.8.0_282-msft"));
    assertTrue(XdsClientValidator.meetsMinimumVersion("1.8.0_283-msft-preview", "1.8.0_282-msft"));
  }
}
