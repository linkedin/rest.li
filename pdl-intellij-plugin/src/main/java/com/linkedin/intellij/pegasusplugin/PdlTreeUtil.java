package com.linkedin.intellij.pegasusplugin;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.linkedin.intellij.pegasusplugin.psi.PdlNamespace;
import com.linkedin.intellij.pegasusplugin.psi.PdlPsiFile;
import com.linkedin.intellij.pegasusplugin.psi.PdlScopedTypeNameDeclaration;


/**
 * Utility for finding useful information in a PDL grammar tree.
 */
public class PdlTreeUtil {
  /**
   * Gets the namespace at a given element in the PSI tree. Takes overridden ("scoped") namespaces into account.
   * @param element the element at which to find the namespace.
   * @return namespace at the given element
   */
  public static PdlNamespace getNamespaceAtElement(PsiElement element) {
    // First, attempt to find overridden "scoped" namespace
    PdlScopedTypeNameDeclaration
        scopedTypeNameDeclaration = PsiTreeUtil.getParentOfType(element, PdlScopedTypeNameDeclaration.class);

    if (scopedTypeNameDeclaration != null && scopedTypeNameDeclaration.getNamespaceDeclaration() != null) {
      return scopedTypeNameDeclaration.getNamespaceDeclaration().getNamespace();
    }

    // No scoped namespace found, so look for root namespace
    PdlPsiFile pdlPsiFile = (PdlPsiFile) element.getContainingFile();

    return pdlPsiFile.getNamespace();
  }
}
