package com.linkedin.d2.balancer.properties;


import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class ClientServiceConfigValidatorTest
{
  @Test
  public void testValidHttpRequestTimeout()
  {
    Map<String, Object> serviceSuppliedProperties = new HashMap<>();
    serviceSuppliedProperties.put(PropertyKeys.HTTP_REQUEST_TIMEOUT, "1000");

    Map<String, Object> clientSuppliedProperties = new HashMap<>();
    clientSuppliedProperties.put(PropertyKeys.HTTP_REQUEST_TIMEOUT, "2000");

    Assert.assertTrue(ClientServiceConfigValidator.isValidValue(serviceSuppliedProperties,
                                                                clientSuppliedProperties,
                                                                PropertyKeys.HTTP_REQUEST_TIMEOUT));
  }

  @Test
  public void testInvalidHttpRequestTimeout()
  {
    Map<String, Object> serviceSuppliedProperties = new HashMap<>();
    serviceSuppliedProperties.put(PropertyKeys.HTTP_REQUEST_TIMEOUT, "1000");

    Map<String, Object> clientSuppliedProperties = new HashMap<>();
    clientSuppliedProperties.put(PropertyKeys.HTTP_REQUEST_TIMEOUT, "100");

    Assert.assertFalse(ClientServiceConfigValidator.isValidValue(serviceSuppliedProperties,
                                                                clientSuppliedProperties,
                                                                PropertyKeys.HTTP_REQUEST_TIMEOUT));
  }

  @Test
  public void testParseFailureHttpRequestTimeout()
  {
    Map<String, Object> serviceSuppliedProperties = new HashMap<>();
    serviceSuppliedProperties.put(PropertyKeys.HTTP_REQUEST_TIMEOUT, "1000");

    Map<String, Object> clientSuppliedProperties = new HashMap<>();
    clientSuppliedProperties.put(PropertyKeys.HTTP_REQUEST_TIMEOUT, "foo");

    Assert.assertFalse(ClientServiceConfigValidator.isValidValue(serviceSuppliedProperties,
                                                                 clientSuppliedProperties,
                                                                 PropertyKeys.HTTP_REQUEST_TIMEOUT));
  }

  @Test
  public void testMaxResponse()
  {
    Map<String, Object> serviceSuppliedProperties = new HashMap<>();
    serviceSuppliedProperties.put(PropertyKeys.HTTP_MAX_RESPONSE_SIZE, "1000");

    Map<String, Object> clientSuppliedProperties = new HashMap<>();
    clientSuppliedProperties.put(PropertyKeys.HTTP_MAX_RESPONSE_SIZE, "10000");

    Assert.assertTrue(ClientServiceConfigValidator.isValidValue(serviceSuppliedProperties,
                                                                 clientSuppliedProperties,
                                                                 PropertyKeys.HTTP_MAX_RESPONSE_SIZE));
  }
}
