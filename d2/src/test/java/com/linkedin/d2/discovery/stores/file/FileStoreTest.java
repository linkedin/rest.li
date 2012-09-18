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

import static org.testng.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.testng.annotations.Test;

import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.d2.discovery.stores.PropertyStoreTest;
import com.linkedin.d2.discovery.stores.PropertyStringSerializer;

public class FileStoreTest extends PropertyStoreTest
{
  @Override
  public PropertyStore<String> getStore()
  {
    try
    {
      return new FileStore<String>(createTempDirectory("file-store-test").toString(),
                                   ".ini",
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

  public static File createTempDirectory(String name) throws IOException
  {
    final File temp;

    temp = File.createTempFile("temp-" + name, Long.toString(System.nanoTime()));

    if (!(temp.delete()))
    {
      throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
    }

    if (!(temp.mkdir()))
    {
      throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
    }

    return (temp);
  }
}
