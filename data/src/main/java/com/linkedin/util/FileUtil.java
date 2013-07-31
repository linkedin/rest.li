package com.linkedin.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Keren Jin
 */
public class FileUtil
{
  public static class FileExtensionFilter implements FileFilter
  {
    public FileExtensionFilter(String extension)
    {
      _extension = extension;
    }

    public boolean accept(File file)
    {
      return file.getName().endsWith(_extension);
    }

    private final String _extension;
  }

  /**
   * Scan a directory for files.
   *
   * Recursively scans a directory for files.
   * Recursive into each directory.
   * Invoke the provided filter on each non-directory file, if the
   * filter accepts the file, then add this file to the list of
   * files to return.
   *
   * @param directory provides the directory to scan for source files.
   * @param fileFilter to apply to each non-directory file.
   *                   null if no filter is applied.
   * @return list of files found in the directory.
   */
  public static List<File> listFiles(File directory, FileFilter fileFilter)
  {
    final List<File> result = new ArrayList<File>();
    final ArrayDeque<File> deque = new ArrayDeque<File>();
    deque.addFirst(directory);

    while (deque.isEmpty() == false)
    {
      File file = deque.removeFirst();
      if (file.isDirectory())
      {
        final File[] filesInDirectory = file.listFiles();
        if (filesInDirectory == null)
        {
          _log.error("Unable to list files under " + file.getPath());
        }
        else
        {
          for (File f : filesInDirectory)
          {
            deque.addLast(f);
          }
        }
      }
      else if (fileFilter == null || fileFilter.accept(file))
      {
        result.add(file);
      }
    }

    return result;
  }

  private static final Logger _log = LoggerFactory.getLogger(FileUtil.class);
}
