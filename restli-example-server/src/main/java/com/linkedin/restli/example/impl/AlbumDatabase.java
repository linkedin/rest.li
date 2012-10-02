package com.linkedin.restli.example.impl;

import java.util.Map;

import com.linkedin.restli.example.Album;

public interface AlbumDatabase
{
  public Long getCurrentId();

  public Map<Long, Album> getData();
}