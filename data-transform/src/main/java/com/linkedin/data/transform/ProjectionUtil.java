/*
 * Copyright (c) 2014 LinkedIn Corp.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.linkedin.data.transform;


import com.linkedin.data.DataMap;
import com.linkedin.data.Null;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.transform.filter.CopyFilter;
import com.linkedin.data.transform.filter.request.MaskTree;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * @author Keren Jin
 */
public class ProjectionUtil
{
  private static class PathSpecFilter extends CopyFilter
  {
    /**
     * The only error supposed to be received here is where the value is primitive but filter is complex
     * This should be ignored to allow a prefix to stay.
     */
    @Override
    protected Object onError(Object field, String format, Object... args)
    {
      return null;
    }
  }

  /**
   * <p>Given a {@link MaskTree} and a {@link PathSpec}, return if the PathSpec is present.
   * In other words, it tells that when Rest.li projects on a record with the given MaskTree, whether the field
   * represented by the PathSpec would survive.</p>
   *
   * Convenient version of getPresentPaths() to only test one {@link PathSpec} at a time.
   * For more details, check the documentation of getPresentPaths().
   *
   * @param filter filter to apply
   * @param path PathSpec to test
   * @return true if the PathSpec is present in the MaskTree filtering
   */
  public static boolean isPathPresent(MaskTree filter, PathSpec path)
  {
    return !getPresentPaths(filter, Collections.singleton(path)).isEmpty();
  }

  /**
   * <p>Given a {@link MaskTree} and a {@link Set} of {@link PathSpec}s, return the PathSpecs that are present.
   * In other words, it tells that when Rest.li projects on a record with the given MaskTree, whether the fields
   * represented by the PathSpecs would survive.</p>
   *
   * <p>If a PathSpec is a prefix of the filter or the filter is a prefix of a PathSpec, it is always considered present. For example,
   * <pre>
   * MaskTree:  /foo/bar:     POSITIVE
   * PathSpecs: /foo:         present
   *            /foo/bar:     present
   *            /foo/bar/baz: present
   *            /xyz:         not present
   * </pre>
   * </p>
   *
   * <p>Empty filter Empty PathSpec is considered prefix of all filters.</p>
   *
   * @param filter filter to apply
   * @param paths PathSpecs to test
   * @return PathSpecs that are present in the MaskTree filtering
   */
  public static Set<PathSpec> getPresentPaths(MaskTree filter, Set<PathSpec> paths)
  {
    // this emulates the behavior of Rest.li server
    // if client does not specify any mask, the server receives null when retrieving the MaskTree
    // in this case, all fields are returned
    if (filter == null)
    {
      return paths;
    }

    final DataMap filterMap = filter.getDataMap();
    if (filter.getDataMap().isEmpty())
    {
      return Collections.emptySet();
    }

    final DataMap pathSpecMap = createPathSpecMap(paths);

    @SuppressWarnings("unchecked")
    final DataMap filteredPathSpecs = (DataMap) new PathSpecFilter().filter(pathSpecMap, filterMap);

    return validate(filteredPathSpecs, paths);
  }

  private static DataMap createPathSpecMap(Set<PathSpec> paths)
  {
    final DataMap pathSpecMap = new DataMap();

    for (PathSpec p : paths)
    {
      final List<String> components = p.getPathComponents();
      DataMap currentMap = pathSpecMap;

      for (int i = 0; i < components.size(); ++i)
      {
        final String currentComponent = components.get(i);
        final Object currentValue = currentMap.get(currentComponent);

        if (i < components.size() - 1)
        {
          if (currentValue instanceof DataMap)
          {
            @SuppressWarnings("unchecked")
            final DataMap valueMap = (DataMap) currentValue;
            currentMap = valueMap;
          }
          else
          {
            final DataMap newMap = new DataMap();
            currentMap.put(currentComponent, newMap);
            currentMap = newMap;
          }
        }
        else if (currentValue == null)
        {
          currentMap.put(currentComponent, Null.getInstance());
        }
      }
    }

    return pathSpecMap;
  }

  private static Set<PathSpec> validate(DataMap filteredPathSpecs, Set<PathSpec> paths)
  {
    final Set<PathSpec> result = new HashSet<>();

    for (PathSpec p : paths)
    {
      final List<String> components = p.getPathComponents();
      DataMap currentMap = filteredPathSpecs;
      boolean isPresent = true;

      for (int i = 0; i < components.size(); ++i)
      {
        final String currentComponent = components.get(i);
        final Object currentValue = currentMap.get(currentComponent);

        if (currentValue instanceof DataMap)
        {
          @SuppressWarnings("unchecked")
          final DataMap valueMap = (DataMap) currentValue;
          currentMap = valueMap;
        }
        else
        {
          isPresent = currentMap.containsKey(currentComponent);
          break;
        }
      }

      if (isPresent)
      {
        result.add(p);
      }
    }

    return result;
  }
}
