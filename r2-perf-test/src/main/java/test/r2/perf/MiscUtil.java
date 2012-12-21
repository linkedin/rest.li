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

package test.r2.perf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class MiscUtil implements TestConstants
{
  public static URI getUri(final String propName)
  {
    final String propVal = System.getProperty(propName);
    return propVal != null ? URI.create(propVal) : URI.create(DEFAULT_RELATIVE_URI);
  }

  /**
   * If there is a system property with name {@code propName}, return it. Otherwise, return the
   * default value.
   *
   * @param propName name of the property
   * @param defaultValue
   * @return value of the property
   */
  public static String getString(final String propName, final String defaultValue)
  {
    final String propVal = System.getProperty(propName);
    return propVal != null ? propVal : defaultValue;
  }

  /**
   * Completely read the given file and return the contents as a byte array.
   *
   * @param file the file to read from
   * @return the content of the file
   * @throws IOException
   */
  public static byte[] getBytesFromFile(final File file) throws IOException
  {
    InputStream is = null;
    try
    {
      is = new FileInputStream(file);

      final long length = file.length();

      if (length > Integer.MAX_VALUE)
      {
        // File is too large
        throw new IOException("File " + file + " is too large.");
      }

      final byte[] bytes = new byte[(int) length];

      int offset = 0;
      int numRead = 0;
      while (offset < bytes.length
          && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0)
      {
        offset += numRead;
      }

      if (offset < bytes.length)
      {
        throw new IOException("Could not completely read file " + file.getName());
      }
      return bytes;
    }
    finally
    {
      if (is != null)
        is.close();
    }
  }
}
