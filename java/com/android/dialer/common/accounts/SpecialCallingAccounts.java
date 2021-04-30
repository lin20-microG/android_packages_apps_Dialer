/*
 * Copyright (C) 2020 The Calyx Institute
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

package com.android.dialer.common.accounts;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import com.android.dialer.callintent.CallIntentBuilder;

import javax.annotation.Nullable;

import static android.telecom.PhoneAccount.SCHEME_SIP;
import static android.telecom.PhoneAccount.SCHEME_TEL;

public interface SpecialCallingAccounts {

  String KEY_SHOW_ACCOUNTS_SELECTION_DIALOG = "show_accounts_selection_dialog";

  String MIME_TYPE_SIGNAL = "vnd.android.cursor.item/vnd.org.thoughtcrime.securesms.call";
  String MIME_TYPE_WHATSAPP = "vnd.android.cursor.item/vnd.com.whatsapp.voip.call";
  String MIME_TYPE_PHONE = ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE;

  String[] ACCOUNTS_PROJECTION = new String[]{Data._ID, Data.MIMETYPE, Data.DATA1};
  String ACCOUNTS_SELECTION = Data.LOOKUP_KEY + " = ? AND " + Data.MIMETYPE + " in (?, ?)";

  // normally this is "market://details?id=org.thoughtcrime.securesms"
  // but we want to force-open F-Droid on CalyxOS
  Uri MARKET_URI_SIGNAL = Uri.parse("fdroid.app:org.thoughtcrime.securesms");

  static boolean showDialog(Context context, String phoneNumber,
      @Nullable CallIntentBuilder builder) {
    if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
            KEY_SHOW_ACCOUNTS_SELECTION_DIALOG, true)) return false;
    if (phoneNumber == null || phoneNumber.isEmpty()) return false;
    return showDialog(context, builder);
  }

  static boolean showDialog(Context context, Intent intent, @Nullable CallIntentBuilder builder) {
    if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
            KEY_SHOW_ACCOUNTS_SELECTION_DIALOG, true)) return false;
    if (Intent.ACTION_CALL.equals(intent.getAction())) return true;
    return showDialog(context, builder);
  }

  static boolean showDialog(Context context, @Nullable CallIntentBuilder builder) {
    if (builder == null) return false;
    if (builder.isDuoCall() || builder.isVideoCall()) return false;
    String scheme = builder.getUri().getScheme();
    if (!SCHEME_TEL.equals(scheme) && !SCHEME_SIP.equals(scheme)) return false;
    return true;
  }

}
