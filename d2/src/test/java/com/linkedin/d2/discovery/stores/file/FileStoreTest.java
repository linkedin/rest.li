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

package com.linkedin.d2.discovery.stores.file;

import com.linkedin.d2.balancer.util.FileSystemDirectory;
import com.linkedin.d2.discovery.PropertySerializationException;
import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import com.linkedin.d2.discovery.stores.PropertyStoreTest;
import com.linkedin.d2.discovery.stores.PropertyStringSerializer;
import java.util.Collections;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

import static com.linkedin.d2.balancer.util.LoadBalancerUtil.createTempDirectory;
import static org.testng.Assert.fail;

public class FileStoreTest extends PropertyStoreTest
{

  @Override
  public PropertyStore<String> getStore()
  {
    try
    {
      return new FileStore<>(createTempDirectory("file-store-test").toString(),
          FileSystemDirectory.FILE_STORE_EXTENSION,
          new PropertyStringSerializer());
    }
    catch (IOException e)
    {
      fail("unable to create file store");
    }

    return null;
  }

  @Test(groups = { "small", "back-end" })
  public void test()
  {

  }

  @Test
  public void testFileStoreGetDeserializationError() throws IOException, PropertyStoreException
  {
    final PropertyStore<String> fileStore = new FileStore<>(createTempDirectory("file-store-test").toString(),
      FileSystemDirectory.FILE_STORE_EXTENSION,
      new TestPropertySerializer<>(new PropertyStringSerializer()));
    final String name = "testFileStoreGet";
    final String contents = "contents";

    fileStore.put(name, contents);

    Assert.assertNull(fileStore.get(name));
  }

  @Test
  public void testFileStoreGetAllDeserializationError() throws IOException, PropertyStoreException
  {
    final FileStore<String> fileStore = new FileStore<>(createTempDirectory("file-store-test").toString(),
        FileSystemDirectory.FILE_STORE_EXTENSION,
        new TestPropertySerializer<>(new PropertyStringSerializer()));
    final String name = "testFileStoreGetAll";
    final String name2 = "testFileStoreGetAll2";
    final String contents = "contents";

    fileStore.put(name, contents);
    fileStore.put(name2, contents);

    Assert.assertEquals(fileStore.getAll(), Collections.emptyMap(), "Expected empty map since all files were not deserialized properly.");
  }

  /**
   * Test serializer that throws when deserializing.
   *
   * @param <T>
   */
  private class TestPropertySerializer<T> implements PropertySerializer<T>
  {
    private final PropertySerializer<T> _serializer;

    private TestPropertySerializer(PropertySerializer<T> serializer)
    {
      _serializer = serializer;
    }

    @Override
    public byte[] toBytes(T property) {
      return _serializer.toBytes(property);
    }

    @Override
    public T fromBytes(byte[] bytes) throws PropertySerializationException
    {
      throw new PropertySerializationException("Expected exception.");
    }
  }
}
