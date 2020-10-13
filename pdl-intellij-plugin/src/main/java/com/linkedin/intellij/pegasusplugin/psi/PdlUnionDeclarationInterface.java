package com.linkedin.intellij.pegasusplugin.psi;

import com.intellij.psi.PsiElement;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Extends the functionality of {@link PdlUnionDeclaration}.
 */
public interface PdlUnionDeclarationInterface extends PsiElement {

  @Nullable
  PdlUnionTypeAssignments getUnionTypeAssignments();

  /**
   * Safely get all the union members for this union declaration.
   * @return list of non-null union member declarations
   */
  @NotNull
  List<PdlUnionMemberDeclaration> getUnionMembers();

  /**
   * Safely get all the existing union member aliases for this union declaration.
   * @return list of non-null union member aliases
   */
  @NotNull
  List<PdlUnionMemberAlias> getUnionMemberAliases();

  /**
   * Returns true if this union declaration contains both aliased and non-aliased union members. Ignores 'null'.
   * @return whether this union is heterogeneously aliased
   */
  boolean isHeterogeneouslyAliased();
}
