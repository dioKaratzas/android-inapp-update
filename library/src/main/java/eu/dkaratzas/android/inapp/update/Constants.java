package eu.dkaratzas.android.inapp.update;

public class Constants {

    public enum UpdateMode {
        FLEXIBLE,
        IMMEDIATE
    }

    public enum UpdateStatus {
        UNKNOWN(0),
        DOWNLOADING(2),
        DOWNLOADED(11),
        FAILED(5),
        CANCELED(6),
        UPDATE_NOT_AVAILABLE(1);

        private int mValue;

        // Constructor
        UpdateStatus(int value) {
            this.mValue = value;
        }

        // Return enum index
        public int id() {
            return mValue;
        }

        public static UpdateStatus fromId(int value) {
            for (UpdateStatus status : values()) {
                if (status.mValue == value) {
                    return status;
                }
            }
            return UNKNOWN;
        }
    }


    public static final int UPDATE_ERROR_START_APP_UPDATE_FLEXIBLE = 100;
    public static final int UPDATE_ERROR_START_APP_UPDATE_IMMEDIATE = 101;

}
