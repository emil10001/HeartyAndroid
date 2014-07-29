package io.hearty.android.app;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import io.hearty.android.app.steps.Steps;
import io.hearty.witness.Reporter;
import io.hearty.witness.Witness;

/**
 * Created by ejf3 on 7/27/14.
 */
public class StepsFrag extends Fragment implements Reporter {
    private static final String TAG = "StepsFrag";
    private Handler handler = new Handler();

    private TextView tv;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.steps, container, false);
        tv = (TextView) rootView.findViewById(R.id.steps);
        tv.setText(String.valueOf(Steps.getLastSteps()));
        return rootView;
    }

    @Override
    public void onResume() {
        Witness.register(Steps.class, this);
        super.onResume();
    }

    @Override
    public void onPause() {
        Witness.remove(Steps.class, this);
        super.onPause();
    }


    @Override
    public void notifyEvent(final Object o) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (o instanceof Steps)
                    tv.setText(String.valueOf(((Steps)o).getSteps()));
            }
        });

    }
}
