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
package com.linkedin.pegasus.gradle.tasks;

import com.linkedin.pegasus.gradle.IOUtil;
import com.linkedin.pegasus.gradle.PathingJarUtil;
import com.linkedin.pegasus.gradle.PegasusPlugin;
import com.linkedin.pegasus.gradle.internal.CompatibilityLogChecker;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ReplacedBy;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;


@CacheableTask
public class CheckRestModelTask extends DefaultTask
{
  private FileCollection _currentSnapshotFiles;
  private File _previousSnapshotDirectory;
  private FileCollection _currentIdlFiles;
  private File _previousIdlDirectory;
  private FileCollection _codegenClasspath;
  private String _modelCompatLevel;
  private File _summaryTarget = new File(getProject().getBuildDir(), "reports/checkRestModel/summary.txt");

  private boolean _modelCompatible = true;
  private boolean _restSpecCompatible = true;
  private boolean _equivalent = true;
  private boolean _restSpecEquivalent = true;
  private String _wholeMessage = "";

  @TaskAction
  public void check()
  {
    getProject().getLogger().info("Checking interface compatibility with API ...");

    List<String> argFiles = new ArrayList<>();

    argFiles.addAll(findMatchingFiles(PegasusPlugin.SNAPSHOT_FILE_SUFFIX, _currentSnapshotFiles,
        getProject().fileTree(_previousSnapshotDirectory), false));

    // We don't pass matching IDL files to RestLiSnapshotCompatibilityChecker. We only specify added or deleted IDL
    // files, for which the checker will generate appropriate message.
    argFiles.addAll(findMatchingFiles(PegasusPlugin.IDL_FILE_SUFFIX, _currentIdlFiles,
        getProject().fileTree(_previousIdlDirectory), true));

    if (argFiles.isEmpty())
    {
      return;
    }

    CompatibilityLogChecker logChecker = new CompatibilityLogChecker();

    FileCollection _pathedCodegenClasspath;
    try
    {
      _pathedCodegenClasspath = PathingJarUtil.generatePathingJar(getProject(), getName(),
          _codegenClasspath, false);
    }
    catch (IOException e)
    {
      throw new GradleException("Error occurred generating pathing JAR.", e);
    }

    getProject().javaexec(javaExecSpec ->
    {
      javaExecSpec.setMain("com.linkedin.restli.tools.snapshot.check.RestLiSnapshotCompatibilityChecker");
      javaExecSpec.setClasspath(_pathedCodegenClasspath);
      javaExecSpec.args("--compat", _modelCompatLevel.toLowerCase());
      javaExecSpec.args("--report");
      javaExecSpec.args(argFiles);
      javaExecSpec.setStandardOutput(logChecker);
    });

    _modelCompatible = logChecker.isModelCompatible();
    _restSpecCompatible = logChecker.isRestSpecCompatible();
    _equivalent = logChecker.getModelCompatibility().isEmpty() && logChecker.getRestSpecCompatibility().isEmpty();
    _restSpecEquivalent = logChecker.getRestSpecCompatibility().isEmpty();
    _wholeMessage = logChecker.getWholeText();
    IOUtil.writeText(_summaryTarget, _wholeMessage);

    if (!_modelCompatible || !_restSpecCompatible)
    {
      throw new GradleException("See output for " + getPath() + ". Summary written to "
          + _summaryTarget.getAbsolutePath());
    }
  }

  @InputFiles
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  public FileCollection getCurrentSnapshotFiles()
  {
    return _currentSnapshotFiles;
  }

  public void setCurrentSnapshotFiles(FileCollection currentSnapshotFiles)
  {
    _currentSnapshotFiles = currentSnapshotFiles;
  }

  @InputDirectory
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  public File getPreviousSnapshotDirectory()
  {
    return _previousSnapshotDirectory;
  }

