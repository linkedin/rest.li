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

package com.linkedin.restli.server.test;


import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.transform.filter.FilterConstants;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.TestConstants;
import com.linkedin.restli.internal.server.PathKeysImpl;
import com.linkedin.restli.internal.server.ResourceContextImpl;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiResponseAttachments;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author Josh Walker
 * @version $Revision: $
 */
public class TestResourceContext
{
  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "projectionMask")
  public Object[][] projectionMask()
  {
    return new Object[][]
      {
        { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "groups/?count=10&emailDomain=foo.com&fields=locale,state&metadataFields=city,region" +
              "&pagingFields=start,links&q=emailDomain&start=0"},
        { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "groups/?count=10&emailDomain=foo.com&fields=locale,state&metadataFields=city,region" +
              "&pagingFields=start,links&q=emailDomain&start=0"}
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "projectionMask")
  public void testResourceContextGetProjectionMask(ProtocolVersion version, String stringUri) throws Exception
  {
    URI uri = URI.create(stringUri);
    Map<String, String> headers = new HashMap<>(1);
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, version.toString());

    ResourceContext context = new ResourceContextImpl(new PathKeysImpl(),
                                                      new MockRequest(uri, headers),
                                                      new RequestContext());

    final MaskTree entityMask = context.getProjectionMask();
    final DataMap expectedEntityMaskMap = new DataMap();
    expectedEntityMaskMap.put("locale", 1);
    expectedEntityMaskMap.put("state", 1);
    Assert.assertEquals(entityMask.getDataMap(), expectedEntityMaskMap, "The generated DataMap for the MaskTree should be correct");

    final MaskTree metadataMask = context.getMetadataProjectionMask();
    final DataMap expectedMetadataMaskMap = new DataMap();
    expectedMetadataMaskMap.put("region", 1);
    expectedMetadataMaskMap.put("city", 1);
    Assert.assertEquals(metadataMask.getDataMap(), expectedMetadataMaskMap, "The generated DataMap for the MaskTree should be correct");

    final MaskTree pagingMask = context.getPagingProjectionMask();
    final DataMap expectedPagingMaskMap = new DataMap();
    expectedPagingMaskMap.put("start", 1);
    expectedPagingMaskMap.put("links", 1);
    Assert.assertEquals(pagingMask.getDataMap(), expectedPagingMaskMap, "The generated DataMap for the MaskTree should be correct");
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "queryParamsProjectionMaskWithSyntax")
  public Object[][] queryParamsProjectionMaskWithSyntax()
  {
    return new Object[][]
      {
        { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "groups/?count=10&emailDomain=foo.com&fields=locale,state,location:(longitude,latitude)" +
              "&metadataFields=city,region,profile:(height,weight)" +
              "&q=emailDomain&start=0"},
        { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "groups/?count=10&emailDomain=foo.com&fields=locale,state,location:(longitude,latitude)" +
              "&metadataFields=city,region,profile:(height,weight)" +
              "&q=emailDomain&start=0"}
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "queryParamsProjectionMaskWithSyntax")
  public void testResourceContextWithQueryParamsGetProjectionMaskWithMaskSyntax(ProtocolVersion version, String stringUri) throws Exception
  {
    URI uri = URI.create(stringUri);
    Map<String, String> headers = new HashMap<>(1);
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, version.toString());

    ResourceContext context = new ResourceContextImpl(new PathKeysImpl(),
                                                      new MockRequest(uri, headers),
                                                      new RequestContext());

    //Assert.assertEquals(mask.toString(), "{location={longitude=1, latitude=1}, locale=1, state=1}");
    final MaskTree entityMask = context.getProjectionMask();
    final DataMap expectedEntityMap = new DataMap();
    expectedEntityMap.put("locale", 1);
    expectedEntityMap.put("state", 1);
    final DataMap locationMap = new DataMap();
    locationMap.put("longitude", 1);
    locationMap.put("latitude", 1);
    expectedEntityMap.put("location", locationMap);
    Assert.assertEquals(entityMask.getDataMap(), expectedEntityMap, "The generated DataMap for the MaskTree should be correct");

    //"{region=1, profile={weight=1, height=1}, city=1}"
    final MaskTree metadataMask = context.getMetadataProjectionMask();
    final DataMap expectedMetadataMap = new DataMap();
    expectedMetadataMap.put("city", 1);
    expectedMetadataMap.put("region", 1);
    final DataMap profileMap = new DataMap();
    profileMap.put("weight", 1);
    profileMap.put("height", 1);
    expectedMetadataMap.put("profile", profileMap);
    Assert.assertEquals(metadataMask.getDataMap(), expectedMetadataMap, "The generated DataMap for the MaskTree should be correct");

    //Note the lack of a test with paging here. This is because paging (CollectionMetadata) has a LinkArray which
    //requires a wildcard path spec in the URI. That behavior is inconsistent with the other projections in this test,
    //therefore it will be included in the subsequent test.
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "projectionMaskWithSyntax")
  public Object[][] projectionMaskWithSyntax()
  {
    return new Object[][]
      {
        { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "groups/?fields=a:($*),b:($*:(c)),d:($*:(e,f))" +
              "&metadataFields=foo:($*:(a,b,c)),bar:($*),baz:($*:(a))" +
              "&pagingFields=count,total,links:($*:(rel))"},
        { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "groups/?fields=a:($*),b:($*:(c)),d:($*:(e,f))" +
              "&metadataFields=foo:($*:(a,b,c)),bar:($*),baz:($*:(a))" +
              "&pagingFields=count,total,links:($*:(rel))"}
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "projectionMaskWithSyntax")
  public void testResourceContextGetProjectionMaskWithSyntax(ProtocolVersion version, String stringUri) throws Exception
  {
    URI uri = URI.create(stringUri);
    Map<String, String> headers = new HashMap<>(1);
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, version.toString());

    ResourceContext context = new ResourceContextImpl(new PathKeysImpl(),
                                                      new MockRequest(uri, headers),
                                                      new RequestContext());

    final MaskTree entityMask = context.getProjectionMask();
    //"{d={$*={f=1, e=1}}, b={$*={c=1}}, a={$*=1}}"
    final DataMap expectedEntityMap = new DataMap();
    final DataMap aMap = new DataMap();
    aMap.put(FilterConstants.WILDCARD, 1);
    expectedEntityMap.put("a", aMap);
    final DataMap bMap = new DataMap();
    final DataMap bWildcardMap = new DataMap();
    bWildcardMap.put("c", 1);
    bMap.put(FilterConstants.WILDCARD, bWildcardMap);
    expectedEntityMap.put("b", bMap);
    final DataMap dMap = new DataMap();
    final DataMap dWildcardMap = new DataMap();
    dWildcardMap.put("f", 1);
    dWildcardMap.put("e", 1);
    dMap.put(FilterConstants.WILDCARD, dWildcardMap);
    expectedEntityMap.put("d", dMap);
    Assert.assertEquals(entityMask.getDataMap(), expectedEntityMap, "The generated DataMap for the MaskTree should be correct");

    final MaskTree metadataMask = context.getMetadataProjectionMask();
    //"{baz={$*={a=1}}, foo={$*={b=1, c=1, a=1}}, bar={$*=1}}"
    final DataMap expectedMetadataMap = new DataMap();
    final DataMap barMap = new DataMap();
    barMap.put(FilterConstants.WILDCARD, 1);
    expectedMetadataMap.put("bar", barMap);
    final DataMap fooWilcardMap = new DataMap();
    fooWilcardMap.put("b", 1);
    fooWilcardMap.put("a", 1);
    fooWilcardMap.put("c", 1);
    final DataMap fooMap = new DataMap();
    fooMap.put(FilterConstants.WILDCARD, fooWilcardMap);
    expectedMetadataMap.put("foo", fooMap);
    final DataMap bazWildcardMap = new DataMap();
    bazWildcardMap.put("a", 1);
    final DataMap bazMap = new DataMap();
    bazMap.put(FilterConstants.WILDCARD, bazWildcardMap);
    expectedMetadataMap.put("baz", bazMap);
    Assert.assertEquals(metadataMask.getDataMap(), expectedMetadataMap, "The generated DataMap for the MaskTree should be correct");

    final MaskTree pagingMask = context.getPagingProjectionMask();
    //"{total=1, count=1, links={$*={rel=1}}}"
    final DataMap expectedPagingMap = new DataMap();
    expectedPagingMap.put("total", 1);
    expectedPagingMap.put("count", 1);
    final DataMap linksWildcardMap = new DataMap();
    linksWildcardMap.put("rel", 1);
    final DataMap linksMap = new DataMap();
    linksMap.put(FilterConstants.WILDCARD, linksWildcardMap);
    expectedPagingMap.put("links", linksMap);
    Assert.assertEquals(pagingMask.getDataMap(), expectedPagingMap, "The generated DataMap for the MaskTree should be correct");
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "uriDecoding")
  public Object[][] uriDecoding()
  {
    return new Object[][]
      {
        { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "groups/?fields=foo,bar,baz&metadataFields=city,region&pagingFields=start,links" +
              "&testParam.a%5B0%5D=b&testParam.a%5B1%5D=c&testParam.a%5B2%5D=d&q=test"},
        { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "groups/?fields=foo,bar,baz&metadataFields=city,region&pagingFields=start,links" +
              "&testParam=(a:List(b,c,d))&q=test"}
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "uriDecoding")
  public void testResourceContextURIDecoding(ProtocolVersion version, String stringUri) throws Exception
  {
    URI uri = URI.create(stringUri);
    Map<String, String> headers = new HashMap<>(1);
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, version.toString());

    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(),
                                                            new MockRequest(uri, headers),
                                                            new RequestContext());

    final MaskTree entityMask = context.getProjectionMask();
    //"{baz=1, foo=1, bar=1}"
    final DataMap expectedEntityMap = new DataMap();
    expectedEntityMap.put("baz", 1);
    expectedEntityMap.put("foo", 1);
    expectedEntityMap.put("bar", 1);
    Assert.assertEquals(entityMask.getDataMap(), expectedEntityMap, "The generated DataMap for the MaskTree should be correct");

    final MaskTree metadataMask = context.getMetadataProjectionMask();
    //"{region=1, city=1}"
    final DataMap expectedmetadataMap = new DataMap();
    expectedmetadataMap.put("region", 1);
    expectedmetadataMap.put("city", 1);
    Assert.assertEquals(metadataMask.getDataMap(), expectedmetadataMap, "The generated DataMap for the MaskTree should be correct");

    final MaskTree pagingMask = context.getPagingProjectionMask();
    //"{start=1, links=1}"
    final DataMap expectedPagingMap = new DataMap();
    expectedPagingMap.put("start", 1);
    expectedPagingMap.put("links", 1);
    Assert.assertEquals(pagingMask.getDataMap(), expectedPagingMap, "The generated DataMap for the MaskTree should be correct");

    DataMap parameters = context.getParameters();
    DataMap expectedParameters = new DataMap();
    expectedParameters.put("fields", "foo,bar,baz");
    expectedParameters.put("metadataFields", "city,region");
    expectedParameters.put("pagingFields", "start,links");
    expectedParameters.put("q", "test");
    DataMap testParam = new DataMap();
    DataList aValue = new DataList();
    aValue.add("b");
    aValue.add("c");
    aValue.add("d");
    testParam.put("a", aValue);
    expectedParameters.put("testParam", testParam);
    Assert.assertEquals(parameters, expectedParameters);
  }

  @Test
  public void testStreamingDataResourceContext() throws Exception
  {
    RestRequest request = new RestRequestBuilder(URI.create("foobar")).addHeaderValue(RestConstants.HEADER_ACCEPT, RestConstants.HEADER_VALUE_MULTIPART_RELATED).build();
    ServerResourceContext fullyStreamingResourceContext = new ResourceContextImpl(new PathKeysImpl(),
                                                                   request,
                                                                   new RequestContext());
    fullyStreamingResourceContext.setRequestAttachmentReader(new RestLiAttachmentReader(null));
    Assert.assertTrue(fullyStreamingResourceContext.responseAttachmentsSupported());
    Assert.assertNotNull(fullyStreamingResourceContext.getRequestAttachmentReader());
    //Now set and get response attachments
    final RestLiResponseAttachments restLiResponseAttachments = new RestLiResponseAttachments.Builder().build();
    fullyStreamingResourceContext.setResponseAttachments(restLiResponseAttachments);
    Assert.assertEquals(fullyStreamingResourceContext.getResponseAttachments(), restLiResponseAttachments);

    ServerResourceContext responseAllowedNoRequestAttachmentsPresent = new ResourceContextImpl(new PathKeysImpl(),
                                                                                        request,
                                                                                        new RequestContext());
    Assert.assertTrue(responseAllowedNoRequestAttachmentsPresent.responseAttachmentsSupported());
    Assert.assertNull(responseAllowedNoRequestAttachmentsPresent.getRequestAttachmentReader());
    //Now set and get response attachments
    responseAllowedNoRequestAttachmentsPresent.setResponseAttachments(restLiResponseAttachments);
    Assert.assertEquals(responseAllowedNoRequestAttachmentsPresent.getResponseAttachments(), restLiResponseAttachments);

    ServerResourceContext noResponseAllowedRequestAttachmentsPresent = new ResourceContextImpl(new PathKeysImpl(),
                                                                                               new MockRequest(URI.create("foobar"), Collections.emptyMap()),
                                                                                               new RequestContext());
    noResponseAllowedRequestAttachmentsPresent.setRequestAttachmentReader(new RestLiAttachmentReader(null));
    Assert.assertFalse(noResponseAllowedRequestAttachmentsPresent.responseAttachmentsSupported());
    Assert.assertNotNull(noResponseAllowedRequestAttachmentsPresent.getRequestAttachmentReader());
    //Now try to set and make sure we fail
    try
    {
      noResponseAllowedRequestAttachmentsPresent.setResponseAttachments(restLiResponseAttachments);
      Assert.fail();
    }
    catch (IllegalStateException illegalStateException)
    {
      //pass
    }
  }

  public static class MockRequest implements RestRequest
  {
    private final URI _uri;
    private final Map<String, String> _headers;
    private final List<String> _cookies;

    public MockRequest(URI uri)
    {
      _uri = uri;
      _headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      _cookies = new ArrayList<>();
    }

    public MockRequest(URI uri, Map<String, String> headers)
    {
      _uri = uri;
      _headers = headers;
      _cookies = new ArrayList<>();
    }

    @Override
    public String getMethod()
    {
      return null;
    }

    @Override
    public RestRequestBuilder builder()
    {
      return null;
    }

    @Override
    public ByteString getEntity()
    {
      return null;
    }

    @Override
    public Map<String, String> getHeaders()
    {
      return _headers;
    }

    @Override
    public List<String> getCookies()
    {
      return _cookies;
    }

    @Override
    public List<String> getHeaderValues(String name)
    {
      return null;
    }

    @Override
    public String getHeader(String name)
    {
      return _headers.get(name);
    }

    @Override
    public URI getURI()
    {
      return _uri;
    }
  }
}
