/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.tools.clientgen;


import com.linkedin.data.schema.DataSchema;
import com.linkedin.pegasus.generator.CodeUtil;
import com.linkedin.pegasus.generator.DataSchemaParser;
import com.linkedin.pegasus.generator.TemplateSpecGenerator;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.common.RestliVersion;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.tools.clientgen.builderspec.ActionBuilderSpec;
import com.linkedin.restli.tools.clientgen.builderspec.BuilderSpec;
import com.linkedin.restli.tools.clientgen.builderspec.CollectionRootBuilderSpec;
import com.linkedin.restli.tools.clientgen.builderspec.FinderBuilderSpec;
import com.linkedin.restli.tools.clientgen.builderspec.PathKeyBindingMethodSpec;
import com.linkedin.restli.tools.clientgen.builderspec.QueryParamBindingMethodSpec;
import com.linkedin.restli.tools.clientgen.builderspec.RestMethodBuilderSpec;
import com.linkedin.restli.tools.clientgen.builderspec.RootBuilderMethodSpec;
import com.linkedin.restli.tools.clientgen.builderspec.RootBuilderSpec;
import com.linkedin.restli.tools.clientgen.builderspec.SimpleRootBuilderSpec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * @author Min Chen
 */
public class TestRequestBuilderSpecGenerator
{
  private static final String FS = File.separator;
  private static final String IDLS_DIR = "src" + FS + "test" + FS + "resources" + FS + "idls";
  private static final String RESOLVER_DIR = "src" + FS + "test" + FS + "resources" + FS + "pegasus";

  // Gradle by default will use the module directory as the working directory
  // IDE such as IntelliJ IDEA may use the project directory instead
  // If you create test in IDE, make sure the working directory is always the module directory
  private String moduleDir;

  @BeforeClass
  public void setUp() throws IOException
  {
    moduleDir = System.getProperty("user.dir");
  }

  private Set<BuilderSpec> generateBuilderSpec(String[] sources)
  {
    final DataSchemaParser schemaParser = new DataSchemaParser(RESOLVER_DIR);
    final TemplateSpecGenerator specGenerator = new TemplateSpecGenerator(schemaParser.getSchemaResolver());
    final RestSpecParser parser = new RestSpecParser();
    final Map<ResourceMethod, String> builderBaseMap = new HashMap<ResourceMethod, String>();
    builderBaseMap.put(ResourceMethod.GET, "GetRequestBuilder");
    builderBaseMap.put(ResourceMethod.DELETE, "DeleteRequestBuilder");
    builderBaseMap.put(ResourceMethod.UPDATE, "UpdateRequestBuilder");
    builderBaseMap.put(ResourceMethod.CREATE, "CreateIdRequestBuilder");
    builderBaseMap.put(ResourceMethod.PARTIAL_UPDATE, "PartialUpdateRequestBuilder");
    builderBaseMap.put(ResourceMethod.GET_ALL, "GetAllRequestBuilder");
    builderBaseMap.put(ResourceMethod.OPTIONS, "OptionsRequestBuilder");
    builderBaseMap.put(ResourceMethod.ACTION, "ActionRequestBuilder");
    builderBaseMap.put(ResourceMethod.FINDER, "FinderRequestBuilder");
    builderBaseMap.put(ResourceMethod.BATCH_GET, "BatchGetRequestBuilder");
    builderBaseMap.put(ResourceMethod.BATCH_UPDATE, "BatchUpdateRequestBuilder");
    builderBaseMap.put(ResourceMethod.BATCH_PARTIAL_UPDATE, "BatchPartialUpdateRequestBuilder");
    builderBaseMap.put(ResourceMethod.BATCH_DELETE, "BatchDeleteRequestBuilder");
    builderBaseMap.put(ResourceMethod.BATCH_CREATE, "BatchCreateIdRequestBuilder");
    final RequestBuilderSpecGenerator builderSpecGenerator =
        new RequestBuilderSpecGenerator(schemaParser.getSchemaResolver(), specGenerator, RestliVersion.RESTLI_2_0_0,
                                        builderBaseMap);

    // parse idl to ResourceSchemas
    final RestSpecParser.ParseResult parseResult = parser.parseSources(sources);

    // generate Builder specs from ResourceSchema
    for (CodeUtil.Pair<ResourceSchema, File> pair : parseResult.getSchemaAndFiles())
    {
      builderSpecGenerator.generate(pair.first, pair.second);
    }
    return builderSpecGenerator.getBuilderSpec();
  }

