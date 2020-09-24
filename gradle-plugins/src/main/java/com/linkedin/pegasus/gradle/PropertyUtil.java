/*
 * Copyright (c) 2020 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.pegasus.gradle;

import org.gradle.api.Project;

import static com.linkedin.pegasus.gradle.PegasusPlugin.SNAPSHOT_COMPAT_REQUIREMENT;
import static com.linkedin.pegasus.gradle.PegasusPlugin.IDL_COMPAT_REQUIREMENT;
import static com.linkedin.pegasus.gradle.PegasusPlugin.PEGASUS_SCHEMA_SNAPSHOT_REQUIREMENT;


public class PropertyUtil
{
  public static String findCompatLevel(Project project, FileCompatibilityType type)
  {
    return findCompatLevel(project, findProperty(type));
  }

  public static String findCompatMode(Project project, String propertyName)
  {
    if (project.hasProperty(propertyName))
    {
      String compatMode = project.property(propertyName).toString().toUpperCase();
      if (compatMode.equals("SCHEMA") || compatMode.equals("DATA"))
      {
        return compatMode;
      }
    }
    return "SCHEMA";
  }

  public static String findProperty(FileCompatibilityType type)
  {
    switch (type)
    {
      case SNAPSHOT:
        return SNAPSHOT_COMPAT_REQUIREMENT;
      case IDL:
        return IDL_COMPAT_REQUIREMENT;
      case PEGASUS_SCHEMA_SNAPSHOT:
        return PEGASUS_SCHEMA_SNAPSHOT_REQUIREMENT;
      default:
        return null;
    }
  }

  public static String findCompatLevel(Project project, String propertyName)
  {
    if (project.hasProperty(propertyName))
    {
      String compatLevel = project.property(propertyName).toString().toUpperCase();

      if (compatLevel.equals("OFF"))
      {
        return "IGNORE";
      }
      else
      {
        return compatLevel;
      }
    }
    else
    {
      if (propertyName.equals(SNAPSHOT_COMPAT_REQUIREMENT) || propertyName.equals(PEGASUS_SCHEMA_SNAPSHOT_REQUIREMENT))
      {
        // backwards compatible by default.
        return "BACKWARDS";
      }
      else
      {
        // off by default
        return "OFF";
      }
    }
  }
}
