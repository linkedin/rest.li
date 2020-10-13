package com.linkedin.intellij.pegasusplugin.messages;

import com.intellij.CommonBundle;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ResourceBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;


public class PdlPluginMessages {

  private static Reference<ResourceBundle> bundle;

  @NonNls
  private static final String BUNDLE = "messages.pdlplugin";

  private PdlPluginMessages() {
  }

  public static String message(@NotNull @NonNls @PropertyKey(resourceBundle = BUNDLE) final String key,
      @NotNull final Object... params) {
    return CommonBundle.message(getBundle(), key, params);
  }

  private static ResourceBundle getBundle() {
    ResourceBundle bundle = com.intellij.reference.SoftReference.dereference(PdlPluginMessages.bundle);
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(BUNDLE);
      PdlPluginMessages.bundle = new SoftReference<>(bundle);
    }
    return bundle;
  }
}