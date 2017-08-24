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

package com.linkedin.d2.discovery.stores.file;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.util.FileSystemDirectory;
import com.linkedin.d2.discovery.PropertySerializationException;
import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.d2.discovery.event.PropertyEventSubscriber;
import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.d2.discovery.util.Stats;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.discovery.util.LogUtil.error;
import static com.linkedin.d2.discovery.util.LogUtil.info;
import static com.linkedin.d2.discovery.util.LogUtil.warn;

/**
 * The following class provides the data structure to write props-values on disk
 *
 * It has implements a global (non-per-prop) fair read-write lock to access the database,
 * allowing multiple reads at the same time.
 */
public class FileStore<T> implements PropertyStore<T>, PropertyEventSubscriber<T>
{
  private static final Logger         _log = LoggerFactory.getLogger(FileStore.class);
  private static final String         TMP_FILE_PREFIX = "d2-";

  private final String _fsPath;
  private final String _fsFileExtension;
  private final PropertySerializer<T> _serializer;
  private final Stats _getStats;
  private final Stats _putStats;
  private final Stats _removeStats;

  private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock(true);
  private final Lock r = rwl.readLock();
  private final Lock w = rwl.writeLock();

  public FileStore(String fsPath, PropertySerializer<T> serializer)
  {
    this(fsPath, FileSystemDirectory.FILE_STORE_EXTENSION, serializer);
  }

  public FileStore(String fsPath, String fsFileExtension, PropertySerializer<T> serializer)
  {
    _getStats = new Stats(60000);
    _putStats = new Stats(60000);
    _removeStats = new Stats(60000);
    _fsPath = fsPath;
    _fsFileExtension = fsFileExtension;
    _serializer = serializer;

    File file = new File(_fsPath);

    if (!file.exists())
    {
      if (!file.mkdirs())
      {
        error(_log, "unable to create file path: " + _fsPath);
      }
    }
  }

  @Override
  public void start(Callback<None> callback)
  {
    if (start())
    {
      callback.onSuccess(None.none());
    }
    else
    {
      callback.onError(new IOException("unable to create file path: " + _fsPath));
    }
  }

  public boolean start()
  {
    File file = new File(_fsPath);
    w.lock();
    try
    {
      if (!file.exists() || !file.isDirectory())
      {
        return file.mkdirs();
      }
    }
    finally
    {
      w.unlock();
    }
    return true;
  }

  @Override
  public T get(String listenTo)
  {
    r.lock();
    try
    {
      _getStats.inc();

      File file = getFile(listenTo);

      if (file.exists())
      {
        try
        {
          byte content[] = new byte[(int) file.length()];
          int offset = 0;
          int read = 0;
          int length = (int) file.length();
          FileInputStream inputStream = new FileInputStream(file);

          while ((read = inputStream.read(content, offset, length - offset)) > 0)
          {
            offset += read;
          }

          inputStream.close();

          return _serializer.fromBytes(content);
        }
        catch (IOException e)
        {
          _log.error("Error reading file: " + file.getAbsolutePath(), e);
        }
        catch (PropertySerializationException e)
        {
          _log.error("Error deserializing property " + listenTo + " for file " + file.getAbsolutePath(), e);
        }
      }

      warn(_log, "file didn't exist on get: ", file);

      return null;
    }
    finally
    {
      r.unlock();
    }
  }

  public Map<String, T> getAll()
  {
    r.lock();
    List<String> props;
    try
    {
      props = FileSystemDirectory.getFileListWithoutExtension(_fsPath);

      Map<String, T> result = new HashMap<>();
      for (String prop : props)
      {
        result.put(prop, get(prop));
      }
      return result;
    }
    finally
    {
      r.unlock();
    }
  }

  @Override
  public void put(String listenTo, T discoveryProperties)
  {
    w.lock();
    try
    {
      if (discoveryProperties == null)
      {
        warn(_log, "received a null property for resource ", listenTo, " received a null property");
      }
      else
      {
        _putStats.inc();

        File file = getFile(listenTo);
        try
        {
          file.createNewFile();

          FileOutputStream outputStream = new FileOutputStream(file,false);

          outputStream.write(_serializer.toBytes(discoveryProperties));
          outputStream.close();
        }
        catch (FileNotFoundException e)
        {
          error(_log, "unable to find file on put: ", file);
        }
        catch (IOException e)
        {
          error(_log, "unable to read file on put: ", file);
        }
      }
    }
    finally
    {
      w.unlock();
    }
  }

  @Override
  public void remove(String listenTo)
  {
    w.lock();
    try
    {
      _removeStats.inc();

      File file = getFile(listenTo);

      if (file.exists())
      {
        file.delete();
      }
      else
      {
        warn(_log, "file didn't exist on remove: ", file);
      }
    }
    finally
    {
      w.unlock();
    }
  }

  public boolean removeDirectory()
  {
    w.lock();
    try
    {
      return FileStore.removeDirectory(_fsPath);
    }
    finally
    {
      w.unlock();
    }
  }

  public static boolean removeDirectory(String fsPath)
  {
    try
    {
      File file = new File(fsPath);
      try
      {
        FileUtils.deleteDirectory(file);
        return true;
      }
      catch (IOException e)
      {
        _log.info("Couldn't remove directory: " + fsPath, e);
        return false;
      }
    }
    finally
    {
    }
  }

  @Override
  public void onAdd(String propertyName, T propertyValue)
  {
    put(propertyName, propertyValue);
  }

  @Override
  public void onInitialize(String propertyName, T propertyValue)
  {
    put(propertyName, propertyValue);
  }

  @Override
  public void onRemove(String propertyName)
  {
    remove(propertyName);
  }

  private File getFile(String listenTo)
  {
    return new File(_fsPath + File.separatorChar + listenTo + _fsFileExtension);
  }

  @Override
  public void shutdown(Callback<None> shutdown)
  {
    info(_log, "shutting down");

    shutdown.onSuccess(None.none());
  }

  public String getPath()
  {
    return _fsPath;
  }

  public PropertySerializer<T> getSerializer()
  {
    return _serializer;
  }

  public long getGetCount()
  {
    return _getStats.getCount();
  }

  public long getPutCount()
  {
    return _putStats.getCount();
  }

  public long getRemoveCount()
  {
    return _removeStats.getCount();
  }
}
