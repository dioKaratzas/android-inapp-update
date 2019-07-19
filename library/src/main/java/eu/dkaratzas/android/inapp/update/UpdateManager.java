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

package eu.dkaratzas.android.inapp.update;

import android.app.Activity;
import android.content.IntentSender;
import android.os.Build;
import android.util.Log;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.OnSuccessListener;
import com.google.android.play.core.tasks.Task;

import static eu.dkaratzas.android.inapp.update.Constants.*;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class UpdateManager implements LifecycleObserver {

    public interface InAppUpdateHandler {
        void onUpdateError(int code, Throwable error);

        void onStatusUpdate(UpdateStatus status);
    }

    // region Declarations
    private static final String LOG_TAG = "UpdateManager";
    private Activity activity;
    private AppUpdateManager appUpdateManager;
    private int requestCode = 64534;
    private String snackBarMessage = "An update has just been downloaded.";
    private String snackBarAction = "RESTART";
    private UpdateMode mode = UpdateMode.FLEXIBLE;
    private boolean resumeUpdates = true;
    private boolean useDefaultSnackbar = true;
    private InAppUpdateHandler handler;
    private Snackbar snackbar;


    private InstallStateUpdatedListener installStateUpdatedListener = new InstallStateUpdatedListener() {
        @Override
        public void onStateUpdate(InstallState installState) {
            int status = installState.installStatus();
            switch (status) {
                case InstallStatus.DOWNLOADING:
                case InstallStatus.DOWNLOADED:
                case InstallStatus.FAILED:
                case InstallStatus.CANCELED:
                    reportStatus(status);
                    break;
            }
            // Show module progress, log state, or install the update.
            if (mode == UpdateMode.FLEXIBLE
                    && installState.installStatus() == InstallStatus.DOWNLOADED) {
                // After the update is downloaded, show a notification
                // and request user confirmation to restart the app.
                popupSnackbarForCompleteUpdate();
            }
        }
    };
    //endregion

    //region Constructor
    private static UpdateManager instance;

    public static UpdateManager Builder(AppCompatActivity activity) {
        if (instance == null) {
            instance = new UpdateManager(activity);
        }
        return instance;
    }

    public static UpdateManager Builder(AppCompatActivity activity, int requestCode) {
        if (instance == null) {
            instance = new UpdateManager(activity, requestCode);
        }
        return instance;
    }

    private UpdateManager(AppCompatActivity activity) {
        this.activity = activity;

        activity.getLifecycle().addObserver(this);

        appUpdateManager = AppUpdateManagerFactory.create(this.activity);
        appUpdateManager.registerListener(installStateUpdatedListener);
    }

    private UpdateManager(AppCompatActivity activity, int requestCode) {
        this.activity = activity;
        this.requestCode = requestCode;

        activity.getLifecycle().addObserver(this);

        appUpdateManager = AppUpdateManagerFactory.create(this.activity);
        appUpdateManager.registerListener(installStateUpdatedListener);
    }
    //endregion

    // region Setters
    public UpdateManager setMode(UpdateMode mode) {
        this.mode = mode;
        return this;
    }

    public UpdateManager setSnackBarMessage(String snackBarMessage) {
        this.snackBarMessage = snackBarMessage;
        return this;
    }

    public UpdateManager setSnackBarAction(String snackBarAction) {
        this.snackBarAction = snackBarAction;
        return this;
    }

    public UpdateManager setResumeUpdates(boolean resumeUpdates) {
        this.resumeUpdates = resumeUpdates;
        return this;
    }

    public UpdateManager setHandler(InAppUpdateHandler handler) {
        this.handler = handler;
        return this;
    }

    public UpdateManager setUseDefaultSnackbar(boolean useDefaultSnackbar) {
        this.useDefaultSnackbar = useDefaultSnackbar;
        return this;
    }

    //endregion

    //region Lifecycle
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        if (resumeUpdates)
            checkNewAppVersionState();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        unregisterInstallStateUpdListener();
    }
    //endregion

    //region Private Methods
    public void checkForAppUpdate() {

        // Returns an intent object that you use to check for an update.
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener(new OnSuccessListener<AppUpdateInfo>() {
            @Override
            public void onSuccess(AppUpdateInfo appUpdateInfo) {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    // Request the update.
                    if (mode == UpdateMode.FLEXIBLE && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        // Start an update.
                        startAppUpdateFlexible(appUpdateInfo);
                    } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        // Start an update.
                        startAppUpdateImmediate(appUpdateInfo);
                    }

                    Log.d(LOG_TAG, "checkForAppUpdate(): Update available. Version Code: " + appUpdateInfo.availableVersionCode());
                } else {
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_NOT_AVAILABLE)
                        reportStatus(UpdateStatus.UPDATE_NOT_AVAILABLE.id());

                    Log.d(LOG_TAG, "checkForAppUpdate(): No Update available. Code: " + appUpdateInfo.updateAvailability());
                }
            }
        });

    }

    // Triggers the completion of the update of the app for the flexible flow.
    public void completeUpdate() {
        appUpdateManager.completeUpdate();
    }

    private void startAppUpdateImmediate(AppUpdateInfo appUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    // The current activity making the update request.
                    activity,
                    // Include a request code to later monitor this update request.
                    requestCode);
        } catch (IntentSender.SendIntentException e) {
            Log.e(LOG_TAG, "error in startAppUpdateImmediate", e);
            reportUpdateError(Constants.UPDATE_ERROR_START_APP_UPDATE_IMMEDIATE, e);
        }
    }

    private void startAppUpdateFlexible(AppUpdateInfo appUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.FLEXIBLE,
                    // The current activity making the update request.
                    activity,
                    // Include a request code to later monitor this update request.
                    requestCode);
        } catch (IntentSender.SendIntentException e) {
            Log.e(LOG_TAG, "error in startAppUpdateFlexible", e);
            reportUpdateError(Constants.UPDATE_ERROR_START_APP_UPDATE_FLEXIBLE, e);
        }
    }

    /**
     * Displays the snackbar notification and call to action.
     * Needed only for Flexible app update
     */
    private void popupSnackbarForCompleteUpdate() {
        if (useDefaultSnackbar) {
            View rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);
            if (snackbar != null)
                snackbar.dismiss();

            snackbar = Snackbar.make(rootView,
                    snackBarMessage,
                    Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction(snackBarAction, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Triggers the completion of the update of the app for the flexible flow.
                    appUpdateManager.completeUpdate();
                }
            });
            snackbar.show();
        }
    }

    /**
     * Checks that the update is not stalled during 'onResume()'.
     * However, you should execute this check at all app entry points.
     */
    private void checkNewAppVersionState() {

        appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(new OnSuccessListener<AppUpdateInfo>() {
                    @Override
                    public void onSuccess(AppUpdateInfo appUpdateInfo) {
                        //FLEXIBLE:
                        // If the update is downloaded but not installed,
                        // notify the user to complete the update.
                        if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                            popupSnackbarForCompleteUpdate();
                            reportStatus(UpdateStatus.DOWNLOADED.id());
                            Log.d(LOG_TAG, "checkNewAppVersionState(): resuming flexible update. Code: " + appUpdateInfo.updateAvailability());
                        }

                        //IMMEDIATE:
                        if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                            // If an in-app update is already running, resume the update.
                            startAppUpdateImmediate(appUpdateInfo);

                            Log.d(LOG_TAG, "checkNewAppVersionState(): resuming immediate update. Code: " + appUpdateInfo.updateAvailability());

                        }
                    }
                });

    }

    /**
     * Needed only for FLEXIBLE update
     */
    private void unregisterInstallStateUpdListener() {
        if (appUpdateManager != null && installStateUpdatedListener != null)
            appUpdateManager.unregisterListener(installStateUpdatedListener);
    }

    private void reportUpdateError(int errorCode, Throwable error) {
        if (handler != null) {
            handler.onUpdateError(errorCode, error);
        }
    }

    private void reportStatus(int status) {
        if (handler != null) {
            handler.onStatusUpdate(UpdateStatus.fromId(status));
        }
    }

    //endregion
}
