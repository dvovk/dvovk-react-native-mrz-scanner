package com.reactlibrary;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.SparseArray;

import com.google.android.gms.internal.fc;
import com.google.android.gms.internal.fm;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.text.TextBlock;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

class MRZDetector extends Detector<TextBlock> {
    private Detector<TextBlock> mDelegate;
    private int mBoxWidth, mBoxHeight;

    MRZDetector(Detector<TextBlock> delegate) {
        mDelegate = delegate;
        mBoxWidth = 500;
        mBoxHeight = 100;
    }

    public SparseArray<TextBlock> detect(Frame frame)
    {
        int width = frame.getMetadata().getWidth();
        int height = frame.getMetadata().getHeight();
        int right = width - (int)(width / 2.5);
        int top = 0;
        int bottom = height;
        int left = width - (int)(width / 1.5);
        /*int right = width;
        int top = 0;
        int bottom = height;
        int left = width - (width / 4);*/

        YuvImage yuvImage = new YuvImage(frame.getGrayscaleImageData().array(), ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        boolean res = yuvImage.compressToJpeg(new Rect(left, top, right, bottom), 100, byteArrayOutputStream);
        byte[] jpegArray = byteArrayOutputStream.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.length);

        Frame croppedFrame =
                new Frame.Builder()
                        .setBitmap(bitmap)
                        .setRotation(frame.getMetadata().getRotation())
                        .build();

        return mDelegate.detect(croppedFrame);
    }

    public boolean isOperational() {
        return mDelegate.isOperational();
    }

    public boolean setFocus(int id) {
        return mDelegate.setFocus(id);
    }
}