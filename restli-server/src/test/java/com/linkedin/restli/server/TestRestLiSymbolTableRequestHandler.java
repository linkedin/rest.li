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

package com.linkedin.restli.server;

import com.google.common.collect.ImmutableList;
import com.linkedin.common.callback.Callback;
import com.linkedin.data.codec.symbol.InMemorySymbolTable;
import com.linkedin.data.codec.symbol.SymbolTable;
import com.linkedin.data.codec.symbol.SymbolTableProvider;
import com.linkedin.data.codec.symbol.SymbolTableProviderHolder;
import com.linkedin.data.codec.symbol.SymbolTableSerializer;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.common.ContentType;
import com.linkedin.restli.common.HttpMethod;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.server.symbol.RestLiSymbolTableRequestHandler;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.junit.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;


public class TestRestLiSymbolTableRequestHandler
{
  private RestLiSymbolTableRequestHandler _requestHandler;
  private SymbolTableProvider _symbolTableProvider;

  @BeforeMethod
  public void setup()
  {
    _requestHandler = new RestLiSymbolTableRequestHandler(10);
    _symbolTableProvider = mock(SymbolTableProvider.class);
    SymbolTableProviderHolder.INSTANCE.setSymbolTableProvider(_symbolTableProvider);
  }

  @AfterMethod
  public void tearDown()
  {
    SymbolTableProviderHolder.INSTANCE.setSymbolTableProvider(new SymbolTableProvider() {});
  }

  @Test
  public void testShouldNotHandleEmptyPath()
  {
    RestRequest request = new RestRequestBuilder(URI.create("d2://service")).build();
    Assert.assertFalse(_requestHandler.shouldHandle(request));
  }

  @Test
  public void testShouldNotHandleNonSymbolTablePath()
  {
    RestRequest request = new RestRequestBuilder(URI.create("d2://service/someResource")).build();
    Assert.assertFalse(_requestHandler.shouldHandle(request));
  }

  @Test
  public void testShouldHandleSymbolTablePath()
  {
    RestRequest request = new RestRequestBuilder(URI.create("d2://service/symbolTable")).build();
    Assert.assertTrue(_requestHandler.shouldHandle(request));
  }

  @Test
  public void testNonGetRequest405()
  {
    RestRequest request = new RestRequestBuilder(URI.create("d2://service/symbolTable"))
        .setMethod(HttpMethod.POST.name()).build();

    CompletableFuture<RestResponse> future = new CompletableFuture<>();
    _requestHandler.handleRequest(request, mock(RequestContext.class), new Callback<RestResponse>() {
      @Override
      public void onError(Throwable e) {
        future.completeExceptionally(e);
        Assert.assertEquals(((RestException) e).getResponse().getStatus(), HttpStatus.S_405_METHOD_NOT_ALLOWED.getCode());
      }

      @Override
      public void onSuccess(RestResponse result) {
        future.complete(result);
      }
    });

    Assert.assertTrue(future.isCompletedExceptionally());
  }


  @Test
  public void testInvalidAcceptTypeRequest406()
  {
    RestRequest request = new RestRequestBuilder(URI.create("d2://service/symbolTable"))
        .setHeader(RestConstants.HEADER_ACCEPT, "application/randomType").build();

    CompletableFuture<RestResponse> future = new CompletableFuture<>();
    _requestHandler.handleRequest(request, mock(RequestContext.class), new Callback<RestResponse>() {
      @Override
      public void onError(Throwable e) {
        future.completeExceptionally(e);
        Assert.assertEquals(((RestException) e).getResponse().getStatus(), HttpStatus.S_406_NOT_ACCEPTABLE.getCode());
      }

      @Override
      public void onSuccess(RestResponse result) {
        future.complete(result);
      }
    });

    Assert.assertTrue(future.isCompletedExceptionally());
  }

  @Test
  public void testSelfSymbolTableNotFound404()
  {
    RestRequest request = new RestRequestBuilder(URI.create("d2://service/symbolTable")).build();

    CompletableFuture<RestResponse> future = new CompletableFuture<>();
    _requestHandler.handleRequest(request, mock(RequestContext.class), new Callback<RestResponse>() {
      @Override
      public void onError(Throwable e) {
        future.completeExceptionally(e);
        Assert.assertEquals(((RestException) e).getResponse().getStatus(), HttpStatus.S_404_NOT_FOUND.getCode());
      }

      @Override
      public void onSuccess(RestResponse result) {
        future.complete(result);
      }
    });

    Assert.assertTrue(future.isCompletedExceptionally());
  }

