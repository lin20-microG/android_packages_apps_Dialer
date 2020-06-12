/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import com.android.dialer.contacts.resources.R;
import com.android.dialer.util.DialerUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;

import static com.android.dialer.common.accounts.SpecialCallingAccounts.ACCOUNTS_PROJECTION;
import static com.android.dialer.common.accounts.SpecialCallingAccounts.ACCOUNTS_SELECTION;
import static com.android.dialer.common.accounts.SpecialCallingAccounts.MIME_TYPE_PHONE;
import static com.android.dialer.common.accounts.SpecialCallingAccounts.MIME_TYPE_SIGNAL;
import static com.android.dialer.common.accounts.SpecialCallingAccounts.MIME_TYPE_WHATSAPP;

/**
 * Dialog that allows the user to select an accounts to call a number.
 */
@SuppressWarnings("deprecation")
public class SelectAccountDialogFragment extends DialogFragment implements LoaderCallbacks<Cursor> {

  /**
   * Create new fragment instance with known account IDs.
   */
  public static SelectAccountDialogFragment newInstance(Intent phoneIntent, @Nullable String number,
                                                        long signalId, long whatsappId) {
    SelectAccountDialogFragment fragment = new SelectAccountDialogFragment();
    Bundle arguments = new Bundle();
    arguments.putLong(MIME_TYPE_SIGNAL, signalId);
    arguments.putLong(MIME_TYPE_WHATSAPP, whatsappId);
    arguments.putString(Phone.NUMBER, number);
    arguments.putParcelable(MIME_TYPE_PHONE, phoneIntent);
    fragment.setArguments(arguments);
    return fragment;
  }

  /**
   * Create new fragment instance with a lookup key to lookup account IDs.
   */
  public static SelectAccountDialogFragment newInstance(Intent phoneIntent, String lookupKey,
      String number) {
    SelectAccountDialogFragment fragment = new SelectAccountDialogFragment();
    Bundle arguments = new Bundle();
    arguments.putString(Data.LOOKUP_KEY, lookupKey);
    arguments.putString(Phone.NUMBER, number);
    arguments.putParcelable(MIME_TYPE_PHONE, phoneIntent);
    fragment.setArguments(arguments);
    return fragment;
  }

  private CallAccountCreator callAccountCreator;
  private CallAccountAdapter adapter;
  private final ArrayList<CallAccount> callAccounts = new ArrayList<>();
  private final NetworkReceiver receiver = new NetworkReceiver();
  private boolean isOnline;
  private String number;
  private long latestSignalId = -1, latestWhatsappId = -1;
  private Intent phoneIntent;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    Bundle arguments = getArguments();
    // initialize variable that need to be available when the loader finishes
    callAccountCreator = new CallAccountCreator(context);
    phoneIntent = arguments.getParcelable(MIME_TYPE_PHONE);
    number = arguments.getString(Phone.NUMBER);
    // kick of loader, if necessary (aka a lookup key was provided)
    String lookupKey = arguments.getString(Data.LOOKUP_KEY, null);
    if (lookupKey != null) {
      getLoaderManager().initLoader(0, getArguments(), this);
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // only update accounts if the loader has not done so already
    if (callAccounts.isEmpty()) {
      Bundle arguments = getArguments();
      final long signalId = arguments.getLong(MIME_TYPE_SIGNAL, -1);
      final long whatsappId = arguments.getLong(MIME_TYPE_WHATSAPP, -1);
      updateAccounts(signalId, whatsappId);
    }
    adapter = new CallAccountAdapter(getContext(), R.layout.call_account_list_item, callAccounts);

    isOnline = isOnline(getContext());
    // Register BroadcastReceiver to track network connection changes.
    IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    getContext().registerReceiver(receiver, filter);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    getContext().unregisterReceiver(receiver);
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    final DialogInterface.OnClickListener selectionListener = (dialog, which) -> {
      CallAccount callAccount = callAccounts.get(which);
      DialerUtils.startActivityWithErrorToast(getContext(), callAccount.intent);
    };
    return builder
        .setTitle(R.string.call_account_choose_title)
        .setAdapter(adapter, selectionListener)
        .create();
  }

  @Override
  public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    String lookupKey = bundle.getString(Data.LOOKUP_KEY);
    return new CursorLoader(
        getContext(),
        Data.CONTENT_URI,
        ACCOUNTS_PROJECTION,
        ACCOUNTS_SELECTION,
        new String[]{lookupKey, MIME_TYPE_SIGNAL, MIME_TYPE_WHATSAPP},
        null
    );
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    CallAccountIds ids = CallAccountIds.fromCursor(cursor, number);
    updateAccounts(ids.signalId, ids.whatsappId);
    // the adapter might not have been initialized
    if (adapter != null) {
      // the adapter is using the callAccounts field,
      // so no need to swap items, just notify about change
      adapter.notifyDataSetChanged();
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
  }

  /**
   * Clears callAccounts and populates it with fresh accounts.
   */
  private void updateAccounts(long signalId, long whatsappId) {
    latestSignalId = signalId;
    latestWhatsappId = whatsappId;
    ArrayList<CallAccount> accounts = new ArrayList<>();
    accounts.add(callAccountCreator.getSignalAccount(signalId));
    CallAccount whatsappAccount = callAccountCreator.getWhatsAppAccount(whatsappId);
    if (whatsappAccount != null) accounts.add(whatsappAccount);
    accounts.addAll(callAccountCreator.getPhoneAccounts(phoneIntent, number));
    callAccounts.clear();
    callAccounts.addAll(accounts);
  }

  private boolean isOnline(Context context) {
    ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
    NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
    return capabilities != null
        && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
  }

  private class NetworkReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      boolean wasOnline = isOnline;
      isOnline = isOnline(context);
      if (wasOnline != isOnline) {
        updateAccounts(latestSignalId, latestWhatsappId);
        if (adapter != null) adapter.notifyDataSetChanged();
      }
    }
  }

}
