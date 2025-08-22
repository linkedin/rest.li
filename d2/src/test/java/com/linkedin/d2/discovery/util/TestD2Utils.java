package com.linkedin.d2.discovery.util;

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
            // samza app name will be used
            {null, null, "samzaApp", "/opt/flink", "samzaApp"},
            // app name has higher priority than samza app name
            {null, "app", "samzaApp", "/opt/flink", "app"},
            // spark app name has highest priority
            {"sparkApp", "app", "samzaApp", "/opt/flink", "sparkApp"},
            // short usr.dir
            {null, null, null, "/opt/flink", "opt-flink"}, // no trailing slash
            {null, null, null, "/opt/flink/", "opt-flink"},
            // usr.dir with no slash after app name
            {null, null, null, "/export/content/lid/apps/seas-cloud-searcher", "export-content-lid-apps-seas-cloud-searcher"},
            // usr.dir with slash after app name
            {null, null, null, "/export/content/lid/apps/seas-cloud-searcher/11ed246acf2e0be26bd44b29fb620df45ca14481",
                "export-content-lid-apps-seas-cloud-searcher"},
            {null, null, null, "/export/content/lid/apps/seas-cloud-searcher/i001/11ed246acf2e0be26bd44b29fb620df45ca14481",
                "export-content-lid-apps-seas-cloud-searcher"},
            // long usr.dir with last two parts removed
            {null, null, null,
                "/grid/g/tmp/yarn/usercache/seascloud/appcache/application_1747631859816_3737754/container_e42_1747631859816_3737754_01_000011",
                "grid-g-tmp-yarn-usercache-seascloud-appcache"},


        };
  }
  @Test(dataProvider = "provideGetAppIdentityNameData")
  public void testGetAppIdentityName(String sparkAppName, String appName, String samzaContainerName, String usrDir,
      String expectedAppIdentityName)

  {
    Properties props = mock(Properties.class);
    when(props.getProperty(D2Utils.SPARK_APP_NAME)).thenReturn(sparkAppName);
    when(props.getProperty(D2Utils.APP_NAME)).thenReturn(appName);
    when(props.getProperty(D2Utils.SAMZA_CONTAINER_NAME)).thenReturn(samzaContainerName);
    when(props.getProperty(D2Utils.USR_DIR_SYS_PROPERTY)).thenReturn(usrDir);

    String appIdentityName = D2Utils.getAppIdentityName(props);
    assertEquals(expectedAppIdentityName, appIdentityName);
  }
}
