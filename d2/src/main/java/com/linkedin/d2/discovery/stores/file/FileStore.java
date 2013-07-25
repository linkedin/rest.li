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

import static com.linkedin.d2.discovery.util.LogUtil.debug;
import static com.linkedin.d2.discovery.util.LogUtil.error;
import static com.linkedin.d2.discovery.util.LogUtil.info;
import static com.linkedin.d2.discovery.util.LogUtil.warn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.linkedin.d2.discovery.PropertySerializationException;
import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.d2.discovery.event.PropertyEventSubscriber;
import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEventShutdownCallback;
import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.d2.discovery.util.Stats;

public class FileStore<T> implements PropertyStore<T>, PropertyEventSubscriber<T>
{
  private static final Logger         _log = LoggerFactory.getLogger(FileStore.class);

  private final String                _path;
  private final String                _extension;
  private final PropertySerializer<T> _serializer;
  private final Stats                 _getStats;
  private final Stats                 _putStats;
  private final Stats                 _removeStats;

  public FileStore(String path, String extension, PropertySerializer<T> serializer)
  {
    _getStats = new Stats(60000);
    _putStats = new Stats(60000);
    _removeStats = new Stats(60000);
    _path = path;
    _extension = extension;
    _serializer = serializer;

    File file = new File(_path);

    if (!file.exists())
    {
      if (!file.mkdirs())
      {
        error(_log, "unable to create file path: " + _path);
      }
    }
  }

  @Override
  public void start(Callback<None> callback)
  {
    File file = new File(_path);
    if (!file.exists())
    {
      if (!file.mkdirs())
      {
        callback.onError(new IOException("unable to create file path: " + _path));
      }
      else
      {
        callback.onSuccess(None.none());
      }
    }
  }

  @Override
  public T get(String listenTo)
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

  @Override
  public void put(String listenTo, T discoveryProperties)
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
        File tempFile = getTempFile(listenTo);
        FileOutputStream outputStream = new FileOutputStream(tempFile);

        outputStream.write(_serializer.toBytes(discoveryProperties));
        outputStream.close();

        if (!tempFile.renameTo(file))
        {
          error(_log, "unable to move temp file ", tempFile, " to ", file);
        }
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

  @Override
  public void remove(String listenTo)
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
    return new File(_path + File.separatorChar + listenTo + _extension);
  }

  private File getTempFile(String listenTo) throws IOException
  {
    return File.createTempFile(listenTo, "tmp", new File(_path));
  }

  @Override
  public void shutdown(PropertyEventShutdownCallback shutdown)
  {
    info(_log, "shutting down");

    shutdown.done();
  }

  public String getPath()
  {
    return _path;
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
