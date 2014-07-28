package io.hearty.android.app.activities;

import com.google.android.gms.location.DetectedActivity;

/**
 * Created by ejf3 on 7/28/14.
 */
public class HeartyActivity {
    private final String activityName;
    private final DetectedActivity detectedActivity;

    public HeartyActivity(String activityName, DetectedActivity detectedActivity) {
        this.activityName = activityName;
        this.detectedActivity = detectedActivity;
    }

    public DetectedActivity getDetectedActivity() {
        return detectedActivity;
    }

    public String getActivityName() {
        return activityName;
    }
}
