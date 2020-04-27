package com.linkedin.d2.balancer.servers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ZookeeperPrefixChildFilterTest
{
  @DataProvider
  public static Object[][] prefixFilterDataProvider()
  {
    return new Object[][] {
      {"fabricPrefix-appInstanceNumber1.subdomain1.subdomain2.com",
        Arrays.asList("fabricPrefix-appInstanceNumber1-0000000001", "fabricPrefix-appInstanceNumber2-0000000002", "fabricPrefix-appInstanceNumber3-0000000003"),
        Collections.singletonList("fabricPrefix-appInstanceNumber1-0000000001")
      },
      {"fabricPrefix-appInstanceNumber.subdomain1.subdomain2.com",
        Arrays.asList("fabricPrefix-appInstanceNumber1-0000000001", "fabricPrefix-appInstanceNumber2-0000000002", "fabricPrefix-appInstanceNumber3-0000000003"),
        Collections.emptyList()
      },
      {"fabricPrefix-appInstanceNumber1",
        Arrays.asList("fabricPrefix-appInstanceNumber1-0000000001", "fabricPrefix-appInstanceNumber2-0000000002", "fabricPrefix-appInstanceNumber3-0000000003"),
        Collections.singletonList("fabricPrefix-appInstanceNumber1-0000000001")
      },
      {"fabricPrefix-appInstanceNumber",
        Arrays.asList("fabricPrefix-appInstanceNumber1-0000000001", "fabricPrefix-appInstanceNumber2-0000000002", "fabricPrefix-appInstanceNumber3-0000000003"),
        Collections.emptyList()},
      {"fabricPrefixAppInstanceNumber1.subdomain1.subdomain2.com",
        Arrays.asList("fabricPrefixAppInstanceNumber1-0000000001", "fabricPrefixAppInstanceNumber2-0000000002", "fabricPrefixAppInstanceNumber3-0000000003"),
        Collections.singletonList("fabricPrefixAppInstanceNumber1-0000000001")
      },
      {"fabricPrefixAppInstanceNumber1",
        Arrays.asList("fabricPrefixAppInstanceNumber1-0000000001", "fabricPrefixAppInstanceNumber2-0000000002", "fabricPrefixAppInstanceNumber3-0000000003"),
        Collections.singletonList("fabricPrefixAppInstanceNumber1-0000000001")
      },
      {"fabricPrefix-appInstanceNumber1.subdomain1.subdomain2.com",
        Arrays.asList("fabricPrefix-appInstanceNumber1-0000000001", "fabricPrefix-appInstanceNumber1-0000000002", "fabricPrefix-appInstanceNumber3-0000000003"),
        Arrays.asList("fabricPrefix-appInstanceNumber1-0000000001", "fabricPrefix-appInstanceNumber1-0000000002")
      },
      {"fabricPrefix-appInstanceNumber1.subdomain1.subdomain2.com",
        null,
        null
      }
    };
  }

  @Test(dataProvider = "prefixFilterDataProvider")
  public void testZookeeperPrefixChildFilter(String hostName, List<String> children, List<String> expectedFilteredChildren)
  {
    ZookeeperPrefixChildFilter filter = new ZookeeperPrefixChildFilter(new AnnouncerHostPrefixGenerator(hostName));
    List<String> actualFilteredChildren = filter.filter(children);
    Assert.assertEquals(actualFilteredChildren, expectedFilteredChildren);
  }
}
