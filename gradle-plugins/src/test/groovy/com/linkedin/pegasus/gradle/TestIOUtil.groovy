package com.linkedin.pegasus.gradle


import org.gradle.util.GFileUtils
import org.testng.annotations.Test

class TestIOUtil {

  @Test
  void writesTextOnNonExistantFile() {
    def f = new File("/tmp/foo/bar/baz.txt")
    f.delete()
    f.parentFile.delete()
    f.deleteOnExit()
    f.parentFile.deleteOnExit()
    assert !f.exists()
    assert !f.parentFile.exists()

    //when
    IOUtil.writeText(f, "foo")

    //then
    assert GFileUtils.readFile(f) == "foo"
  }
}
