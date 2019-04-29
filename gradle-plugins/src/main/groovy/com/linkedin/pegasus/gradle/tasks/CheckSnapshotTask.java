package com.linkedin.pegasus.gradle.tasks;

import com.linkedin.pegasus.gradle.IOUtil;
import com.linkedin.pegasus.gradle.PegasusPlugin;
import com.linkedin.pegasus.gradle.internal.CompatibilityLogChecker;
import com.linkedin.pegasus.gradle.internal.FileExtensionFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;


@CacheableTask
public class CheckSnapshotTask extends DefaultTask
{
  private static final FileExtensionFilter SNAPSHOT_FILTER =
      new FileExtensionFilter(PegasusPlugin.SNAPSHOT_FILE_SUFFIX);

  private FileCollection _currentSnapshotFiles;
  private File _previousSnapshotDirectory;
  private FileCollection _codegenClasspath;
  private String _snapshotCompatLevel;
  private File _summaryTarget = new File(getProject().getBuildDir(), "reports/checkSnapshot/summary.txt");

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
    checkSnapshotCompatibility(getProject(), _currentSnapshotFiles, _previousSnapshotDirectory,
        SNAPSHOT_FILTER, argFiles);

    if (argFiles.isEmpty())
    {
      return;
    }

    CompatibilityLogChecker logChecker = new CompatibilityLogChecker();

    getProject().javaexec(javaExecSpec -> {
      javaExecSpec.setMain("com.linkedin.restli.tools.snapshot.check.RestLiSnapshotCompatibilityChecker");
      javaExecSpec.setClasspath(_codegenClasspath);
      javaExecSpec.args("--compat", _snapshotCompatLevel);
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
  public File getPreviousSnapshotDirectory()
  {
    return _previousSnapshotDirectory;
  }

  public void setPreviousSnapshotDirectory(File previousSnapshotDirectory)
  {
    _previousSnapshotDirectory = previousSnapshotDirectory;
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
  public String getSnapshotCompatLevel()
  {
    return _snapshotCompatLevel;
  }

  public void setSnapshotCompatLevel(String snapshotCompatLevel)
  {
    _snapshotCompatLevel = snapshotCompatLevel;
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

  private void checkSnapshotCompatibility(Project project,
                                          FileCollection currentFiles,
                                          File previousDirectory,
                                          FileExtensionFilter filter,
                                          List<String> fileArgs)
  {

    boolean isCheckRestSpecVsSnapshot = filter.getSuffix().equals(PegasusPlugin.IDL_FILE_SUFFIX);

    for (File currentFile : currentFiles)
    {
      getProject().getLogger().info("Checking interface file: {}", currentFile.getPath());

      String apiFilename;
      if (isCheckRestSpecVsSnapshot)
      {
        String fileName = currentFile.getName().substring(0,
            currentFile.getName().length() - PegasusPlugin.SNAPSHOT_FILE_SUFFIX.length());

        apiFilename = fileName + PegasusPlugin.IDL_FILE_SUFFIX;
      }
      else
      {
        apiFilename = currentFile.getName();
      }
      String apiFilePath = previousDirectory.getPath() + File.separatorChar + apiFilename;
      File apiFile = project.file(apiFilePath);
      if (apiFile.exists())
      {
        fileArgs.add(apiFilePath);
        fileArgs.add(currentFile.getPath());
      }
    }
  }
}
