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
  private final Map<Long, Album> _data   = new ConcurrentHashMap<Long, Album>();
}
