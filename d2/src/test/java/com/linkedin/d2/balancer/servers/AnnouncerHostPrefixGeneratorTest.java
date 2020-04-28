package com.linkedin.d2.balancer.servers;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AnnouncerHostPrefixGeneratorTest
{
  @DataProvider
  public static Object[][] prefixGeneratorDataProvider()
  {
    return new Object[][] {
      {"fabricPrefix-appInstanceNumber.subdomain1.subdomain2.com", "fabricPrefix-appInstanceNumber"},
      {"fabricPrefix-appInstanceNumber", "fabricPrefix-appInstanceNumber"},
      {"fabricPrefixAppInstanceNumber.subdomain1.subdomain2.com", "fabricPrefixAppInstanceNumber"},
      {"fabricPrefixAppInstanceNumber", "fabricPrefixAppInstanceNumber"},
      {"", ""},
      {null, null}
    };
  }

  @Test(dataProvider = "prefixGeneratorDataProvider")
  public void testAnnouncerHostPrefixGenerator(String hostName, String expectedPrefix)
  {
    AnnouncerHostPrefixGenerator prefixGenerator = new AnnouncerHostPrefixGenerator(hostName);
    String actualPrefix = prefixGenerator.generatePrefix();
    Assert.assertEquals(actualPrefix, expectedPrefix);
  }
}
