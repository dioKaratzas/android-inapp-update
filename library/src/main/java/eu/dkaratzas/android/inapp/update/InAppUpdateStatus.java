package eu.dkaratzas.android.inapp.update;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

/**
 * This class is just a wrapper for AppUpdateInfo and InstallState
 * Used by InAppUpdateManager
 */
public class InAppUpdateStatus {

    private static final int NO_UPDATE = 0;
    private AppUpdateInfo appUpdateInfo;
    private InstallState installState;

    public InAppUpdateStatus() {
    }

    public void setAppUpdateInfo(AppUpdateInfo appUpdateInfo) {
        this.appUpdateInfo = appUpdateInfo;
    }

    public void setInstallState(InstallState installState) {
        this.installState = installState;
    }

    public boolean isDownloading() {
        if (installState != null)
            return installState.installStatus() == InstallStatus.DOWNLOADING;

        return false;
    }

    public boolean isDownloaded() {
        if (installState != null)
            return installState.installStatus() == InstallStatus.DOWNLOADED;

        return false;
    }

    public boolean isFailed() {
        if (installState != null)
            return installState.installStatus() == InstallStatus.FAILED;

        return false;
    }

    public boolean isUpdateAvailable() {
        if (appUpdateInfo != null)
            return appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE;

        return false;
    }

    public int availableVersionCode() {
        if (appUpdateInfo != null)
            return appUpdateInfo.availableVersionCode();

        return NO_UPDATE;
    }
}
