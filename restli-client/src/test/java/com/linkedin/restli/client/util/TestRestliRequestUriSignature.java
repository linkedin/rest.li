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

package com.linkedin.restli.client.util;


import com.linkedin.data.DataMap;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.util.RestliRequestUriSignature.SignatureField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Keren Jin
 */
public class TestRestliRequestUriSignature
{
  private static final String BASE_URI_TEMPLATE;
  private static final Map<String, Object> PATH_KEYS;
  private static final Object ID;
  private static final Map<String, Object> QUERY_PARAMS_OBJECTS;
  static
  {
    BASE_URI_TEMPLATE = "myBaseUriTemplate";

    final DataMap nestedMap = new DataMap();
    nestedMap.put("foo", 1);
    nestedMap.put("bar", 2);

    PATH_KEYS = new HashMap<String, Object>();
    PATH_KEYS.put("pathKey1", "value1");
    PATH_KEYS.put("pathKey2", nestedMap);

    ID = "myID";

    QUERY_PARAMS_OBJECTS = new HashMap<String, Object>();
    QUERY_PARAMS_OBJECTS.put("queryKey1", "value1");
    QUERY_PARAMS_OBJECTS.put("queryKey2", nestedMap);
  }

  @Test
  public void testFullFields()
  {
    for (SignatureField field : SignatureField.values())
    {
      Assert.assertTrue(RestliRequestUriSignature.ALL_FIELDS.contains(field));
    }
  }

  @Test
  public void testBasic()
  {
    final Request<?> request = Mockito.mock(Request.class);
    Mockito.when(request.getBaseUriTemplate()).thenReturn(BASE_URI_TEMPLATE);
    Mockito.when(request.getPathKeys()).thenReturn(PATH_KEYS);
    Mockito.when(request.getQueryParamsObjects()).thenReturn(QUERY_PARAMS_OBJECTS);

    final RestliRequestUriSignature signature = new RestliRequestUriSignature(request, RestliRequestUriSignature.ALL_FIELDS);
    Assert.assertEquals(signature.getMaskFields(), RestliRequestUriSignature.ALL_FIELDS);

    final String signatureString = signature.toString();
    Assert.assertTrue(signatureString.contains(BASE_URI_TEMPLATE));
    Assert.assertTrue(isMapContainedInString(signatureString, PATH_KEYS));
    Assert.assertTrue(isMapContainedInString(signatureString, QUERY_PARAMS_OBJECTS));
  }

  @Test
  public void testPartialFields()
  {
    final Request<?> request = Mockito.mock(Request.class);
    Mockito.when(request.getBaseUriTemplate()).thenReturn(BASE_URI_TEMPLATE);
    Mockito.when(request.getPathKeys()).thenReturn(PATH_KEYS);
    Mockito.when(request.getQueryParamsObjects()).thenReturn(QUERY_PARAMS_OBJECTS);

    final Set<SignatureField> fields = new HashSet<SignatureField>(Arrays.asList(SignatureField.BASE_URI_TEMPLATE, SignatureField.PATH_KEYS));
    final RestliRequestUriSignature signature = new RestliRequestUriSignature(request, fields);
    Assert.assertEquals(signature.getMaskFields(), fields);

    final String signatureString = signature.toString();
    Assert.assertTrue(signatureString.contains(BASE_URI_TEMPLATE));
    Assert.assertTrue(isMapContainedInString(signatureString, PATH_KEYS));
    Assert.assertFalse(isMapContainedInString(signatureString, QUERY_PARAMS_OBJECTS));
  }

  @Test
  public void testID()
  {
    final GetRequest<?> getRequest = Mockito.mock(GetRequest.class);
    Mockito.when(getRequest.getBaseUriTemplate()).thenReturn(BASE_URI_TEMPLATE);
    Mockito.when(getRequest.getPathKeys()).thenReturn(PATH_KEYS);
    Mockito.when(getRequest.getObjectId()).thenReturn(ID);
    Mockito.when(getRequest.getQueryParamsObjects()).thenReturn(QUERY_PARAMS_OBJECTS);

    final RestliRequestUriSignature signature = new RestliRequestUriSignature(getRequest, RestliRequestUriSignature.ALL_FIELDS);
    Assert.assertTrue(signature.toString().contains(ID.toString()));
  }

