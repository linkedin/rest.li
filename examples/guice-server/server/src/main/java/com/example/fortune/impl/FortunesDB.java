package com.example.fortune.impl;


import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;


/**
 * Very simple class that provides fortunes.
 */
@Singleton
public class FortunesDB
{
  // Create trivial db for fortunes
  static Map<Long, String> fortunes = new HashMap<>();
  static {
    fortunes.put(1L, "Today is your lucky day.");
    fortunes.put(2L, "There's no time like the present.");
    fortunes.put(3L, "Don't worry, be happy.");
  }

  public String get(Long id)
  {
    return fortunes.get(id);
  }
}
