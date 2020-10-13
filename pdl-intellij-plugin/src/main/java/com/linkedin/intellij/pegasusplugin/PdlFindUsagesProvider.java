package com.linkedin.intellij.pegasusplugin;

import com.intellij.lang.cacheBuilder.DefaultWordsScanner;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.tree.TokenSet;
import com.linkedin.intellij.pegasusplugin.psi.PdlTypeNameDeclaration;
import com.linkedin.intellij.pegasusplugin.psi.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Provides find usages support for PDL schema types.
 */
public class PdlFindUsagesProvider implements FindUsagesProvider {
  private static final TokenSet IDENTIFIERS =
    TokenSet.create(Types.IDENTIFIER);

  private static final TokenSet COMMENTS = TokenSet.create(
    Types.LINE_COMMENT,
    Types.BLOCK_COMMENT_NON_EMPTY,
    Types.BLOCK_COMMENT_EMPTY,
    Types.COMMA);

  private static final TokenSet LITERALS = TokenSet.create(
    Types.JSON_STRING,
    Types.JSON_NUMBER,
    Types.JSON_BOOLEAN,
    Types.JSON_NULL);

  @Nullable
  @Override
  public WordsScanner getWordsScanner() {
    // See https://devnet.jetbrains.com/message/5537369
    return new DefaultWordsScanner(new PdlLexerAdapter(), IDENTIFIERS, COMMENTS, LITERALS);
  }

  @Override
  public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
    return psiElement instanceof PsiNamedElement;
  }

  @Nullable
  @Override
  public String getHelpId(@NotNull PsiElement psiElement) {
    return null;
  }

  @NotNull
  @Override
  public String getType(@NotNull PsiElement element) {
    if (element instanceof PdlTypeNameDeclaration) {
      return "type"; // TODO: lookup the data schema type
    } else {
      return "";
    }
  }

  @NotNull
  @Override
  public String getDescriptiveName(@NotNull PsiElement element) {
    if (element instanceof PdlTypeNameDeclaration) {
      return ((PdlTypeNameDeclaration) element).getFullname().toString();
    } else {
      return "";
    }
  }

  @NotNull
  @Override
  public String getNodeText(@NotNull PsiElement element, boolean useFullName) {
    if (element instanceof PdlTypeNameDeclaration) {
      PdlTypeNameDeclaration declaration = (PdlTypeNameDeclaration) element;
      String name = declaration.getName();
      if (name != null) {
        return name;
      } else {
        return "";
      }
    } else {
      return "";
    }
  }
}
