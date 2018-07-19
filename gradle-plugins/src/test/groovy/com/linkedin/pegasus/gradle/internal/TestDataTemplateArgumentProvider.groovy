package com.linkedin.pegasus.gradle.internal


import org.testng.annotations.Test

class TestDataTemplateArgumentProvider {

  @Test
  void testArgumentProvider() {
    //when
    def argumentProvider = new DataTemplateArgumentProvider(['foo', 'bar'])

    //then
    assert argumentProvider.asArguments() == ['foo', 'bar']
  }
}
