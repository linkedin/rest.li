package com.linkedin.intellij.pegasusplugin.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.util.ProcessingContext;
import com.linkedin.intellij.pegasusplugin.psi.PdlPsiFile;
import com.linkedin.intellij.pegasusplugin.psi.PdlTypeName;
import org.jetbrains.annotations.NotNull;


/**
 * Completion provider for import declarations. Uses only the bare functionality of
 * {@link FullyQualifiedNameCompletionProvider}, but also filters out types within the file's root namespace.
 */
public class ImportDeclarationCompletionProvider extends FullyQualifiedNameCompletionProvider {
  @Override
  protected void addPackagelessCompletions(@NotNull CompletionParameters parameters,
      @NotNull ProcessingContext context,
      @NotNull CompletionResultSet resultSet) {
    // Add logic here if we ever extend the import functionality (e.g. '*' syntax)
  }

  @Override
  protected boolean filterFullname(@NotNull String fullname, @NotNull PdlPsiFile pdlFile) {
    // Exclude types within the file's root namespace
    String rootNamespace = pdlFile.getNamespaceText();
    PdlTypeName importName = new PdlTypeName(fullname);
    return !rootNamespace.equals(importName.getNamespace());
  }
}