  @Test
  public void testEquality()
  {
    final GetRequest<?> equalRequest1 = Mockito.mock(GetRequest.class);
    Mockito.when(equalRequest1.getBaseUriTemplate()).thenReturn(BASE_URI_TEMPLATE);
    Mockito.when(equalRequest1.getPathKeys()).thenReturn(PATH_KEYS);
    Mockito.when(equalRequest1.getObjectId()).thenReturn(ID);
    Mockito.when(equalRequest1.getQueryParamsObjects()).thenReturn(QUERY_PARAMS_OBJECTS);

    final GetRequest<?> equalRequest2 = Mockito.mock(GetRequest.class);
    Mockito.when(equalRequest2.getBaseUriTemplate()).thenReturn(BASE_URI_TEMPLATE);
    Mockito.when(equalRequest2.getPathKeys()).thenReturn(PATH_KEYS);
    Mockito.when(equalRequest2.getObjectId()).thenReturn(ID);
    Mockito.when(equalRequest2.getQueryParamsObjects()).thenReturn(QUERY_PARAMS_OBJECTS);

    final GetRequest<?> idDifferRequest = Mockito.mock(GetRequest.class);
    Mockito.when(idDifferRequest.getBaseUriTemplate()).thenReturn(BASE_URI_TEMPLATE);
    Mockito.when(idDifferRequest.getPathKeys()).thenReturn(PATH_KEYS);
    Mockito.when(idDifferRequest.getObjectId()).thenReturn(null);
    Mockito.when(idDifferRequest.getQueryParamsObjects()).thenReturn(QUERY_PARAMS_OBJECTS);

    final RestliRequestUriSignature equalSignature1 = new RestliRequestUriSignature(equalRequest1, RestliRequestUriSignature.ALL_FIELDS);
    final RestliRequestUriSignature equalSignature2 = new RestliRequestUriSignature(equalRequest2, RestliRequestUriSignature.ALL_FIELDS);
    Assert.assertEquals(equalSignature1.hashCode(), equalSignature2.hashCode());
    Assert.assertEquals(equalSignature1, equalSignature2);

    final Set<SignatureField> nonIDFields = new HashSet<SignatureField>(Arrays.asList(SignatureField.BASE_URI_TEMPLATE,
                                                                                      SignatureField.PATH_KEYS,
                                                                                      SignatureField.QUERY_PARAMS));
    final RestliRequestUriSignature equalSignature3 = new RestliRequestUriSignature(equalRequest1, nonIDFields);
    final RestliRequestUriSignature equalSignature4 = new RestliRequestUriSignature(idDifferRequest, RestliRequestUriSignature.ALL_FIELDS);
    Assert.assertEquals(equalSignature3.hashCode(), equalSignature4.hashCode());
    Assert.assertEquals(equalSignature3, equalSignature4);
  }

