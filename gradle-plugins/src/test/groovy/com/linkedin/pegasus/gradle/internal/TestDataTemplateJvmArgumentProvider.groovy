package com.linkedin.pegasus.gradle.internal


import org.testng.annotations.Test

class TestDataTemplateJvmArgumentProvider {

  @Test
  void testJvmArgumentProvider() {
    //when
    def jvmArgumentProvider = new DataTemplateJvmArgumentProvider('foo', new File('/tmp/foo/bar'))

    //then
    assert jvmArgumentProvider.asArguments() == ['-Dgenerator.resolver.path=foo', '-Droot.path=/tmp/foo/bar']
  }
}
