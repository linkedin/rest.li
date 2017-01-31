/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.restli.disruptor;

import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestDisruptRestControllerContainer
{
  @Test
  public static void testGetInstanceNull()
  {
    DisruptRestControllerContainer.resetInstance();
    Assert.assertNull(DisruptRestControllerContainer.getInstance());
  }

  @Test
  public static void testSetInstance()
  {
    DisruptRestController controller = EasyMock.createMock(DisruptRestController.class);
    DisruptRestControllerContainer.resetInstance();
    DisruptRestControllerContainer.setInstance(controller);
    Assert.assertSame(DisruptRestControllerContainer.getInstance(), controller);
  }

  @Test
  public static void testMultiGetInstance()
  {
    DisruptRestController controller = EasyMock.createMock(DisruptRestController.class);
    DisruptRestControllerContainer.resetInstance();
    DisruptRestControllerContainer.setInstance(controller);
    Assert.assertSame(DisruptRestControllerContainer.getInstance(), controller);
    Assert.assertSame(DisruptRestControllerContainer.getInstance(), controller);
    Assert.assertSame(DisruptRestControllerContainer.getInstance(), controller);
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public static void testMultiSetInstance()
  {
    DisruptRestController controller = EasyMock.createMock(DisruptRestController.class);
    DisruptRestControllerContainer.resetInstance();
    DisruptRestControllerContainer.setInstance(controller);
    DisruptRestControllerContainer.setInstance(controller);
  }
}