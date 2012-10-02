package com.linkedin.restli.example.impl;

import java.util.Map;

import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.example.AlbumEntry;

public interface AlbumEntryDatabase
{
  public Map<CompoundKey, AlbumEntry> getData();
}