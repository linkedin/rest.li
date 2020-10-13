package com.linkedin.intellij.pegasusplugin.formatter;

import com.intellij.codeInsight.editorActions.CommentCompleteHandler;
import com.intellij.lang.CodeDocumentationAwareCommenter;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.linkedin.intellij.pegasusplugin.psi.PdlElementType;
import com.linkedin.intellij.pegasusplugin.schemadoc.psi.SchemadocTypes;


/**
 * Based on the Java plugin for IntelliJ, this instructs IntelliJ on how to format documentation comment strings.
 */
public class PdlCommentCompleteHandler implements CommentCompleteHandler {
  @Override
  public boolean isCommentComplete(PsiComment psiComment, CodeDocumentationAwareCommenter codeDocumentationAwareCommenter, Editor editor) {
    if (psiComment.getTokenType() == PdlElementType.DOC_COMMENT) {
      PsiElement last = psiComment.getLastChild();
      String text = psiComment.getText();
      int firstDocCommentStart = text.indexOf("/**");
      int lastDocCommentStart = text.lastIndexOf("/**");
      boolean hasNestedDocCommentStart = (firstDocCommentStart > -1 && lastDocCommentStart > -1 && firstDocCommentStart != lastDocCommentStart);
      return (!hasNestedDocCommentStart && last != null && last.getNode().getElementType() == SchemadocTypes.DOC_COMMENT_END);
    }
    return true;
  }

  @Override
  public boolean isApplicable(PsiComment psiComment, CodeDocumentationAwareCommenter codeDocumentationAwareCommenter) {
    return psiComment.getTokenType() == PdlElementType.DOC_COMMENT;
  }
}
