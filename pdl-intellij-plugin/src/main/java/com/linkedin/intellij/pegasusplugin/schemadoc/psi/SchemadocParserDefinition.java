package com.linkedin.intellij.pegasusplugin.schemadoc.psi;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import com.linkedin.intellij.pegasusplugin.schemadoc.SchemadocLexer;
import com.linkedin.intellij.pegasusplugin.schemadoc.parser.SchemadocParser;
import org.jetbrains.annotations.NotNull;

/**
 * @see ElementType#DOC_COMMENT
 */
public class SchemadocParserDefinition implements ParserDefinition {
  public static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);
  public static final TokenSet COMMENTS = TokenSet.create();

  public static final SchemadocParserDefinition INSTANCE = new SchemadocParserDefinition();

  @NotNull
  @Override
  public Lexer createLexer(Project project) {
    return new SchemadocLexer();
  }

  @NotNull
  public TokenSet getWhitespaceTokens() {
    return WHITE_SPACES;
  }

  @NotNull
  public TokenSet getCommentTokens() {
    return COMMENTS;
  }

  @NotNull
  public TokenSet getStringLiteralElements() {
    return TokenSet.EMPTY;
  }

  @NotNull
  public PsiParser createParser(final Project project) {
    return new SchemadocParser();
  }

  @Override
  public IFileElementType getFileNodeType() {
    return null;
  }

  public PsiFile createFile(FileViewProvider viewProvider) {
    return null;
  }

  public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
    return SpaceRequirements.MAY;
  }

  @NotNull
  public PsiElement createElement(ASTNode node) {
    return SchemadocTypes.Factory.createElement(node);
  }
}
