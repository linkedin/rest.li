package com.linkedin.restli.example.impl;

import com.linkedin.restli.example.Photo;
import java.util.Map;

public interface PhotoDatabase
{
  public Long getCurrentId();
  public Map<Long, Photo> getData();
}
