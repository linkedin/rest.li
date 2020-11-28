/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.restli.tools.symbol;

import com.linkedin.data.codec.symbol.InMemorySymbolTable;
import com.linkedin.data.codec.symbol.SymbolTable;
import com.linkedin.data.codec.symbol.SymbolTableSerializer;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.Name;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.restli.common.ContentType;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.server.ResourceDefinition;
import com.linkedin.restli.server.symbol.RestLiSymbolTableRequestHandler;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;


public class TestRestLiSymbolTableProvider
{
  private Client _client;
  private RestLiSymbolTableProvider _provider;
  private ResourceDefinition _resourceDefinition;

  @SuppressWarnings("unchecked")
  @BeforeMethod
  public void setup()
  {
    _client = mock(Client.class);
    _provider = new RestLiSymbolTableProvider(_client, "d2://", 10, "Test", "https://Host:100/service");

    _resourceDefinition = mock(ResourceDefinition.class);
    doAnswer(invocation -> {
      Set<DataSchema> schemas = (Set<DataSchema>) invocation.getArguments()[0];
      EnumDataSchema schema = new EnumDataSchema(new Name("TestEnum"));
      schema.setSymbols(Collections.unmodifiableList(Arrays.asList("Symbol1", "Symbol2")), new StringBuilder());
      schemas.add(schema);
      return null;
    }).when(_resourceDefinition).collectReferencedDataSchemas(any(Set.class));
  }

  @Test
  public void testGetResponseSymbolTableBeforeInit()
  {
    Assert.assertNull(_provider.getResponseSymbolTable(URI.create("https://Host:100/service/symbolTable"), Collections.emptyMap()));
  }

  @Test
  public void testGetResponseSymbolTableAfterInit()
  {
    _provider.onInitialized(Collections.unmodifiableMap(Collections.singletonMap("TestResourceName", _resourceDefinition)));

    SymbolTable symbolTable = _provider.getResponseSymbolTable(URI.create("https://Host:100/service/symbolTable"), Collections.emptyMap());
    Assert.assertNotNull(symbolTable);
    Assert.assertEquals(39, symbolTable.size());
    Assert.assertEquals("https://Host:100/service|Test--332004310", symbolTable.getName());
  }

