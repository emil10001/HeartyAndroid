package io.hearty.android.app;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import io.hearty.android.app.activities.HeartyActivity;
import io.hearty.witness.Reporter;
import io.hearty.witness.Witness;

/**
 * Created by ejf3 on 7/28/14.
 */
public class ActivitiesFrag extends Fragment implements Reporter {
    private static final String TAG = "StepsFrag";
    private Handler handler = new Handler();

    private TextView tv;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activities, container, false);
        tv = (TextView) rootView.findViewById(R.id.activities);

        return rootView;
    }

    @Override
    public void onResume() {
        Witness.register(HeartyActivity.class, this);
        super.onResume();
    }

    @Override
    public void onPause() {
        Witness.remove(HeartyActivity.class, this);
        super.onPause();
    }


    @Override
    public void notifyEvent(final Object o) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (o instanceof HeartyActivity)
                    tv.setText(String.valueOf(((HeartyActivity)o).getActivityName()));
            }
        });

    }

}
