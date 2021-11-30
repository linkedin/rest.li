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

import com.linkedin.restli.example.EXIF;
import com.linkedin.restli.example.LatLong;
import com.linkedin.restli.example.Photo;
import com.linkedin.restli.example.PhotoFormats;

public class PhotoDatabaseImpl implements PhotoDatabase
{
  public PhotoDatabaseImpl(int numInitPhotos)
  {
    final Random r = new Random();

    // initialize some random photo at the first the resource class is loaded
    for (int i = 0; i < numInitPhotos; i++)
    {
      final long id = _currId.incrementAndGet();
      final PhotoFormats[] formats = PhotoFormats.values();

      final LatLong ll = new LatLong().setLatitude(r.nextFloat() * 180 - 90).setLongitude(r.nextFloat() * 180 - 90);
      final EXIF e = new EXIF().setLocation(ll);
      final Photo p = new Photo().setId(id)
          .setUrn(String.valueOf(id))
          .setTitle("Photo " + id)
          .setFormat(formats[r.nextInt(formats.length)])
          .setExif(e);

      _data.put(p.getId(), p);
    }
  }

  @Override
  public Long getCurrentId()
  {
    return _currId.incrementAndGet();
  }

  @Override
  public Map<Long, Photo> getData()
  {
    return _data;
  }

  // Although a new resource instance is created for each request/response session,
  // the database variables are set through dependency injection. Thus, the underlying
  // database instances and hash maps are the same for all sessions.
  // These shared variables need synchronization for consistency.
  private final AtomicLong _currId = new AtomicLong();
  private final Map<Long, Photo> _data = new ConcurrentHashMap<>();
}
