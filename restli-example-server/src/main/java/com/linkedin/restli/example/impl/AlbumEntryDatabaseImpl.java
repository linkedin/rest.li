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

import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.example.AlbumEntry;

public class AlbumEntryDatabaseImpl implements AlbumEntryDatabase
{
  public AlbumEntryDatabaseImpl(PhotoDatabase photoDb, int numInitEntries)
  {
    final int photoCount = photoDb.getData().size();
    final Random r = new Random();

    final long randPhotoId = (long) r.nextInt(photoCount - numInitEntries) + 1;
    for (int i = 0; i < numInitEntries; ++i)
    {
      final long photoId = randPhotoId + i;
      final long albumId = 7;

      final CompoundKey key = new CompoundKey();
      key.append("photoId", photoId);
      key.append("albumId", albumId);
      final AlbumEntry entry = new AlbumEntry();
      entry.setPhotoId(photoId);
      entry.setAlbumId(albumId);
      entry.setAddTime(r.nextInt(Integer.MAX_VALUE));
      _data.put(key, entry);
    }
  }

  @Override
  public Map<CompoundKey, AlbumEntry> getData()
  {
    return _data;
  }

  // Although a new resource instance is created for each request/response session,
  // the database variables are set through dependency injection. Thus, the underlying
  // database instances and hash maps are the same for all sessions.
  // These shared variables need synchronization for consistency.
  private final Map<CompoundKey, AlbumEntry> _data = new ConcurrentHashMap<>();
}
