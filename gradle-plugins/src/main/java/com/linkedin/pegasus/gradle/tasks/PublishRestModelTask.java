package com.linkedin.pegasus.gradle.tasks;

import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.Internal;

import static com.linkedin.pegasus.gradle.SharedFileUtils.getSuffixedFiles;


/**
 * Check idl compatibility between current project and the api project.
 * If check succeeds and not equivalent, copy all idl files to the api project.
 * This task overwrites existing api idl files.
 *
 * As prerequisite of this task, the api project needs to be designated. There are multiple ways to do this.
 * Please refer to the documentation section for detail.
 */
@CacheableTask
public class PublishRestModelTask extends Copy
{
  private String _suffix;

  @Override
  public void copy()
  {
    if (getSource().isEmpty())
    {
      getProject().getLogger().error("No interface file is found. Skip publishing interface.");
      return;
    }

    getProject().getLogger().lifecycle("Publishing rest model to API project ...");

    FileTree apiRestModelFiles = getSuffixedFiles(getProject(), getDestinationDir(), _suffix);
    int apiRestModelFileCount = apiRestModelFiles.getFiles().size();

    super.copy();

    // FileTree is lazily evaluated, so that it scans for files only when the contents of the file tree are queried
    if (apiRestModelFileCount != 0 && apiRestModelFileCount != apiRestModelFiles.getFiles().size())
    {
      getProject().getLogger()
          .warn("{} files count changed after publish. You may have duplicate files with different names.", _suffix);
    }
  }

  @Internal
  public String getSuffix()
  {
    return _suffix;
  }

  public void setSuffix(String suffix)
  {
    _suffix = suffix;
  }
}
