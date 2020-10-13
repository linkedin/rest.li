package com.linkedin.intellij.pegasusplugin;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.IStubFileElementType;
import com.intellij.psi.tree.TokenSet;
import com.linkedin.intellij.pegasusplugin.parser.Parser;
import com.linkedin.intellij.pegasusplugin.psi.PdlElementType;
import com.linkedin.intellij.pegasusplugin.psi.PdlPsiFile;
import com.linkedin.intellij.pegasusplugin.psi.Types;
import com.linkedin.intellij.pegasusplugin.psi.impl.PdlTypeNameDeclarationImpl;
import com.linkedin.intellij.pegasusplugin.schemadoc.psi.SchemadocTypes;
import org.jetbrains.annotations.NotNull;


/**
 * Configures the Grammar-Kit parser for the grammar at src/main/grammars/Pdl.bnf.
 */
public class PdlParserDefinition implements ParserDefinition {
  public static final PdlParserDefinition INSTANCE = new PdlParserDefinition();
  public static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);
  public static final TokenSet COMMENTS = TokenSet.create(
    Types.SINGLE_LINE_COMMENT,
    Types.BLOCK_COMMENT_EMPTY,
    Types.BLOCK_COMMENT_NON_EMPTY,
    Types.COMMA,
    PdlElementType.DOC_COMMENT,
    SchemadocTypes.DOC_COMMENT_START,
    SchemadocTypes.DOC_COMMENT_CONTENT,
    SchemadocTypes.DOC_COMMENT_END);
  public static final TokenSet STRING = TokenSet.create(Types.STRING);

  public static final IStubFileElementType FILE = new PdlStubFileElementType(PdlFileType.INSTANCE.getLanguage());

  @NotNull
  @Override
  public Lexer createLexer(Project project) {
    return new PdlLexerAdapter();
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
    return STRING;
  }

  @NotNull
  public PsiParser createParser(final Project project) {
    return new Parser();
  }

  @Override
  public IFileElementType getFileNodeType() {
    return FILE;
  }

  public PsiFile createFile(FileViewProvider viewProvider) {
    return new PdlPsiFile(viewProvider);
  }

  public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
    if (left.getElementType() == Types.SINGLE_LINE_COMMENT) {
      return SpaceRequirements.MUST_LINE_BREAK;
    }
    return SpaceRequirements.MAY;
  }

  @NotNull
  public PsiElement createElement(ASTNode node) {
    final IElementType type = node.getElementType();
    if (type instanceof PdlElementType) {
      return ((PdlElementType) type).createPsi(node);
    } else if (type == Types.TYPE_NAME_DECLARATION) {
      return new PdlTypeNameDeclarationImpl(node);
    }
    throw new IllegalStateException("Incorrect node for PdlParserDefinition: " + node + " (" + type + ")");
  }
}
