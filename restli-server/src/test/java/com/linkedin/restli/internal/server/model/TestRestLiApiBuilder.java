/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.internal.server.model;

import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.server.ResourceConfigException;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.PathKeyParam;
import com.linkedin.restli.server.annotations.RestLiActions;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestLiSimpleResource;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.resources.SimpleResourceTemplate;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestRestLiApiBuilder
{
  @RestLiCollection(name = "foo")
  public static class FooResource1 extends CollectionResourceTemplate<Long, EmptyRecord> {}

  @RestLiCollection(name = "foo")
  public static class FooResource2 extends CollectionResourceTemplate<Long, EmptyRecord> {}

  @RestLiSimpleResource(name = "foo")
  public static class FooResource3 extends SimpleResourceTemplate<EmptyRecord> {}

  @RestLiActions(name = "foo")
  public static class FooResource4 {}

  @RestLiCollection(name = "bar")
  public static class BarResource extends CollectionResourceTemplate<Long, EmptyRecord> {}

  @RestLiCollection(name = "FOO")
  public static class FOOResource extends CollectionResourceTemplate<Long, EmptyRecord> {}

  @DataProvider(name = "resourcesWithClashingNamesDataProvider")
  public Object[][] provideResourcesWithClashingNames()
  {
    return new Object[][]
        {
            {new Class<?>[]{FooResource1.class, FooResource2.class}},
            {new Class<?>[]{FooResource1.class, FooResource2.class, BarResource.class}},
            {new Class<?>[]{FooResource1.class, FooResource3.class}},
            {new Class<?>[]{FooResource1.class, FooResource4.class}},
            {new Class<?>[]{FooResource3.class, FooResource4.class}},
        };
  }

  @Test(dataProvider = "resourcesWithClashingNamesDataProvider")
  public void testResourceNameClash(Class<?>[] classes)
  {
//    Set<Class<?>> resourceClasses = new HashSet<Class<?>>(Arrays.<Class<?>>asList(classes));
//    try
//    {
//      RestLiApiBuilder.buildResourceModels(resourceClasses);
//      Assert.fail("API generation should have failed due to name clashes!");
//    }
//    catch (ResourceConfigException e)
//    {
//      Assert.assertTrue(e.getMessage().contains("clash on the resource name \"foo\""),
//                        "The exception did not contain the expected resource name clashing error message.");
//    }
  }

  @DataProvider(name = "resourcesWithNoClashingNamesDataProvider")
  public Object[][] provideResourcesWithNoClashingNames()
  {
    return new Object[][]
        {
            {new Class<?>[] {}},
            {new Class<?>[]{FooResource1.class, FOOResource.class}},
            {new Class<?>[]{FooResource1.class, BarResource.class}},
        };
  }

  @Test(dataProvider = "resourcesWithNoClashingNamesDataProvider")
  public void testResourceNameNoClash(Class<?>[] classes)
  {
//    Set<Class<?>> resourceClasses = new HashSet<Class<?>>(Arrays.<Class<?>>asList(classes));
//    Map<String, ResourceModel> resourceModels = RestLiApiBuilder.buildResourceModels(resourceClasses);
//    Assert.assertEquals(resourceModels.size(),
//                        classes.length,
//                        "The number of ResourceModels generated does not match the number of resource classes.");
  }

  @Test
  public void testProcessResource()
  {
    Set<Class<?>> set = new HashSet<>();
    set.add(com.linkedin.restli.internal.server.model.ParentResource.class);
    set.add(TestResource.class);
    Map<String, ResourceModel> models = RestLiApiBuilder.buildResourceModels(set);

    ResourceModel parentResource = models.get("/ParentResource");
    junit.framework.Assert.assertTrue(parentResource.getSubResource("TestResource") != null);
  }

  @Test
  public void testBadResource()
  {
//    Set<Class<?>> set = new HashSet<>();
//    set.add(com.linkedin.restli.internal.server.model.ParentResource.class);
//    set.add(BadResource.class);
//
//    try
//    {
//      Map<String, ResourceModel> models = RestLiApiBuilder.buildResourceModels(set);
//      junit.framework.Assert.fail(
//          "Building api with BadResource should throw " + ResourceConfigException.class);
//    }
//    catch (ResourceConfigException e)  {
//      junit.framework.Assert.assertTrue(e.getMessage().contains("bogusKey not found in path keys"));
//    }
  }
}

@RestLiCollection(
    name = "TestResource",
    namespace = "com.linkedin.restli.internal.server.model",
    parent = com.linkedin.restli.internal.server.model.ParentResource.class
)
class TestResource extends CollectionResourceTemplate<String, EmptyRecord>
{
  @Action(name="testResourceAction")
  public void takeAction()
  {}
}

@RestLiCollection(
    name = "ParentResource",
    namespace = "com.linkedin.restli.internal.server.model"
)
class ParentResource extends CollectionResourceTemplate<String, EmptyRecord>
{}

@RestLiCollection(
    name = "BadResource",
    namespace = "com.linkedin.restli.internal.server.model"
)
class BadResource extends CollectionResourceTemplate<String, EmptyRecord>
{
  @Action(name="badResourceAction")
  public void takeAction(@PathKeyParam("bogusKey") String bogusKey)
  {}
}

