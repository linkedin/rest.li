package com.linkedin.intellij.pegasusplugin.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import com.linkedin.intellij.pegasusplugin.schemadoc.psi.SchemadocComment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public abstract class PdlNamedTypeDeclarationBase extends ASTWrapperPsiElement
    implements PdlNamedTypeDeclarationInterface, PdlNamedTypeDeclaration {

  public PdlNamedTypeDeclarationBase(@NotNull ASTNode node) {
    super(node);
  }

  @Nullable
  public String getDocumentation() {
    SchemadocComment schemadoc = PsiTreeUtil.getChildOfType(this, SchemadocComment.class);
    if (schemadoc != null) {
      return schemadoc.getComment();
    } else {
      return null;
    }
  }

  @Override
  public boolean isDeprecated() {
    return getAnnotations().getPropDeclarationList().stream().anyMatch(
        prop -> prop.getPropNameDeclaration().getText().equals("@deprecated")
    );
  }

  @Nullable
  @Override
  public PdlTypeNameDeclaration getTypeNameDeclaration() {
    if (getEnumDeclaration() != null) {
      return getEnumDeclaration().getTypeNameDeclaration();
    } else if (getFixedDeclaration() != null) {
      return getFixedDeclaration().getTypeNameDeclaration();
    } else if (getRecordDeclaration() != null) {
      return getRecordDeclaration().getTypeNameDeclaration();
    } else if (getTyperefDeclaration() != null) {
      return getTyperefDeclaration().getTypeNameDeclaration();
    }
    return null;
  }
}
