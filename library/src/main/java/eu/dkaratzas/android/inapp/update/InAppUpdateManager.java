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

import android.content.IntentSender;
import android.util.Log;
import android.view.View;

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

import static eu.dkaratzas.android.inapp.update.Constants.UpdateMode;

/**
 * A simple implementation of the Android In-App Update API.
 * <p>
 * <div class="special reference">
 * <h3>In-App Updates</h3>
 * <p>For more information about In-App Updates you can check the official
 * <a href="https://developer.android.com/guide/app-bundle/in-app-updates">documentation</a>
 * </p>
 * </div>
 */
public class InAppUpdateManager implements LifecycleObserver {

    /**
     * Callback methods where update events are reported.
     */
    public interface InAppUpdateHandler {
        /**
         * On update error.
         *
         * @param code  the code
         * @param error the error
         */
        void onInAppUpdateError(int code, Throwable error);


        /**
         * Monitoring the update state of the flexible downloads.
         * For immediate updates, Google Play takes care of downloading and installing the update for you.
         *
         * @param status the status
         */
        void onInAppUpdateStatus(InAppUpdateStatus status);
    }

    // region Declarations
    private static final String LOG_TAG = "InAppUpdateManager";
    private AppCompatActivity activity;
    private AppUpdateManager appUpdateManager;
    private int requestCode = 64534;
    private String snackBarMessage = "An update has just been downloaded.";
    private String snackBarAction = "RESTART";
    private UpdateMode mode = UpdateMode.FLEXIBLE;
    private boolean resumeUpdates = true;
    private boolean useCustomNotification = false;
    private InAppUpdateHandler handler;
    private Snackbar snackbar;
    private InAppUpdateStatus inAppUpdateStatus = new InAppUpdateStatus();


    private InstallStateUpdatedListener installStateUpdatedListener = new InstallStateUpdatedListener() {
        @Override
        public void onStateUpdate(InstallState installState) {
            inAppUpdateStatus.setInstallState(installState);

            reportStatus();

            // Show module progress, log state, or install the update.
            if (installState.installStatus() == InstallStatus.DOWNLOADED) {
                // After the update is downloaded, show a notification
                // and request user confirmation to restart the app.
                popupSnackbarForUserConfirmation();
            }
        }
    };
    //endregion

    //region Constructor
    private static InAppUpdateManager instance;

    /**
     * Creates a builder that uses the default requestCode.
     *
     * @param activity the activity
     * @return a new {@link InAppUpdateManager} instance
     */
    public static InAppUpdateManager Builder(AppCompatActivity activity) {
        if (instance == null) {
            instance = new InAppUpdateManager(activity);
        }
        return instance;
    }

    /**
     * Creates a builder
     *
     * @param activity    the activity
     * @param requestCode the request code to later monitor this update request via onActivityResult()
     * @return a new {@link InAppUpdateManager} instance
     */
    public static InAppUpdateManager Builder(AppCompatActivity activity, int requestCode) {
        if (instance == null) {
            instance = new InAppUpdateManager(activity, requestCode);
        }
        return instance;
    }

    private InAppUpdateManager(AppCompatActivity activity) {
        this.activity = activity;
        setupSnackbar();
        activity.getLifecycle().addObserver(this);

        init();
    }

    private InAppUpdateManager(AppCompatActivity activity, int requestCode) {
        this.activity = activity;
        this.requestCode = requestCode;

        init();
    }

    private void init() {
        setupSnackbar();
        activity.getLifecycle().addObserver(this);

        appUpdateManager = AppUpdateManagerFactory.create(this.activity);

        if (mode == UpdateMode.FLEXIBLE)
            appUpdateManager.registerListener(installStateUpdatedListener);

        checkForUpdate(false);
    }
    //endregion

    // region Setters

    /**
     * Set the update mode.
     *
     * @param mode the update mode
     * @return the update manager instance
     */
    public InAppUpdateManager mode(UpdateMode mode) {
        this.mode = mode;
        return this;
    }

