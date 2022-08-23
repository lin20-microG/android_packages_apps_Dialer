/*
 * Copyright (C) 2020-2022 The Calyx Institute
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
 * limitations under the License.
 */

package com.android.dialer.common.accounts;

import static android.content.Intent.ACTION_VIEW;
import static android.telephony.PhoneNumberUtils.formatNumberToE164;

import static com.android.dialer.common.accounts.SpecialCallingAccounts.MIME_TYPE_SIGNAL;
import static com.android.dialer.common.accounts.SpecialCallingAccounts.MIME_TYPE_WHATSAPP;
import static com.android.dialer.common.accounts.SpecialCallingAccounts.PACKAGE_NAME_SIGNAL;
import static com.android.dialer.common.accounts.SpecialCallingAccounts.PACKAGE_NAME_WHATSAPP;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;

import com.android.dialer.common.PackageUtils;
import com.android.dialer.location.GeoUtil;

import java.util.List;

import javax.annotation.Nullable;

public class CallAccountUtils {

    private static boolean isInstalled(Context context, String packageName, String mimeType) {
        if (PackageUtils.isPackageEnabled(packageName, context)) {
            Intent i = getCustomCallIntent(0, mimeType);
            final List<ResolveInfo> resolveInfos = context.getPackageManager().
                    queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY);
            return !resolveInfos.isEmpty();
        }
        return false;
    }

    public static boolean isSignalInstalled(Context context) {
        return isInstalled(context, PACKAGE_NAME_SIGNAL, MIME_TYPE_SIGNAL);
    }

    public static boolean isWhatsAppInstalled(Context context) {
        return isInstalled(context, PACKAGE_NAME_WHATSAPP, MIME_TYPE_WHATSAPP);
    }

    @Nullable
    public static Drawable getAppIcon(PackageManager packageManager, Intent i) {
        final List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(i,
                PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfos.isEmpty()) return null;
        ResolveInfo match = resolveInfos.get(0);
        if (match != null) return match.loadIcon(packageManager);
        return null;
    }

    public static Intent getCustomCallIntent(long id, String mimeType) {
        Intent intent = new Intent(ACTION_VIEW);
        final Uri uri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, id);
        intent.setDataAndType(uri, mimeType);
        return intent;
    }

    public static Intent getMsgMeIntent(Context context, String number, String mimeType) {
        // get Uri prefix
        String prefix;
        if (MIME_TYPE_SIGNAL.equals(mimeType)) prefix = "sgnl://signal.me/#p/";
        else if (MIME_TYPE_WHATSAPP.equals(mimeType)) prefix = "whatsapp://send?phone=";
        else throw new IllegalArgumentException("Unknown mimeType: " + mimeType);

        // get Uri
        String e164 = formatNumberToE164(number, GeoUtil.getCurrentCountryIso(context));
        final Uri uri = Uri.parse(prefix + e164);

        Intent intent = new Intent(ACTION_VIEW);
        intent.setData(uri);
        return intent;
    }

    public static boolean isE164Number(Context context, String number) {
        return formatNumberToE164(number, GeoUtil.getCurrentCountryIso(context)) != null;
    }

}
