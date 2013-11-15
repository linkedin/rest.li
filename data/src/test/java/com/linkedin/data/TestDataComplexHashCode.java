/*
   Copyright (c) 2012 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

/* $Id$ */
package com.linkedin.data;


import java.util.concurrent.CountDownLatch;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestDataComplexHashCode
{
  @Test
  public void testHashCodeOnMultipleThreads() throws InterruptedException
  {
    final int numThreads = 10;
    final CountDownLatch latch = new CountDownLatch(numThreads);

    for (int i=0; i < numThreads; ++i)
    {
      Thread thread = new Thread(new Runnable(){

        public void run()
        {
          HashCodeContainer hashCodeContainer = new HashCodeContainer();

          for (int i= 0; i < 1000; ++i)
          {
            checkHashCode(hashCodeContainer, new DataMap());
            checkHashCode(hashCodeContainer, new DataList());
          }

          latch.countDown();
        }
      });

      thread.start();
    }

    latch.await();
  }

  private void checkHashCode(HashCodeContainer hashCodeContainer, DataComplex dataComplex)
  {
      if (hashCodeContainer.getHashCode() == 0 || hashCodeContainer.getHashCode() == -1)
      {
        hashCodeContainer.setHashCode(dataComplex.dataComplexHashCode());
        Assert.assertNotEquals(hashCodeContainer.getHashCode(), 0);
      }
      else
      {
        hashCodeContainer.setHashCode(hashCodeContainer.getHashCode() + 1);
        Assert.assertEquals(hashCodeContainer.getHashCode(), dataComplex.dataComplexHashCode());
      }
  }

  private static class HashCodeContainer
  {
    private int _hashCode = 0;

    public void setHashCode(int hashCode)
    {
      _hashCode = hashCode;
    }

    public int getHashCode()
    {
      return _hashCode;
    }
  }
}
