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

package com.linkedin.restli.server.resources;

import static com.linkedin.restli.server.RestLiTestHelper.buildResourceModels;
import static org.easymock.EasyMock.createMock;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import com.linkedin.restli.internal.server.RestLiInternalException;
import org.easymock.EasyMock;
import org.testng.annotations.Test;

import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.resources.fixtures.ConstructorArgResource;
import com.linkedin.restli.server.resources.fixtures.SomeDependency1;
import com.linkedin.restli.server.resources.fixtures.SomeDependency2;
import com.linkedin.restli.server.resources.fixtures.SomeResource1;
import com.linkedin.restli.server.resources.fixtures.SomeResource2;
import com.linkedin.restli.server.resources.fixtures.SomeResource3;
import com.linkedin.restli.server.resources.fixtures.SomeResource4;
import com.linkedin.restli.server.resources.fixtures.SomeResource5;

/**
 * @author dellamag
 */
public class TestInjectResourceFactory
{
  @Test
  public void testHappyPath()
  {
    Map<String, ResourceModel> pathRootResourceMap =
      buildResourceModels(SomeResource1.class,
                          SomeResource2.class,
                          SomeResource5.class);

    // set up mock ApplicationContext
    BeanProvider ctx = createMock(BeanProvider.class);

    EasyMock.expect(ctx.getBean(EasyMock.eq("dep1"))).andReturn(new SomeDependency1()).anyTimes();
    EasyMock.expect(ctx.getBean(EasyMock.eq("dep3"))).andReturn(new SomeDependency1()).anyTimes();

    Map<String, SomeDependency2> map = new HashMap<>();
    map.put("someBeanName", new SomeDependency2());

    EasyMock.expect(ctx.getBeansOfType(EasyMock.eq(SomeDependency2.class)))
        .andReturn(map).anyTimes();

    EasyMock.replay(ctx);

    InjectResourceFactory factory = new InjectResourceFactory(ctx);

    factory.setRootResources(pathRootResourceMap);

    // #1 happy path
    SomeResource1 r1 = factory.create(SomeResource1.class);
    assertNotNull(r1);
    assertNotNull(r1.getDependency1());
    assertNotNull(r1.getDependency2());
    assertNull(r1.getNonInjectedDependency());

    // #2 No deps
    SomeResource2 r2 = factory.create(SomeResource2.class);
    assertNotNull(r2);

    // #3 bean not registered with ResourceFactory
    try
    {
      factory.create(SomeResource3.class);
      fail("Expected no such bean exception");
    }
    catch (RestLiInternalException e)
    {
      // expected
    }

    EasyMock.verify(ctx);
    EasyMock.reset(ctx);

    // #4 derived resource
    SomeResource5 r5 = factory.create(SomeResource5.class);
    assertNotNull(r5);
    assertNotNull(r5.getDependency1());
    assertNotNull(r5.getDerivedDependency1());
    assertNotNull(r5.getDependency2());
    assertNotNull(r5.getDependency3());
    assertNull(r5.getNonInjectedDependency());

  }

  @Test
  public void testAmbiguousBeanResolution() throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap =
      buildResourceModels(SomeResource1.class,
                          SomeResource2.class,
                          SomeResource4.class);

    // set up mock ApplicationContext
    BeanProvider ctx = EasyMock.createMock(BeanProvider.class);
    EasyMock.expect(ctx.getBean(EasyMock.eq("dep1"))).andReturn(new SomeDependency1()).anyTimes();

    Map<String, SomeDependency2> map2 = new HashMap<>();
    map2.put("someBeanName", new SomeDependency2());
    EasyMock.expect(ctx.getBeansOfType(EasyMock.eq(SomeDependency2.class)))
        .andReturn(map2).anyTimes();


    Map<String, SomeDependency1> map1  = new HashMap<>();
    map1.put("someDep1", new SomeDependency1());
    map1.put("anotherDep1", new SomeDependency1());
    EasyMock.expect(ctx.getBeansOfType(EasyMock.eq(SomeDependency1.class)))
        .andReturn(map1).anyTimes();

    EasyMock.replay(ctx);

    InjectResourceFactory factory = new InjectResourceFactory(ctx);


    // #4 ambiguous dep
    try
    {
      factory.setRootResources(pathRootResourceMap);

      fail("Expected unresolvable bean exception");
    }
    catch (RestLiInternalException e)
    {
      assertTrue(e.getMessage().startsWith("Expected to find"));
    }

    EasyMock.verify(ctx);
    EasyMock.reset(ctx);
  }

  @Test
  public void testMissingNamedDependency()
  {
    Map<String, ResourceModel> pathRootResourceMap =
        buildResourceModels(SomeResource1.class);

    BeanProvider ctx = EasyMock.createMock(BeanProvider.class);
    EasyMock.expect(ctx.getBean(EasyMock.eq("dep1"))).andReturn(null).anyTimes();
    EasyMock.expect(ctx.getBean(EasyMock.eq("dep3"))).andReturn(new SomeDependency1()).anyTimes();

    Map<String, SomeDependency2> map = new HashMap<>();
    map.put("someBeanName", new SomeDependency2());

    EasyMock.expect(ctx.getBeansOfType(EasyMock.eq(SomeDependency2.class)))
        .andReturn(map).anyTimes();

    EasyMock.replay(ctx);

    InjectResourceFactory factory = new InjectResourceFactory(ctx);

    try
    {
      factory.setRootResources(pathRootResourceMap);

      fail("Expected unresolvable bean exception");
    }
    catch (RestLiInternalException e)
    {
      assertTrue(e.getMessage().startsWith("Expected to find"));
    }

    EasyMock.verify(ctx);
  }

  @Test
  public void testInjectConstructorArgs()
  {
    Map<String, ResourceModel> pathRootResourceMap =
            buildResourceModels(ConstructorArgResource.class);

    // set up mock ApplicationContext
    BeanProvider ctx = createMock(BeanProvider.class);

    EasyMock.expect(ctx.getBean(EasyMock.eq("dep1"))).andReturn(new SomeDependency1()).anyTimes();
    EasyMock.expect(ctx.getBean(EasyMock.eq("dep3"))).andReturn(new SomeDependency1()).anyTimes();

    Map<String, SomeDependency2> map = new HashMap<>();
    map.put("someBeanName", new SomeDependency2());

    EasyMock.expect(ctx.getBeansOfType(EasyMock.eq(SomeDependency2.class)))
            .andReturn(map).anyTimes();

    EasyMock.replay(ctx);

    InjectResourceFactory factory = new InjectResourceFactory(ctx);

    factory.setRootResources(pathRootResourceMap);

    // #1 happy path
    ConstructorArgResource r1 = factory.create(ConstructorArgResource.class);
    assertNotNull(r1);
    assertNotNull(r1.getDependency1());
    assertNotNull(r1.getDependency2());
    assertNull(r1.getNonInjectedDependency());
  }
}
