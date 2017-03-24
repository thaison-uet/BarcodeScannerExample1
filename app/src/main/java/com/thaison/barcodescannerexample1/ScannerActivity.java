package com.thaison.barcodescannerexample1;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.util.List;

/**
 * Created by H81 on 3/21/2017.
 */

public class ScannerActivity extends AppCompatActivity implements View.OnClickListener {
    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;
    private Camera.PreviewCallback previewCallback;
    private Camera.AutoFocusCallback autoFocusCallback;

    private ImageScanner scanner;
    private RelativeLayout rl_preview;
    private ImageView ivLight;

    private boolean isScanned = false;
    private boolean isPreviewing = true;
    private float mDist = 0;

    static {
        System.loadLibrary("iconv");
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        intiViews();
        initControl();
    }

    private void intiViews() {
        rl_preview = (RelativeLayout) findViewById(R.id.rl_preview);
        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth() - 128;
        int height = width;
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) rl_preview.getLayoutParams();
        params.width = width;
        params.height = height;
        rl_preview.setLayoutParams(params);

        ivLight = (ImageView) findViewById(R.id.iv_light);
        ivLight.setOnClickListener(this);
    }

    private void initControl() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);

        autoFocusHandler = new Handler();
        mCamera = getCameraInstance();

        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        previewCallback = new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = parameters.getPreviewSize();

                Image barcode = new Image(size.width, size.height, "Y800");
                barcode.setData(data);

                int result = scanner.scanImage(barcode);

                if (result != 0) {
                    // make vibrate

                    Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(300);

                    // sop previewing

                    isPreviewing = false;
                    mCamera.setPreviewCallback(null);
                    mCamera.stopPreview();

                    // scan the result and show dialog

                    SymbolSet symbols = scanner.getResults();
                    for (Symbol symbol : symbols) {
                        String scanResult = symbol.getData().trim();
                        showAlertDialog(scanResult);
                        isScanned = true;
                        break;
                    }
                }
            }
        };

        autoFocusCallback = new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, final Camera camera) {
                autoFocusHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isPreviewing) {
                            camera.autoFocus(autoFocusCallback);
                        }
                    }
                }, 1000);
            }
        };

        mPreview = new CameraPreview(this, mCamera, previewCallback, autoFocusCallback);
        rl_preview.addView(mPreview);
    }

    @Override
    public void onBackPressed() {
        if (isScanned) {
            isScanned = false;
            mCamera.setPreviewCallback(previewCallback);
            mCamera.autoFocus(autoFocusCallback);
            mCamera.startPreview();
            isPreviewing = true;
        } else {
            releaseCamera();
            super.onBackPressed();
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            isPreviewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    public static Camera getCameraInstance() {
        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return camera;

    }

    private void showAlertDialog(final String message) {
        new AlertDialog.Builder(this)
                .setTitle("Kết quả")
                .setCancelable(false)
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(ScannerActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    // zoom

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Camera.Parameters parameters = mCamera.getParameters();
        int action = event.getAction();
        if (event.getPointerCount() > 1) {
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                mDist = getFingerSpacing(event);
            } else {
                if (action == MotionEvent.ACTION_MOVE && parameters.isZoomSupported()) {
                    mCamera.cancelAutoFocus();
                    handleZoom(event, parameters);
                }
            }
        } else {
            if (action == MotionEvent.ACTION_UP) {
                handleFocus(parameters);
            }
        }

        return true;
    }

    private void handleZoom(MotionEvent event, Camera.Parameters params) {
        int maxZoom = params.getMaxZoom();
        int zoom = params.getZoom();
        float newDist = getFingerSpacing(event);
        if (newDist - 30 > mDist) {
            //zoom in
            if (zoom < maxZoom)
                zoom++;
        } else if (newDist + 30 < mDist) {
            //zoom out
            if (zoom > 0)
                zoom--;
        }
        mDist = newDist;
        params.setZoom(zoom);
        mCamera.setParameters(params);
    }

    public void handleFocus(Camera.Parameters params) {
        List<String> supportedFocusModes = params.getSupportedFocusModes();
        if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                    Toast.makeText(ScannerActivity.this, "onAutoFocus()", Toast.LENGTH_SHORT).show();
                    // currently set to auto-focus on single touch
                }
            });
        }
    }

    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        Log.i("TAG", "newDist: " + (float) Math.sqrt(x * x + y * y) + "; mDist: " + mDist);
        return (float) Math.sqrt(x * x + y * y);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_light:

                break;
        }
    }
}
