package com.linkedin.pegasus.gradle.internal;

import java.io.File;
import java.util.Arrays;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public final class TestDataTemplateJvmArgumentProvider
{
  @Test
  public void testJvmArgumentProvider()
  {
    //when
    DataTemplateJvmArgumentProvider jvmArgumentProvider = new DataTemplateJvmArgumentProvider(
        "foo", new File("/tmp/foo/bar"));

    //then
    assertEquals(jvmArgumentProvider.asArguments(),
        Arrays.asList("-Dgenerator.resolver.path=foo", "-Droot.path=/tmp/foo/bar"));
  }
}
