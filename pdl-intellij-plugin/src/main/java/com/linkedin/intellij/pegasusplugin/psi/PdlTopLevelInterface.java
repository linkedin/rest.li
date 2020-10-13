package com.linkedin.intellij.pegasusplugin.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;


public interface PdlTopLevelInterface extends PsiElement {

  @Nullable
  String getNamedTypeDocumentation();
}
