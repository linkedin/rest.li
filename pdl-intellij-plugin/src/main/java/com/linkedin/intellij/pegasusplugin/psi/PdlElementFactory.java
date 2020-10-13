package com.linkedin.intellij.pegasusplugin.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.util.PsiTreeUtil;
import com.linkedin.intellij.pegasusplugin.PdlFileType;
import java.util.Collection;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;


/**
 * Utilities for creating PSI elements programmatically. This is the approach IntelliJ recommends.
 */
public class PdlElementFactory {
  private PdlElementFactory() {
  }

  @NotNull
  public static PdlPsiFile createPdlFile(@NotNull Project project, @NonNls @NotNull String text) {
    @NonNls String filename = PdlFileType.toFilename("dummy");
    return (PdlPsiFile) PsiFileFactory.getInstance(project)
      .createFileFromText(filename, PdlFileType.INSTANCE, text);
  }

  public static PdlImportDeclaration createImport(@NotNull Project project, @NonNls @NotNull PdlTypeName typeName) {
    PdlPsiFile pdlPsiFile = createPdlFile(project, "namespace dummynamespace\nimport " + typeName.unescape() + "\nrecord Dummy {}");
    return PsiTreeUtil.findChildOfType(pdlPsiFile, PdlImportDeclaration.class);
  }

  public static PdlImportDeclarations createImports(@NotNull Project project, @NonNls @NotNull Collection<PdlTypeName> typeNames) {
    StringBuilder builder = new StringBuilder();
    builder.append("namespace dummynamespace\n");
    for (PdlTypeName typeName: typeNames) {
      builder.append("import " + typeName.unescape() + "\n");
    }
    builder.append("record Dummy {}");
    PdlPsiFile pdlPsiFile = createPdlFile(project, builder.toString());
    return PsiTreeUtil.findChildOfType(pdlPsiFile, PdlImportDeclarations.class);
  }

  public static PdlTypeNameDeclaration createTypeNameDeclaration(@NotNull Project project, @NonNls @NotNull String name) {
    PdlPsiFile pdlPsiFile = createPdlFile(project, "namespace dummynamespace\nrecord " + name + " {}");
    return PsiTreeUtil.findChildOfType(pdlPsiFile, PdlTypeNameDeclaration.class);
  }

  public static PdlTypeReference createPdlTypeReference(@NotNull Project project, @NonNls @NotNull String name) {
    PdlPsiFile pdlPsiFile = createPdlFile(project, "namespace dummynamespace\nrecord Dummy { dummyField: " + name + "}");
    return PsiTreeUtil.findChildOfType(pdlPsiFile, PdlTypeReference.class);
  }

  public static PdlUnionMemberAlias createUnionMemberAlias(@NotNull Project project, @NonNls @NotNull String name) {
    PdlPsiFile pdlPsiFile = createPdlFile(project, "union[" + name + ": int]");
    return PsiTreeUtil.findChildOfType(pdlPsiFile, PdlUnionMemberAlias.class);
  }
}
