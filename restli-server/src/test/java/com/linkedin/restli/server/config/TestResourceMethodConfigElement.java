package com.linkedin.restli.server.config;

import com.linkedin.restli.common.ResourceMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.testng.Assert.assertEquals;

public class TestResourceMethodConfigElement
{
  @DataProvider
  public Object[][] validTimeoutConfigs()
  {
    return new Object[][]
        {
          {"*.*", Optional.empty(), Optional.empty(), Optional.empty(), 100L},
          {"profile.*", Optional.of("profile"), Optional.empty(), Optional.empty(), 200L},
          {"profile.BATCH_GET", Optional.of("profile"), Optional.of(ResourceMethod.BATCH_GET), Optional.empty(), 200L},
          {"*.DELETE", Optional.empty(), Optional.of(ResourceMethod.DELETE), Optional.empty(), 100L},
          {"*.FINDER-*", Optional.empty(), Optional.of(ResourceMethod.FINDER), Optional.empty(), 300L},
          {"profile.FINDER-*", Optional.of("profile"), Optional.of(ResourceMethod.FINDER), Optional.empty(), 300L},
          {"profile-prod-lsg1.FINDER-*", Optional.of("profile-prod-lsg1"), Optional.of(ResourceMethod.FINDER), Optional.empty(), 300L},
          {"profile.FINDER-firstDegree", Optional.of("profile"), Optional.of(ResourceMethod.FINDER), Optional.of("firstDegree"), 400L},
          {"profile.FINDER-first_degree", Optional.of("profile"), Optional.of(ResourceMethod.FINDER), Optional.of("first_degree"), 400L},
          {"mini_profile.FINDER-first_degree", Optional.of("mini_profile"), Optional.of(ResourceMethod.FINDER), Optional.of("first_degree"), 400L},
          {"assets:media.ACTION-purge", Optional.of("assets:media"), Optional.of(ResourceMethod.ACTION), Optional.of("purge"), 350L},
          {"profile.BATCH_FINDER-*", Optional.of("profile"), Optional.of(ResourceMethod.BATCH_FINDER), Optional.empty(), 200L},
          {"profile.BATCH_FINDER-findUsers", Optional.of("profile"), Optional.of(ResourceMethod.BATCH_FINDER), Optional.of("findUsers"), 250L}
        };
  }

  @Test(dataProvider = "validTimeoutConfigs")
  public void testValidTimeoutConfigParsing(String configKey,
                                            Optional<String> restResource,
                                            Optional<ResourceMethod> opType,
                                            Optional<String> opName,
                                            Long configValue
                                            ) throws ResourceMethodConfigParsingException {
    ResourceMethodConfigElement el = ResourceMethodConfigElement.parse("timeoutMs", configKey, configValue);
    assertEquals(el.getResourceName(), restResource);
    assertEquals(el.getOpType(), opType);
    assertEquals(el.getOpName(), opName);
    assertEquals(el.getProperty(), "timeoutMs");
    assertEquals(el.getValue(), configValue);
  }

  @DataProvider
  public Object[][] invalidTimeoutConfigs()
  {
    return new Object[][]
        {
          {"blah", "*.*", 100L}, // invalid property
          {"timeoutMs", "*.*", true}, // invalid config value
          {"timeoutMs", "*.FINDER", 100L}, // missing operation name for FINDER
          {"timeoutMs", "*.BATCH_FINDER", 100L}, // missing operation name for BATCH_FINDER
          {"timeoutMs", "greetings.DELETE/timeoutMs", 100L}, // invalid config key
          {"timeoutMs", "greetings.foo.DELETE", 100L} // invalid subresource key
        };
  }


  @Test(dataProvider = "invalidTimeoutConfigs", expectedExceptions = {ResourceMethodConfigParsingException.class})
  public void testInvalidTimeoutConfigParsing(String property,
                                              String configKey,
                                              Object configValue) throws ResourceMethodConfigParsingException {
    ResourceMethodConfigElement.parse(property, configKey, configValue);
  }
}
