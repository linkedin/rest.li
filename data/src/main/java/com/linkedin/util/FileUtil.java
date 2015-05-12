package com.linkedin.util;


import java.io.File;
import java.io.FileFilter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Keren Jin
 */
public class FileUtil
{
  private static final Logger _log = LoggerFactory.getLogger(FileUtil.class);

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

  public static String buildSystemIndependentPath(String ... pathPieces)
  {
    char separtor = File.separatorChar;
    StringBuilder sb = new StringBuilder();
    for (String piece: pathPieces)
    {
      sb.append(separtor);
      sb.append(piece);
    }
    return sb.toString();
  }

  public static String removeFileExtension(String filename)
  {
    final int idx = filename.lastIndexOf('.');
    if (idx == 0)
    {
      return filename;
    }
    else
    {
      return filename.substring(0, idx);
    }
  }

  /**
   * Whether the files that would be generated into the specified target directory are more recent than the most recent source files.
   * <p/>
   * This used to check if the output file is already up-to-date and need not be overwritten with generated output.
   *
   * @param sourceFiles provides the source files that were parsed.
   * @param targetFiles provides the files that would have been generated.
   *
   * @return true if the files that would be generated are more recent than the most recent source files.
   */
  public static boolean upToDate(Collection<File> sourceFiles, Collection<File> targetFiles)
  {
    final long sourceLastModified = mostRecentLastModified(sourceFiles);
    return filesLastModifiedMoreRecentThan(targetFiles, sourceLastModified);
  }

  /**
   * Compute the most recent last modified time of the provided files.
   *
   * @param files to compute most recent modified time from.
   *
   * @return the most resent last modified of the provided files.
   */
  private static long mostRecentLastModified(Collection<File> files)
  {
    long mostRecent = 0L;
    for (File file : files)
    {
      final long fileLastModified = file.lastModified();
      if (mostRecent < fileLastModified)
      {
        mostRecent = fileLastModified;
      }
    }
    return mostRecent;
  }

  /**
   * Determine whether the provided files has been modified more recently than the provided time.
   *
   * @param files whose last modified times will be compared to provided time.
   * @param time  to compare the files' last modified times to.
   *
   * @return true if the provided files has been modified more recently than the provided time.
   */
  private static boolean filesLastModifiedMoreRecentThan(Collection<File> files, long time)
  {
    for (File file : files)
    {
      if (!file.exists() || time >= file.lastModified())
      {
        return false;
      }
    }
    return true;
  }
}
