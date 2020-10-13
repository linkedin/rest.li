package com.linkedin.intellij.pegasusplugin.psi;

import com.intellij.psi.PsiElement;


public interface PdlEnumSymbolInterface extends PsiElement {
  PdlEnumSymbolDeclaration getEnumSymbolDeclaration();
  boolean isDeprecated();
}
