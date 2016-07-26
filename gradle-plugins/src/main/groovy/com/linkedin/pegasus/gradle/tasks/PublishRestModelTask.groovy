package com.linkedin.pegasus.gradle.tasks;


import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.Copy;

import static com.linkedin.pegasus.gradle.SharedFileUtils.getSuffixedFiles;


/**
 * Check idl compatibility between current project and the api project.
 * If check succeeds and not equivalent, copy all idl files to the api project.
 * This task overwrites existing api idl files.
 *
 * As prerequisite of this task, the api project needs to be designated. There are multiple ways to do this.
 * Please refer to the documentation section for detail.*/
class PublishRestModelTask extends Copy
{
  String suffix

  @Override
  protected void copy()
  {
    if (source.empty)
    {
      project.logger.error('No interface file is found. Skip publishing interface.')
      return
    }

    project.logger.lifecycle('Publishing rest model to API project ...')

    final FileTree apiRestModelFiles = getSuffixedFiles(project, destinationDir, suffix)
    final int apiRestModelFileCount = apiRestModelFiles.files.size()

    super.copy()

    // FileTree is lazily evaluated, so that it scans for files only when the contents of the file tree are queried
    if (apiRestModelFileCount != 0 && apiRestModelFileCount != apiRestModelFiles.files.size())
    {
      project.logger.warn(suffix + ' files count changed after publish. You may have duplicate files with different names.')
    }
  }
}
