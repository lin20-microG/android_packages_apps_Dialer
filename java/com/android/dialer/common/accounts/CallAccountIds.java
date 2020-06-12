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

import android.database.Cursor;
import android.telephony.PhoneNumberUtils;
import com.android.dialer.common.LogUtil;

import static com.android.dialer.common.accounts.SpecialCallingAccounts.MIME_TYPE_SIGNAL;
import static com.android.dialer.common.accounts.SpecialCallingAccounts.MIME_TYPE_WHATSAPP;

public class CallAccountIds {

  public final long signalId, whatsappId;

  private CallAccountIds(long signalId, long whatsappId) {
    this.signalId = signalId;
    this.whatsappId = whatsappId;
  }

  public static CallAccountIds fromCursor(Cursor cursor, String number) {
    String normalizedNumber = normalizeNumber(number);
    long signalId = -1;
    long whatsappId = -1;
    while (cursor.moveToNext()) {
      long id = cursor.getLong(0);
      String mimeType = cursor.getString(1);
      String data1 = cursor.getString(2);
      if (isMatch(data1, normalizedNumber)) {
        if (MIME_TYPE_SIGNAL.equals(mimeType)) signalId = id;
        else if (MIME_TYPE_WHATSAPP.equals(mimeType)) whatsappId = id;
      } else {
        LogUtil.w("CallAccountIds", "Numbers did not match: data=" + data1 + " input=" + number
            + " inputNorm="+ normalizedNumber);
      }
    }
    return new CallAccountIds(signalId, whatsappId);
  }

  private static boolean isMatch(String dataNumber, String targetNumber) {
    String normalizedData = normalizeNumber(dataNumber);
    if (normalizedData.length() >= targetNumber.length())
      return normalizedData.endsWith(targetNumber);
    else return targetNumber.endsWith(normalizedData);
  }

  private static String normalizeNumber(String number) {
    String result = number;
    // remove 00 that is sometimes used instead of +
    if (result.startsWith("00")) result = result.replaceFirst("00", "");
    // remove email style number that WhatsApp is using
    if (result.indexOf('@') != -1) result = result.substring(0, result.indexOf('@'));
    // normalize number, removes dashes, brackets and other stuff
    result = PhoneNumberUtils.normalizeNumber(result);
    // remove the + that sometimes appears after normalizing
    if (result.startsWith("+")) result = result.replaceFirst("\\+", "");
    return result;
  }

}
