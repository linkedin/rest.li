package com.linkedin.intellij.pegasusplugin;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.psi.PsiElement;
import com.linkedin.intellij.pegasusplugin.psi.PdlNamedElement;
import com.linkedin.intellij.pegasusplugin.psi.PdlTypeNameDeclaration;


public class PdlRefactoringSupportProvider extends RefactoringSupportProvider {
  @Override
  public boolean isMemberInplaceRenameAvailable(PsiElement element, PsiElement context) {
    return element instanceof PdlNamedElement || element instanceof PdlTypeNameDeclaration;
  }

  /* TODO: http://www.jetbrains.org/intellij/sdk/docs/reference_guide/custom_language_support/safe_delete_refactoring.html
  @Override
  public boolean isSafeDeleteAvailable(@NotNull PsiElement element) {
    return element instanceof PdlNamedElement || element instanceof PdlTypeNameDeclaration;
  }
  */
}
