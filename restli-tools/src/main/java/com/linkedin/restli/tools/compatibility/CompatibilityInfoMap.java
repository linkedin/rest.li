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
  private Map<CompatibilityInfo.Level, Collection<CompatibilityInfo>> _map = new HashMap<CompatibilityInfo.Level, Collection<CompatibilityInfo>>();

  public CompatibilityInfoMap()
  {
    _map.put(CompatibilityInfo.Level.INCOMPATIBLE, new ArrayList<CompatibilityInfo>());
    _map.put(CompatibilityInfo.Level.COMPATIBLE, new ArrayList<CompatibilityInfo>());
  }

  public void addInfo(CompatibilityInfo.Type infoType, Stack<Object> path, Object... parameters)
  {
    _map.get(infoType.getLevel()).add(new CompatibilityInfo(path, infoType, parameters));
  }

  public void addInfo(Object pathTail, CompatibilityInfo.Type infoType, Stack<Object> path, Object... parameters)
  {
    path.push(pathTail);
    _map.get(infoType.getLevel()).add(new CompatibilityInfo(path, infoType, parameters));
    path.pop();
  }

  public void addInfo(Object pathTail, CompatibilityMessage message, Stack<Object> path)
  {
    path.push(pathTail);

    final CompatibilityInfo.Type infoType;
    if (message.isError())
    {
       infoType = CompatibilityInfo.Type.TYPE_INCOMPATIBLE;
      _map.get(infoType.getLevel()).add(new CompatibilityInfo(path, infoType, message.getArgs()));
    }
    else
    {
      infoType = CompatibilityInfo.Type.TYPE_INFO;
      String info = String.format(message.getFormat(), message.getArgs());
      _map.get(infoType.getLevel()).add(new CompatibilityInfo(Arrays.asList(message.getPath()), infoType, info));
    }

    path.pop();
  }

  /**
   * Add info used for adding errors related to {@link com.linkedin.data.schema.NamedDataSchema} compatibility.
   * The path will be the path to the relevant field within the NamedDataSchema.
   * @param message {@link CompatibilityMessage}
   */
  public void addInfo(CompatibilityMessage message)
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
    _map.get(infoType.getLevel()).add(info);

  }

  public void addInfo(Message message)
  {
    final CompatibilityInfo.Type infoType = CompatibilityInfo.Type.OTHER_ERROR;
    _map.get(infoType.getLevel()).add(new CompatibilityInfo(Arrays.asList(message.getPath()),
                                                            infoType,
                                                            message.toString()));
  }

  /**
   * @return summary message about the check result, including all categories
   *         empty string if called before checking any files
   */
  public String createSummary(String prevRestspecPath, String currRestspecPath)
  {
    final StringBuilder summaryMessage = new StringBuilder();

    createSummaryForInfo(getIncompatibles(), "Incompatible changes", summaryMessage);
    createSummaryForInfo(getCompatibles(), "Compatible changes", summaryMessage);

    if (summaryMessage.length() != 0)
    {
      summaryMessage.insert(0, "\nidl compatibility report between published \"" + prevRestspecPath + "\" and current \"" + currRestspecPath + "\":\n");
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

    return ((incompatibles.isEmpty()        || level.ordinal() < CompatibilityLevel.BACKWARDS.ordinal()) &&
            (compatibles.isEmpty()          || level.ordinal() < CompatibilityLevel.EQUIVALENT.ordinal()));
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

  public Collection<CompatibilityInfo> get(CompatibilityInfo.Level level)
  {
    return _map.get(level);
  }

  public boolean addAll(CompatibilityInfoMap other)
  {
    for(Map.Entry<CompatibilityInfo.Level, Collection<CompatibilityInfo>> entry : _map.entrySet())
    {
      entry.getValue().addAll(other.get(entry.getKey()));
    }
    return true;
  }

}