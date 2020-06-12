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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.dialer.common.accounts.CallAccount.Status;
import com.android.dialer.contacts.resources.R;

import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

class CallAccountAdapter extends ArrayAdapter<CallAccount> {

  static final class ViewHolder {
    ImageView accountIcon;
    TextView label;
    ImageView statusIcon;
    TextView status;
  }

  private final int mResId;

  CallAccountAdapter(Context context, int resource, List<CallAccount> accounts) {
    super(context, resource, accounts);
    mResId = resource;
  }

  @Override
  public boolean areAllItemsEnabled() {
    return false;
  }

  @Override
  public boolean isEnabled(int position) {
    return getItem(position).status != Status.DISABLED;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    LayoutInflater inflater =
        (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    View rowView;
    final ViewHolder ui;

    if (convertView == null) {
      rowView = inflater.inflate(mResId, null);
      ui = new ViewHolder();
      ui.accountIcon = rowView.findViewById(R.id.icon);
      ui.label = rowView.findViewById(R.id.label);
      ui.statusIcon = rowView.findViewById(R.id.statusIcon);
      ui.status = rowView.findViewById(R.id.status);
      rowView.setTag(ui);
    } else {
      rowView = convertView;
      ui = (ViewHolder) rowView.getTag();
    }

    CallAccount account = getItem(position);
    rowView.setEnabled(account.status == Status.ENABLED);
    ui.accountIcon.setImageDrawable(account.icon);
    ui.accountIcon.setAlpha(account.status == Status.ENABLED ? 1.0f : 0.5f);
    ui.label.setText(account.name);
    if (account.encrypted) {
      ui.statusIcon.setImageResource(account.status == Status.ENABLED ?
          R.drawable.ic_baseline_lock : R.drawable.ic_baseline_warning);
      ui.statusIcon.setVisibility(VISIBLE);
    } else {
      ui.statusIcon.setVisibility(GONE);
    }
    Context context = rowView.getContext();
    if (account.status == Status.ENABLED) {
      ui.status.setText(account.encrypted
          ? context.getString(R.string.call_account_private)
          : context.getString(R.string.call_account_not_private));
    } else {
      ui.status.setText(account.unavailableText);
    }
    return rowView;
  }

}
