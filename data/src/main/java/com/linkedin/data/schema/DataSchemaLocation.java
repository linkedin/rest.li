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

package com.linkedin.data.schema;

import java.io.File;

/**
 * Represents the source location of the {@link DataSchema}.
 *
 * @author slim
 */
public interface DataSchemaLocation
{
  /**
   * Return {@link File} that is this location or contains this location.
   *
   * This {@link File} is used to check the freshness of the output files.
   *
   * @return the file that is this location or contains this location,
   *         return null if the location is not within a file.
   *
   */
  File getSourceFile();

  final DataSchemaLocation NO_LOCATION = new DataSchemaLocation()
  {
    @Override
    public File getSourceFile()
    {
      return null;
    }

    @Override
    public String toString()
    {
      return "";
    }
  };
}
