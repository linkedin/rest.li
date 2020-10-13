package com.linkedin.intellij.pegasusplugin.inspections.union;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.SmartList;
import com.linkedin.intellij.pegasusplugin.IdentifierGenerator;
import com.linkedin.intellij.pegasusplugin.psi.PdlElementFactory;
import com.linkedin.intellij.pegasusplugin.psi.PdlPsiFile;
import com.linkedin.intellij.pegasusplugin.psi.PdlUnionDeclaration;
import com.linkedin.intellij.pegasusplugin.psi.PdlUnionMemberAlias;
import com.linkedin.intellij.pegasusplugin.psi.PdlUnionMemberDeclaration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.linkedin.intellij.pegasusplugin.messages.PdlPluginMessages.*;


/**
 * Detects unions containing a heterogeneous mix of aliased and non-aliased union members. Valid unions either have
 * all aliased union members or all non-aliased union members. The only exception is 'null', which cannot be aliased
 * and is thus ignored.
 */
public class HeterogeneouslyAliasedUnionInspection extends LocalInspectionTool {
  @Nullable
  @Override
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    if (!(file instanceof PdlPsiFile)) {
      return null;
    }
    PdlPsiFile pdlFile = (PdlPsiFile) file;

    final List<ProblemDescriptor> descriptors = new SmartList<>();

    // Perform this check for each union declaration in the file
    Collection<PdlUnionDeclaration> unionDeclarations = PsiTreeUtil.findChildrenOfType(pdlFile, PdlUnionDeclaration.class);
    for (PdlUnionDeclaration unionDeclaration : unionDeclarations) {
      if (unionDeclaration == null) {
        continue;
      }

      // If the problem is present, show an error and provide a quick fix
      if (unionDeclaration.isHeterogeneouslyAliased()) {
        descriptors.add(manager.createProblemDescriptor(unionDeclaration,
            message("inspection.union.heterogeneously_aliased"),
            true,
            ProblemHighlightType.GENERIC_ERROR,
            isOnTheFly,
            new AddAliasesFix(),
            new RemoveAliasesFix()));
      }
    }

    return descriptors.toArray(new ProblemDescriptor[]{});
  }

  /**
   * Quick fix which adds a unique alias to each union member without an alias.
   */
  private class AddAliasesFix implements LocalQuickFix {
    private static final String BASE_GENERATED_ALIAS_NAME = "alias";

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
      return getName();
    }

    @Nls
    @NotNull
    @Override
    public String getName() {
      return message("inspection.union.heterogeneously_aliased.add_fix");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PdlUnionDeclaration unionDeclaration = (PdlUnionDeclaration) descriptor.getPsiElement();

      // Get the set of existing aliases in this union declaration
      Set<String> existingAliases = unionDeclaration.getUnionMembers()
          .stream()
          .map(PdlUnionMemberDeclaration::getAlias)
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());

      final IdentifierGenerator aliasGenerator = new IdentifierGenerator(BASE_GENERATED_ALIAS_NAME, existingAliases);

      // For each union member without an alias, add a unique alias (skip the 'null' type)
      for (PdlUnionMemberDeclaration unionMemberDeclaration : unionDeclaration.getUnionMembers()) {
        if (!unionMemberDeclaration.isAliased() && !unionMemberDeclaration.isNullType()) {
          unionMemberDeclaration.addBefore(
              PdlElementFactory.createUnionMemberAlias(project, aliasGenerator.next()),
              unionMemberDeclaration.getTypeAssignment());
        }
      }
    }
  }

  /**
   * Quick fix which simply removes all the aliases in the union.
   */
  private class RemoveAliasesFix implements LocalQuickFix {
    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
      return getName();
    }

    @Nls
    @NotNull
    @Override
    public String getName() {
      return message("inspection.union.heterogeneously_aliased.remove_fix");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PdlUnionDeclaration unionDeclaration = (PdlUnionDeclaration) descriptor.getPsiElement();

      unionDeclaration.getUnionMemberAliases().forEach(PdlUnionMemberAlias::delete);
    }
  }
}
