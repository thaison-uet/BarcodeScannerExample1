package com.thaison.barcodescannerexample1;

import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import me.dm7.barcodescanner.zbar.ZBarScannerView;

/**
 * Created by H81 on 3/21/2017.
 */

public class ScannerActivity extends AppCompatActivity {
    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;
    private Camera.PreviewCallback previewCallback;
    private Camera.AutoFocusCallback autoFocusCallback;

    private ImageScanner scanner;

    private FrameLayout previewLayout;

    private boolean isScanned = false;
    private boolean isPreviewing = true;

    static {
        System.loadLibrary("iconv");
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        initControl();
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
                    isPreviewing = false;
                    mCamera.setPreviewCallback(null);
                    mCamera.stopPreview();

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
        previewLayout = (FrameLayout) findViewById(R.id.fl_preview);
        previewLayout.addView(mPreview);
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
                .setPositiveButton("Okie", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(ScannerActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
}
