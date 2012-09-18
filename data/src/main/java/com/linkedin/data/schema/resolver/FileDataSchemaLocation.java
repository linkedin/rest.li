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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class FileDataSchemaLocation implements DataSchemaLocation, InputStreamProvider
{
  private final File _file;
  private final String _path;

  public FileDataSchemaLocation(File file)
  {
    _file = file;
    _path = getFilePath(file);
  }

  @Override
  public File getSourceFile()
  {
    return _file;
  }

  @Override
  public InputStream asInputStream(StringBuilder errorMessageBuilder)
  {
    InputStream inputStream = null;
    try
    {
      if (_file.exists())
      {
        inputStream = new FileInputStream(_file);
      }
    }
    catch (FileNotFoundException exc)
    {
      errorMessageBuilder.append(_path).append(" not found.\n");
    }
    return inputStream;
  }

  @Override
  public int hashCode()
  {
    return _path.hashCode();
  }

  @Override
  public boolean equals(Object o)
  {
    return o != null &&
           o instanceof FileDataSchemaLocation &&
           _path.equals(((FileDataSchemaLocation) o)._path);
  }

  @Override
  public String toString()
  {
    return _path;
  }

  private static String getFilePath(File file)
  {
    try
    {
      return file.getCanonicalPath();
    }
    catch (IOException e)
    {
      return file.getAbsolutePath();
    }
  }
}