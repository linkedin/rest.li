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

package com.linkedin.restli.example.impl;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.linkedin.restli.example.Album;

public class AlbumDatabaseImpl implements AlbumDatabase
{
  public AlbumDatabaseImpl(int numInitAlbums)
  {
    final Random r = new Random();

    // initialize some random albums at the first the resource class is loaded
    for (int i = 0; i < numInitAlbums; i++)
    {
      final long id = _currId.incrementAndGet();
      final long date = r.nextInt(Integer.MAX_VALUE);
      final Album album = new Album()
          .setId(id)
          .setUrn(String.valueOf(id))
          .setTitle("Awesome Album #" + id)
          .setCreationTime(date);

      _data.put(album.getId(), album);
    }
  }

  @Override
  public Long getCurrentId()
  {
    return _currId.incrementAndGet();
  }

  @Override
  public Map<Long, Album> getData()
  {
    return _data;
  }

  // Although a new resource instance is created for each request/response session,
  // the database variables are set through dependency injection. Thus, the underlying
  // database instances and hash maps are the same for all sessions.
  // These shared variables need synchronization for consistency.
  private final AtomicLong       _currId = new AtomicLong();
  private final Map<Long, Album> _data   = new ConcurrentHashMap<>();
}
