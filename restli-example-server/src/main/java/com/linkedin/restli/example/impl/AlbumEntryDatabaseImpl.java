package com.linkedin.restli.example.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.example.AlbumEntry;

public class AlbumEntryDatabaseImpl implements AlbumEntryDatabase
{
  @Override
  public Map<CompoundKey, AlbumEntry> getData()
  {
    return _data;
  }

  // Although a new resource instance is created for each request/response session,
  // the database variables are set through dependency injection. Thus, the underlying
  // database instances and hash maps are the same for all sessions.
  // These shared variables need synchronization for consistency.
  private final Map<CompoundKey, AlbumEntry> _data =
                                                       new ConcurrentHashMap<CompoundKey, AlbumEntry>();
}