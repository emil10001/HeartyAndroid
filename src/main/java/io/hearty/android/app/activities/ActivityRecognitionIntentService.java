package io.hearty.android.app.activities;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import io.hearty.android.app.R;
import io.hearty.android.app.utils.Constants;
import io.hearty.witness.Witness;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Service that receives ActivityRecognition updates. It receives updates
 * in the background, even if the main Activity is not visible.
 */
public class ActivityRecognitionIntentService extends IntentService {
    private static final String TAG = "ActivityRecognitionIntentService";
    private static final String RIDE_TYPE = "rideType";
    private static final String RUN = "Run";
    private static final String RIDE = "Ride";
    private static final Uri STRAVA_URI = Uri.parse("http://strava.com/nfc/record");

    // Formats the timestamp in the log
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss.SSSZ";

    // Delimits the timestamp from the log info
    private static final String LOG_DELIMITER = ";;";

    // A date formatter
    private SimpleDateFormat mDateFormat;

    // Store the app's shared preferences repository
    private SharedPreferences mPrefs;

    public ActivityRecognitionIntentService() {
        // Set the label for the service's background thread
        super("ActivityRecognitionIntentService");
    }

    /**
     * Called when a new activity detection update is available.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent");

        // Get a handle to the repository
        mPrefs = getApplicationContext().getSharedPreferences(
                Constants.SHARED_PREFERENCES, Context.MODE_PRIVATE);

        // Get a date formatter, and catch errors in the returned timestamp
        try {
            mDateFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance();
        } catch (Exception e) {
            Log.e(TAG, getString(R.string.date_format_error));
        }

        // Format the timestamp according to the pattern, then localize the pattern
        mDateFormat.applyPattern(DATE_FORMAT_PATTERN);
        mDateFormat.applyLocalizedPattern(mDateFormat.toLocalizedPattern());

        // If the intent contains an update
        if (ActivityRecognitionResult.hasResult(intent)) {

            // Get the update
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            // Log the update
            logActivityRecognitionResult(result);

            // Get the most probable activity from the list of activities in the update
            DetectedActivity mostProbableActivity = result.getMostProbableActivity();

            // Get the confidence percentage for the most probable activity
            int confidence = mostProbableActivity.getConfidence();

            // Get the type of activity
            int activityType = mostProbableActivity.getType();

            Log.d(TAG, "acitivty: " + getNameFromType(activityType));

            if (confidence >= 50) {
                String mode = getNameFromType(activityType);

                if (activityType == DetectedActivity.ON_FOOT) {
                    DetectedActivity betterActivity = walkingOrRunning(result.getProbableActivities());

                    if (null != betterActivity) {
                        mode = getNameFromType(betterActivity.getType());
                        mostProbableActivity = betterActivity;
                    }
                }

                sendNotification(mode);
                Witness.notify(new HeartyActivity(mode, mostProbableActivity));
            }
        }
    }

    private DetectedActivity walkingOrRunning(List<DetectedActivity> probableActivities) {
        DetectedActivity myActivity = null;
        int confidence = 0;
        for (DetectedActivity activity : probableActivities) {
            if (activity.getType() != DetectedActivity.RUNNING && activity.getType() != DetectedActivity.WALKING)
                continue;

            if (activity.getConfidence() > confidence)
                myActivity = activity;
        }

        return myActivity;
    }


    /**
     * Post a notification to the user. The notification prompts the user to click it to
     * open the device's GPS settings
     */
    private void sendNotification(String mode) {

        // Create a notification builder that's compatible with platforms >= version 4
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(getApplicationContext());

        // Set the title, text, and icon
        builder.setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_launcher);


        if (RUN.equals(mode) || RIDE.equals(mode)) {
            builder.setContentText("start a " + mode)
                    // Get the Intent that starts the Location settings panel
                    .setContentIntent(getStravaIntent(mode));
        } else {
            builder.setContentText("activity: " + mode);
        }

        // Get an instance of the Notification Manager
        NotificationManager notifyManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);

        // Build the notification and post it
        notifyManager.notify(0, builder.build());
    }

    /**
     * Get a content Intent for the notification
     *
     * @return A PendingIntent that starts the device's Location Settings panel.
     */
    private PendingIntent getStravaIntent(String mode) {

        Intent intent = new Intent(Intent.ACTION_RUN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(RIDE_TYPE, mode);
        intent.setData(STRAVA_URI);

        // Create a PendingIntent to start an Activity
        return PendingIntent.getActivity(getApplicationContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Tests to see if the activity has changed
     *
     * @param currentType The current activity type
     * @return true if the user's current activity is different from the previous most probable
     * activity; otherwise, false.
     */
    private boolean activityChanged(int currentType) {

        // Get the previous type, otherwise return the "unknown" type
        int previousType = mPrefs.getInt(Constants.KEY_PREVIOUS_ACTIVITY_TYPE,
                DetectedActivity.UNKNOWN);

        // If the previous type isn't the same as the current type, the activity has changed
        if (previousType != currentType) {
            return true;

            // Otherwise, it hasn't.
        } else {
            return false;
        }
    }

    /**
     * Determine if an activity means that the user is moving.
     *
     * @param type The type of activity the user is doing (see DetectedActivity constants)
     * @return true if the user seems to be moving from one location to another, otherwise false
     */
    private boolean isMoving(int type) {
        switch (type) {
            // These types mean that the user is probably not moving
            case DetectedActivity.STILL:
            case DetectedActivity.TILTING:
            case DetectedActivity.UNKNOWN:
                return false;
            default:
                return true;
        }
    }

    /**
     * Write the activity recognition update to the log file
     *
     * @param result The result extracted from the incoming Intent
     */
    private void logActivityRecognitionResult(ActivityRecognitionResult result) {
        // Get all the probably activities from the updated result
        for (DetectedActivity detectedActivity : result.getProbableActivities()) {

            // Get the activity type, confidence level, and human-readable name
            int activityType = detectedActivity.getType();
            int confidence = detectedActivity.getConfidence();
            String activityName = getNameFromType(activityType);

            // Make a timestamp
            String timeStamp = mDateFormat.format(new Date());

            // Get the current log file or create a new one, then log the activity
            Log.d(TAG, getApplicationContext().getString(R.string.log_message, activityType, activityName, confidence));
        }
    }

    /**
     * Map detected activity types to strings
     *
     * @param activityType The detected activity type
     * @return A user-readable name for the type
     */
    private String getNameFromType(int activityType) {
        switch (activityType) {
            case DetectedActivity.IN_VEHICLE:
                return "in_vehicle";
            case DetectedActivity.ON_BICYCLE:
                return RIDE;
            case DetectedActivity.RUNNING:
                return RUN;
            case DetectedActivity.WALKING:
                return "walking";
            case DetectedActivity.ON_FOOT:
                return "on_foot";
            case DetectedActivity.STILL:
                return "still";
            case DetectedActivity.TILTING:
                return "tilting";
            default:
                return "unknown";
        }
    }
}
