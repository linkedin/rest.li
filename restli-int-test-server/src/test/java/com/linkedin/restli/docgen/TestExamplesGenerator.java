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

package com.linkedin.restli.docgen;

import com.linkedin.data.ByteString;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.resolver.ClassNameDataSchemaResolver;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.FieldDef;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.common.ActionResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.server.ActionsResource;
import com.linkedin.restli.examples.greetings.server.CollectionUnderSimpleResource;
import com.linkedin.restli.examples.greetings.server.GreetingsResource;
import com.linkedin.restli.examples.greetings.server.RootSimpleResource;
import com.linkedin.restli.examples.greetings.server.SimpleResourceUnderCollectionResource;
import com.linkedin.restli.examples.groups.api.Group;
import com.linkedin.restli.examples.groups.api.GroupContact;
import com.linkedin.restli.examples.groups.server.rest.impl.GroupContactsResource2;
import com.linkedin.restli.examples.groups.server.rest.impl.GroupMembershipsResource2;
import com.linkedin.restli.examples.groups.server.rest.impl.GroupsResource2;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.model.RestLiApiBuilder;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.restspec.ActionSchema;
import com.linkedin.restli.restspec.ActionSchemaArray;
import com.linkedin.restli.restspec.ActionsSetSchema;
import com.linkedin.restli.restspec.AssociationSchema;
import com.linkedin.restli.restspec.CollectionSchema;
import com.linkedin.restli.restspec.EntitySchema;
import com.linkedin.restli.restspec.FinderSchema;
import com.linkedin.restli.restspec.FinderSchemaArray;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestMethodSchema;
import com.linkedin.restli.restspec.RestMethodSchemaArray;
import com.linkedin.restli.restspec.SimpleSchema;
import com.linkedin.restli.server.ResourceLevel;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test suite for {@link com.linkedin.restli.docgen.RestLiExampleGenerator}.
 *
 * @author Keren Jin
 */
