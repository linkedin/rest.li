package com.linkedin.intellij.pegasusplugin.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Implementation for extra functionality defined in {@link PdlUnionMemberDeclarationInterface}.
 */
public abstract class PdlUnionMemberDeclarationBase extends ASTWrapperPsiElement
    implements PdlUnionMemberDeclarationInterface {

  public PdlUnionMemberDeclarationBase(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public boolean isAliased() {
    return getUnionMemberAlias() != null;
  }

  @Override
  public boolean isNullType() {
    final PdlTypeAssignment typeAssignment = getTypeAssignment();

    if (typeAssignment == null) {
      return false;
    }

    return PdlTokenType.NULL.equals(typeAssignment.getText());
  }

  @Nullable
  @Override
  public String getAlias() {
    final PdlUnionMemberAlias unionMemberAlias = getUnionMemberAlias();

    if (unionMemberAlias == null) {
      return null;
    }

    final PdlUnionMemberAliasName unionMemberAliasName = unionMemberAlias.getUnionMemberAliasName();

    if (unionMemberAliasName == null) {
      return null;
    }

    return unionMemberAliasName.getText();
  }
}
