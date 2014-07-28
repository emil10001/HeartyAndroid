package io.hearty.android.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import io.hearty.ble.lib.data.HeartRate;
import io.hearty.ble.lib.heart.BleHeartService;
import io.hearty.ble.lib.utils.BleUtils;
import io.hearty.witness.Reporter;
import io.hearty.witness.Witness;

/**
 * Created by ejf3 on 7/27/14.
 */
public class HrmFrag extends Fragment implements Reporter {
    private static final String TAG = "HrmFrag";
    private static final int REQUEST_ENABLE_BT = 23484;
    private static final int WEAR_REQUEST = 239;
    private Handler handler = new Handler();
    private TextView tv;
    public static final String EXTRA_REPLY = "reply";
    private static final String ACTION_RESPONSE = "io.hearty.hrm.REPLY";
    private BroadcastReceiver mReceiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.hrm, container, false);
        tv = (TextView) rootView.findViewById(R.id.hrm);

        BluetoothAdapter bleAdapter = BleUtils.getAdapter(getActivity());
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bleAdapter == null || !bleAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                processResponse(intent);
            }
        };

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        Witness.register(BluetoothDevice.class, this);
        Witness.register(HeartRate.class, this);

        getActivity().registerReceiver(mReceiver, new IntentFilter(ACTION_RESPONSE));
        getActivity().startService(new Intent(getActivity(), BleHeartService.class));
    }

    @Override
    public void onPause() {
        Witness.remove(BluetoothDevice.class, this);
        Witness.remove(HeartRate.class, this);

        getActivity().unregisterReceiver(mReceiver);

        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUEST_ENABLE_BT == requestCode) {
            handleBleEnableResult(resultCode, data);
        } else if (WEAR_REQUEST == requestCode) {
            processResponse(data);
        }
    }

    private void handleBleEnableResult(int resultCode, Intent data) {
        Log.d(TAG, "handleBleEnableResult: " + resultCode);
        if (Activity.RESULT_OK == resultCode) {
            if (null == data.getExtras() || null == data.getExtras().keySet())
                return;

            for (String extra : data.getExtras().keySet()) {
                Log.d(TAG, extra + ": " + data.getExtras().get(extra));
            }
        }
    }

    private void updateText(String name) {
        Log.d(MainActivity.class.toString(), "updateText " + name);
        tv.setText(name);
    }

    // for Android Wear
    private void notifyHeartRate(HeartRate heartRate) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity())
                .setContentTitle(getString(R.string.app_name))
                .setContentText(Integer.toString(heartRate.getHeartRate()))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));

        NotificationManager notifyManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

        notifyManager.notify(0, builder.build());

    }

    @Override
    public void notifyEvent(final Object o) {
        handler.post(new Runnable() {
            @Override
            public void run() {

                if (o instanceof BluetoothDevice) {
                    BluetoothDevice device = ((BluetoothDevice) o);
                    Log.d(MainActivity.class.toString(), "found device " + device.getName());
                    updateText(device.getName());
                } else if (o instanceof HeartRate) {
                    HeartRate heartRate = ((HeartRate) o);
                    Log.d(MainActivity.class.toString(), "heart rate " + heartRate.getHeartRate());
                    notifyHeartRate(heartRate);
                }

            }
        });

    }

    private void processResponse(Intent intent) {
        String text = intent.getStringExtra(EXTRA_REPLY);
        if (text != null && !text.equals("")) {
            Log.d(MainActivity.class.toString(), "response was " + text);
        }
    }

}