public class TestExamplesGenerator
{
  @Test
  public void testExamples() throws IOException
  {
    final Map<String, ResourceModel> resources = buildResourceModels(ActionsResource.class,
                                                                     GreetingsResource.class,
                                                                     GroupsResource2.class,
                                                                     GroupContactsResource2.class,
                                                                     GroupMembershipsResource2.class,
                                                                     RootSimpleResource.class,
                                                                     CollectionUnderSimpleResource.class,
                                                                     SimpleResourceUnderCollectionResource.class);
    final ResourceSchemaCollection resourceSchemas = ResourceSchemaCollection.loadOrCreateResourceSchema(resources);
    final RestLiExampleGenerator.RequestGenerationSpec spec = new RestLiExampleGenerator.RequestGenerationSpec();
    final DataSchemaResolver schemaResolver = new ClassNameDataSchemaResolver();
    final RestLiExampleGenerator generator = new RestLiExampleGenerator(resourceSchemas,
                                                                        resources,
                                                                        schemaResolver);
    final ValidationOptions valOptions = new ValidationOptions(RequiredMode.MUST_BE_PRESENT);
    RequestResponsePair capture;
    ValidationResult valRet;

    final ResourceSchema greetings = resourceSchemas.getResource("greetings");
    final ResourceSchema groups = resourceSchemas.getResource("groups");
    final ResourceSchema groupsContacts = resourceSchemas.getResource("groups.contacts");
    final ResourceSchema greeting = resourceSchemas.getResource("greeting");
    final ResourceSchema actions = resourceSchemas.getResource("actions");

    List<ResourceSchema> subResources = resourceSchemas.getSubResources(greeting);
    final ResourceSchema subgreetings = subResources.get(0);
    subResources = resourceSchemas.getSubResources(subgreetings);
    final ResourceSchema subsubgreeting = subResources.get(0);

    final RestMethodSchema greetingsGet = findRestMethod(greetings, ResourceMethod.GET);
    capture = generator.generateRestMethodExample(greetings, greetingsGet, spec);
    valRet = validateSingleResponse(capture.getResponse(), Greeting.class, valOptions);
    Assert.assertTrue(valRet.isValid(), valRet.getMessages().toString());

    final RestMethodSchema greetingsCreate = findRestMethod(greetings, ResourceMethod.CREATE);
    capture = generator.generateRestMethodExample(greetings, greetingsCreate, spec);
    Assert.assertSame(capture.getResponse().getEntity().length(), 0);

    final FinderSchema greetingsSearch = findFinder(greetings, "search");
    capture = generator.generateFinderExample(greetings, greetingsSearch, spec);
    valRet = validateCollectionResponse(capture.getResponse(), Greeting.class, valOptions);
    Assert.assertNull(valRet, (valRet == null ? null : valRet.getMessages().toString()));

    final RestMethodSchema groupsContactsGet = findRestMethod(groupsContacts, ResourceMethod.GET);
    capture = generator.generateRestMethodExample(groupsContacts, groupsContactsGet, spec);
    valRet = validateSingleResponse(capture.getResponse(), GroupContact.class, valOptions);
    Assert.assertTrue(valRet.isValid(), valRet.getMessages().toString());

    final FinderSchema groupsSearch = findFinder(groups, "search");
    capture = generator.generateFinderExample(groups, groupsSearch, spec);
    valRet = validateCollectionResponse(capture.getResponse(), Group.class, valOptions);
    Assert.assertNull(valRet, (valRet == null ? null : valRet.getMessages().toString()));

    final ActionSchema greetingsPurge = findCollectionAction(greetings, "purge");
    capture = generator.generateActionExample(greetings, greetingsPurge, ResourceLevel.COLLECTION, spec);
    final DataMap purgeResponse = DataMapUtils.readMap(capture.getResponse());
    Assert.assertTrue(purgeResponse.containsKey("value"));

    final ActionSchema greetingsUpdateTone = findEntityAction(greetings, "updateTone");
    capture = generator.generateActionExample(greetings, greetingsUpdateTone, ResourceLevel.ENTITY, spec);
    valRet = validateCollectionResponse(capture.getResponse(), Greeting.class, valOptions);
    Assert.assertNull(valRet, (valRet == null ? null : valRet.getMessages().toString()));

    final ActionSchema groupsSendTestAnnouncement = findEntityAction(groups, "sendTestAnnouncement");
    capture = generator.generateActionExample(groups, groupsSendTestAnnouncement, ResourceLevel.ENTITY, spec);
    Assert.assertSame(capture.getResponse().getEntity().length(), 0);

    final RestMethodSchema greetingGet = findRestMethod(greeting, ResourceMethod.GET);
    capture = generator.generateRestMethodExample(greeting, greetingGet, spec);
    valRet = validateSingleResponse(capture.getResponse(), Greeting.class, valOptions);
    Assert.assertTrue(valRet.isValid(), valRet.getMessages().toString());
    RestRequest request = capture.getRequest();
    Assert.assertEquals(request.getURI(), URI.create("/greeting"));

    final RestMethodSchema greetingUpdate = findRestMethod(greeting, ResourceMethod.UPDATE);
    capture = generator.generateRestMethodExample(greeting, greetingUpdate, spec);
    Assert.assertSame(capture.getResponse().getEntity().length(), 0);
    request = capture.getRequest();
    Assert.assertEquals(request.getURI(), URI.create("/greeting"));
    valRet = validateSingleRequest(capture.getRequest(), Greeting.class, valOptions);
    Assert.assertTrue(valRet.isValid(), valRet.getMessages().toString());

    final RestMethodSchema greetingPartialUpdate = findRestMethod(greeting, ResourceMethod.PARTIAL_UPDATE);
    capture = generator.generateRestMethodExample(greeting, greetingPartialUpdate, spec);
    Assert.assertSame(capture.getResponse().getEntity().length(), 0);
    request = capture.getRequest();
    Assert.assertEquals(request.getURI(), URI.create("/greeting"));
    DataMap patchMap = _codec.bytesToMap(capture.getRequest().getEntity().copyBytes());
    checkPatchMap(patchMap);

    final RestMethodSchema greetingDelete = findRestMethod(greeting, ResourceMethod.DELETE);
    capture = generator.generateRestMethodExample(greeting, greetingUpdate, spec);
    Assert.assertSame(capture.getResponse().getEntity().length(), 0);
    request = capture.getRequest();
    Assert.assertEquals(request.getURI(), URI.create("/greeting"));

    final ActionSchema greetingExampleAction = findSimpleResourceAction(greeting, "exampleAction");
    capture = generator.generateActionExample(greeting, greetingExampleAction, ResourceLevel.ENTITY, spec);
    DataMap exampleActionResponse = DataMapUtils.readMap(capture.getResponse());
    Assert.assertTrue(exampleActionResponse.containsKey("value"));
    request = capture.getRequest();
    Assert.assertTrue(validateUrlPath(request.getURI(), new String[]{ "greeting" }));

    final RestMethodSchema subgreetingsCreate = findRestMethod(subgreetings, ResourceMethod.CREATE);
    capture = generator.generateRestMethodExample(subgreetings, subgreetingsCreate, spec);
    Assert.assertSame(capture.getResponse().getEntity().length(), 0);
    request = capture.getRequest();
    Assert.assertEquals(request.getURI(), URI.create("/greeting/subgreetings"));
    valRet = validateSingleRequest(capture.getRequest(), Greeting.class, valOptions);
    Assert.assertTrue(valRet.isValid(), valRet.getMessages().toString());

    final RestMethodSchema subsubgreetingGet = findRestMethod(subsubgreeting, ResourceMethod.GET);
    capture = generator.generateRestMethodExample(subsubgreeting, subsubgreetingGet, spec);
    valRet = validateSingleResponse(capture.getResponse(), Greeting.class, valOptions);
    Assert.assertTrue(valRet.isValid(), valRet.getMessages().toString());
    request = capture.getRequest();
    Assert.assertTrue(validateUrlPath(request.getURI(),
                                      new String[]{"greeting", "subgreetings", null, "subsubgreeting"}));

    final RestMethodSchema subsubgreetingUpdate = findRestMethod(subsubgreeting, ResourceMethod.UPDATE);
    capture = generator.generateRestMethodExample(subsubgreeting, subsubgreetingUpdate, spec);
    Assert.assertSame(capture.getResponse().getEntity().length(), 0);
    request = capture.getRequest();
    Assert.assertTrue(validateUrlPath(request.getURI(),
                                      new String[]{"greeting", "subgreetings", null, "subsubgreeting"}));
    valRet = validateSingleRequest(capture.getRequest(), Greeting.class, valOptions);
    Assert.assertTrue(valRet.isValid(), valRet.getMessages().toString());

    final RestMethodSchema subsubgreetingPartialUpdate = findRestMethod(subsubgreeting, ResourceMethod.PARTIAL_UPDATE);
    capture = generator.generateRestMethodExample(subsubgreeting, subsubgreetingPartialUpdate, spec);
    Assert.assertSame(capture.getResponse().getEntity().length(), 0);
    request = capture.getRequest();
    Assert.assertTrue(validateUrlPath(request.getURI(),
                                      new String[]{"greeting", "subgreetings", null, "subsubgreeting"}));

    patchMap = _codec.bytesToMap(capture.getRequest().getEntity().copyBytes());
    checkPatchMap(patchMap);

    final RestMethodSchema subsubgreetingDelete = findRestMethod(subsubgreeting, ResourceMethod.DELETE);
    capture = generator.generateRestMethodExample(subsubgreeting, subsubgreetingDelete, spec);
    Assert.assertSame(capture.getResponse().getEntity().length(), 0);
    request = capture.getRequest();
    Assert.assertTrue(validateUrlPath(request.getURI(),
                                      new String[]{"greeting", "subgreetings", null, "subsubgreeting"}));

    final ActionSchema subsubgreetingExampleAction = findSimpleResourceAction(subsubgreeting, "exampleAction");
    capture = generator.generateActionExample(subsubgreeting, subsubgreetingExampleAction, ResourceLevel.ENTITY, spec);
    exampleActionResponse = DataMapUtils.readMap(capture.getResponse());
    Assert.assertTrue(exampleActionResponse.containsKey("value"));
    request = capture.getRequest();
    Assert.assertTrue(validateUrlPath(request.getURI(),
                                      new String[]{"greeting", "subgreetings", null, "subsubgreeting"}));

    final ActionSchema actionsEchoMessageArray = findActionsSetAction(actions, "echoMessageArray");
    capture = generator.generateActionExample(actions, actionsEchoMessageArray, ResourceLevel.COLLECTION, spec);
    final DataMap echoMessageArrayResponse = DataMapUtils.readMap(capture.getResponse());
    Assert.assertTrue(echoMessageArrayResponse.containsKey("value"));

    final ActionSchema actionsEchoToneArray = findActionsSetAction(actions, "echoToneArray");
    capture = generator.generateActionExample(actions, actionsEchoToneArray, ResourceLevel.COLLECTION, spec);
    final DataMap echoToneArrayResponse = DataMapUtils.readMap(capture.getResponse());
    Assert.assertTrue(echoToneArrayResponse.containsKey("value"));

    final ActionSchema actionsEchoStringArray = findActionsSetAction(actions, "echoStringArray");
    capture = generator.generateActionExample(actions, actionsEchoStringArray, ResourceLevel.COLLECTION, spec);
    final DataMap echoStringArrayResponse = DataMapUtils.readMap(capture.getResponse());
    Assert.assertTrue(echoStringArrayResponse.containsKey("value"));
  }

