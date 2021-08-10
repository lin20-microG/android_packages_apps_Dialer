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
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.provider.ContactsContract.Data;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import com.android.contacts.common.compat.PhoneAccountCompat;
import com.android.dialer.common.accounts.CallAccount.Status;
import com.android.dialer.common.PackageUtils;
import com.android.dialer.contacts.resources.R;
import com.android.dialer.util.CallUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.ACTION_VIEW;
import static android.telecom.PhoneAccount.SCHEME_SIP;
import static com.android.dialer.common.accounts.SpecialCallingAccounts.MARKET_URI_SIGNAL;
import static com.android.dialer.common.accounts.SpecialCallingAccounts.MIME_TYPE_SIGNAL;
import static com.android.dialer.common.accounts.SpecialCallingAccounts.MIME_TYPE_WHATSAPP;
import static com.android.dialer.common.accounts.SpecialCallingAccounts.PACKAGE_NAME_SIGNAL;
import static com.android.dialer.common.accounts.SpecialCallingAccounts.PACKAGE_NAME_WHATSAPP;

class CallAccountCreator {

  private final Context context;
  private final TelecomManager telecomManager;
  private final PackageManager packageManager;

  CallAccountCreator(Context context) {
    this.context = context;
    this.telecomManager = context.getSystemService(TelecomManager.class);
    this.packageManager = context.getPackageManager();
  }

  CallAccount getSignalAccount(long id) {
    Intent intent;
    Drawable icon;
    String name = context.getString(R.string.call_account_signal);
    boolean isOffline = isOffline();
    if (id == -1 || isOffline) {
      Status status;
      String unavailableText;
      if (isInstalled(PACKAGE_NAME_SIGNAL, MIME_TYPE_SIGNAL)) {
        intent = getCustomCallIntent(id, MIME_TYPE_SIGNAL);
        icon = getAppIcon(intent);
        status = Status.DISABLED;
        int unavailableRes = isOffline
            ? R.string.call_account_offline : R.string.call_account_unavailable;
        unavailableText = context.getString(unavailableRes);
      } else {
        intent = new Intent(ACTION_VIEW, MARKET_URI_SIGNAL);
        icon = context.getDrawable(R.drawable.logo_signal_disabled);
        status = Status.NOT_INSTALLED;
        unavailableText = context.getString(R.string.call_account_not_installed);
      }
      return new CallAccount(intent, name, icon, true, status, unavailableText);
    } else {
      intent = getCustomCallIntent(id, MIME_TYPE_SIGNAL);
      icon = getAppIcon(intent);
      return new CallAccount(intent, name, icon, true);
    }
  }

  @Nullable
  CallAccount getWhatsAppAccount(long id) {
    Status status;
    String unavailableText;
    if (id == -1) {
      if (isInstalled(PACKAGE_NAME_WHATSAPP, MIME_TYPE_WHATSAPP)) {
        status = Status.DISABLED;
        unavailableText = context.getString(R.string.call_account_unavailable);
      } else {
        // do not show WhatsApp if it is not installed
        return null;
      }
    } else if (isOffline()) {
      status = Status.DISABLED;
      unavailableText = context.getString(R.string.call_account_offline);
    } else {
      status = Status.ENABLED;
      unavailableText = null;
    }
    Intent intent = getCustomCallIntent(id, MIME_TYPE_WHATSAPP);
    String name = context.getString(R.string.call_account_whatsapp);
    return new CallAccount(intent, name, getAppIcon(intent), true, status, unavailableText);
  }

  private Intent getCustomCallIntent(long id, String mimeType) {
    Intent intent = new Intent(ACTION_VIEW);
    final Uri uri = android.content.ContentUris.withAppendedId(Data.CONTENT_URI, id);
    intent.setDataAndType(uri, mimeType);
    return intent;
  }

  private boolean isOffline() {
    ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
    NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
    return capabilities == null
        || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
  }

  private boolean isInstalled(String packageName, String mimeType) {
    if (PackageUtils.isPackageEnabled(packageName, context)) {
      Intent i = getCustomCallIntent(0, mimeType);
      final List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(i,
          PackageManager.MATCH_DEFAULT_ONLY);
      return !resolveInfos.isEmpty();
    }
    return false;
  }

  @Nullable
  private Drawable getAppIcon(Intent i) {
    final List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(i,
        PackageManager.MATCH_DEFAULT_ONLY);
    if (resolveInfos.isEmpty()) return null;
    ResolveInfo match = resolveInfos.get(0);
    if (match != null) return match.loadIcon(packageManager);
    return null;
  }

  List<CallAccount> getPhoneAccounts(Intent phoneIntent, @Nullable String number) {
    List<PhoneAccountHandle> accountHandles = telecomManager.getCallCapablePhoneAccounts();
    ArrayList<CallAccount> callAccounts = new ArrayList<>(accountHandles.size());
    for (PhoneAccountHandle accountHandle : accountHandles) {
      CallAccount callAccount = getPhoneAccount(accountHandle, phoneIntent, number);
      if (callAccount != null) callAccounts.add(callAccount);
    }
    return callAccounts;
  }

  @Nullable
  private CallAccount getPhoneAccount(PhoneAccountHandle accountHandle, Intent phoneIntent,
      @Nullable String number) {
    PhoneAccount account = telecomManager.getPhoneAccount(accountHandle);
    Intent intent = transformIntent(account, phoneIntent, number);
    if (intent == null) return null;
    intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle);
    String name = account.getLabel().toString();
    if (name.isEmpty()) {
      TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class);
      name = telephonyManager.getSimOperatorName();
      if (name == null || name.isEmpty()) {
        name = context.getString(R.string.call_account_unknown);
      }
    }
    Drawable icon = PhoneAccountCompat.createIconDrawable(account, context);
    return new CallAccount(intent, name, icon, false);
  }

  /**
   * Creates a copy of the given Intent or transforms the Intent from sip:// to tel:// if needed.
   * Returns null, if the given account does not support the URI scheme.
   */
  @Nullable
  private Intent transformIntent(PhoneAccount account, Intent phoneIntent,
      @Nullable String number) {
    Uri uri = phoneIntent.getData();
    if (uri != null && SCHEME_SIP.equals(uri.getScheme())
        && !account.supportsUriScheme(uri.getScheme()) && number != null && !number.isEmpty()) {
      Intent intent = new Intent(phoneIntent);
      intent.setData(CallUtil.getCallUri(number));
      return intent;
    } else if (uri != null && !account.supportsUriScheme(uri.getScheme())) {
      return null;
    }
    return new Intent(phoneIntent);
  }

}
