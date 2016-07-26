package com.linkedin.pegasus.gradle.tasks;


import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;


public class ChangedFileReportTask extends DefaultTask
{
  @InputFiles
  @SkipWhenEmpty
  FileCollection idlFiles = getProject().files();

  @InputFiles
  @SkipWhenEmpty
  FileCollection snapshotFiles = getProject().files();

  @TaskAction
  public void checkFilesForChanges(IncrementalTaskInputs inputs)
  {
    Set<String> filesRemoved = new HashSet<>();
    Set<String> filesAdded = new HashSet<>();
    Set<String> filesChanged = new HashSet<>();

    if (inputs.isIncremental())
    {
      inputs.outOfDate(inputFileDetails -> {
        if (inputFileDetails.isAdded())
        {
          filesAdded.add(inputFileDetails.getFile().getAbsolutePath());
        }

        if (inputFileDetails.isRemoved())
        {
          filesRemoved.add(inputFileDetails.getFile().getAbsolutePath());
        }

        if (inputFileDetails.isModified())
        {
          filesChanged.add(inputFileDetails.getFile().getAbsolutePath());
        }
      });

      inputs.removed(inputFileDetails -> filesRemoved.add(inputFileDetails.getFile().getAbsolutePath()));

      if (!filesRemoved.isEmpty())
      {
        String files = joinByComma(filesRemoved);
        getLogger().lifecycle("The following files have been removed, be sure to remove them from source control: {}", files);
      }

      if (!filesAdded.isEmpty())
      {
        String files = joinByComma(filesAdded);
        getLogger().lifecycle("The following files have been added, be sure to add them to source control: {}", files);
      }

      if (!filesChanged.isEmpty())
      {
        String files = joinByComma(filesChanged);
        getLogger().lifecycle(
            "The following files have been changed, be sure to commit the changes to source control: {}", files);
      }
    }
  }

  private String joinByComma(Set<String> filesRemoved)
  {
    return filesRemoved.stream().collect(Collectors.joining(", "));
  }

  public FileCollection getSnapshotFiles()
  {
    return snapshotFiles;
  }

  public void setSnapshotFiles(FileCollection snapshotFiles)
  {
    this.snapshotFiles = snapshotFiles;
  }

  public FileCollection getIdlFiles()
  {
    return idlFiles;
  }

  public void setIdlFiles(FileCollection idlFiles)
  {
    this.idlFiles = idlFiles;
  }
}
