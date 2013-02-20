package com.linkedin.d2.balancer.util;


import com.linkedin.r2.RemoteInvocationException;
import java.net.ConnectException;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Tests for {@link LoadBalancerUtil}
 *
 * @author Oby Sumampouw (osumampouw@linkedin.com)
 */
public class LoadBalancerUtilTest
{
  @Test
  public void testFindOriginalThrowable()
  {
    ConnectException connectException = new ConnectException("Foo");
    RemoteInvocationException e = new RemoteInvocationException("Failed to get connect to a server", connectException);
    Throwable throwable = LoadBalancerUtil.findOriginalThrowable(e);

    Assert.assertEquals(throwable, connectException);

    //we only go as far as 100 level deep for finding exception
    Exception npe = new NullPointerException();
    Exception temp = npe;
    for (int i = 0; i < 100; i++)
    {
      e = new RemoteInvocationException(temp);
      temp = e;
    }

    throwable = LoadBalancerUtil.findOriginalThrowable(e);
    Assert.assertEquals(throwable, npe);

    //we add the 101th exception then we lost the reference to NullPointerException
    e = new RemoteInvocationException(temp);
    throwable = LoadBalancerUtil.findOriginalThrowable(e);
    Assert.assertFalse(throwable instanceof NullPointerException);
  }
}