    /**
     * Checks that the update is not stalled during 'onResume()'.
     * If the update is downloaded but not installed, will notify
     * the user to complete the update.
     *
     * @param resumeUpdates the resume updates
     * @return the update manager instance
     */
    public InAppUpdateManager resumeUpdates(boolean resumeUpdates) {
        this.resumeUpdates = resumeUpdates;
        return this;
    }

    /**
     * Set the callback handler
     *
     * @param handler the handler
     * @return the update manager instance
     */
    public InAppUpdateManager handler(InAppUpdateHandler handler) {
        this.handler = handler;
        return this;
    }

    /**
     * Use custom notification for the user confirmation needed by the {@link UpdateMode#FLEXIBLE} flow.
     * If this will set to true, need to implement the {@link InAppUpdateHandler} and listen for the {@link InAppUpdateStatus#isDownloaded()} status
     * via {@link InAppUpdateHandler#onInAppUpdateStatus} callback. Then a notification (or some other UI indication) can be used,
     * to inform the user that installation is ready and requests user confirmation to restart the app. The confirmation must
     * call the {@link #completeUpdate} method to finish the update.
     *
     * @param useCustomNotification use custom user confirmation
     * @return the update manager instance
     */
    public InAppUpdateManager useCustomNotification(boolean useCustomNotification) {
        this.useCustomNotification = useCustomNotification;
        return this;
    }

    public InAppUpdateManager snackBarMessage(String snackBarMessage) {
        this.snackBarMessage = snackBarMessage;
        setupSnackbar();
        return this;
    }

    public InAppUpdateManager snackBarAction(String snackBarAction) {
        this.snackBarAction = snackBarAction;
        setupSnackbar();
        return this;
    }


    public InAppUpdateManager snackBarActionColor(int color) {
        snackbar.setActionTextColor(color);
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
        unregisterListener();
    }
    //endregion

    //region Methods

    /**
     * Check for update availability. If there will be an update available
     * will start the update process with the selected {@link UpdateMode}.
     */
    public void checkForAppUpdate() {
        checkForUpdate(true);
    }

    /**
     * Triggers the completion of the app update for the flexible flow.
     */
    public void completeUpdate() {
        appUpdateManager.completeUpdate();
    }
    //endregion

    //region Private Methods

    /**
     * Check for update availability. If there will be an update available
     * will start the update process with the selected {@link UpdateMode}.
     */
    private void checkForUpdate(final boolean startUpdate) {

        // Returns an intent object that you use to check for an update.
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();


        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener(new OnSuccessListener<AppUpdateInfo>() {
            @Override
            public void onSuccess(AppUpdateInfo appUpdateInfo) {
                inAppUpdateStatus.setAppUpdateInfo(appUpdateInfo);

                if (startUpdate) {
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
                    } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_NOT_AVAILABLE) {
                        Log.d(LOG_TAG, "checkForAppUpdate(): No Update available. Code: " + appUpdateInfo.updateAvailability());
                    }
                }

                reportStatus();
            }
        });

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
    private void popupSnackbarForUserConfirmation() {
        if (!useCustomNotification) {
            if (snackbar != null && snackbar.isShownOrQueued())
                snackbar.dismiss();


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

                        inAppUpdateStatus.setAppUpdateInfo(appUpdateInfo);

                        //FLEXIBLE:
                        // If the update is downloaded but not installed,
                        // notify the user to complete the update.
                        if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                            popupSnackbarForUserConfirmation();
                            reportStatus();
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

    private void setupSnackbar() {
        View rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);

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
    }

    private void unregisterListener() {
        if (appUpdateManager != null && installStateUpdatedListener != null)
            appUpdateManager.unregisterListener(installStateUpdatedListener);
    }

    private void reportUpdateError(int errorCode, Throwable error) {
        if (handler != null) {
            handler.onInAppUpdateError(errorCode, error);
        }
    }

    private void reportStatus() {
        if (handler != null) {
            handler.onInAppUpdateStatus(inAppUpdateStatus);
        }
    }

    //endregion
}
