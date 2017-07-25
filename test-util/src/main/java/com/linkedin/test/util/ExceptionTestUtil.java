package com.linkedin.test.util;
import org.testng.Assert;

final public class ExceptionTestUtil
{

  public static void verifyCauseChain(Throwable throwable, Class<?>... causes)
  {
    Throwable t = throwable;
    for (Class<?> c : causes)
    {
      Throwable cause = t.getCause();
      if (cause == null)
      {
        Assert.fail("Cause chain ended too early", throwable);
      }
      if (!c.isAssignableFrom(cause.getClass()))
      {
        Assert.fail("Expected cause " + c.getName() + " not " + cause.getClass().getName(), throwable);
      }
      t = cause;
    }
  }
}