  public void setPreviousSnapshotDirectory(File previousSnapshotDirectory)
  {
    _previousSnapshotDirectory = previousSnapshotDirectory;
  }

  @InputFiles
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  public FileCollection getCurrentIdlFiles()
  {
    return _currentIdlFiles;
  }

  public void setCurrentIdlFiles(FileCollection currentIdlFiles)
  {
    _currentIdlFiles = currentIdlFiles;
  }

  @InputDirectory
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  public File getPreviousIdlDirectory()
  {
    return _previousIdlDirectory;
  }

  public void setPreviousIdlDirectory(File previousIdlDirectory)
  {
    _previousIdlDirectory = previousIdlDirectory;
  }

  @Classpath
  public FileCollection getCodegenClasspath()
  {
    return _codegenClasspath;
  }

  public void setCodegenClasspath(FileCollection codegenClasspath)
  {
    _codegenClasspath = codegenClasspath;
  }

  @Input
  public String getModelCompatLevel()
  {
    return _modelCompatLevel;
  }

  public void setModelCompatLevel(String modelCompatLevel)
  {
    _modelCompatLevel = modelCompatLevel;
  }

  @OutputFile
  public File getSummaryTarget()
  {
    return _summaryTarget;
  }

  public void setSummaryTarget(File summaryTarget)
  {
    _summaryTarget = summaryTarget;
  }

  /**
   * This method is kept for backwards compatibility.
   * <p>
   * A Groovy property with this name was exposed, which leads to this lengthy
   * getter name. In Java, boolean fields are named without the "is" prefix.
   *
   * @deprecated use {@link #isModelCompatible()} instead
   */
  @Deprecated
  @ReplacedBy("modelCompatible")
  public boolean getIsModelCompatible()
  {
    return isModelCompatible();
  }

  /**
   * This method is kept for backwards compatibility.
   * <p>
   * A Groovy property with this name was exposed, which leads to this lengthy
   * getter name. In Java, boolean fields are named without the "is" prefix.
   *
   * @deprecated use {@link #isModelCompatible()} instead
   */
  @Deprecated
  @ReplacedBy("modelCompatible")
  public boolean isIsModelCompatible()
  {
    return isModelCompatible();
  }

  @Internal
  public boolean isModelCompatible()
  {
    return _modelCompatible;
  }

  /**
   * This method is kept for backwards compatibility.
   * <p>
   * A Groovy property with this name was exposed, which leads to this lengthy
   * getter name. In Java, boolean fields are named without the "is" prefix.
   *
   * @deprecated use {@link #isRestSpecCompatible()} instead
   */
  @Deprecated
  @ReplacedBy("restSpecCompatible")
  public boolean getIsRestSpecCompatible()
  {
    return isRestSpecCompatible();
  }

  /**
   * This method is kept for backwards compatibility.
   * <p>
   * A Groovy property with this name was exposed, which leads to this lengthy
   * getter name. In Java, boolean fields are named without the "is" prefix.
   *
   * @deprecated use {@link #isRestSpecCompatible()} instead
   */
  @Deprecated
  @ReplacedBy("restSpecCompatible")
  public boolean isIsRestSpecCompatible()
  {
    return isRestSpecCompatible();
  }

  @Internal
  public boolean isRestSpecCompatible()
  {
    return _restSpecCompatible;
  }

  /**
   * This method is kept for backwards compatibility.
   * <p>
   * A Groovy property with this name was exposed, which leads to this lengthy
   * getter name. In Java, boolean fields are named without the "is" prefix.
   *
   * @deprecated use {@link #isEquivalent()} instead
   */
  @Deprecated
  @ReplacedBy("equivalent")
  public boolean getIsEquivalent()
  {
    return isEquivalent();
  }

  /**
   * This method is kept for backwards compatibility.
   * <p>
   * A Groovy property with this name was exposed, which leads to this lengthy
   * getter name. In Java, boolean fields are named without the "is" prefix.
   *
   * @deprecated use {@link #isEquivalent()} instead
   */
  @Deprecated
  @ReplacedBy("equivalent")
  public boolean isIsEquivalent()
  {
    return isEquivalent();
  }

