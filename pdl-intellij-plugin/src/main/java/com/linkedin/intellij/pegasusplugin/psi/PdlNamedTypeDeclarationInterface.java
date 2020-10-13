package com.linkedin.intellij.pegasusplugin.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;


public interface PdlNamedTypeDeclarationInterface extends PsiElement {
  @Nullable
  String getDocumentation();

  @Nullable
  PdlAnnotations getAnnotations();

  boolean isDeprecated();

  /**
   * Safely gets the type name in this type declaration, if it exists.
   * @return type name declaration
   */
  @Nullable
  PdlTypeNameDeclaration getTypeNameDeclaration();
}
