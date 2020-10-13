package com.linkedin.intellij.pegasusplugin.schemadoc.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;


public interface SchemadocCommentInterface extends PsiElement {
  @Nullable
  public String getComment();
}