  private static void checkPatchMap(DataMap patchMap)
  {
    DataMap patch = patchMap.getDataMap("patch");
    Assert.assertNotNull(patch);
    Assert.assertTrue(patch.containsKey("$set"));
  }

  private static Map<String, ResourceModel> buildResourceModels(Class<?>... resourceClasses)
  {
    final Set<Class<?>> classes = new HashSet<Class<?>>(Arrays.asList(resourceClasses));
    return RestLiApiBuilder.buildResourceModels(classes);
  }

  private static RestMethodSchema findRestMethod(ResourceSchema resourceSchema, ResourceMethod method)
  {
    RestMethodSchemaArray methods = null;
    RestMethodSchema methodResult = null;

    final CollectionSchema collectionSchema = resourceSchema.getCollection();
    if (collectionSchema != null)
    {
      methods = collectionSchema.getMethods();
    }

    final AssociationSchema associationSchema = resourceSchema.getAssociation();
    if (associationSchema != null)
    {
      methods = associationSchema.getMethods();
    }

    final SimpleSchema simpleSchema = resourceSchema.getSimple();
    if (simpleSchema != null)
    {
      methods = simpleSchema.getMethods();
    }

    if (methods != null)
    {
      for (RestMethodSchema restMethodSchema: methods)
      {
        if (restMethodSchema.getMethod().equalsIgnoreCase(method.name()))
        {
          methodResult = restMethodSchema;
          break;
        }
      }
    }

    return methodResult;
  }

