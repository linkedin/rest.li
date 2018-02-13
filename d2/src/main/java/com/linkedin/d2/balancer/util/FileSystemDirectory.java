/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.d2.balancer.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FileSystemDirectory retrieves the list of cluster and service names saved on the local disk. There is no guarantee of being
 * aligned with the ZooKeeper directory
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class FileSystemDirectory
{
  private static final Logger LOG = LoggerFactory.getLogger(FileSystemDirectory.class);

  public static final String FILE_STORE_EXTENSION = ".ini";
  public static final String CLUSTER_DIRECTORY = "clusters";
  public static final String DEFAULT_SERVICES_DIRECTORY = "services";

  private final String _d2FsDirPath;
  private String _d2ServicePath;

  public FileSystemDirectory(String d2FsDirPath, String d2ServicePath)
  {
    _d2FsDirPath = d2FsDirPath;
    _d2ServicePath = d2ServicePath;
  }

  public List<String> getServiceNames()
  {
    return getFileListWithoutExtension(getServiceDirectory(_d2FsDirPath, _d2ServicePath));
  }

  public void removeAllServicesWithExcluded(Set<String> excludedServices)
  {
    List<String> serviceNames = getServiceNames();
    serviceNames.removeAll(excludedServices);
    removeAllPropertiesFromDirectory(getServiceDirectory(_d2FsDirPath, _d2ServicePath), serviceNames);
  }

  public void removeAllClustersWithExcluded(Set<String> excludedClusters)
  {
    List<String> serviceNames = getClusterNames();
    serviceNames.removeAll(excludedClusters);
    removeAllPropertiesFromDirectory(getServiceDirectory(_d2FsDirPath, _d2ServicePath), serviceNames);
  }

  public static void removeAllPropertiesFromDirectory(String path, List<String> properties)
  {
    for (String property : properties)
    {
      try
      {
        Files.deleteIfExists(Paths.get(path + File.separator + property + FileSystemDirectory.FILE_STORE_EXTENSION));
      } catch (IOException e)
      {
        LOG.warn("IO Error, continuing deletion", e);
      }
    }
  }

  public List<String> getClusterNames()
  {
    return getFileListWithoutExtension(getClusterDirectory(_d2ServicePath));
  }

  public static List<String> getFileListWithoutExtension(String path)
  {
    File dir = new File(path);
    File[] files = dir.listFiles((dir1, name) -> name.endsWith(FileSystemDirectory.FILE_STORE_EXTENSION));
    if (files == null)
    {
      return Collections.emptyList();
    }

    // cleaning the list from the extension
    return Arrays.stream(files)
      .map(file -> file.getName().replace(FILE_STORE_EXTENSION, ""))
      .collect(Collectors.toList());
  }

  public static String getServiceDirectory(String d2FsDirPath, String d2ServicePath)
  {
    if (d2ServicePath == null || d2ServicePath.isEmpty())
    {
      d2ServicePath = DEFAULT_SERVICES_DIRECTORY;
    }
    return d2FsDirPath + File.separator + d2ServicePath;
  }

  public static String getClusterDirectory(String d2FsDirPath)
  {
    return d2FsDirPath + File.separator + CLUSTER_DIRECTORY;
  }
}
