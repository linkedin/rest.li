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
    // initialize numInitPhotos random photo at the first the resource class is loaded
    for (int i = 0; i < numInitPhotos; i++)
    {
      final long id = _currId.incrementAndGet();
      final PhotoFormats[] formats = PhotoFormats.values();

      final Random r = new Random();

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
  private final Map<Long, Photo> _data = new ConcurrentHashMap<Long, Photo>();
}