  private static FinderSchema findFinder(ResourceSchema resourceSchema, String finderName)
  {
    final CollectionSchema collectionSchema = resourceSchema.getCollection();
    if (collectionSchema != null)
    {
      final FinderSchemaArray finders = collectionSchema.getFinders();
      if (finders != null)
      {
        for (FinderSchema finderSchema: finders)
        {
          if (finderSchema.getName().equals(finderName))
          {
            return finderSchema;
          }
        }
      }
    }

    final AssociationSchema associationSchema = resourceSchema.getAssociation();
    if (associationSchema != null)
    {
      final FinderSchemaArray finders = associationSchema.getFinders();
      if (finders != null)
      {
        for (FinderSchema finderSchema: finders)
        {
          if (finderSchema.getName().equals(finderName))
          {
            return finderSchema;
          }
        }
      }
    }

    return null;
  }

  private static ActionSchema findActionsSetAction(ResourceSchema resourceSchema, String actionName)
  {
    final ActionsSetSchema actionsSetSchema = resourceSchema.getActionsSet();
    if (actionsSetSchema != null)
    {
      final ActionSchemaArray actions = actionsSetSchema.getActions();
      if (actions != null)
      {
        for (ActionSchema actionSchema: actions)
        {
          if (actionSchema.getName().equals(actionName))
          {
            return actionSchema;
          }
        }
      }
    }

    return null;
  }

  private static ActionSchema findCollectionAction(ResourceSchema resourceSchema, String actionName)
  {
    final CollectionSchema collectionSchema = resourceSchema.getCollection();
    if (collectionSchema != null)
    {
      final ActionSchemaArray actions = collectionSchema.getActions();
      if (actions != null)
      {
        for (ActionSchema actionSchema: actions)
        {
          if (actionSchema.getName().equals(actionName))
          {
            return actionSchema;
          }
        }
      }
    }

    final AssociationSchema associationSchema = resourceSchema.getAssociation();
    if (associationSchema != null)
    {
      final ActionSchemaArray actions = associationSchema.getActions();
      if (actions != null)
      {
        for (ActionSchema actionSchema: actions)
        {
          if (actionSchema.getName().equals(actionName))
          {
            return actionSchema;
          }
        }
      }
    }

    return null;
  }

  private static ActionSchema findSimpleResourceAction(ResourceSchema resourceSchema, String actionName)
  {
    final SimpleSchema simpleSchema = resourceSchema.getSimple();
    if (simpleSchema != null)
    {
      final ActionSchemaArray actions = simpleSchema.getActions();
      if (actions != null)
      {
        for (ActionSchema actionSchema: actions)
        {
          if (actionSchema.getName().equals(actionName))
          {
            return actionSchema;
          }
        }
      }
    }

    return null;
  }

