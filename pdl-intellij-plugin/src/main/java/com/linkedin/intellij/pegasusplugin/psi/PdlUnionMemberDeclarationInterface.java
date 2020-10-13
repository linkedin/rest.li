package com.linkedin.intellij.pegasusplugin.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Extends the functionality of {@link PdlUnionMemberDeclaration}.
 */
public interface PdlUnionMemberDeclarationInterface extends PsiElement {

  @NotNull
  PdlTypeAssignment getTypeAssignment();

  @Nullable
  PdlUnionMemberAlias getUnionMemberAlias();

  /**
   * Returns true if this union member has an alias.
   * @return whether this member is aliased
   */
  boolean isAliased();

  /**
   * Returns true if this union member represents the 'null' type.
   * @return whether this member is 'null'
   */
  boolean isNullType();

  /**
   * Safely gets the string alias of this union member, or returns null if it's not aliased.
   * @return this member's alias or null
   */
  @Nullable
  String getAlias();
}