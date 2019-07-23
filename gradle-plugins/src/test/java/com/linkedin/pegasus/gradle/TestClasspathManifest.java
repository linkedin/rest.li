package com.linkedin.pegasus.gradle;

import java.io.File;
import java.util.Arrays;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public final class TestClasspathManifest
{
  @Test
  public void testCreatesClasspath()
  {
    //setup
    File dir = new File("/tmp/foo");
    File subdir = new File(dir, "sub");
    subdir.mkdirs();
    File f1 = new File(dir, "foo.jar");    //different directory
    File f2 = new File(subdir, "bar.jar");
    File f3 = new File(subdir, "aaa.jar");

    //when
    String cp = ClasspathManifest.relativeClasspathManifest(subdir, Arrays.asList(f1, f2, f3));

    //then
    assertEquals(cp, "../foo.jar bar.jar aaa.jar");
  }
}
