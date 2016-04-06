/*
 * Copyright (C) 2013 The Android Open Source Project
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

package ru.taaasty;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.Random;

import retrofit.RetrofitError;
import ru.taaasty.rest.ApiErrorException;
import ru.taaasty.rest.RestClient;
import ru.taaasty.utils.GcmUtils;

public class GcmIntentService extends IntentService {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "GcmIntentService";

    private static final String ACTION_START_REGISTER_GCM = "ru.taaasty.GcmIntentService.action.ACTION_START_REGISTER_GCM";
    private static final String ACTION_SEND_GCM_ID_TO_SERVER = "ru.taaasty.GcmIntentService.action.ACTION_SEND_GCM_ID_TO_SERVER";

    private static final int MAX_ATTEMPTS = 5;

    private static final int BACKOFF_MILLI_SECONDS = 2000;

    public static void startSendGcmIdToServer(Context context) {
        Intent intent = new Intent(context, GcmIntentService.class);
        intent.setAction(ACTION_SEND_GCM_ID_TO_SERVER);
        context.startService(intent);
    }

    public static void startRegisterGcm(Context context) {
        Intent intent = new Intent(context, GcmIntentService.class);
        intent.setAction(ACTION_START_REGISTER_GCM);
        context.startService(intent);
    }

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (ACTION_START_REGISTER_GCM.equals(action)) {
            handleRegisterGcm();
        } else if (ACTION_SEND_GCM_ID_TO_SERVER.equals(action)) {
            if (GcmUtils.getInstance(getApplicationContext()).getRegistrationId() == null) return;
            handleRegisterGcmOnServer();
        }
    }

    private void handleRegisterGcmOnServer() {
        String regId = GcmUtils.getInstance(getApplicationContext()).getRegistrationId();
        if (regId == null) return;

        try {
            sendRegistrationIdToBackend(regId);
        } catch (IOException e) {
            if (DBG) Log.e(TAG, "register on server error", e);
        }
    }

    private void handleRegisterGcm() {
        Random random = new Random();
        String newRegId;
        long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            if (DBG) Log.d(TAG, "Attempt #" + i + " to register");
            try {
                newRegId = gcm.register(BuildConfig.GOOGLE_APP_ID);
                sendRegistrationIdToBackend(newRegId);
                GcmUtils.getInstance(getApplicationContext()).onGcmRegistrationComplete(newRegId);
            } catch (IOException ex) {
                Log.e(TAG, "Failed to register on attempt " + i + ":" + ex);
                try {
                    if (DBG) Log.d(TAG, "Sleeping for " + backoff + " ms before retry");
                    Thread.sleep(backoff);
                } catch (InterruptedException e1) {
                    // Activity finished before we complete - exit.
                    if (DBG) Log.d(TAG, "Thread interrupted: abort remaining retries!");
                    Thread.currentThread().interrupt();
                    return;
                }
                // increase backoff exponentially
                backoff *= 2;
            }
        }
    }

    private void sendRegistrationIdToBackend(String regId) throws IOException {
        String userToken = Session.getInstance().getCurrentUserToken();
        try {
            if (userToken != null) {
                RestClient.getAPiDevice().register(regId);
            }
        } catch (ApiErrorException ree) {
            RetrofitError err = (RetrofitError)ree.getCause();
            if (err.getKind() == RetrofitError.Kind.HTTP
                    && err.getResponse() != null
                    && err.getResponse().getStatus() >= 400
                    && err.getResponse().getStatus() < 500) {
                // Скорее всего, "уже существует". Не считаем ошибкой.
                if (DBG) Log.v(TAG, "sendRegistrationIdToBackend response: " + ree.getErrorUserMessage(getResources(), "no response"));
            } else {
                throw new IOException(err);
            }
        } catch (RuntimeException e) {
            throw new IOException(e);
        }
    }

}
