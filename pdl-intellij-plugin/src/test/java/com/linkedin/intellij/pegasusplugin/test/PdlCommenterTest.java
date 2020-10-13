package com.linkedin.intellij.pegasusplugin.test;

import com.intellij.codeInsight.generation.actions.CommentByLineCommentAction;


public class PdlCommenterTest extends PdlLightTestBase {
  @Override
  protected String getTestDataPath() {
    return "src/test/resources/commenter/fixtures/";
  }

  public void testCommenter() {
    myFixture.configureByFile("Commenter.pdl");
    CommentByLineCommentAction commentAction = new CommentByLineCommentAction();
    commentAction.actionPerformedImpl(getProject(), myFixture.getEditor());
    myFixture.checkResult("<caret>//record Commenter {}");
    commentAction.actionPerformedImpl(getProject(), myFixture.getEditor());
    myFixture.checkResult("<caret>record Commenter {}");
  }

  public void testDocCommentAutocomplete() {
    myFixture.configureByFile("DocCommentAutocomplete.pdl");
    myFixture.type('\n');
    myFixture.checkResultByFile("DocCommentAutocomplete.pdl.after");
  }
}
