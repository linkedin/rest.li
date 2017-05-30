package com.linkedin.pegasus.gradle


import org.testng.annotations.Test

class TestClasspathManifest {

  @Test
  void testCreatesClasspath() {
    //setup
    def dir = new File("/tmp/foo")
    def subdir = new File(dir, "sub")
    subdir.mkdirs()
    def f1 = new File(dir, "foo.jar")    //different directory
    def f2 = new File(subdir, "bar.jar")
    def f3 = new File(subdir, "aaa.jar")

    //when
    def cp = ClasspathManifest.relativeClasspathManifest(subdir, [f1, f2, f3])

    //then
    assert cp == "../foo.jar bar.jar aaa.jar"
  }
}
