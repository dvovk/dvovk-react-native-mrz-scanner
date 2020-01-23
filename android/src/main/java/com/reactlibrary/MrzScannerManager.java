package com.reactlibrary;

import android.app.Activity;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.view.View;
import android.widget.RelativeLayout;

import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;

import java.util.Map;

public class MrzScannerManager extends SimpleViewManager<RelativeLayout> {

    public static final String REACT_CLASS = "MrzScanner";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected MrzScanner createViewInstance(ThemedReactContext reactContext) {
        reactContext.getCurrentActivity();
        MrzScanner scanner = new MrzScanner(reactContext);
        scanner.setBackgroundColor(Color.parseColor("#ff0000"));

        return scanner;
    }

    @Override
    public Map getExportedCustomBubblingEventTypeConstants() {
        return MapBuilder.builder()
                .put(
                        "onScanSuccess",
                        MapBuilder.of(
                                "phasedRegistrationNames",
                                MapBuilder.of("bubbled", "onScanSuccess")))
                .build();
    }
}