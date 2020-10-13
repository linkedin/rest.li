package com.linkedin.intellij.pegasusplugin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;


/**
 * Models a by-name reference in PDL.
 */
public abstract class PdlNamedElementTypeReferenceBase extends PdlNamedElementReferenceBase implements PdlTypeReference {
  public PdlNamedElementTypeReferenceBase(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
    PdlFullyQualifiedName toReplace = getFullyQualifiedName();
    PdlTypeName toReplaceName = new PdlTypeName(toReplace.getText());
    String replacementName = new PdlTypeName(toReplaceName.getNamespace(), name).toString();
    PdlTypeReference replacement = PdlElementFactory.createPdlTypeReference(this.getProject(), replacementName);
    toReplace.replace(replacement.getFullyQualifiedName());
    return this;
  }

  @Override
  public boolean isSimpleReference() {
    if (getFullyQualifiedName() != null) {
      String identifier = getFullyQualifiedName().getText();
      return !PdlTypeName.isPrimitive(identifier) && !identifier.contains(".");
    }
    return false;
  }
}
