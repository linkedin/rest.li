package com.example.fortune.impl;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component("fortunesBean")
public class FortunesBean {

  static Map<Long, String> fortunes = new HashMap<>();
  static {
    fortunes.put(1L, "Today is your lucky day.");
    fortunes.put(2L, "There's no time like the present. There's no time like the present. There's no time like the present. There's no time like the present.");
    fortunes.put(3L, "Don't worry, be happy.");
  }

  public String getFortune(Long id)
  {
    return fortunes.get(id);
  }
}