  @Test
  public void testSimpleResource() throws Exception
  {
    String idl = moduleDir + FS + IDLS_DIR + FS + "testSimple.restspec.json";
    Set<BuilderSpec> builderSpecs = generateBuilderSpec(new String[] {idl});
    Assert.assertNotNull(builderSpecs);
    Assert.assertTrue(builderSpecs.size() == 6);
    Map<String, String> methodMap = new HashMap<String, String>();
    methodMap.put("get", "Gets the greeting.");
    methodMap.put("delete","Deletes the greeting.");
    methodMap.put("update", "Updates the greeting.");
    for (BuilderSpec spec : builderSpecs)
    {
      Assert.assertTrue(spec instanceof RootBuilderSpec || spec instanceof RestMethodBuilderSpec);
      if (spec instanceof RootBuilderSpec)
      {
        Assert.assertTrue(spec instanceof SimpleRootBuilderSpec);
        SimpleRootBuilderSpec simpleSpec = (SimpleRootBuilderSpec) spec;
        if (simpleSpec.getResourcePath().indexOf('/') >= 0)
        {
          Assert.assertEquals(simpleSpec.getSourceIdlName(), idl);
          Assert.assertEquals(simpleSpec.getResourcePath(), "testSimple/testSimpleSub");
          Assert.assertNotNull(simpleSpec.getRestMethods());
          Assert.assertTrue(simpleSpec.getRestMethods().size() == 1);
          Assert.assertEquals("get", simpleSpec.getRestMethods().get(0).getName());
          Assert.assertTrue(simpleSpec.getResourceActions().isEmpty());
          Assert.assertTrue(simpleSpec.getSubresources().isEmpty());
        }
        else
        {
          Assert.assertTrue(simpleSpec.getResourceActions().isEmpty());
          Assert.assertTrue(simpleSpec.getSubresources().size() == 1);
          Assert.assertEquals(simpleSpec.getSourceIdlName(), idl);
          Assert.assertEquals(simpleSpec.getResourcePath(), "testSimple");
          Assert.assertNotNull(simpleSpec.getRestMethods());
          Assert.assertTrue(simpleSpec.getRestMethods().size() == 3);
          List<RootBuilderMethodSpec> restMethods = simpleSpec.getRestMethods();
          for (RootBuilderMethodSpec method : restMethods)
          {
            Assert.assertTrue(method.getReturn() instanceof RestMethodBuilderSpec);
            Assert.assertTrue(methodMap.containsKey(method.getName()));
            Assert.assertEquals(methodMap.get(method.getName()), method.getDoc());
          }
        }
      }
      else if (spec instanceof RestMethodBuilderSpec)
      {
        ResourceMethod method = ((RestMethodBuilderSpec) spec).getResourceMethod();
        Assert.assertTrue(methodMap.containsKey(method.toString()));
      }
    }
  }

