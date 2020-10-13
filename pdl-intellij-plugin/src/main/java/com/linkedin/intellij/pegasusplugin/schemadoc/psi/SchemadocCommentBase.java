package com.linkedin.intellij.pegasusplugin.schemadoc.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;


public class SchemadocCommentBase extends ASTWrapperPsiElement {
  public SchemadocCommentBase(ASTNode node) {
    super(node);
  }

  @Nullable
  public String getComment() {
    PsiSchemadocElement schemadoc = PsiTreeUtil.getChildOfType(this, PsiSchemadocElement.class);
    if (schemadoc != null) {
      return schemadoc.getComment();
    }
    return null;
  }
}
