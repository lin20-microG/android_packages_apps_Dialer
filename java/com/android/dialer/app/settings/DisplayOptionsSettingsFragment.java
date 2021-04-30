/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.dialer.app.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;

import com.android.dialer.app.R;

import static com.android.dialer.common.accounts.SpecialCallingAccounts.KEY_SHOW_ACCOUNTS_SELECTION_DIALOG;

public class DisplayOptionsSettingsFragment extends PreferenceFragment {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.display_options_settings);
    SwitchPreference switchPref =
        (SwitchPreference) findPreference(KEY_SHOW_ACCOUNTS_SELECTION_DIALOG);
    switchPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean(
                KEY_SHOW_ACCOUNTS_SELECTION_DIALOG, (boolean) newValue).apply();
        return true;
      }
    });
  }
}
