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

package com.linkedin.restli.internal.testutils;


import com.linkedin.data.DataMap;
import com.linkedin.jersey.api.uri.UriComponent;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.uribuilders.RestliUriBuilderUtil;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.QueryParamsDataMap;
import com.linkedin.restli.internal.common.URIParamUtils;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;


/**
 * General purpose test utility used to verify the integrity of URIs constructed by rest.li. Users of this class
 * can pass in a {@link com.linkedin.restli.client.Request} or a {@link java.lang.String}
 * along with the desired {@link com.linkedin.restli.internal.testutils.URIDetails}
 *
 * This utility uses methods similar to what the server uses to break down URIs, and then verifies that they are
 * structurally as expected.
 *
 * Usage of this API is broken up into 5 arguments which are passed to the constructor:
 * 1. The protocol to use when parsing the URI (V1 vs V2)
 *
 * 2. The expected path. This includes all resources and sub resources along with their respective serialized keys. Since
 * {@link com.linkedin.restli.common.ComplexResourceKey}s and {@link com.linkedin.restli.common.CompoundKey}s are
 * serialized in a sorted, stable manner, clients of this utility simply have to pass in the expected path
 * (everything up until '?' in the URI) as a pre-determined {@link java.lang.String}.
 *
 * 3. {@link java.util.Set} of IDs for the various batch operations. Since the ids could be in any order, clients will need to
 * pass a {@link java.util.Set} of expected IDs. Note that an id could be a {@link com.linkedin.restli.common.ComplexResourceKey},
 * a {@link com.linkedin.restli.common.CompoundKey} or just a primitive value. Also note that
 * {@link com.linkedin.restli.common.ComplexResourceKey}s or {@link com.linkedin.restli.common.CompoundKey}s
 * are parsed into a {@link com.linkedin.data.DataMap}. Therefore for these types of keys, clients should pass in a
 * {@link java.util.Set} of {@link com.linkedin.data.DataMap}s.
 *
 * 4. A query parameter {@link java.util.Map}. Note that complex values can be passed in, such as a {@link com.linkedin.data.DataMap}
 * or a {@link com.linkedin.data.DataList}. Note that query parameters passed in must NOT INCLUDE the IDs or the fields
 * as those are handled separately. These will typically include things such as the finder name, custom query parameters, etc...
 *
 * 5. {@link java.util.Set} of fields. Since the fields can be in any order, clients will pass in a {@link java.util.Set}
 * of expected fields for projection.
 *
 * Since the assertion utility here uses code similar to the server side to parse URIs, it is worthwhile to mention
 * {@link com.linkedin.restli.common.CompoundKey}s when used in batch operations:
 * For V1: The server begins by breaking the {@link com.linkedin.restli.common.CompoundKey}s into partially complete DataMaps.
 * Later on, the server converts {@link java.lang.String}s from within this partially constructed {@link com.linkedin.data.DataMap}
 * directly into {@link com.linkedin.restli.common.CompoundKey}s.
 * For V2: The server begins by breaking down the {@link com.linkedin.restli.common.CompoundKey}s into complete
 * {@link com.linkedin.data.DataMap}s. Later on, the server then converts these fully complete {@link com.linkedin.data.DataMap}s
 * into {@link com.linkedin.restli.common.CompoundKey}s.
 *
 * Therefore for batch operations with {@link com.linkedin.restli.common.CompoundKey}s, since there is a mismatch during
 * the intermediary step between the two protocols, the clients of this assertion utility can pass in separate {@link Set}s
 * each time they make an instance of this class (one for V1 and one for V2).
 * For V1, the {@link com.linkedin.restli.common.CompoundKey}s should be serialized into a {@link java.util.Set} of {@link java.lang.String}s.
 * This is acceptable because (as mentioned above) each key is serialized into a stable and sorted order.
 * For V2, the {@link com.linkedin.restli.common.CompoundKey}s can be passed in as a {@link Set} of fully constructed {@link com.linkedin.data.DataMap}s.
 *
 * For an example of this behavior, please see {@link com.linkedin.restli.client.TestClientBuilders#batchGetWithEncoding()}
 *
 * Note that this only applies to batch operations with {@link com.linkedin.restli.common.CompoundKey}s.
 * For non-batch operations, such as a GET, clients can simply pass in the stable serialized form of the {@link com.linkedin.restli.common.CompoundKey}
 * as a {@link String} to the {@link #_path} as mentioned earlier.
 *
 * @author kvidhani
 */
public class URIDetails
{
  private final ProtocolVersion _protocolVersion;
  private final Map<String, ?> _queryParams; //everything other than fields and ids.
  private final Set<?> _ids; //ids could be in any order
  private final Set<String> _fields; //fields could appear in any order
  private final String _path; //includes the key or all possible sub resources and the respective keys

  /**
   * Constructor for URIDetails. Please refer to the class documentation on correct usage.
   *
   * @param protocolVersion
   * @param path
   * @param ids
   * @param queryParams
   * @param fields
   */
  public URIDetails(ProtocolVersion protocolVersion, String path, Set<?> ids, Map<String, ?> queryParams, Set<String> fields)
  {
    _protocolVersion = protocolVersion;
    _queryParams = queryParams;
    _ids = ids;
    _fields = fields;
    _path = path;
  }

