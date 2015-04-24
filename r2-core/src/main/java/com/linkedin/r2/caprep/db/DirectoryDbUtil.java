/*
   Copyright (c) 2012 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

/* $Id$ */
package com.linkedin.r2.caprep.db;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * Utilities for Directory-based {@link DbSink} and {@link DbSource} implementations.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class DirectoryDbUtil
{
  private static final String REQ_SUFFIX = ".req";
  private static final String RES_SUFFIX = ".res";
  private static final String REST_SUFFIX = ".rest";
  private static final String RPC_SUFFIX = ".rpc";

  /**
   * Create the request file name for a request message.
   *
   * @param dir the directory of the message store.
   * @param id the id of the message.
   * @return the {@link File} object for the request file.
   */
  public static File requestFileName(File dir, String id)
  {
    return new File(dir.getAbsolutePath() + File.separator + id + REQ_SUFFIX);
  }

  /**
   * Create the RPC request file name for a message.
   *
   * @param dir the directory of the message store.
   * @param id the id of the message.
   * @return the {@link File} object for the request file.
   */
  @Deprecated
  public static File rpcRequestFileName(File dir, int id)
  {
    return new File(dir.getAbsolutePath() + File.separator + id + RPC_SUFFIX + REQ_SUFFIX);
  }

  /**
   * Create the REST request file name for a message.
   *
   * @param dir the directory of the message store.
   * @param id the id of the message.
   * @return the {@link File} object for the request file.
   */
  public static File restRequestFileName(File dir, int id)
  {
    return new File(dir.getAbsolutePath() + File.separator + id + REST_SUFFIX + REQ_SUFFIX);
  }

  /**
   * Create the response file name for a request message.
   *
   * @param dir the directory of the message store.
   * @param id the id of the message.
   * @return the {@link File} object for the response file.
   */
  public static File responseFileName(File dir, String id)
  {
    return new File(dir.getAbsolutePath() + File.separator + id + RES_SUFFIX);
  }

  /**
   * Create the RPC response file name for a message.
   *
   * @param dir the directory of the message store.
   * @param id the id of the message.
   * @return the {@link File} object for the response file.
   */
  @Deprecated
  public static File rpcResponseFileName(File dir, int id)
  {
    return new File(dir.getAbsolutePath() + File.separator + id + RPC_SUFFIX + RES_SUFFIX);
  }

  /**
   * Create the REST response file name for a message.
   *
   * @param dir the directory of the message store.
   * @param id the id of the message.
   * @return the {@link File} object for the response file.
   */
  public static File restResponseFileName(File dir, int id)
  {
    return new File(dir.getAbsolutePath() + File.separator + id + REST_SUFFIX + RES_SUFFIX);
  }

  /**
   * Return a list of all available request ids for a given directory.
   *
   * @param dir the directory of the message store.
   * @return a list of available request ids, as a string array.
   * @throws IOException
   */
  public static String[] listRequestIds(File dir) throws IOException
  {
    final File[] files = dir.listFiles(new FileFilter() {
      @Override
      public boolean accept(File pathname)
      {
        final String name = pathname.getName();

        return pathname.isFile() &&
               name.endsWith(REQ_SUFFIX) &&
               (stripSuffix(name, REQ_SUFFIX).endsWith(REST_SUFFIX) ||
                stripSuffix(name, REQ_SUFFIX).endsWith(RPC_SUFFIX));
      }
    });

    if (files == null)
    {
      throw new IOException("Path is not a directory or an IO error occurred while reading: " + dir);
    }

    final String[] ids = new String[files.length];
    for (int i = 0; i < ids.length; i++)
    {
      final String name = files[i].getName();
      ids[i] = stripSuffix(name, REQ_SUFFIX);
    }

    return ids;
  }

  /**
   * Return the integer index portion of a given id.
   *
   * @param id the string id for which the index should be obtained.
   * @return the integer id of the given string id.
   */
  public static int getIndex(String id)
  {
    // Remove all suffixes
    final int dotIndex = id.indexOf('.');
    if (dotIndex != -1)
    {
      id = id.substring(0, dotIndex);
    }
    return Integer.parseInt(id);
  }

  private static String stripSuffix(String str, String suffix)
  {
    return str.substring(0, str.length() - suffix.length());
  }
}
