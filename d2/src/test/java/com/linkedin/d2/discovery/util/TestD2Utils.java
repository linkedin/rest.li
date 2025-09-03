package com.linkedin.d2.discovery.util;

import java.util.Map;
import java.util.Properties;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


public class TestD2Utils {

  @DataProvider(name = "provideGetAppIdentityNameData")
  public Object[][] provideGetAppIdentityNameData()
  {
    return new Object[][]
        {
            // samza app name in system properties will be used
            {null, null, "samzaApp", "/opt/flink", null, null, null, "envSamzaApp", "samzaApp"},
            // app name has higher priority than samza app name
            {null, "app", "samzaApp", "/opt/flink", null, "envApp", null, null, "app"},
            // spark app name has highest priority
            {"sparkApp", "app", "samzaApp", "/opt/flink", "envSparkApp", null, null, null, "sparkApp"},
            // short usr.dir
            {null, null, null, "/opt/flink", null, null, null, null, "opt-flink"}, // no trailing slash
            {null, null, null, "/opt/flink/", null, null, null, null, "opt-flink"},
            // usr.dir with no slash after app name
            {null, null, null, "/export/content/lid/apps/seas-cloud-searcher", null, null, null, null,
                "export-content-lid-apps-seas-cloud-searcher"},
            // usr.dir with slash after app name
            {null, null, null, "/export/content/lid/apps/seas-cloud-searcher/11ed246acf2e0be26bd44b29fb620df45ca14481",
                null, null, null, null, "export-content-lid-apps-seas-cloud-searcher"},
            {null, null, null, "/export/content/lid/apps/seas-cloud-searcher/i001/11ed246acf2e0be26bd44b29fb620df45ca14481",
                null, null, null, null, "export-content-lid-apps-seas-cloud-searcher"},
            // long usr.dir with last two parts removed
            {null, null, null,
                "/grid/g/tmp/yarn/usercache/seascloud/appcache/application_1747631859816_3737754/container_e42_1747631859816_3737754_01_000011",
                null, null, null, null, "grid-g-tmp-yarn-usercache-seascloud-appcache"},

            // Env vars will be used when the corresponding sys prop is null
            {null, null, null, "/opt/flink", null, null, "envSamzaApp", null, "envSamzaApp"},
            {null, null, "samzaApp", "/opt/flink", null, "envApp", null, null, "envApp"},
            {null, "app", "samzaApp", "/opt/flink", "envSparkApp", null, null, null, "envSparkApp"},
            {null, null, null, null, null, null, null, "/opt/flink", "opt-flink"}
        };
  }
  @Test(dataProvider = "provideGetAppIdentityNameData")
  public void testGetAppIdentityName(String sparkAppNameInSys, String appNameInSys, String samzaContainerNameInSys,
      String usrDirInSys, String sparkAppNameInEnv, String appNameInEnv, String samzaContainerNameInEnv,
      String usrDirInEnv, String expectedAppIdentityName)

  {
    Properties props = mock(Properties.class);
    when(props.getProperty(D2Utils.SPARK_APP_NAME)).thenReturn(sparkAppNameInSys);
    when(props.getProperty(D2Utils.APP_NAME)).thenReturn(appNameInSys);
    when(props.getProperty(D2Utils.SAMZA_CONTAINER_NAME)).thenReturn(samzaContainerNameInSys);
    when(props.getProperty(D2Utils.USR_DIR_SYS_PROPERTY)).thenReturn(usrDirInSys);

    Map<String, String> env = mock(Map.class);
    when(env.get(D2Utils.SPARK_APP_NAME)).thenReturn(sparkAppNameInEnv);
    when(env.get(D2Utils.APP_NAME)).thenReturn(appNameInEnv);
    when(env.get(D2Utils.SAMZA_CONTAINER_NAME)).thenReturn(samzaContainerNameInEnv);
    when(env.get(D2Utils.USR_DIR_SYS_PROPERTY)).thenReturn(usrDirInEnv);

    String appIdentityName = D2Utils.getAppIdentityName(props, env);
    assertEquals(expectedAppIdentityName, appIdentityName);
  }

