package com.linkedin.intellij.pegasusplugin.inspections;

import com.intellij.codeInsight.daemon.impl.quickfix.RenameElementFix;
import com.intellij.codeInsight.daemon.impl.quickfix.RenameFileFix;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.SmartList;
import com.linkedin.intellij.pegasusplugin.PdlFileType;
import com.linkedin.intellij.pegasusplugin.psi.PdlPsiFile;
import com.linkedin.intellij.pegasusplugin.psi.PdlTypeNameDeclaration;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.linkedin.intellij.pegasusplugin.messages.PdlPluginMessages.*;


/**
 * Detects top-level types declared in files with mismatched names. Any Pegasus schema declared as the root type in a
 * file should have a name matching that of the file.
 */
public class FilenameInspection extends LocalInspectionTool {
  @Nullable
  @Override
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    if (!(file instanceof PdlPsiFile)) {
      return null;
    }
    PdlPsiFile pdlFile = (PdlPsiFile) file;

    // Get the base filename
    final VirtualFile virtualFile = pdlFile.getVirtualFile();
    if (virtualFile == null) {
      return null;
    }
    final String baseFilename = virtualFile.getNameWithoutExtension();

    // Get the root named type declaration
    final PdlTypeNameDeclaration typeNameDeclaration = pdlFile.getTopLevelTypeDeclaration();
    if (typeNameDeclaration == null) {
      return null;
    }

    // Get the root named type's name
    final String rootTypeName = typeNameDeclaration.getName();
    if (rootTypeName == null) {
      return null;
    }

    final List<ProblemDescriptor> descriptors = new SmartList<>();

    // If the filename doesn't match the root type name, show an error and provide quick fixes
    if (!rootTypeName.equals(baseFilename)) {
      descriptors.add(manager.createProblemDescriptor(typeNameDeclaration,
          message("inspection.filename", rootTypeName),
          true,
          ProblemHighlightType.GENERIC_ERROR,
          isOnTheFly,
          new RenameFileFix(PdlFileType.toFilename(rootTypeName)),
          new RenameElementFix(typeNameDeclaration, baseFilename)));
    }

    return descriptors.toArray(new ProblemDescriptor[]{});
  }
}
