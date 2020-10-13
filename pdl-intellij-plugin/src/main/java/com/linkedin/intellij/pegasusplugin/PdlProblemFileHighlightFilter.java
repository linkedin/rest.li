package com.linkedin.intellij.pegasusplugin;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;

// https://intellij-support.jetbrains.com/hc/en-us/community/posts/206755995-Show-errors-in-project-view
public class PdlProblemFileHighlightFilter implements Condition<VirtualFile> {

  @Override
  public boolean value(VirtualFile virtualFile) {
    return virtualFile.getFileType() == PdlFileType.INSTANCE;
  }
}
