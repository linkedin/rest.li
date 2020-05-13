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

  /**
   * Return {@link DataSchemaLocation} that is a lightweight representation of this location for storing in-memory.
   *
   * For example, {@link com.linkedin.data.schema.resolver.InJarFileDataSchemaLocation}
   * contains an entire Jar-file in it's impl; this isn't necessary for storing as a {@link DataSchemaLocation},
   * so it could return a lighter-weight implementation.
   *
   * @return a light-weight representation of this DataSchemaLocation. Default is {@code this}
   */
  default DataSchemaLocation getLightweightRepresentation() {
    return this;
  }

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