  @Internal
  public boolean isEquivalent()
  {
    return _equivalent;
  }

  /**
   * This method is kept for backwards compatibility.
   * <p>
   * A Groovy property with this name was exposed, which leads to this lengthy
   * getter name. In Java, boolean fields are named without the "is" prefix.
   *
   * @deprecated use {@link #isRestSpecEquivalent()} instead
   */
  @Deprecated
  @ReplacedBy("restSpecEquivalent")
  public boolean getIsRestSpecEquivalent()
  {
    return isRestSpecEquivalent();
  }

  /**
   * This method is kept for backwards compatibility.
   * <p>
   * A Groovy property with this name was exposed, which leads to this lengthy
   * getter name. In Java, boolean fields are named without the "is" prefix.
   *
   * @deprecated use {@link #isRestSpecEquivalent()} instead
   */
  @Deprecated
  @ReplacedBy("restSpecEquivalent")
  public boolean isIsRestSpecEquivalent()
  {
    return isRestSpecEquivalent();
  }

  @Internal
  public boolean isRestSpecEquivalent()
  {
    return _restSpecEquivalent;
  }

  @Internal
  public String getWholeMessage()
  {
    return _wholeMessage;
  }

  /**
   * Given two file collections and an extension, find files with the same names from the previous and current files.
   * In the case that either the current or the previous is missing, it will be <pre>""</pre>.
   *
   * @param ext           The file extension.
   * @param currentFiles  Current files which are newly generated IDL or snapshot files.
   * @param previousFiles Previous files which are existing IDL or snapshot files.
   * @param diffOnly      Return only the difference between current files and previous files without files with
   *                      matching names.
   *
   * @return A list of filepath which are pairs of current file and previous file concatenated together.
   */
  List<String> findMatchingFiles(String ext, FileCollection currentFiles, FileCollection previousFiles, boolean diffOnly)
  {
    Map<String, String> currentFilenameToAbsolutePath = createMapFromFiles(currentFiles, ext);
    Map<String, String> previousFilenameToAbsolutePath = createMapFromFiles(previousFiles, ext);

    List<String> filePairs = new ArrayList<>();

    currentFilenameToAbsolutePath.forEach((filename, absolutePath) ->
    {
      if (previousFilenameToAbsolutePath.containsKey(filename))
      {
        if (!diffOnly)
        {
          //Add both files (prev, current)
          filePairs.add(previousFilenameToAbsolutePath.get(filename));
          filePairs.add(absolutePath);
        }

        previousFilenameToAbsolutePath.remove(filename);
        //remove it from the map, so that we can loop over everything left
      }
      else
      {
        // Add missing file
        filePairs.add("");
        filePairs.add(absolutePath);
      }
    });
    previousFilenameToAbsolutePath.forEach((filename, absolutePath) ->
    {
      //Add missing file
      filePairs.add(absolutePath);
      filePairs.add("");
    });

    return filePairs;
  }

  private static Map<String, String> createMapFromFiles(FileCollection currentFiles, String ext)
  {
    FileCollection files = currentFiles.filter(file -> file.getName().endsWith(ext));

    return StreamSupport.stream(files.spliterator(), false)
        .collect(Collectors.toMap(File::getName, File::getAbsolutePath,
    // TODO: Fix tasks so that the following map merge function isn't needed.
    // When this task is run against multiple modules at the same time (e.g. in
    // voyager-api for performance reasons), the same snapshot or IDL can exist
    // in multiple directories when a resource is moved. This should be fixed in
    // the IDL and snapshot generation; it will help in cache effectiveness for
    // those tasks, and in correctness if the two snapshots diverge.
            (oldFileName, newFileName) -> newFileName));
  }
}
