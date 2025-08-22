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
}
