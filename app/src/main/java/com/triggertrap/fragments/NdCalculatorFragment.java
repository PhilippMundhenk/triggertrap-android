package com.triggertrap.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.triggertrap.R;
import com.triggertrap.core.NdCalculator;
import com.triggertrap.TTApp;
import com.triggertrap.view.ZeroTopPaddingTextView;
import com.triggertrap.widget.ArrayWheelDoubleAdapter;
import com.triggertrap.widget.TimerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import antistatic.spinnerwheel.AbstractWheel;
import antistatic.spinnerwheel.OnWheelScrollListener;
import antistatic.spinnerwheel.adapters.ArrayWheelAdapter;

/**
 * Fragment that handles the display ND Filter Calculator and associated
 * calculations.
 *
 * @author scottmellors
 * @since 2.2
 */
public class NdCalculatorFragment extends TriggertrapFragment {

    private List<String> mNdValues;
    private String[] mBaseShutterSpeedVals;
    private String[] mNdNameVals;

    private int mCurrentBaseShutterSpeed;
    private int mCurrentStops;

    private TextView mNdFilterTextView;
    private TextView mResultTextView;
    private ZeroTopPaddingTextView mResultTextLabel;

    private TimerView mTimedView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.nd_calc_mode, container,
                false);


        mNdFilterTextView = (TextView) rootView
                .findViewById(R.id.ndTFilterTypeText);

        mTimedView = (TimerView) rootView.findViewById(R.id.timerTimeText);
        mTimedView.drawAllBold();

        mResultTextView = (TextView) rootView.findViewById(R.id.resultTextView);
        mResultTextLabel = (ZeroTopPaddingTextView) rootView.findViewById(R.id.result_label);

        mNdValues = new ArrayList<String>();

        for (int i = 1; i < 21; i++) {
            mNdValues.add(getResources().getQuantityString(
                    R.plurals.numberOfStops, i, i));
        }

        mBaseShutterSpeedVals = getResources().getStringArray(
                R.array.base_shutter_speeds);
        mNdNameVals = getResources().getStringArray(R.array.nd_names);

        mNdFilterTextView.setText(getResources().getQuantityString(
                R.plurals.numberOfStops, 1, 1)
                + " ("
                + mNdNameVals[0]
                + ") "
                + getResources().getString(R.string.nd_filter_end));

        ArrayWheelDoubleAdapter ndValueAdapter = new ArrayWheelDoubleAdapter(
                getActivity(), mNdValues, mNdNameVals);

        View ndValueView = rootView.findViewById(R.id.nd_filter_picker);
        AbstractWheel ndValueWheel = (AbstractWheel) ndValueView
                .findViewById(R.id.wheelHorizontalView);
        ndValueAdapter.setItemResource(R.layout.wheel_double_text_centered);
        ndValueAdapter.setItemTextResource(R.id.text);
        ndValueWheel.setViewAdapter(ndValueAdapter);

        ndValueWheel.addScrollingListener(new OnWheelScrollListener() {
            public void onScrollingStarted(AbstractWheel wheel) {

            }

            public void onScrollingFinished(AbstractWheel wheel) {
                mCurrentStops = wheel.getCurrentItem();

                mNdFilterTextView.setText(getResources().getQuantityString(
                        R.plurals.numberOfStops, wheel.getCurrentItem() + 1,
                        wheel.getCurrentItem() + 1)
                        + " ("
                        + mNdNameVals[wheel.getCurrentItem()]
                        + ") "
                        + getResources().getString(R.string.nd_filter_end));

                mResultTextView.setText(getNdTime(mCurrentStops,
                        mCurrentBaseShutterSpeed));
            }
        });

        ndValueWheel.setCurrentItem(0);

        ArrayWheelAdapter<String> shutterSpeedAdapter = new ArrayWheelAdapter<String>(
                getActivity(), mBaseShutterSpeedVals);

        View shutterSpeedView = rootView
                .findViewById(R.id.base_shutter_speed_picker);

        AbstractWheel shutterSpeedWheel = (AbstractWheel) shutterSpeedView
                .findViewById(R.id.wheelHorizontalView);
        shutterSpeedAdapter.setItemResource(R.layout.wheel_text_centered);
        shutterSpeedAdapter.setItemTextResource(R.id.text);
        TTApp app = TTApp.getInstance(getActivity());

        shutterSpeedWheel.addScrollingListener(new OnWheelScrollListener() {
            public void onScrollingStarted(AbstractWheel wheel) {

            }

            public void onScrollingFinished(AbstractWheel wheel) {
                mCurrentBaseShutterSpeed = wheel.getCurrentItem();

                TTApp.getInstance(getActivity()).setDefaultShutterSpeedVal(
                        wheel.getCurrentItem());

                mResultTextView.setText(getNdTime(mCurrentStops,
                        mCurrentBaseShutterSpeed));
            }
        });

        shutterSpeedWheel.setViewAdapter(shutterSpeedAdapter);

        shutterSpeedWheel.setCurrentItem(app.getDefaultShutterSpeedVal());

        mCurrentBaseShutterSpeed = app.getDefaultShutterSpeedVal();

        mResultTextView.setText(getNdTime(0, mCurrentBaseShutterSpeed));

        return rootView;
    }

    /**
     * Function which takes two values based on the position of the wheel
     * pickers and calculates the ND filter modified time.
     *
     * @param stops        - currently selected stops value
     * @param shutterSpeed - currently selected shutterSpeed value
     * @return String output of calculated time value.
     */
    public String getNdTime(int stops, int shutterSpeed) {
        String output = "";
        // the picker index is 0-based, the first entry is one stop
        NdCalculator.Result result = NdCalculator.calculate(shutterSpeed, stops + 1);
        if (result instanceof NdCalculator.Result.Hours) {
            mTimedView.setVisibility(View.GONE);
            mResultTextView.setVisibility(View.VISIBLE);
            mResultTextLabel.setVisibility(View.VISIBLE);
            mResultTextLabel.setText(getText(R.string.hours_label));
            output = String.valueOf(((NdCalculator.Result.Hours) result).getHours());
        } else if (result instanceof NdCalculator.Result.Timed) {
            mTimedView.setVisibility(View.VISIBLE);
            mResultTextView.setVisibility(View.GONE);
            mResultTextLabel.setVisibility(View.GONE);
            NdCalculator.Result.Timed timed = (NdCalculator.Result.Timed) result;
            output = timed.getText();
            mTimedView.setTextInputTime(timed.getMillis());
        } else {
            mTimedView.setVisibility(View.GONE);
            mResultTextView.setVisibility(View.VISIBLE);
            output = mBaseShutterSpeedVals[((NdCalculator.Result.Nearest) result).getIndex()];
            if (output.contains("\"") || output.contains("/")) {
                mResultTextLabel.setVisibility(View.GONE);
            } else {
                mResultTextLabel.setText(getText(R.string.seconds_label));
                mResultTextLabel.setVisibility(View.VISIBLE);
            }
        }
        return output;
    }

}
