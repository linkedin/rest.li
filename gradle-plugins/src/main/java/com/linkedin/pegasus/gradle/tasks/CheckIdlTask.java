package com.linkedin.pegasus.gradle.tasks;

import com.linkedin.pegasus.gradle.IOUtil;
import com.linkedin.pegasus.gradle.PathingJarUtil;
import com.linkedin.pegasus.gradle.PegasusPlugin;
import com.linkedin.pegasus.gradle.internal.ArgumentFileGenerator;
import com.linkedin.pegasus.gradle.internal.CompatibilityLogChecker;
import com.linkedin.pegasus.gradle.internal.FileExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
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
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;


@CacheableTask
public class CheckIdlTask extends DefaultTask
{
  private final FileExtensionFilter _idlFilter = new FileExtensionFilter(PegasusPlugin.IDL_FILE_SUFFIX);

  private FileCollection _currentIdlFiles;
  private File _previousIdlDirectory;
  private FileCollection _resolverPath;
  private FileCollection _codegenClasspath;
  private String _idlCompatLevel;
  private File _summaryTarget = new File(getProject().getBuildDir(), "reports/checkIdl/summary.txt");

  private boolean _modelCompatible = true;
  private boolean _restSpecCompatible = true;
  private boolean _equivalent = true;
  private String _wholeMessage = "";
  private boolean _enableArgFile;

  @TaskAction
  public void check()
  {
    getProject().getLogger().info("Checking interface compatibility with API ...");
    List<String> errorFilePairs = findErrorFilePairs();
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
      String resolverPathArg = _resolverPath.getAsPath();
      if (isEnableArgFile())
      {
        resolverPathArg = ArgumentFileGenerator.getArgFileSyntax(ArgumentFileGenerator.createArgFile(
            "checkIdl_resolverPath", Collections.singletonList(resolverPathArg), getTemporaryDir()));
      }
      javaExecSpec.setMain("com.linkedin.restli.tools.idlcheck.RestLiResourceModelCompatibilityChecker");
      javaExecSpec.setClasspath(_pathedCodegenClasspath);
      javaExecSpec.jvmArgs("-Dgenerator.resolver.path=" + resolverPathArg);
      javaExecSpec.args("--compat", _idlCompatLevel);
      javaExecSpec.args("--report");
      javaExecSpec.args(errorFilePairs);
      javaExecSpec.setStandardOutput(logChecker);
    });

    _modelCompatible = logChecker.isModelCompatible();
    _restSpecCompatible = logChecker.isRestSpecCompatible();
    _equivalent = logChecker.getModelCompatibility().isEmpty() && logChecker.getRestSpecCompatibility().isEmpty();
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
  public FileCollection getResolverPath()
  {
    return _resolverPath;
  }

  public void setResolverPath(FileCollection resolverPath)
  {
    _resolverPath = resolverPath;
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
  public String getIdlCompatLevel()
  {
    return _idlCompatLevel;
  }

  public void setIdlCompatLevel(String idlCompatLevel)
  {
    _idlCompatLevel = idlCompatLevel;
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
  @Internal
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
  @Internal
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
  @Internal
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
  @Internal
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
  @Internal
  public boolean getIsEquivalent()
  {
    return _equivalent;
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
  @Internal
  public boolean isIsEquivalent()
  {
    return _equivalent;
  }

  @Internal
  public boolean isEquivalent()
  {
    return _equivalent;
  }

  @Internal
  public String getWholeMessage()
  {
    return _wholeMessage;
  }

  @Input
  public boolean isEnableArgFile()
  {
    return _enableArgFile;
  }

  public void setEnableArgFile(boolean enable)
  {
    _enableArgFile = enable;
  }

  private List<String> findErrorFilePairs()
  {
    List<String> errorFilePairs = new ArrayList<>();

    Set<String> apiExistingFilePaths = Arrays.stream(_previousIdlDirectory.listFiles(_idlFilter))
        .map(File::getAbsolutePath)
        .collect(Collectors.toSet());

    for (File currentIdlFile : _currentIdlFiles)
    {
      String expectedOldFilePath = _previousIdlDirectory.getPath() + File.separatorChar + currentIdlFile.getName();
      File expectedFile = getProject().file(expectedOldFilePath);
      if (expectedFile.exists())
      {
        apiExistingFilePaths.remove(expectedOldFilePath);
        errorFilePairs.add(expectedFile.getAbsolutePath());
        errorFilePairs.add(currentIdlFile.getPath());
      }
      else
      {
        // found new file that has no matching old file
        errorFilePairs.add("");
        errorFilePairs.add(currentIdlFile.getPath());
      }
    }

    for (String apiExistingFilePath : apiExistingFilePaths) {
      errorFilePairs.add(apiExistingFilePath);
      errorFilePairs.add("");
    }

    return errorFilePairs;
  }
}
