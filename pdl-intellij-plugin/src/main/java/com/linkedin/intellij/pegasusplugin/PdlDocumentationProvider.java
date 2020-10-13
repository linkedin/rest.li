package com.linkedin.intellij.pegasusplugin;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.linkedin.intellij.pegasusplugin.psi.PdlTopLevel;
import com.linkedin.intellij.pegasusplugin.psi.PdlTypeName;
import com.linkedin.intellij.pegasusplugin.psi.PdlTypeNameDeclaration;
import org.jetbrains.annotations.Nullable;


public class PdlDocumentationProvider extends AbstractDocumentationProvider {
  @Nullable
  @Override
  public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {

    if (element instanceof PdlTypeNameDeclaration) {
      PdlTypeNameDeclaration declaration = (PdlTypeNameDeclaration) element;
      PdlTypeName name = declaration.getFullname();
      return String.format("[%s] %s\n%s %s",
          element.getProject().getName(),
          name.getNamespace(),
          declaration.getSchemaTypeName(),
          name.getName());
    }
    return null;
  }

  @Override
  public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
    if (element instanceof PdlTypeNameDeclaration) {
      PdlTypeNameDeclaration declaration = (PdlTypeNameDeclaration) element;
      if (declaration.getPdlFile().getTopLevelTypeDeclaration().equals(declaration)) {
        // If the type is a top level type, find the doc string.
        // Inline type doc string lookup is not yet supported.
        PdlTopLevel topLevel = PsiTreeUtil.getParentOfType(element, PdlTopLevel.class);
        if (topLevel != null) {
          return topLevel.getNamedTypeDocumentation();
        }
      }
    }
    return null;
  }
}
