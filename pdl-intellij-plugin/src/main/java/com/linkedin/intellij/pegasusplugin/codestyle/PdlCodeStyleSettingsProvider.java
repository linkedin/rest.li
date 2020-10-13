package com.linkedin.intellij.pegasusplugin.codestyle;

import com.intellij.lang.Language;
import com.intellij.openapi.options.Configurable;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider;
import com.linkedin.intellij.pegasusplugin.PdlLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.linkedin.intellij.pegasusplugin.messages.PdlPluginMessages.*;


/**
 * Sets up the code style settings page for PDL.
 */
public class PdlCodeStyleSettingsProvider extends CodeStyleSettingsProvider {
  @Override
  public String getConfigurableDisplayName() {
    return message("plugin.title");
  }

  @NotNull
  @Override
  public Configurable createSettingsPage(@NotNull CodeStyleSettings settings, CodeStyleSettings originalSettings) {
    return new PdlCodeStyleConfigurable(settings, originalSettings);
  }

  @Nullable
  @Override
  public Language getLanguage() {
    return PdlLanguage.INSTANCE;
  }
}
