package com.linkedin.restli.internal.server.model;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;


@Test
public class TestResourceModelEncoder {

  public void testResourceMethodComparator()
  {
    ResourceMethodDescriptor appleMethod = mockResourceMethodDescriptor("apple");
    ResourceMethodDescriptor orangeMethod = mockResourceMethodDescriptor("orange");
    ResourceMethodDescriptor nullMethodOne = mockResourceMethodDescriptor(null);
    ResourceMethodDescriptor nullMethodTwo = mockResourceMethodDescriptor(null);

    Assert.assertTrue(ResourceModelEncoder.RESOURCE_METHOD_COMPARATOR.compare(orangeMethod, appleMethod) > 0);

    Assert.assertTrue(ResourceModelEncoder.RESOURCE_METHOD_COMPARATOR.compare(appleMethod, orangeMethod) < 0);

    Assert.assertTrue(ResourceModelEncoder.RESOURCE_METHOD_COMPARATOR.compare(appleMethod, appleMethod) == 0);

    Assert.assertTrue(ResourceModelEncoder.RESOURCE_METHOD_COMPARATOR.compare(appleMethod, nullMethodOne) > 0);

    Assert.assertTrue(ResourceModelEncoder.RESOURCE_METHOD_COMPARATOR.compare(nullMethodOne, appleMethod) < 0);

    Assert.assertTrue(ResourceModelEncoder.RESOURCE_METHOD_COMPARATOR.compare(nullMethodOne, nullMethodTwo) == 0);

    Assert.assertTrue(ResourceModelEncoder.RESOURCE_METHOD_COMPARATOR.compare(nullMethodTwo, nullMethodOne) == 0);
  }

  private ResourceMethodDescriptor mockResourceMethodDescriptor(String name)
  {
    ResourceMethodDescriptor resourceMethodDescriptor = Mockito.mock(ResourceMethodDescriptor.class);
    Mockito.when(resourceMethodDescriptor.getMethodName()).thenReturn(name);
    return resourceMethodDescriptor;
  }
}
