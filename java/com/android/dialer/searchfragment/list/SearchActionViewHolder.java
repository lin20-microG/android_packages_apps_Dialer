/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.dialer.searchfragment.list;

import static com.android.dialer.common.accounts.CallAccountUtils.getAppIcon;
import static com.android.dialer.common.accounts.CallAccountUtils.getMsgMeIntent;
import static com.android.dialer.common.accounts.SpecialCallingAccounts.MIME_TYPE_SIGNAL;
import static com.android.dialer.common.accounts.SpecialCallingAccounts.MIME_TYPE_WHATSAPP;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.dialer.common.Assert;
import com.android.dialer.logging.DialerImpression;
import com.android.dialer.logging.Logger;
import com.android.dialer.searchfragment.common.RowClickListener;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.util.IntentUtil;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * {@link RecyclerView.ViewHolder} for showing an {@link SearchActionViewHolder.Action} in a list.
 */
final class SearchActionViewHolder extends RecyclerView.ViewHolder implements OnClickListener {

  /** IntDef for the different types of actions that can be used. */
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({
    Action.INVALID,
    Action.CREATE_NEW_CONTACT,
    Action.ADD_TO_CONTACT,
    Action.SEND_SMS,
    Action.MAKE_VILTE_CALL,
    Action.MAKE_VOICE_CALL
  })
  @interface Action {
    int INVALID = 0;
    /** Opens the prompt to create a new contact. */
    int CREATE_NEW_CONTACT = 1;
    /** Opens a prompt to add to an existing contact. */
    int ADD_TO_CONTACT = 2;
    /** Opens the SMS conversation with the default SMS app. */
    int SEND_SMS = 3;
    /** Attempts to make a VILTE call to the number. */
    int MAKE_VILTE_CALL = 4;
    /** Places a voice call to the number. */
    int MAKE_VOICE_CALL = 5;
    /** Opens a wa.me link */
    int WHATSAPP = Integer.MAX_VALUE - 1;
    /** Opens a signal.me link */
    int SIGNAL = Integer.MAX_VALUE;
  }

  private final Context context;
  private final ImageView actionImage;
  private final TextView actionText;
  private final RowClickListener listener;
  private final ColorStateList actionImageTint;

  private @Action int action;
  private int position;
  private String query;

  SearchActionViewHolder(View view, RowClickListener listener) {
    super(view);
    context = view.getContext();
    actionImage = view.findViewById(R.id.search_action_image);
    actionText = view.findViewById(R.id.search_action_text);
    this.listener = listener;
    view.setOnClickListener(this);
    actionImageTint = actionImage.getImageTintList();
  }

  void setAction(@Action int action, int position, String query) {
    this.action = action;
    this.position = position;
    this.query = query;
    // set default tint to prevent view recycling issues when clearing tint for custom actions
    actionImage.setImageTintList(actionImageTint);
    switch (action) {
      case Action.ADD_TO_CONTACT:
        actionText.setText(R.string.search_shortcut_add_to_contact);
        actionImage.setImageResource(R.drawable.quantum_ic_person_add_vd_theme_24);
        break;
      case Action.CREATE_NEW_CONTACT:
        actionText.setText(R.string.search_shortcut_create_new_contact);
        actionImage.setImageResource(R.drawable.quantum_ic_person_add_vd_theme_24);
        break;
      case Action.MAKE_VILTE_CALL:
        actionText.setText(R.string.search_shortcut_make_video_call);
        actionImage.setImageResource(R.drawable.quantum_ic_videocam_vd_theme_24);
        break;
      case Action.SEND_SMS:
        actionText.setText(R.string.search_shortcut_send_sms_message);
        actionImage.setImageResource(R.drawable.quantum_ic_message_vd_theme_24);
        break;
      case Action.MAKE_VOICE_CALL:
        actionText.setText(context.getString(R.string.search_shortcut_make_voice_call, query));
        actionImage.setImageResource(R.drawable.quantum_ic_phone_vd_theme_24);
        break;
      case Action.WHATSAPP:
        actionText.setText(context.getString(R.string.dialer_search_action,
            context.getString(R.string.call_account_whatsapp)));
        Intent iw = getMsgMeIntent(context, query, MIME_TYPE_WHATSAPP);
        Drawable drawableW = getAppIcon(context.getPackageManager(), iw);
        actionImage.setImageTintList(null);
        actionImage.setImageDrawable(drawableW);
        break;
      case Action.SIGNAL:
        actionText.setText(context.getString(R.string.dialer_search_action,
            context.getString(R.string.call_account_signal)));
        Intent is = getMsgMeIntent(context, query, MIME_TYPE_SIGNAL);
        Drawable drawableS = getAppIcon(context.getPackageManager(), is);
        actionImage.setImageTintList(null);
        actionImage.setImageDrawable(drawableS);
        break;
      case Action.INVALID:
      default:
        throw Assert.createIllegalStateFailException("Invalid action: " + action);
    }
  }

  @VisibleForTesting
  @Action
  int getAction() {
    return action;
  }

  @Override
  public void onClick(View v) {
    switch (action) {
      case Action.ADD_TO_CONTACT:
        Logger.get(context).logImpression(DialerImpression.Type.ADD_TO_A_CONTACT_FROM_DIALPAD);
        Intent intent = IntentUtil.getAddToExistingContactIntent(query);
        @StringRes int errorString = R.string.add_contact_not_available;
        DialerUtils.startActivityWithErrorToast(context, intent, errorString);
        break;

      case Action.CREATE_NEW_CONTACT:
        Logger.get(context).logImpression(DialerImpression.Type.CREATE_NEW_CONTACT_FROM_DIALPAD);
        intent = IntentUtil.getNewContactIntent(query);
        DialerUtils.startActivityWithErrorToast(context, intent);
        break;

      case Action.MAKE_VILTE_CALL:
        listener.placeVideoCall(query, position);
        break;

      case Action.SEND_SMS:
        intent = IntentUtil.getSendSmsIntent(query);
        DialerUtils.startActivityWithErrorToast(context, intent);
        break;

      case Action.MAKE_VOICE_CALL:
        listener.placeVoiceCall(query, null, position);
        break;

      case Action.SIGNAL:
        intent = getMsgMeIntent(context, query, MIME_TYPE_SIGNAL);
        DialerUtils.startActivityWithErrorToast(context, intent);
        break;

      case Action.WHATSAPP:
        intent = getMsgMeIntent(context, query, MIME_TYPE_WHATSAPP);
        DialerUtils.startActivityWithErrorToast(context, intent);
        break;

      case Action.INVALID:
      default:
        throw Assert.createIllegalStateFailException("Invalid action: " + action);
    }
  }
}