  @DataProvider(name = "provideJavaVersionTestData")
  public Object[][] provideJavaVersionTestData()
  {
    return new Object[][]
        {
            // Test cases for Java 8 versions
            {"1.8.0_282-msft", "1.8.0_282-msft", true},  // exact match
            {"1.8.0_283-msft", "1.8.0_282-msft", true},  // higher build number
            {"1.8.0_300", "1.8.0_282-msft", true},       // higher build number, no vendor
            {"1.8.0_172", "1.8.0_282-msft", false}, // lower build number

            // Test cases for Java 9+ versions (should always pass)
            {"9.0.1", "1.8.0_282-msft", true},           // Java 9
            {"10.0.1", "1.8.0_282-msft", true},          // Java 10
            {"11.0.1", "1.8.0_282-msft", true},          // Java 11
            {"17.0.1", "1.8.0_282-msft", true},          // Java 17
            {"21.0.1", "1.8.0_282-msft", true},          // Java 21

            // Test cases for edge cases
            {"1.8.0_282-msft", null, true},               // no minimum version specified
            {"1.8.0_282-msft", "", true},                 // empty minimum version
            {null, "1.8.0_282-msft", false},              // null current version
            {"", "1.8.0_282-msft", false},                // empty current version
            {"   ", "1.8.0_282-msft", false},             // whitespace current version
            {"1.8.0_282-msft", "   ", true},              // whitespace minimum version

            // Test cases for Java 7 and earlier (should fail)
            {"1.7.0_80", "1.8.0_282-msft", false},       // Java 7
            {"1.6.0_45", "1.8.0_282-msft", false},       // Java 6

            // Test cases for invalid version formats
            {"invalid-version", "1.8.0_282-msft", false}, // invalid current version
            {"1.8.0_282-msft", "invalid-version", false}, // invalid minimum version
            {"1.8.0", "1.8.0_282-msft", false},          // missing build number
            {"1.8.0_", "1.8.0_282-msft", false},         // incomplete build number

            // Test cases for different vendor suffixes
            {"1.8.0_282-oracle", "1.8.0_282-msft", true}, // different vendor, same build
            {"1.8.0_282-openjdk", "1.8.0_282-msft", true}, // different vendor, same build
            {"1.8.0_282", "1.8.0_282-msft", true},        // no vendor vs vendor

            // Test cases for very high build numbers
            {"1.8.0_999", "1.8.0_282-msft", true},       // very high build number
            {"1.8.0_282-msft", "1.8.0_999", false},      // very high minimum requirement
        };
  }

  @Test(dataProvider = "provideJavaVersionTestData")
  public void testIsJavaVersionAtLeast(String currentVersion, String minVersion, boolean expectedResult)
  {
    boolean result = D2Utils.isJavaVersionAtLeast(currentVersion, minVersion);
    assertEquals("Failed for currentVersion: " + currentVersion + ", minVersion: " + minVersion,
                 expectedResult, result);
  }

  @Test
  public void testIsJavaVersionAtLeastWithRealSystemProperty()
  {
    // Test with actual system property
    String realJavaVersion = System.getProperty("java.version");
    assertNotNull("java.version system property should not be null", realJavaVersion);

    // Test that real version passes with a very low minimum requirement
    boolean result = D2Utils.isJavaVersionAtLeast(realJavaVersion, "1.0.0");
    assertTrue("Real Java version should pass with very low minimum requirement", result);

    // Test that real version fails with a very high minimum requirement
    result = D2Utils.isJavaVersionAtLeast(realJavaVersion, "99.0.0");
    assertFalse("Real Java version should fail with very high minimum requirement", result);
  }

  @Test
  public void testIsJavaVersionAtLeastWithSpecialCases()
  {
    // Test with whitespace trimming
    assertTrue(D2Utils.isJavaVersionAtLeast("  1.8.0_282-msft  ", "1.8.0_282-msft"));
    assertTrue(D2Utils.isJavaVersionAtLeast("1.8.0_282-msft", "  1.8.0_282-msft  "));

    // Test with very long vendor suffixes
    assertTrue(D2Utils.isJavaVersionAtLeast("1.8.0_282-very-long-vendor-suffix", "1.8.0_282-msft"));
    assertTrue(D2Utils.isJavaVersionAtLeast("1.8.0_283-very-long-vendor-suffix", "1.8.0_282-msft"));

    // Test with multiple dashes in vendor suffix
    assertTrue(D2Utils.isJavaVersionAtLeast("1.8.0_282-msft-preview", "1.8.0_282-msft"));
    assertTrue(D2Utils.isJavaVersionAtLeast("1.8.0_283-msft-preview", "1.8.0_282-msft"));
  }
}
