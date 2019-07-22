/*
 * Copyright 2019 Dionysios Karatzas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dkaratzas.android.inapp.update.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import eu.dkaratzas.android.inapp.update.UpdateManager;

import static eu.dkaratzas.android.inapp.update.Constants.UpdateMode;
import static eu.dkaratzas.android.inapp.update.Constants.UpdateStatus;

public class Immediate extends AppCompatActivity implements UpdateManager.InAppUpdateHandler {
    private static final int REQ_CODE_VERSION_UPDATE = 530;
    private static final String TAG = "Immediate";
    private UpdateManager updateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateManager = UpdateManager.Builder(this, REQ_CODE_VERSION_UPDATE)
                .resumeUpdates(true) // Resume the update, if the update was stalled. Default is true
                .mode(UpdateMode.IMMEDIATE);

        updateManager.checkForAppUpdate();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_CODE_VERSION_UPDATE) {
            if (resultCode == Activity.RESULT_CANCELED) {
                // If the update is cancelled by the user,
                // you can request to start the update again.
                updateManager.checkForAppUpdate();

                Log.d(TAG, "Update flow failed! Result code: " + resultCode);
            }
        }

    }

    // InAppUpdateHandler implementation

    @Override
    public void onUpdateError(int code, Throwable error) {
        /*
         * Called when some error occurred. See Constants class for more details
         */
        Log.d(TAG, "code: " + code, error);
    }

    @Override
    public void onStatusUpdate(UpdateStatus status) {
        /*
         * Called when the update status change occurred. See Constants class for more details
         */
        Log.d(TAG, "status: " + status.id());
    }
}
