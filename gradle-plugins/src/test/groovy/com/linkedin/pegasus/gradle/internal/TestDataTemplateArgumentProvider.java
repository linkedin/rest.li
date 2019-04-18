package com.linkedin.pegasus.gradle.internal;

import java.util.Arrays;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public final class TestDataTemplateArgumentProvider
{
  @Test
  public void testArgumentProvider()
  {
    //when
    DataTemplateArgumentProvider argumentProvider = new DataTemplateArgumentProvider(Arrays.asList("foo", "bar"));

    //then
    assertEquals(argumentProvider.asArguments(), Arrays.asList("foo", "bar"));
  }
}
