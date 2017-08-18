package com.linkedin.test.util;

public interface RetryableAssertion
{
  void doAssertion() throws Exception, AssertionError;
}
