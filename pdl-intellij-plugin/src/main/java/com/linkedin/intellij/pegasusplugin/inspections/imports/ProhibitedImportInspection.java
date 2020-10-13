package com.linkedin.intellij.pegasusplugin.inspections.imports;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.SmartList;
import com.linkedin.intellij.pegasusplugin.PdlImportError;
import com.linkedin.intellij.pegasusplugin.psi.PdlElementFactory;
import com.linkedin.intellij.pegasusplugin.psi.PdlImportDeclaration;
import com.linkedin.intellij.pegasusplugin.psi.PdlNamedElement;
import com.linkedin.intellij.pegasusplugin.psi.PdlPsiFile;
import com.linkedin.intellij.pegasusplugin.psi.PdlTypeName;
import java.util.List;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.linkedin.intellij.pegasusplugin.messages.PdlPluginMessages.*;


/**
 * Detects prohibited imports that will cause failures in the PDL parser.
 */
public class ProhibitedImportInspection extends LocalInspectionTool {
  @Nullable
  @Override
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    if (!(file instanceof PdlPsiFile)) {
      return null;
    }
    PdlPsiFile pdlFile = (PdlPsiFile) file;

    final List<ProblemDescriptor> descriptors = new SmartList<>();

    for (PdlImportDeclaration importDeclaration : pdlFile.getImports()) {
      PdlImportError importError = importDeclaration.getImportError();

      if (importError != null) {
        switch (importError.getType()) {
          case REFERENCES_ROOT_NAMESPACE:
            descriptors.add(createProblemDescriptor(importDeclaration.getFullyQualifiedName(),
                message("inspection.imports.prohibited.references_root"),
                manager));
            break;
          case REFERENCES_LOCAL_TYPE:
            descriptors.add(createProblemDescriptor(importDeclaration.getFullyQualifiedName(),
                message("inspection.imports.prohibited.references_local"),
                manager));
            break;
          case CONFLICTS_WITH_LOCAL_TYPE:
            descriptors.add(createProblemDescriptor(importDeclaration.getFullyQualifiedName(),
                message("inspection.imports.prohibited.conflicts_local", importDeclaration.getName()),
                manager));
            importError.getConflictingTypes()
                .forEach(typeNameDeclaration -> {
                  String importFullname = importDeclaration.getFullname().toString();
                  descriptors.add(createProblemDescriptor(typeNameDeclaration,
                      message("inspection.imports.prohibited.conflicts_local.type", importFullname),
                      manager));
                });
            break;
        }
      }
    }
    return descriptors.toArray(new ProblemDescriptor[]{});
  }

  private static ProblemDescriptor createProblemDescriptor(PsiElement element, String message, InspectionManager manager) {
    return manager.createProblemDescriptor(
        element,
        message,
        new RemoveUsedImport(),
        ProblemHighlightType.GENERIC_ERROR,
        true);
  }

  /**
   * Quick fix which removes a given import declaration and replaces all references to it with fully qualified names.
   */
  private static class RemoveUsedImport implements LocalQuickFix {
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
      return message("inspection.imports.prohibited.remove_import_fix");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PsiElement element = descriptor.getPsiElement();
      PsiFile file = element.getContainingFile();
      if (!(file instanceof PdlPsiFile)) {
        return;
      }
      PdlPsiFile pdlFile = (PdlPsiFile) file;

      PdlImportDeclaration importDeclaration = PsiTreeUtil.getParentOfType(element, PdlImportDeclaration.class);
      if (importDeclaration == null) {
        return;
      }

      PdlTypeName importName = importDeclaration.getFullname();
      if (importName == null) {
        return;
      }

      // For each reference that relies on this import, replace it with its fully-qualified name
      pdlFile.getTypeReferences()
          .stream()
          .filter(typeReference -> typeReference.getFullname().equals(importName))
          .filter(PdlNamedElement::isCrossNamespace)
          .forEach(typeReference -> typeReference.replace(PdlElementFactory.createPdlTypeReference(project, importName.toString())));

      // Finally, delete the import declaration
      importDeclaration.delete();
    }
  }
}