  @Test
  public void testOtherSymbolTableNotFound404()
  {
    RestRequest request = new RestRequestBuilder(URI.create("d2://service/symbolTable/SomeName")).build();

    CompletableFuture<RestResponse> future = new CompletableFuture<>();
    _requestHandler.handleRequest(request, mock(RequestContext.class), new Callback<RestResponse>() {
      @Override
      public void onError(Throwable e) {
        future.completeExceptionally(e);
        Assert.assertEquals(((RestException) e).getResponse().getStatus(), HttpStatus.S_404_NOT_FOUND.getCode());
      }

      @Override
      public void onSuccess(RestResponse result) {
        future.complete(result);
      }
    });

    Assert.assertTrue(future.isCompletedExceptionally());
  }

  @Test
  public void testReturnSelfSymbolTable() throws Exception
  {
    SymbolTable symbolTable =
        new InMemorySymbolTable("TestName", ImmutableList.of("Haha", "Hehe", "Hoho"));
    URI uri = URI.create("d2://service/symbolTable");
    RestRequest request = new RestRequestBuilder(uri).build();
    when(_symbolTableProvider.getResponseSymbolTable(eq(uri), eq(Collections.emptyMap()))).thenReturn(symbolTable);

    CompletableFuture<RestResponse> future = new CompletableFuture<>();
    _requestHandler.handleRequest(request, mock(RequestContext.class), new Callback<RestResponse>() {
      @Override
      public void onError(Throwable e) {
        future.completeExceptionally(e);
      }

      @Override
      public void onSuccess(RestResponse result) {
        future.complete(result);
      }
    });

    Assert.assertFalse(future.isCompletedExceptionally());
    Assert.assertTrue(future.isDone());

    RestResponse response = future.get();
    Assert.assertEquals(response.getStatus(), HttpStatus.S_200_OK.getCode());
    Assert.assertEquals(response.getHeader(RestConstants.HEADER_CONTENT_TYPE), ContentType.PROTOBUF.getHeaderKey());
    Assert.assertEquals(symbolTable, SymbolTableSerializer.fromByteString(response.getEntity(), ContentType.PROTOBUF.getCodec()));
  }

  @Test
  public void testReturnOtherSymbolTable() throws Exception
  {
    SymbolTable symbolTable =
        new InMemorySymbolTable("TestName", ImmutableList.of("Haha", "Hehe", "Hoho"));
    URI uri = URI.create("d2://service/symbolTable/OtherTable");
    RestRequest request = new RestRequestBuilder(uri).build();
    when(_symbolTableProvider.getSymbolTable(eq("OtherTable"))).thenReturn(symbolTable);

    CompletableFuture<RestResponse> future = new CompletableFuture<>();
    _requestHandler.handleRequest(request, mock(RequestContext.class), new Callback<RestResponse>() {
      @Override
      public void onError(Throwable e) {
        future.completeExceptionally(e);
      }

      @Override
      public void onSuccess(RestResponse result) {
        future.complete(result);
      }
    });

    Assert.assertFalse(future.isCompletedExceptionally());
    Assert.assertTrue(future.isDone());

    RestResponse response = future.get();
    Assert.assertEquals(response.getStatus(), HttpStatus.S_200_OK.getCode());
    Assert.assertEquals(response.getHeader(RestConstants.HEADER_CONTENT_TYPE), ContentType.PROTOBUF.getHeaderKey());
    Assert.assertEquals(symbolTable, SymbolTableSerializer.fromByteString(response.getEntity(), ContentType.PROTOBUF.getCodec()));
  }

  @Test
  public void testReturnOtherSymbolTableNonDefaultAcceptType() throws Exception
  {
    SymbolTable symbolTable =
        new InMemorySymbolTable("TestName", ImmutableList.of("Haha", "Hehe", "Hoho"));
    URI uri = URI.create("d2://service/symbolTable/OtherTable");
    RestRequest request =
        new RestRequestBuilder(uri).setHeader(RestConstants.HEADER_ACCEPT, ContentType.JSON.getHeaderKey()).build();
    when(_symbolTableProvider.getSymbolTable(eq("OtherTable"))).thenReturn(symbolTable);

    CompletableFuture<RestResponse> future = new CompletableFuture<>();
    _requestHandler.handleRequest(request, mock(RequestContext.class), new Callback<RestResponse>() {
      @Override
      public void onError(Throwable e) {
        future.completeExceptionally(e);
      }

      @Override
      public void onSuccess(RestResponse result) {
        future.complete(result);
      }
    });

    Assert.assertFalse(future.isCompletedExceptionally());
    Assert.assertTrue(future.isDone());

    RestResponse response = future.get();
    Assert.assertEquals(response.getStatus(), HttpStatus.S_200_OK.getCode());
    Assert.assertEquals(response.getHeader(RestConstants.HEADER_CONTENT_TYPE), ContentType.JSON.getHeaderKey());
    Assert.assertEquals(symbolTable, SymbolTableSerializer.fromByteString(response.getEntity(), ContentType.JSON.getCodec()));
  }
}