  @Test
  public void testNotEqualRequestWithEqualMask()
  {
    final GetRequest<?> fullRequest = Mockito.mock(GetRequest.class);
    Mockito.when(fullRequest.getBaseUriTemplate()).thenReturn(BASE_URI_TEMPLATE);
    Mockito.when(fullRequest.getPathKeys()).thenReturn(PATH_KEYS);
    Mockito.when(fullRequest.getObjectId()).thenReturn(ID);
    Mockito.when(fullRequest.getQueryParamsObjects()).thenReturn(QUERY_PARAMS_OBJECTS);

    final GetRequest<?> nullBaseURITemplateRequest = Mockito.mock(GetRequest.class);
    Mockito.when(nullBaseURITemplateRequest.getBaseUriTemplate()).thenReturn(null);
    Mockito.when(nullBaseURITemplateRequest.getPathKeys()).thenReturn(PATH_KEYS);
    Mockito.when(nullBaseURITemplateRequest.getObjectId()).thenReturn(ID);
    Mockito.when(nullBaseURITemplateRequest.getQueryParamsObjects()).thenReturn(QUERY_PARAMS_OBJECTS);

    final GetRequest<?> nullPathKeysRequest = Mockito.mock(GetRequest.class);
    Mockito.when(nullPathKeysRequest.getBaseUriTemplate()).thenReturn(BASE_URI_TEMPLATE);
    Mockito.when(nullPathKeysRequest.getPathKeys()).thenReturn(null);
    Mockito.when(nullPathKeysRequest.getObjectId()).thenReturn(ID);
    Mockito.when(nullPathKeysRequest.getQueryParamsObjects()).thenReturn(QUERY_PARAMS_OBJECTS);

    final GetRequest<?> nullIDRequest = Mockito.mock(GetRequest.class);
    Mockito.when(nullIDRequest.getBaseUriTemplate()).thenReturn(BASE_URI_TEMPLATE);
    Mockito.when(nullIDRequest.getPathKeys()).thenReturn(PATH_KEYS);
    Mockito.when(nullIDRequest.getObjectId()).thenReturn(null);
    Mockito.when(nullIDRequest.getQueryParamsObjects()).thenReturn(QUERY_PARAMS_OBJECTS);

    final GetRequest<?> nullQueryParamsRequest = Mockito.mock(GetRequest.class);
    Mockito.when(nullQueryParamsRequest.getBaseUriTemplate()).thenReturn(BASE_URI_TEMPLATE);
    Mockito.when(nullQueryParamsRequest.getPathKeys()).thenReturn(PATH_KEYS);
    Mockito.when(nullQueryParamsRequest.getObjectId()).thenReturn(ID);
    Mockito.when(nullQueryParamsRequest.getQueryParamsObjects()).thenReturn(null);

    final List<GetRequest<?>> requestsWithout =
      Arrays.<GetRequest<?>>asList(nullBaseURITemplateRequest, nullPathKeysRequest, nullIDRequest, nullQueryParamsRequest);

    Assert.assertEquals(requestsWithout.size(), SignatureField.values().length);

    for (GetRequest<?> r : requestsWithout)
    {
      Assert.assertNotEquals(fullRequest, r);

      final RestliRequestUriSignature signature1 = new RestliRequestUriSignature(fullRequest, RestliRequestUriSignature.ALL_FIELDS);
      final RestliRequestUriSignature signature2 = new RestliRequestUriSignature(r, RestliRequestUriSignature.ALL_FIELDS);
      Assert.assertNotEquals(signature1, signature2);
    }
  }

  @Test
  public void testEqualRequestWithNotEqualMask()
  {
    final GetRequest<?> getRequest = Mockito.mock(GetRequest.class);
    Mockito.when(getRequest.getBaseUriTemplate()).thenReturn(BASE_URI_TEMPLATE);
    Mockito.when(getRequest.getPathKeys()).thenReturn(PATH_KEYS);
    Mockito.when(getRequest.getObjectId()).thenReturn(ID);
    Mockito.when(getRequest.getQueryParamsObjects()).thenReturn(QUERY_PARAMS_OBJECTS);

    final List<Set<SignatureField>> maskFields = new ArrayList<Set<SignatureField>>();
    for (SignatureField f : SignatureField.values())
    {
      final Set<SignatureField> maskFieldsWithout = new HashSet<SignatureField>(RestliRequestUriSignature.ALL_FIELDS);
      maskFieldsWithout.remove(f);
      maskFields.add(maskFieldsWithout);
    }

    Assert.assertEquals(maskFields.size(), SignatureField.values().length);

    for (Set<SignatureField> f : maskFields)
    {
      Assert.assertNotEquals(RestliRequestUriSignature.ALL_FIELDS, f);

      final RestliRequestUriSignature signature1 = new RestliRequestUriSignature(getRequest, RestliRequestUriSignature.ALL_FIELDS);
      final RestliRequestUriSignature signature2 = new RestliRequestUriSignature(getRequest, f);
      Assert.assertNotEquals(signature1, signature2);
    }
  }

  @Test
  public void testDump()
  {
    final Request<?> request = Mockito.mock(Request.class);
    Mockito.when(request.getBaseUriTemplate()).thenReturn(BASE_URI_TEMPLATE);
    Mockito.when(request.getPathKeys()).thenReturn(PATH_KEYS);
    Mockito.when(request.getQueryParamsObjects()).thenReturn(QUERY_PARAMS_OBJECTS);

    final RestliRequestUriSignature signature = new RestliRequestUriSignature(request, RestliRequestUriSignature.ALL_FIELDS);
    final String dump = signature.dump();
    Assert.assertNotNull(dump);
  }

  private static boolean isMapContainedInString(String str, Map<String, Object> map)
  {
    for (Map.Entry<String, Object> e : map.entrySet())
    {
      if (!str.contains(e.getKey() + "=" + e.getValue()))
      {
        return false;
      }
    }

    return true;
  }
}
