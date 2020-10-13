package com.linkedin.intellij.pegasusplugin.schemadoc;

import com.intellij.lang.Language;

public class SchemadocLanguage extends Language {
  public static final SchemadocLanguage INSTANCE = new SchemadocLanguage();

  private SchemadocLanguage() {
    super("PdlDocComments");
  }
}
