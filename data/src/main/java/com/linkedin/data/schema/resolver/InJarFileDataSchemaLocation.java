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

package com.linkedin.data.schema.resolver;


import com.linkedin.data.schema.DataSchemaLocation;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Represents a file locations in a jar file.
 */
public class InJarFileDataSchemaLocation implements DataSchemaLocation, InputStreamProvider
{
  private final JarFile _jarFile;
  private final String _pathInJar;

  public InJarFileDataSchemaLocation(JarFile jarFile, String pathInJar)
  {
    _jarFile = jarFile;
    _pathInJar = pathInJar;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    if (o instanceof InJarFileDataSchemaLocation == false)
      return false;
    InJarFileDataSchemaLocation other = (InJarFileDataSchemaLocation) o;
    return (_jarFile.equals(other._jarFile) && _pathInJar.equals(other._pathInJar));
  }

  @Override
  public int hashCode()
  {
    return _jarFile.hashCode() ^ _pathInJar.hashCode();
  }

  @Override
  public String toString()
  {
    return getSourceFile().getPath() + ":" + _pathInJar;

  }

  @Override
  public File getSourceFile()
  {
    return new File(_jarFile.getName());
  }

  @Override
  public InputStream asInputStream(StringBuilder errorMessageBuilder)
  {
    InputStream inputStream = null;
    try
    {
      ZipEntry zipEntry = _jarFile.getEntry(_pathInJar);
      if (zipEntry != null)
      {
        inputStream = _jarFile.getInputStream(zipEntry);
      }
    }
    catch (IOException exc)
    {
      errorMessageBuilder.append(_pathInJar).append(" not found in ").append(getSourceFile().toString()).append("\n");
    }
    return inputStream;
  }
}
