package com.linkedin.intellij.pegasusplugin.codestyle;

import com.intellij.application.options.CodeStyleAbstractConfigurable;
import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.application.options.TabbedLanguageCodeStylePanel;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.linkedin.intellij.pegasusplugin.PdlLanguage;
import org.jetbrains.annotations.NotNull;


/**
 * Provides IntelliJ with general guidance on how to format PDL source.
 */
public class PdlCodeStyleConfigurable extends CodeStyleAbstractConfigurable {
  public PdlCodeStyleConfigurable(@NotNull CodeStyleSettings settings, CodeStyleSettings cloneSettings) {
    super(settings, cloneSettings, "Pdl");
  }

  @NotNull
  @Override
  protected CodeStyleAbstractPanel createPanel(CodeStyleSettings settings) {
    return new PdlCodeStyleMainPanel(getCurrentSettings(), settings);
  }

  @Override
  public String getHelpTopic() {
    return null;
  }

  private static class PdlCodeStyleMainPanel extends TabbedLanguageCodeStylePanel {
    private PdlCodeStyleMainPanel(CodeStyleSettings currentSettings, CodeStyleSettings settings) {
      super(PdlLanguage.INSTANCE, currentSettings, settings);
    }

    @Override
    protected void addSpacesTab(CodeStyleSettings settings) {
    }

    @Override
    protected void addBlankLinesTab(CodeStyleSettings settings) {
    }

    @Override
    protected void addWrappingAndBracesTab(CodeStyleSettings settings) {
    }
  }
}
