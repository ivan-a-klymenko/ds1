package tech.ai_robotics.drone_shooter_2.bluetooth;

import com.google.android.datatransport.backend.cct.BuildConfig;

class Constants {

    // values have to be globally unique
    static final String INTENT_ACTION_DISCONNECT = "tech.ai_robotics.drone_shooter_2.Disconnect";
    static final String NOTIFICATION_CHANNEL = "tech.ai_robotics.drone_shooter_2.Channel";
    static final String INTENT_CLASS_MAIN_ACTIVITY = "tech.ai_robotics.drone_shooter_2.MainActivity";

    // values have to be unique within each app
    static final int NOTIFY_MANAGER_START_FOREGROUND_SERVICE = 1001;

    public static final String APPLICATION_ID = "tech.ai_robotics.drone_shooter_2";

    private Constants() {}
}
