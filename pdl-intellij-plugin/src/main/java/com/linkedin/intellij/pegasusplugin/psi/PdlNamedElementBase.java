package com.linkedin.intellij.pegasusplugin.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public abstract class PdlNamedElementBase extends ASTWrapperPsiElement implements PdlNamedElement {
  public PdlNamedElementBase(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public String getName() {
    return getFullname().getName();
  }

  public PdlPsiFile getPdlFile() {
    return (PdlPsiFile) getContainingFile();
  }
}
