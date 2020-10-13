package com.linkedin.intellij.pegasusplugin;

import com.intellij.lexer.FlexAdapter;

public class PdlLexerAdapter extends FlexAdapter {
  public PdlLexerAdapter() {
    super(new PdlLexer(null));
  }
}
