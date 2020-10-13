package com.linkedin.intellij.pegasusplugin.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.linkedin.intellij.pegasusplugin.schemadoc.psi.SchemadocTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public abstract class PdlTopLevelBase extends ASTWrapperPsiElement implements PdlTopLevelInterface {

  public PdlTopLevelBase(@NotNull ASTNode node) {
    super(node);
  }

  @Nullable
  @Override
  public String getNamedTypeDocumentation() {
    for (PsiElement psiElement : getChildren()) {
      if (psiElement.getNode().getElementType() == PdlElementType.DOC_COMMENT) {
        StringBuilder builder = new StringBuilder();
        for (PsiElement token : psiElement.getChildren()) {
          if (token.getNode().getElementType() == SchemadocTypes.DOC_COMMENT_CONTENT) {
            builder.append(token.getText());
            builder.append("\n");
          }
        }
        return builder.toString().trim();
      }
    }
    return null;
  }
}
