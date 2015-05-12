/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.taaasty;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import ru.taaasty.utils.GcmUtils;


/**
 * This {@code WakefulBroadcastReceiver} takes care of creating and managing a
 * partial wake lock for your app. It passes off the work of processing the GCM
 * message to an {@code IntentService}, while ensuring that the device does not
 * go back to sleep in the transition. The {@code IntentService} calls
 * {@code GcmBroadcastReceiver.completeWakefulIntent()} when it is ready to
 * release the wake lock.
 */

public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {
    private static final String TAG = "GcmBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        setResultCode(Activity.RESULT_OK);
        if (!"com.google.android.c2dm.intent.RECEIVE".equals(intent.getAction())) return;
        if (intent.getExtras() == null || intent.getExtras().isEmpty()) return;

        String messageType = intent.getStringExtra("message_type");
        if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
            // Не знаю, при каких обстоятельствах может возникнуть
            Log.v(TAG, "GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR");
        } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
            // А похер, ну удалил и удалил
            Log.v(TAG, "GoogleCloudMessaging.MESSAGE_TYPE_DELETED");
        } else if (messageType == null || GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            ComponentName comp;
            // Explicitly specify that GcmIntentService will handle the intent.
            if (GcmUtils.isTastyMessage(intent.getExtras())) {
                Log.v(TAG, "GCM tasty message " + GcmUtils.getGcmNotificationType(intent.getExtras()));
                comp = new ComponentName(context.getPackageName(),
                        PusherService.class.getName());
                intent.setAction(PusherService.ACTION_HANDLE_GCM_PUSH);
                // Start the service, keeping the device awake while it is launching.
                startWakefulService(context, (intent.setComponent(comp)));
            } else {
                if (BuildConfig.DEBUG) Log.v(TAG, "Skipped GCM message " + intent.getExtras());
            }
        }
    }
}
