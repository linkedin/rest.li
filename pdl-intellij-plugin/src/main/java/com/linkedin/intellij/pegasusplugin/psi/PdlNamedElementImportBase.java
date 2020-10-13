package com.linkedin.intellij.pegasusplugin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.IncorrectOperationException;
import com.linkedin.intellij.pegasusplugin.PdlImportError;
import java.util.Collection;
import java.util.Optional;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public abstract class PdlNamedElementImportBase extends PdlNamedElementReferenceBase implements PdlNamedElementImport, PdlImportDeclaration {
  public PdlNamedElementImportBase(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public PdlTypeName getFullname() {
    return PdlTypeName.decode(getFullyQualifiedName().getText());
  }

  @Override
  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
    String namespace = getFullname().getNamespace();
    PdlTypeName fullname = new PdlTypeName(namespace, name);
    PdlTypeReference replacement = PdlElementFactory.createPdlTypeReference(this.getProject(), fullname.toString());
    getFullyQualifiedName().replace(replacement.getFullyQualifiedName());
    return this;
  }

  @Override
  public boolean isUsed() {
    for (PdlTypeReference ref: getPdlFile().getTypeReferences()) {
      if (ref.getFullname() != null && ref.getFullname().equals(getFullname())) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  @Override
  public PdlImportError getImportError() {
    PdlPsiFile pdlFile = getPdlFile();
    PdlTypeName importFullname = getFullname();
    if (pdlFile == null || importFullname == null) {
      return null;
    }

    return getImportError(pdlFile, importFullname);
  }

  /**
   * A static version of {@link #getImportError()}. This can be used to spot prohibited import declarations before
   * they're created by looking at the theoretical import's fullname and the file in which it would be located.
   *
   * TODO(evwillia): Include "conflicts with another import" as a "prohibited" import rule.
   *
   * @param pdlFile file containing the theoretical import
   * @param importFullname fullname of the theoretical import
   * @return which error this import declaration would result in, or null if none
   */
  public static PdlImportError getImportError(@NotNull PdlPsiFile pdlFile, @NotNull PdlTypeName importFullname) {
    PdlNamespace rootNamespace = pdlFile.getNamespace();
    String rootNamespaceText = rootNamespace == null ? "" : rootNamespace.getText();

    // First, check if this import references the root namespace
    if (importFullname.getNamespace().equals(rootNamespaceText)) {
      return new PdlImportError(PdlImportError.Type.REFERENCES_ROOT_NAMESPACE);
    }

    String importName = importFullname.getName();

    if (importName == null) {
      return null;
    }

    GlobalSearchScope scope = GlobalSearchScope.fileScope(pdlFile);
    Project project = pdlFile.getProject();

    // Find a type declaration within this file that has the same full name
    Optional<PdlTypeNameDeclaration> matchingLocalType = PdlFullnameStubIndex.getInstance()
        .get(importFullname.toString(), project, scope)
        .stream()
        .findAny();

    if (matchingLocalType.isPresent()) {
      return new PdlImportError(PdlImportError.Type.REFERENCES_LOCAL_TYPE);
    }

    // Find type declarations within this file that has the same simple name
    Collection<PdlTypeNameDeclaration> conflictingLocalTypes = PdlNameStubIndex.getInstance()
        .get(importName, project, scope);

    if (conflictingLocalTypes != null && !conflictingLocalTypes.isEmpty()) {
      return new PdlImportError(PdlImportError.Type.CONFLICTS_WITH_LOCAL_TYPE, conflictingLocalTypes);
    }

    // No error
    return null;
  }
}
