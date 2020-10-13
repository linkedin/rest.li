package com.linkedin.intellij.pegasusplugin.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;


public class PdlEnumSymbolBase extends ASTWrapperPsiElement implements PdlEnumSymbolInterface {

  public PdlEnumSymbolBase(@NotNull ASTNode node) {
    super(node);
  }

  public PdlEnumSymbolDeclaration getEnumSymbolDeclaration() {
    PsiElement parent = this.getParent();
    if (parent instanceof PdlEnumSymbolDeclaration) {
      return (PdlEnumSymbolDeclaration) parent;
    } else {
      throw new IncorrectOperationException("Unable to traverse .pdl AST from enum symbol node to declaration node.");
    }
  }

  @Override
  public boolean isDeprecated() {
    PdlEnumSymbolDeclaration enumSymbolDecl = getEnumSymbolDeclaration();
    return enumSymbolDecl.getAnnotations().getPropDeclarationList().stream().anyMatch(
        prop -> prop.getPropNameDeclaration().getText().equals("@deprecated")
    );
  }
}
