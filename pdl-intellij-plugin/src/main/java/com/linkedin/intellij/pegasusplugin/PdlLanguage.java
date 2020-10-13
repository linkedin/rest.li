package com.linkedin.intellij.pegasusplugin;

import com.intellij.lang.Language;


public class PdlLanguage extends Language {
  public static final PdlLanguage INSTANCE = new PdlLanguage();

  private PdlLanguage() {
    super("Pdl");
  }
}
