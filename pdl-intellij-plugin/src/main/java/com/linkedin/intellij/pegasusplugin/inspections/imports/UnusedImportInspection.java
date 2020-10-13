package com.linkedin.intellij.pegasusplugin.inspections.imports;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.SmartList;
import com.linkedin.intellij.pegasusplugin.psi.PdlImportDeclaration;
import com.linkedin.intellij.pegasusplugin.psi.PdlPsiFile;
import java.util.List;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.linkedin.intellij.pegasusplugin.messages.PdlPluginMessages.*;


public class UnusedImportInspection extends LocalInspectionTool {
  @Nullable
  @Override
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    if (!(file instanceof PdlPsiFile)) {
      return null;
    }
    PdlPsiFile pdlFile = (PdlPsiFile) file;

    final List<ProblemDescriptor> descriptors = new SmartList<>();

    for (PdlImportDeclaration pdlImport : pdlFile.getImports()) {
      if (!pdlImport.isUsed()) {
        descriptors.add(
            manager.createProblemDescriptor(
                pdlImport.getFullyQualifiedName(), message(
                    "inspection.imports.unused",
                    pdlImport.getFullyQualifiedName().getText()),
                new OptimizeImportsFix(),
                ProblemHighlightType.LIKE_UNUSED_SYMBOL, true));
      }
    }
    return descriptors.toArray(new ProblemDescriptor[]{});
  }

  private static class OptimizeImportsFix implements LocalQuickFix {
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
      return message("inspection.imports.optimize.fix");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PsiElement element = descriptor.getPsiElement();
      PsiFile file = element.getContainingFile();
      if (!(file instanceof PdlPsiFile)) {
        return;
      }
      PdlPsiFile pdlFile = (PdlPsiFile) file;
      pdlFile.optimizeImports();
    }
  }
}
