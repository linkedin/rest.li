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

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;

import static com.linkedin.pegasus.gradle.PegasusPlugin.IDL_FILE_SUFFIX;
import static com.linkedin.pegasus.gradle.PegasusPlugin.SNAPSHOT_FILE_SUFFIX;


public class SharedFileUtils
{
  public static FileTree getSuffixedFiles(Project project, Object baseDir, String suffix)
  {
    return getSuffixedFiles(project, baseDir, Collections.singletonList(suffix));
  }

  public static FileTree getSuffixedFiles(Project project, Object baseDir, Collection<String> suffixes)
  {
    List<String> includes = suffixes.stream().map(suffix -> "**" + File.separatorChar + "*" + suffix)
        .collect(Collectors.toList());

    return project.fileTree(baseDir, fileTree -> fileTree.include(includes));
  }

  public static FileCollection getIdlFiles(Project project, Object destinationDirPrefix)
  {
    return getSuffixedFiles(project, project.file(destinationDirPrefix + "idl"), IDL_FILE_SUFFIX);
  }

  public static FileCollection getSnapshotFiles(Project project, Object destinationDirPrefix)
  {
    return getSuffixedFiles(project, project.file(destinationDirPrefix + "snapshot"), SNAPSHOT_FILE_SUFFIX);
  }
}
