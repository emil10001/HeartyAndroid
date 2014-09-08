package io.hearty.android.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.*;
import io.hearty.android.app.activities.DetectionRemover;
import io.hearty.android.app.activities.DetectionRequester;
import io.hearty.android.app.steps.Steps;
import io.hearty.android.app.utils.Constants;
import io.hearty.witness.Witness;

import java.util.List;

/**
 * Created by ejf3 on 7/17/14.
 */
public class HeartyService extends WearableListenerService {
    private static final long THREE_MINUTES = 3 * 60 * 1000;
    private static final String STEP_COUNT_PATH = "/step-count";
    private static final String STEP_COUNT_KEY = "step-count";


    public static final String TAG = "HeartyService";

    /*
     *  Intent filter for incoming broadcasts from the
     *  IntentService.
     */
    IntentFilter mBroadcastFilter;

    // Instance of a local broadcast manager
    private LocalBroadcastManager mBroadcastManager;

    // The activity recognition update request object
    private DetectionRequester mDetectionRequester;

    // The activity recognition update removal object
    private DetectionRemover mDetectionRemover;

    private GoogleApiClient mGoogleApiClient;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        setAlarm();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        if (null == mBroadcastManager) {
            // Set the broadcast receiver intent filer
            mBroadcastManager = LocalBroadcastManager.getInstance(this);
        }

        if (null == mBroadcastFilter) {
            // Create a new Intent filter for the broadcast receiver
            mBroadcastFilter = new IntentFilter(Constants.ACTION_REFRESH_STATUS_LIST);
            mBroadcastFilter.addCategory(Constants.CATEGORY_LOCATION_SERVICES);
        }

        if (null == mDetectionRequester)
            // Get detection requester and remover objects
            mDetectionRequester = new DetectionRequester(this);

        if (null == mDetectionRemover)
            mDetectionRemover = new DetectionRemover(this);

        if (null == mGoogleApiClient)
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .build();

        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();

        mDetectionRequester.requestUpdates();

        // register activity recognition
        return super.onStartCommand(intent, flags, startId);
    }

    private void setAlarm() {
        Intent intent = new Intent(this, AlarmNotify.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 234324243, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        long firstRun = System.currentTimeMillis() + THREE_MINUTES;
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstRun, THREE_MINUTES, pendingIntent);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged: " + dataEvents);
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();
        for (DataEvent event : events) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                Log.d(TAG, "event " + event.getDataItem().toString());
                if (STEP_COUNT_PATH.equals(path)) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    Log.d(TAG, "step count updated " + dataMapItem.getDataMap().getInt(STEP_COUNT_KEY));
                    Witness.notify(new Steps((dataMapItem.getDataMap().getInt(STEP_COUNT_KEY))));
                }
            } else {
                Log.d(TAG, "unknown event type");
            }
        }
    }

    @Override
    public void onPeerConnected(Node peer) {
        Log.d(TAG, "onPeerConnected: " + peer);
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        Log.d(TAG, "onPeerDisconnected: " + peer);
    }

}