  private static ActionSchema findActionInEntity(EntitySchema entitySchema, String actionName)
  {
    final ActionSchemaArray actions = entitySchema.getActions();
    if (actions != null)
    {
      for (ActionSchema actionSchema: actions)
      {
        if (actionSchema.getName().equals(actionName))
        {
          return actionSchema;
        }
      }
    }

    return null;
  }

  private static ActionSchema findEntityAction(ResourceSchema resourceSchema, String actionName)
  {
    ActionSchema actionSchema;

    final CollectionSchema collectionSchema = resourceSchema.getCollection();
    if (collectionSchema != null)
    {
      final EntitySchema entity = collectionSchema.getEntity();
      if (entity != null)
      {
        actionSchema = findActionInEntity(entity, actionName);
        if (actionSchema != null)
        {
          return actionSchema;
        }
      }
    }

    final AssociationSchema associationSchema = resourceSchema.getAssociation();
    if (associationSchema != null)
    {
      final EntitySchema entity = associationSchema.getEntity();
      if (entity != null)
      {
        actionSchema = findActionInEntity(entity, actionName);
        if (actionSchema != null)
        {
          return actionSchema;
        }
      }
    }

    return null;
  }

  private boolean validateUrlPath(URI uri, String[] expected)
  {
    boolean result = true;
    String path = uri.getPath();

    if (path.startsWith("/"))
    {
      path = path.substring(1);
    }

    String[] segments = path.split("/");
    if (segments.length != expected.length)
    {
      result = false;
    }
    else
    {
      for (int i = 0; i < segments.length; ++i)
      {
        //null is considered as a wildcard segment in the expected
        if (expected[i] != null && !expected[i].equals(segments[i]))
        {
          result = false;
          break;
        }
      }
    }

    return result;
  }

  private ValidationResult validateSingleResponse(RestResponse response,
                                                  Class<? extends RecordTemplate> recordClass,
                                                  ValidationOptions options) throws IOException
  {
    return validateEntity(response.getEntity(), recordClass, options);
  }

  private ValidationResult validateSingleRequest(RestRequest request,
                                                 Class<? extends RecordTemplate> recordClass,
                                                 ValidationOptions options) throws IOException
  {
    return validateEntity(request.getEntity(), recordClass, options);
  }

  private ValidationResult validateEntity(ByteString entity,
                                          Class<? extends RecordTemplate> recordClass,
                                          ValidationOptions options) throws IOException
  {
    final DataMap respData = _codec.bytesToMap(entity.copyBytes());
    final DataSchema recordSchema = DataTemplateUtil.getSchema(recordClass);
    return ValidateDataAgainstSchema.validate(respData, recordSchema, options);
  }

  private <T extends RecordTemplate>
  ValidationResult validateCollectionResponse(RestResponse response,
                                              Class<T> recordClass,
                                              ValidationOptions options)
      throws IOException
  {
    final DataMap respData = _codec.bytesToMap(response.getEntity().copyBytes());
    final CollectionResponse<T> collResp = new CollectionResponse<T>(respData, recordClass);
    final DataSchema recordSchema = DataTemplateUtil.getSchema(recordClass);

    for (T record: collResp.getElements())
    {
      final ValidationResult valRet = ValidateDataAgainstSchema.validate(record.data(), recordSchema, options);
      if (!valRet.isValid())
      {
        return valRet;
      }
    }

    return null;
  }

  private <T extends RecordTemplate>
  ValidationResult validateActionResponse(RestResponse response,
                                          Class<T> recordClass,
                                          ValidationOptions options)
      throws IOException
  {
    final DataMap respData = _codec.bytesToMap(response.getEntity().copyBytes());
    final FieldDef<T> responseFieldDef = new FieldDef<T>(ActionResponse.VALUE_NAME,
                                                         recordClass,
                                                         DataTemplateUtil.getSchema(recordClass));
    final RecordDataSchema recordDataSchema = DynamicRecordMetadata.buildSchema(ActionResponse.class.getName(),
                                                                                Collections.<FieldDef<?>>singletonList(responseFieldDef));
    final ActionResponse<T> actionResp = new ActionResponse<T>(respData, responseFieldDef, recordDataSchema);
    final DataSchema recordSchema = DataTemplateUtil.getSchema(recordClass);

    return ValidateDataAgainstSchema.validate(actionResp.getValue().data(), recordSchema, options);
  }

  private JacksonDataCodec _codec = new JacksonDataCodec();
}
