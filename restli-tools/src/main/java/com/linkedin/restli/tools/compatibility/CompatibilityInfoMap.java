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

package com.linkedin.restli.tools.compatibility;

import com.linkedin.data.message.Message;
import com.linkedin.data.schema.compatibility.CompatibilityMessage;
import com.linkedin.restli.tools.idlcheck.CompatibilityInfo;
import com.linkedin.restli.tools.idlcheck.CompatibilityLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class CompatibilityInfoMap
{
  private Map<CompatibilityInfo.Level, Collection<CompatibilityInfo>> _restSpecMap = new HashMap<>();
  private Map<CompatibilityInfo.Level, Collection<CompatibilityInfo>> _modelMap = new HashMap<>();
  private Map<CompatibilityInfo.Level, Collection<CompatibilityInfo>> _annotationMap = new HashMap<>();

  public CompatibilityInfoMap()
  {
    _restSpecMap.put(CompatibilityInfo.Level.INCOMPATIBLE, new ArrayList<>());
    _restSpecMap.put(CompatibilityInfo.Level.COMPATIBLE, new ArrayList<>());

    _modelMap.put(CompatibilityInfo.Level.INCOMPATIBLE, new ArrayList<>());
    _modelMap.put(CompatibilityInfo.Level.COMPATIBLE, new ArrayList<>());

    _annotationMap.put(CompatibilityInfo.Level.INCOMPATIBLE, new ArrayList<>());
    _annotationMap.put(CompatibilityInfo.Level.COMPATIBLE, new ArrayList<>());
  }

  public void addRestSpecInfo(CompatibilityInfo.Type infoType, Stack<Object> path,
                              Object... parameters)
  {
    _restSpecMap.get(infoType.getLevel()).add(new CompatibilityInfo(path, infoType, parameters));
  }

  public void addRestSpecInfo(Object pathTail, CompatibilityInfo.Type infoType, Stack<Object> path,
                              Object... parameters)
  {
    path.push(pathTail);
    _restSpecMap.get(infoType.getLevel()).add(new CompatibilityInfo(path, infoType, parameters));
    path.pop();
  }

  public void addRestSpecInfo(Object pathTail, CompatibilityMessage message, Stack<Object> path)
  {
    path.push(pathTail);

    final CompatibilityInfo.Type infoType;
    if (message.isError())
    {
      infoType = CompatibilityInfo.Type.TYPE_ERROR;
      final String info = String.format(message.getFormat(), message.getArgs());
      _restSpecMap.get(infoType.getLevel()).add(new CompatibilityInfo(path, infoType, info));
    }
    else
    {
      infoType = CompatibilityInfo.Type.TYPE_INFO;
      final String info = String.format(message.getFormat(), message.getArgs());
      _restSpecMap.get(infoType.getLevel()).add(new CompatibilityInfo(Arrays.asList(message.getPath()), infoType, info));
    }

    path.pop();
  }

  public void addRestSpecInfo(Message message)
  {
    final CompatibilityInfo.Type infoType = CompatibilityInfo.Type.OTHER_ERROR;
    _restSpecMap.get(infoType.getLevel()).add(new CompatibilityInfo(Arrays.asList(message.getPath()),
                                                                    infoType,
                                                                    message.toString()));
  }

  /**
   * Add info used for adding errors related to {@link com.linkedin.data.schema.NamedDataSchema} compatibility.
   * The path will be the path to the relevant field within the NamedDataSchema.
   * @param message {@link CompatibilityMessage}
   */
  public void addModelInfo(CompatibilityMessage message)
  {
    final CompatibilityInfo.Type infoType;
    CompatibilityInfo info;
    String infoMessage = String.format(message.getFormat(), message.getArgs());

    if (message.isError())
    {
      switch (message.getImpact())
      {
        case BREAKS_NEW_READER:
          infoType = CompatibilityInfo.Type.TYPE_BREAKS_NEW_READER;
          break;
        case BREAKS_OLD_READER:
          infoType = CompatibilityInfo.Type.TYPE_BREAKS_OLD_READER;
          break;
        case BREAKS_NEW_AND_OLD_READERS:
          infoType = CompatibilityInfo.Type.TYPE_BREAKS_NEW_AND_OLD_READERS;
          break;
        case BREAK_OLD_CLIENTS:
          infoType = CompatibilityInfo.Type.BREAK_OLD_CLIENTS;
          break;
        default:
          infoType = CompatibilityInfo.Type.OTHER_ERROR;
          break;
      }
    }
    else
    {
      infoType = CompatibilityInfo.Type.TYPE_INFO;
    }
    info = new CompatibilityInfo(Arrays.asList(message.getPath()), infoType, infoMessage);
    _modelMap.get(infoType.getLevel()).add(info);

  }

  /**
   * @return summary message about the check result, including all categories
   *         empty string if called before checking any files
   */
  public String createSummary(String prevRestModelPath, String currRestModelPath)
  {
    final StringBuilder summaryMessage = new StringBuilder();

    createSummaryForInfo(getIncompatibles(), "Incompatible changes", summaryMessage);
    createSummaryForInfo(getCompatibles(), "Compatible changes", summaryMessage);

    if (summaryMessage.length() != 0)
    {
      summaryMessage.insert(0, new StringBuilder("\nRest.li compatibility report between published \"")
                                 .append(prevRestModelPath)
                                 .append("\" and current \"")
                                 .append(currRestModelPath)
                                 .append("\":\n"));
    }

    return summaryMessage.toString();
  }

  public String createSummary()
  {
    final StringBuilder summaryMessage = new StringBuilder();

    createSummaryForInfo(getIncompatibles(), "Incompatible changes", summaryMessage);
    createSummaryForInfo(getCompatibles(), "Compatible changes", summaryMessage);

    if (summaryMessage.length() != 0)
    {
      summaryMessage.insert(0, "\nidl compatibility report:\n");
    }

    return summaryMessage.toString();
  }

  private static void createSummaryForInfo(Collection<CompatibilityInfo> info,
                                           String description,
                                           StringBuilder summaryMessage)
  {
    if (info.isEmpty())
    {
      return;
    }

    summaryMessage.append(description).append(":\n");
    int issueIndex = 1;
    for (CompatibilityInfo i: info)
    {
      summaryMessage.append("  ").append(issueIndex).append(") ").append(i.toString()).append("\n");
      ++issueIndex;
    }
  }

  public boolean isCompatible(CompatibilityLevel level)
  {
    final Collection<CompatibilityInfo> incompatibles = getIncompatibles();
    final Collection<CompatibilityInfo> compatibles = getCompatibles();

    return isCompatible(incompatibles, compatibles, level);
  }

  public boolean isRestSpecCompatible(CompatibilityLevel level)
  {
    final Collection<CompatibilityInfo> incompatibles = getRestSpecIncompatibles();
    final Collection<CompatibilityInfo> compatibles = getRestSpecCompatibles();

    return isCompatible(incompatibles, compatibles, level);
  }

  public boolean isModelCompatible(CompatibilityLevel level)
  {
    final Collection<CompatibilityInfo> incompatibles = getModelIncompatibles();
    final Collection<CompatibilityInfo> compatibles = getModelCompatibles();

    return isCompatible(incompatibles, compatibles, level);
  }

  private boolean isCompatible(Collection<CompatibilityInfo> incompatibles, Collection<CompatibilityInfo> compatibles, CompatibilityLevel level)
  {
    return ((incompatibles.isEmpty() || level.ordinal() < CompatibilityLevel.BACKWARDS.ordinal()) &&
            (compatibles.isEmpty()   || level.ordinal() < CompatibilityLevel.EQUIVALENT.ordinal()));
  }

  public boolean isEquivalent()
  {
    return isCompatible(CompatibilityLevel.EQUIVALENT);
  }

  public boolean isRestSpecEquivalent()
  {
    return isRestSpecCompatible(CompatibilityLevel.EQUIVALENT);
  }

  public boolean isModelEquivalent()
  {
    return isModelCompatible(CompatibilityLevel.EQUIVALENT);
  }

  /**
   * @return check results in the backwards incompatibility category.
   *         empty collection if called before checking any files
   */
  public Collection<CompatibilityInfo> getIncompatibles()
  {
    return get(CompatibilityInfo.Level.INCOMPATIBLE);
  }

  /**
   * @return check results in the backwards compatibility category.
   *         empty collection if called before checking any files
   */
  public Collection<CompatibilityInfo> getCompatibles()
  {
    return get(CompatibilityInfo.Level.COMPATIBLE);
  }

  public Collection<CompatibilityInfo> getRestSpecIncompatibles()
  {
    return getRestSpecInfo(CompatibilityInfo.Level.INCOMPATIBLE);
  }

  public Collection<CompatibilityInfo> getRestSpecCompatibles()
  {
    return getRestSpecInfo(CompatibilityInfo.Level.COMPATIBLE);
  }

  public Collection<CompatibilityInfo> getModelIncompatibles()
  {
    return getModelInfo(CompatibilityInfo.Level.INCOMPATIBLE);
  }

  public Collection<CompatibilityInfo> getModelCompatibles()
  {
    return getModelInfo(CompatibilityInfo.Level.COMPATIBLE);
  }

  public Collection<CompatibilityInfo> get(CompatibilityInfo.Level level)
  {
    Collection<CompatibilityInfo> infos = new ArrayList<>(getRestSpecInfo(level));
    infos.addAll(getModelInfo(level));
    return infos;
  }

  public Collection<CompatibilityInfo> getRestSpecInfo(CompatibilityInfo.Level level)
  {
    return _restSpecMap.get(level);
  }

  public Collection<CompatibilityInfo> getModelInfo(CompatibilityInfo.Level level)
  {
    return _modelMap.get(level);
  }

  public boolean addAll(CompatibilityInfoMap other)
  {
    for(Map.Entry<CompatibilityInfo.Level, Collection<CompatibilityInfo>> entry : _restSpecMap.entrySet())
    {
      entry.getValue().addAll(other.getRestSpecInfo(entry.getKey()));
    }
    for(Map.Entry<CompatibilityInfo.Level, Collection<CompatibilityInfo>> entry : _modelMap.entrySet())
    {
      entry.getValue().addAll(other.getModelInfo(entry.getKey()));
    }
    return true;
  }

  public void addAnnotation(CompatibilityMessage message)
  {
    final CompatibilityInfo.Type infoType;
    CompatibilityInfo info;
    String infoMessage = String.format(message.getFormat(), message.getArgs());

    if (message.isError())
    {
      switch (message.getImpact())
      {
        case ANNOTATION_INCOMPATIBLE_CHANGE:
          infoType = CompatibilityInfo.Type.SCHEMA_ANNOTATION_INCOMPATIBLE_CHANGE;
          break;
        default:
          infoType = CompatibilityInfo.Type.OTHER_ERROR;
          break;
      }
    }
    else
    {
      infoType = CompatibilityInfo.Type.TYPE_INFO;
    }
    info = new CompatibilityInfo(Arrays.asList(message.getPath()), infoType, infoMessage);
    _annotationMap.get(infoType.getLevel()).add(info);
  }

  /**
   * This method indicates whether the schema annotation changes are compatible or not,
   * by default it uses "backwards" as compatibility level.
   * @return boolean
   */
  public boolean isAnnotationCompatible()
  {
    return isAnnotationCompatible(CompatibilityLevel.BACKWARDS);
  }

  /**
   * This method indicates whether the schema annotation changes are compatible or not based on the given compatibility level.
   * @param level, the given {@link CompatibilityLevel}.
   * @return boolean
   */
  public boolean isAnnotationCompatible(CompatibilityLevel level)
  {
    return isCompatible(_annotationMap.get(CompatibilityInfo.Level.INCOMPATIBLE),
            _annotationMap.get(CompatibilityInfo.Level.COMPATIBLE), level);
  }

  public Collection<CompatibilityInfo> getAnnotationInfo(CompatibilityInfo.Level level)
  {
    return _annotationMap.get(level);
  }
}