  public ProtocolVersion getProtocolVersion()
  {
    return _protocolVersion;
  }

  /**
   * Tests the deprecated API for getting the URI of a request, as well as the new way of constructing the URI using
   * a builder. Requires an URIDetails object with the broken down URI to make sure that out of URIs are still considered
   * valid.
   *
   * @param request
   * @param expectedURIDetails
   */
  @SuppressWarnings({"deprecation"})
  public static void testUriGeneration(Request<?> request, URIDetails expectedURIDetails)
  {
    final ProtocolVersion version = expectedURIDetails.getProtocolVersion();
    final String createdURIString = RestliUriBuilderUtil.createUriBuilder(request, version).build().toString();
    testUriGeneration(createdURIString, expectedURIDetails);
  }

  /**
   * Tests the construction and validity of the provided URI. Requires an URIDetails object with the broken down URI.
   *
   * @param createdURIString
   * @param expectedURIDetails
   */
  @SuppressWarnings({"unchecked"})
  public static void testUriGeneration(String createdURIString, URIDetails expectedURIDetails)
  {
    final ProtocolVersion version = expectedURIDetails.getProtocolVersion();

    //Compare the path. Note that in both V1 and V2 all the keys (complex, compound and simple) are serialized recursively
    //in a sorted order. Hence the path will be the same regardless of the underlying Map implementation (order or unordered)
    final String actualPath = createdURIString.split("\\?")[0];
    Assert.assertEquals("The path should be correct", expectedURIDetails._path, actualPath);

    //Move onto the rest of the URI
    final URI createdURI = URI.create(createdURIString);
    //We will parse the created URI into memory and compare it to what's inside the URI details
    final DataMap actualURIDataMap;

    //Compare the DataMaps created by parsing the URI into memory vs the ones created in the test.
    //Note that the actualURIDataMap that is created is composed of query parameters (including ids) and fields
    try
    {
      if (version.compareTo(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()) <= 0)
      {
        //1.0 - decode and then parse into a data map
        final Map<String, List<String>> queryParameters = UriComponent.decodeQuery(createdURI.getRawQuery(), true);
        actualURIDataMap = QueryParamsDataMap.parseDataMapKeys(queryParameters);
      }
      else
      {
        //2.0 - no need to decode as the parsing decodes for us
        final Map<String, List<String>> queryParameters = UriComponent.decodeQuery(createdURI.getRawQuery(), false);
        actualURIDataMap = URIParamUtils.parseUriParams(queryParameters);
      }

      //This will cover everything except fields and ids
      if(expectedURIDetails._queryParams != null)
      {
        for (final Map.Entry<String, ?> entry : expectedURIDetails._queryParams.entrySet())
        {
          Assert.assertNotNull("We should have a valid (non null) key for query param: " + entry.getKey() + " in our created URI",
              actualURIDataMap.get(entry.getKey()));
          Assert.assertEquals("The value portion of for the query param: " + entry.getKey() + " URI should be correct",
              actualURIDataMap.get(entry.getKey()), entry.getValue());
        }
      }

      //IDs is to be treated a bit differently since client requests put them in a set
      if(expectedURIDetails._ids != null)
      {
        final Set<Object> idSet;
        final List<Object> idList;
        if (actualURIDataMap.get("ids") instanceof List)
        {
          idList =  (List<Object>) actualURIDataMap.get("ids");
        }
        else
        {
          //Just a single object, (i.e String, DataMap, etc...)
          idList = Arrays.asList(actualURIDataMap.get("ids"));
        }
        idSet = new HashSet<Object>(idList);
        Assert.assertEquals("The set of IDs must match", expectedURIDetails._ids, idSet);
      }

      //Fields is to be treated a bit differently since they could be in any order, hence we compare sets
      if(expectedURIDetails._fields != null)
      {
        final String[] fieldsArray = ((String) actualURIDataMap.get("fields")).split(",");
        final Set<String> actualFieldSet = new HashSet<String>(Arrays.asList(fieldsArray));
        Assert.assertEquals("The set of projection fields should be correct", expectedURIDetails._fields, actualFieldSet);
      }

      //Now we want to make sure that we have the desired number of keys in the actualURIDataMap
      int desiredKeyCount = 0;
      desiredKeyCount += expectedURIDetails._queryParams == null ? 0 : expectedURIDetails._queryParams.size();
      desiredKeyCount += expectedURIDetails._fields == null ? 0 : 1; //values stored in a single key
      desiredKeyCount += expectedURIDetails._ids == null ? 0 : 1; //values stored in a single key
      Assert.assertEquals("Incorrect number of keys discovered in URI", desiredKeyCount, actualURIDataMap.size());
    }
    catch (Exception e)
    {
      Assert.fail("Unexpected exception when parsing created URI with exception: " + e);
    }
  }
}