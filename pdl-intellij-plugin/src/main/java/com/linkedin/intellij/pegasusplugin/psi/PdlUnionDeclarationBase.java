package com.linkedin.intellij.pegasusplugin.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;


/**
 * Implementation for extra functionality defined in {@link PdlUnionDeclarationInterface}.
 */
public abstract class PdlUnionDeclarationBase extends ASTWrapperPsiElement implements PdlUnionDeclarationInterface {
  public PdlUnionDeclarationBase(@NotNull ASTNode node) {
    super(node);
  }

  @NotNull
  @Override
  public List<PdlUnionMemberDeclaration> getUnionMembers() {
    PdlUnionTypeAssignments typeAssignments = getUnionTypeAssignments();

    if (typeAssignments == null) {
      return Collections.emptyList();
    }

    List<PdlUnionMemberDeclaration> unionMemberDeclarations = typeAssignments.getUnionMemberDeclarationList();

    if (unionMemberDeclarations == null) {
      return Collections.emptyList();
    }

    return unionMemberDeclarations.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @NotNull
  @Override
  public List<PdlUnionMemberAlias> getUnionMemberAliases() {
    return getUnionMembers().stream()
        .map(PdlUnionMemberDeclaration::getUnionMemberAlias)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Override
  public boolean isHeterogeneouslyAliased() {
    int numAliased = 0;
    int numNonAliased = 0;

    // Count the aliased and non-aliased members, ignoring 'null' members
    for (PdlUnionMemberDeclaration unionMemberDeclaration : getUnionMembers()) {
      if (unionMemberDeclaration.isNullType()) {
        continue;
      }

      if (unionMemberDeclaration.isAliased()) {
        numAliased++;
      } else {
        numNonAliased++;
      }
    }

    // Return true if there are both aliased members and non-aliased members
    return numAliased != 0 && numNonAliased != 0;
  }
}