  @Test
  public void testCollectionResource() throws Exception
  {
    String idl = moduleDir + FS + IDLS_DIR + FS + "testCollection.restspec.json";
    Set<BuilderSpec> builderSpecs = generateBuilderSpec(new String[] {idl});
    Assert.assertNotNull(builderSpecs);
    Assert.assertTrue(builderSpecs.size() == 15);
    List<String> expectedMethods = Arrays.asList("actionAnotherAction", "actionSomeAction", "actionVoidAction", "batchGet", "create", "delete", "findBySearch", "get", "getAll", "partialUpdate", "update");
    List<String> actualMethods = new ArrayList<String>();
    CollectionRootBuilderSpec rootBuilder = null;
    CollectionRootBuilderSpec subRootBuilder = null;
    FinderBuilderSpec finderBuilder = null;
    List<ActionBuilderSpec> actionBuilders = new ArrayList<ActionBuilderSpec>();
    List<RestMethodBuilderSpec> basicMethodBuilders = new ArrayList<RestMethodBuilderSpec>();

    for (BuilderSpec spec : builderSpecs)
    {
      if (spec instanceof RootBuilderSpec)
      {
        Assert.assertTrue(spec instanceof CollectionRootBuilderSpec);
        CollectionRootBuilderSpec collSpec = (CollectionRootBuilderSpec)spec;
        if (collSpec.getResourcePath().indexOf('/') >= 0 )
        {
          subRootBuilder = collSpec;
        }
        else
        {
          rootBuilder = collSpec;
        }
      }
      else if (spec instanceof FinderBuilderSpec)
      {
        finderBuilder = (FinderBuilderSpec) spec;
      }
      else if (spec instanceof ActionBuilderSpec)
      {
        actionBuilders.add((ActionBuilderSpec) spec);
      }
      else if (spec instanceof RestMethodBuilderSpec)
      {
        basicMethodBuilders.add((RestMethodBuilderSpec) spec);
      }
      else
      {
        Assert.fail("There should not be any other builder spec generated!");
      }
    }

    // assert sub resource root builder spec
    Assert.assertNotNull(subRootBuilder);
    Assert.assertEquals(subRootBuilder.getSourceIdlName(), idl);
    Assert.assertEquals(subRootBuilder.getResourcePath(), "testCollection/{testCollectionId}/testCollectionSub");
    Assert.assertNotNull(subRootBuilder.getRestMethods());
    Assert.assertTrue(subRootBuilder.getRestMethods().size() == 2);
    Assert.assertTrue(subRootBuilder.getFinders().isEmpty());
    Assert.assertTrue(subRootBuilder.getResourceActions().isEmpty());
    Assert.assertTrue(subRootBuilder.getEntityActions().isEmpty());
    Assert.assertTrue(subRootBuilder.getSubresources().isEmpty());

    // assert root builder spec
    Assert.assertNotNull(rootBuilder);
    Assert.assertEquals(rootBuilder.getSourceIdlName(), idl);
    Assert.assertEquals(rootBuilder.getResourcePath(), "testCollection");
    Assert.assertNotNull(rootBuilder.getRestMethods());
    Assert.assertTrue(rootBuilder.getRestMethods().size() == 7);
    for (RootBuilderMethodSpec method : rootBuilder.getRestMethods())
    {
      actualMethods.add(method.getName());
    }
    Assert.assertNotNull(rootBuilder.getFinders());
    Assert.assertTrue(rootBuilder.getFinders().size() == 1);
    actualMethods.add(rootBuilder.getFinders().get(0).getName());
    Assert.assertNotNull(rootBuilder.getResourceActions());
    Assert.assertTrue(rootBuilder.getResourceActions().size() == 1);
    actualMethods.add(rootBuilder.getResourceActions().get(0).getName());
    Assert.assertNotNull(rootBuilder.getEntityActions());
    Assert.assertTrue(rootBuilder.getEntityActions().size() == 2);
    actualMethods.add(rootBuilder.getEntityActions().get(0).getName());
    actualMethods.add(rootBuilder.getEntityActions().get(1).getName());
    Assert.assertNotNull(rootBuilder.getSubresources());
    Assert.assertTrue(rootBuilder.getSubresources().size() == 1);
    Collections.sort(actualMethods);
    Assert.assertEquals(actualMethods, expectedMethods);

    // assert finder builder spec
    Assert.assertNotNull(finderBuilder);
    Assert.assertEquals("search", finderBuilder.getFinderName());
    Assert.assertNotNull(finderBuilder.getQueryParamMethods());
    Assert.assertEquals(finderBuilder.getMetadataType().getFullName(),
                        "com.linkedin.restli.tools.test.TestRecord");
    Assert.assertTrue(finderBuilder.getQueryParamMethods().size() == 1);
    QueryParamBindingMethodSpec finderQuery = finderBuilder.getQueryParamMethods().get(0);
    Assert.assertEquals(finderQuery.getParamName(), "tone");
    Assert.assertEquals(finderQuery.getMethodName(), "toneParam");
    Assert.assertEquals(finderQuery.getArgType().getFullName(), "com.linkedin.restli.tools.test.TestEnum");
    Assert.assertFalse(finderQuery.isNeedAddParamMethod());
    Assert.assertTrue(finderQuery.isOptional());

    // assert action builder spec
    Assert.assertNotNull(actionBuilders);
    Assert.assertTrue(actionBuilders.size() == 3);
    for (ActionBuilderSpec spec : actionBuilders)
    {
      Assert.assertTrue(spec.getActionName().equals("someAction") || spec.getActionName().equals("anotherAction") || spec.getActionName().equals("voidAction"));
    }

    // assert get method builder query method
    Assert.assertNotNull(basicMethodBuilders);
    Assert.assertTrue(basicMethodBuilders.size() == 9); // 7 for root resource, 2 for sub resource
    for (RestMethodBuilderSpec spec : basicMethodBuilders)
    {
      if (spec.getResourceMethod() == ResourceMethod.GET)
      {
        Assert.assertNotNull(spec.getQueryParamMethods());
        Assert.assertTrue(spec.getQueryParamMethods().size() == 1);
        QueryParamBindingMethodSpec getQuery = spec.getQueryParamMethods().get(0);
        Assert.assertEquals(getQuery.getParamName(), "message");
        Assert.assertEquals(getQuery.getMethodName(), "messageParam");
        Assert.assertEquals(getQuery.getArgType().getSchema().getType(), DataSchema.Type.STRING);
        Assert.assertFalse(getQuery.isNeedAddParamMethod());
        Assert.assertTrue(getQuery.isOptional());
      }
      else if (spec.getResourceMethod() == ResourceMethod.DELETE && spec.getClassName().startsWith("TestCollectionSub"))
      {
        // sub resource delete method should have path keys
        List<PathKeyBindingMethodSpec> pathKeys = spec.getPathKeyMethods();
        Assert.assertNotNull(pathKeys);
        Assert.assertTrue(pathKeys.size() == 1);
        PathKeyBindingMethodSpec pathKeyMethod = pathKeys.get(0);
        Assert.assertEquals(pathKeyMethod.getPathKey(), "testCollectionId");
        Assert.assertEquals(pathKeyMethod.getMethodName(), "testCollectionIdKey");
        Assert.assertEquals(pathKeyMethod.getArgType().getSchema().getType(), DataSchema.Type.LONG);
      }
    }
  }
}