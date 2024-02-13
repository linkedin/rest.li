package com.linkedin.pegasus.gradle.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.ChangeType;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


public abstract class ChangedFileReportTask extends DefaultTask
{
  private final Collection<String> _needCheckinFiles = new ArrayList<>();

  public ChangedFileReportTask()
  {
    //with Gradle 6.0, Declaring an incremental task without outputs is not allowed.
    getOutputs().upToDateWhen(Specs.satisfyNone());
  }

  @TaskAction
  public void checkFilesForChanges(InputChanges inputs)
  {
    getLogger().lifecycle("Checking idl and snapshot files for changes...");
    getLogger().info("idlFiles: " + getIdlFiles().getAsPath());
    getLogger().info("snapshotFiles: " + getSnapshotFiles().getAsPath());

    Set<String> filesRemoved = new HashSet<>();
    Set<String> filesAdded = new HashSet<>();
    Set<String> filesChanged = new HashSet<>();

    if (inputs.isIncremental())
    {
      for (FileCollection fileCollection : Arrays.asList(getIdlFiles(), getSnapshotFiles())) {
        inputs.getFileChanges(fileCollection).forEach(inputFileDetails -> {
          if (inputFileDetails.getChangeType().equals(ChangeType.ADDED))
          {
            filesAdded.add(inputFileDetails.getFile().getAbsolutePath());
          }

          if (inputFileDetails.getChangeType().equals(ChangeType.REMOVED))
          {
            filesRemoved.add(inputFileDetails.getFile().getAbsolutePath());
          }

          if (inputFileDetails.getChangeType().equals(ChangeType.MODIFIED))
          {
            filesChanged.add(inputFileDetails.getFile().getAbsolutePath());
          }
        });
      }

      if (!filesRemoved.isEmpty())
      {
        String files = joinByComma(filesRemoved);
        _needCheckinFiles.add(files);
        getLogger().lifecycle(
            "The following files have been removed, be sure to remove them from source control: {}", files);
      }

      if (!filesAdded.isEmpty())
      {
        String files = joinByComma(filesAdded);
        _needCheckinFiles.add(files);
        getLogger().lifecycle("The following files have been added, be sure to add them to source control: {}", files);
      }

      if (!filesChanged.isEmpty())
      {
        String files = joinByComma(filesChanged);
        _needCheckinFiles.add(files);
        getLogger().lifecycle(
            "The following files have been changed, be sure to commit the changes to source control: {}", files);
      }
    }
  }

  private String joinByComma(Set<String> files)
  {
    return files.stream().collect(Collectors.joining(", "));
  }

  @InputFiles
  @Incremental
  public abstract ConfigurableFileCollection getSnapshotFiles();

  @InputFiles
  @Incremental
  public abstract ConfigurableFileCollection getIdlFiles();

  @Internal
  public Collection<String> getNeedCheckinFiles()
  {
    return _needCheckinFiles;
  }
}
