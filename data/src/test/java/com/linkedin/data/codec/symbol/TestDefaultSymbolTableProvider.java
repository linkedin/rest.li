/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.data.codec.symbol;

import com.linkedin.data.ByteString;
import com.linkedin.data.codec.ProtobufDataCodec;
import java.net.HttpURLConnection;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestDefaultSymbolTableProvider
{
  private final String _symbolTableName = "https://someservice:100|tableName";
  private final SymbolTable _symbolTable = new InMemorySymbolTable(_symbolTableName, Collections.singletonList("test"));

  @Test
  public void testRemoteSymbolTableSuccess() throws Exception
  {
    Map<String, String> defaultHeader = new HashMap<>();
    defaultHeader.put("test", "test");
    ByteString serializedTable = SymbolTableSerializer.toByteString(DefaultSymbolTableProvider.CODEC, _symbolTable);

    HttpURLConnection connection = mock(HttpURLConnection.class);
    DefaultSymbolTableProvider provider = spy(new DefaultSymbolTableProvider());
    provider.setDefaultHeaders(defaultHeader);
    doReturn(connection).when(provider).openConnection(eq("https://someservice:100/symbolTable/tableName"));
    when(connection.getResponseCode()).thenReturn(200);
    when(connection.getInputStream()).thenReturn(serializedTable.asInputStream());

    SymbolTable remoteTable = provider.getSymbolTable(_symbolTableName);
    verify(connection).setRequestProperty(eq("Accept"), eq(ProtobufDataCodec.DEFAULT_HEADER));
    verify(connection).setRequestProperty(eq("test"), eq("test"));
    verify(connection).setRequestProperty(eq("X-LI-R2-W-IC-1"), anyString());
    verify(connection).disconnect();

    // Verify table is deserialized correctly.
    Assert.assertEquals(_symbolTable, remoteTable);

    // Mock out the network to throw exceptions on any interactions.
    doThrow(new RuntimeException()).when(provider).openConnection(anyString());

    // Verify that table is in cache by retrieving it again.
    Assert.assertEquals(provider.getSymbolTable("tableName"), remoteTable);
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testRemoteSymbolTableMalformedUrl()
  {
    String symbolTableName = "https\\someservice:100|tableName";
    new DefaultSymbolTableProvider().getSymbolTable(symbolTableName);
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testRemoteSymbolTableErrorResponseCode() throws Exception
  {
    HttpURLConnection connection = mock(HttpURLConnection.class);
    DefaultSymbolTableProvider provider = spy(new DefaultSymbolTableProvider());
    doReturn(connection).when(provider).openConnection(eq("https://someservice:100/symbolTable/tableName"));
    when(connection.getResponseCode()).thenReturn(500);
    provider.getSymbolTable(_symbolTableName);
    verify(connection).disconnect();
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testRemoteSymbolTableParsingFailure() throws Exception
  {
    ByteString serializedTable = ByteString.unsafeWrap("random".getBytes());

    HttpURLConnection connection = mock(HttpURLConnection.class);
    DefaultSymbolTableProvider provider = spy(new DefaultSymbolTableProvider());
    doReturn(connection).when(provider).openConnection(eq("https://someservice:100/symbolTable/tableName"));
    when(connection.getResponseCode()).thenReturn(200);
    when(connection.getInputStream()).thenReturn(serializedTable.asInputStream());

    provider.getSymbolTable(_symbolTableName);
    verify(connection).setRequestProperty(eq("Accept"), eq(ProtobufDataCodec.DEFAULT_HEADER));
    verify(connection).disconnect();
  }

  @Test
  public void testLocalSymbolTableSuccess()
  {
    DefaultSymbolTableProvider provider = new DefaultSymbolTableProvider();
    SymbolTable localSymbolTable =  new InMemorySymbolTable("local", Collections.singletonList("test"));
    provider.injectLocalSymbolTable(localSymbolTable);
    Assert.assertEquals(provider.getSymbolTable(localSymbolTable.getName()), localSymbolTable);
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testLocalSymbolTableFailure()
  {
    DefaultSymbolTableProvider provider = new DefaultSymbolTableProvider();
    provider.getSymbolTable("random");
  }
}
