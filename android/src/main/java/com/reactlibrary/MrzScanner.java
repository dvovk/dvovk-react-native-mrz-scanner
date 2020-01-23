package com.reactlibrary;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.LongDef;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.PermissionListener;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

public class MrzScanner extends RelativeLayout
{
    private static final String TAG = "MrzScanner";
    private static final int requestPermissionID = 101;
    SurfaceView mCameraView;
    CameraSource mCameraSource;
    boolean is_finished = false;
    TextView mTextView;
    String pnum;
    String pdob;
    String pexp;

    ThemedReactContext mContext;

    ArrayList<String> check_digits =  new ArrayList<String>(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"));

    private boolean hasCameraPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA);
            return result == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    public MrzScanner(ThemedReactContext context) {
        super(context);
        mContext = context;
        if(hasCameraPermissions()) {
            initCamera();
        }
        else
        {
            ActivityCompat.requestPermissions(mContext.getCurrentActivity(), new String[] {Manifest.permission.CAMERA}, requestPermissionID);
        }
    }

    private void initCamera()
    {
        mCameraView = new SurfaceView(mContext.getApplicationContext());
        this.addView(mCameraView);

        mTextView = new TextView(mContext.getApplicationContext());
        RelativeLayout.LayoutParams params= new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, mTextView.getId());
        mTextView.setLayoutParams(params);
        this.addView(mTextView);

        startCameraSource();
    }

    public void onScanSuccess()
    {
        Log.d("SCAN", "success");
        WritableMap event = Arguments.createMap();
        WritableMap data = Arguments.createMap();
        data.putString("num", pnum);
        data.putString("dob", pdob);
        data.putString("exp", pexp);
        event.putMap("data", data);
        ReactContext reactContext = (ReactContext)getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                "onScanSuccess",
                event);
    }

    private void startCameraSource() {

        //Create the TextRecognizer
        final TextRecognizer textRecognizer = new TextRecognizer.Builder(mContext.getApplicationContext()).build();

        if (!textRecognizer.isOperational()) {
            Log.w("MrzScanner", "Detector dependencies not loaded yet");
        } else {

            MRZDetector myDetector = new MRZDetector(textRecognizer);

            myDetector.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {
                }

                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {
                    if(!is_finished) {
                        final SparseArray<TextBlock> items = detections.getDetectedItems();
                        //if (items.size() != 0 ){
                        if (items.size() != 0) {
                            Log.d("BLOCKS", "" + items.size());
                            mTextView.post(new Runnable() {
                                @Override
                                public void run() {
                                    StringBuilder stringBuilder = new StringBuilder();

                                    for (int i = 0; i < items.size(); i++)
                                    {
                                        TextBlock item = items.valueAt(i);

                                        if(checkMRZFirstLine(item.getValue()))
                                        {
                                            stringBuilder.append(item.getValue());
                                            stringBuilder.append("\n");
                                            Log.d("SCAN", stringBuilder.toString());

                                            if(i + 1 < items.size())
                                            {
                                                if(grabPassportData(items.valueAt(i+1).getValue()))
                                                {
                                                    is_finished = true;
                                                    stringBuilder.append(items.valueAt(i+1).getValue());
                                                    Log.d("SCAN", "FINISHED");
                                                    mCameraSource.stop();
                                                    onScanSuccess();

                                                }
                                            }

                                            mTextView.setText(stringBuilder.toString());
                                        }

                                    }
                                }
                            });
                        }
                    }
                }
            });

            mCameraSource = new CameraSource.Builder(mContext.getApplicationContext(), myDetector)
                    .setAutoFocusEnabled(true)
                    .setRequestedFps(30.0f)
                    .build();
            /**
             * Add call back to SurfaceView and check if camera permission is granted.
             * If permission is granted we can start our cameraSource and pass it to surfaceView
             */
            mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    Log.d(TAG,"surfaceCreated");
                    try {
                        mCameraSource.start(mCameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
                {
                    Log.d(TAG,"surfaceChanged");
                }

                /**
                 * Release resources for cameraSource
                 */
                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mCameraSource.stop();
                }
            });
        }
    }

    public boolean checkMRZFirstLine(String val) {
        if(val != null)
        {
            val = val.replace(" ", "");

            if(val.length() > 1)
            {
                String p_start = val.substring(0, 2);
                if (!p_start.equals("P<"))
                    return false;
                else
                    return true;
            }
            else
                return false;
        }
        else
            return false;
    }

    public boolean grabPassportData(String val)
    {
        if(val != null) {
            val = val.replace(" ", "");

            if(val.length() == 44)
            {
                String passNumber = val.substring(0,9);
                String passNumberCheckDigit = val.substring(9,10);

                if (!isStringValid(passNumber, passNumberCheckDigit))
                    return false;

                String dob = val.substring(13, 19);
                String exp = val.substring(21, 27);

                if (!isDateValid(dob))
                    return false;

                if (!isDateValid(exp))
                    return false;

                String st = val.substring(0, 10) + val.substring(13, 20) + val.substring(21, 43);

                if (!isStringValid(st, val.substring(43, 44)))
                    return false;
                else
                {
                    Log.d("SCAN_NUM", passNumber);
                    Log.d("SCAN_DOB", dob);
                    Log.d("SCAN_EXP", exp);

                    String shg = passNumber.substring(8,9);
                    if(shg.equals("<")) {
                        passNumber = passNumber.substring(0, 8);
                    }

                    pnum = passNumber;
                    pdob = dob;
                    pexp = exp;

                    return true;
                }
            }
            else
                return false;
        }
        else
            return false;
    }

    public boolean isStringValid(String text, String check)
    {
        if(!isDateValid(check))
            return false;

        int sum = 0;

        for(int i = 0; i < text.length(); i++)
        {
            String s = text.substring(i, i+1);
            int dg = getDigitForValue(s, i);
            sum += dg;
        }

        int check_digit = sum % 10;
        int str_check = Integer.parseInt(check);
        if(check_digit == str_check)
            return true;

        return false;
        //return text.matches("^[A-Z0-9]+");
    }

    public boolean isDateValid(String text){

        return text.matches("^[0-9]+");
    }

    public int getDigitForValue(String val, int weight)
    {
        int mult = getMultiplier(weight);

        int res = 0;
        if(val.equals("<"))
        {
            return res;
        }
        else if(isDateValid(val))
        {
            res = Integer.parseInt(val);
            return res * mult;
        }
        else
        {
            for(int i = 0; i < check_digits.size(); i++)
            {
                if(val.equals(check_digits.get(i)))
                {
                    return (i + 10) * mult;
                }
            }
        }

        return res;
    }

    public int getMultiplier(int weight)
    {
        int cw = weight % 3;

        if(cw == 0)
            return 7;
        else if(cw == 1)
            return 3;
        else
            return 1;
    }
}