  @Test
  public void testGetValidLocalSymbolTable()
  {
    _provider.onInitialized(Collections.unmodifiableMap(Collections.singletonMap("TestResourceName", _resourceDefinition)));
    SymbolTable symbolTable = _provider.getSymbolTable("https://Host:100/service|Test--332004310");
    Assert.assertNotNull(symbolTable);
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testGetMissingLocalSymbolTable()
  {
    _provider.onInitialized(Collections.unmodifiableMap(Collections.singletonMap("TestResourceName", _resourceDefinition)));
    _provider.getSymbolTable("https://Host:100/service|Blah-100");
  }

  @Test
  public void testGetRemoteSymbolTableFetchSuccess() throws IOException
  {
    RestResponseBuilder builder = new RestResponseBuilder();
    builder.setStatus(200);
    SymbolTable symbolTable = new InMemorySymbolTable("https://OtherHost:100/service|Test--332004310",
        Collections.unmodifiableList(Arrays.asList("Haha", "Hehe")));
    builder.setEntity(SymbolTableSerializer.toByteString(ContentType.PROTOBUF2.getCodec(), symbolTable));
    builder.setHeader(RestConstants.HEADER_CONTENT_TYPE, ContentType.PROTOBUF2.getHeaderKey());
    when(_client.restRequest(eq(new RestRequestBuilder(
            URI.create("https://OtherHost:100/service/symbolTable/Test--332004310"))
            .setHeaders(Collections.singletonMap(RestConstants.HEADER_FETCH_SYMBOL_TABLE, Boolean.TRUE.toString()))
            .build()))).thenReturn(CompletableFuture.completedFuture(builder.build()));

    SymbolTable remoteSymbolTable = _provider.getSymbolTable("https://OtherHost:100/service|Test--332004310");
    Assert.assertNotNull(remoteSymbolTable);
    Assert.assertEquals("https://Host:100/service|Test--332004310", remoteSymbolTable.getName());
    Assert.assertEquals(2, remoteSymbolTable.size());

    // Subsequent fetch should not trigger network fetch and get the table from the cache.
    when(_client.restRequest(any(RestRequest.class))).thenThrow(new IllegalStateException());
    SymbolTable cachedSymbolTable = _provider.getSymbolTable("https://OtherHost:100/service|Test--332004310");
    Assert.assertSame(remoteSymbolTable, cachedSymbolTable);
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testGetRemoteSymbolTableFetchError()
  {
    RestResponseBuilder builder = new RestResponseBuilder();
    builder.setStatus(404);
    when(_client.restRequest(eq(new RestRequestBuilder(URI.create("https://OtherHost:100/service/symbolTable/Test--332004310")).build())))
        .thenReturn(CompletableFuture.completedFuture(builder.build()));

    _provider.getSymbolTable("https://OtherHost:100/service|Test--332004310");
  }

  @Test
  public void testGetRemoteRequestSymbolTableFetchSuccess() throws IOException
  {
    RestResponseBuilder builder = new RestResponseBuilder();
    builder.setStatus(200);
    SymbolTable symbolTable = new InMemorySymbolTable("https://OtherHost:100/service|Test--332004310",
        Collections.unmodifiableList(Arrays.asList("Haha", "Hehe")));
    builder.setEntity(SymbolTableSerializer.toByteString(ContentType.PROTOBUF2.getCodec(), symbolTable));
    builder.setHeader(RestConstants.HEADER_CONTENT_TYPE, ContentType.PROTOBUF2.getHeaderKey());
    when(_client.restRequest(eq(new RestRequestBuilder(URI.create("d2://someservice/symbolTable"))
        .setHeaders(Collections.singletonMap(RestConstants.HEADER_FETCH_SYMBOL_TABLE, Boolean.TRUE.toString())).build())))
        .thenReturn(CompletableFuture.completedFuture(builder.build()));

    SymbolTable remoteSymbolTable = _provider.getRequestSymbolTable(URI.create("d2://someservice/path"));
    Assert.assertNotNull(remoteSymbolTable);
    Assert.assertEquals("https://Host:100/service|Test--332004310", remoteSymbolTable.getName());
    Assert.assertEquals(2, remoteSymbolTable.size());

    // Subsequent fetch should not trigger network fetch and get the table from the cache, regardless of
    // whether the table is fetched by request URI or symbol table name.
    when(_client.restRequest(any(RestRequest.class))).thenThrow(new IllegalStateException());
    SymbolTable cachedSymbolTable = _provider.getRequestSymbolTable(URI.create("d2://someservice/path"));
    Assert.assertSame(remoteSymbolTable, cachedSymbolTable);
    cachedSymbolTable = _provider.getSymbolTable("https://OtherHost:100/service|Test--332004310");
    Assert.assertSame(remoteSymbolTable, cachedSymbolTable);
  }

  @Test
  public void testGetRemoteRequestSymbolTableDifferentUriPrefix()
  {
    Assert.assertNull(_provider.getRequestSymbolTable(URI.create("http://blah:100/bleh")));
  }

  @Test
  public void testGetRemoteRequestSymbolTableFetch404Error()
  {
    RestResponseBuilder builder = new RestResponseBuilder();
    builder.setStatus(404);
    when(_client.restRequest(eq(new RestRequestBuilder(URI.create("d2://serviceName/symbolTable")).build())))
        .thenReturn(CompletableFuture.completedFuture(builder.build()));

    Assert.assertNull(_provider.getRequestSymbolTable(URI.create("d2://serviceName")));

    // Subsequent fetch should not trigger network fetch and get the table from the cache.
    when(_client.restRequest(any(RestRequest.class))).thenThrow(new IllegalStateException());
    Assert.assertNull(_provider.getRequestSymbolTable(URI.create("d2://serviceName")));
  }

  @Test
  public void testGetRemoteRequestSymbolTableFetchNon404Error()
  {
    AtomicInteger networkCallCount = new AtomicInteger(0);
    RestResponseBuilder builder = new RestResponseBuilder();
    builder.setStatus(500);
    when(_client.restRequest(eq(new RestRequestBuilder(URI.create("d2://serviceName/symbolTable")).build()))).thenAnswer(
        invocation -> {
          networkCallCount.incrementAndGet();
          return CompletableFuture.completedFuture(builder.build());
        });

    // First fetch should trigger a network request.
    Assert.assertNull(_provider.getRequestSymbolTable(URI.create("d2://serviceName")));
    Assert.assertEquals(networkCallCount.get(), 1);

    // Subsequent fetch should also trigger a network request because response should not have been cached.
    Assert.assertNull(_provider.getRequestSymbolTable(URI.create("d2://serviceName")));
    Assert.assertEquals(networkCallCount.get(), 2);
  }
}
