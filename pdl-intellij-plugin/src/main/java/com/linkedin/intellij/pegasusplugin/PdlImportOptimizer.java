package com.linkedin.intellij.pegasusplugin;

import com.intellij.lang.ImportOptimizer;
import com.intellij.psi.PsiFile;
import com.linkedin.intellij.pegasusplugin.psi.PdlPsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Optimize imports in .pdl files.  Sorts imports and removes unused imports.
 */
public class PdlImportOptimizer implements ImportOptimizer {
  @Override
  public boolean supports(PsiFile file) {
    return file instanceof PdlPsiFile;
  }

  @NotNull
  @Override
  public Runnable processFile(final PsiFile file) {
    return new CollectingInfoRunnable() {
      @Nullable
      @Override
      public String getUserNotificationInfo() {
        return null;
      }

      @Override
      public void run() {
        if (file instanceof PdlPsiFile) {
          PdlPsiFile f = (PdlPsiFile) file;
          f.optimizeImports();
        }
      }
    };
  }
}
