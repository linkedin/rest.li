package com.linkedin.intellij.pegasusplugin;

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler;
import com.intellij.psi.TokenType;
import com.linkedin.intellij.pegasusplugin.psi.Types;

public class PdlQuoteHandler extends SimpleTokenSetQuoteHandler {
  public PdlQuoteHandler() {
    super(Types.STRING, TokenType.BAD_CHARACTER);
  }
}
