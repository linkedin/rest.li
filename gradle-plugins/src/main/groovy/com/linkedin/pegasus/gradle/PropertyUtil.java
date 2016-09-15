package com.linkedin.pegasus.gradle;


import org.gradle.api.Project;

import static com.linkedin.pegasus.gradle.PegasusPlugin.SNAPSHOT_COMPAT_REQUIREMENT;
import static com.linkedin.pegasus.gradle.PegasusPlugin.IDL_COMPAT_REQUIREMENT;


public class PropertyUtil
{
  public static String findCompatLevel(Project project, FileCompatibilityType type)
  {
    return findCompatLevel(project, findProperty(type));
  }

  public static String findProperty(FileCompatibilityType type)
  {
    switch (type)
    {
      case SNAPSHOT:
        return SNAPSHOT_COMPAT_REQUIREMENT;
      case IDL:
        return IDL_COMPAT_REQUIREMENT;
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
      if (propertyName.equals(SNAPSHOT_COMPAT_REQUIREMENT))
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
