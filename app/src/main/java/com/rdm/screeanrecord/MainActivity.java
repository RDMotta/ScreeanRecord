package com.rdm.screeanrecord;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;



import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_CODE = 1000;
    private static final int REQUEST_PERMISSION = 1001;
    private static final SparseIntArray ORIENTATION = new SparseIntArray();

    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    private final int _DisplayWidth = 720;
    private final int _DisplayHeight = 1280;
    private String videoUri = "";
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaProjectionCallback mediaProjectionCallback;
    private MediaRecorder mediaRecorder;
    private int mScreeanDensity;
    private ToggleButton toggleButton;
    private VideoView videoView;
    private RelativeLayout rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        mScreeanDensity = metrics.densityDpi;
        mediaRecorder = new MediaRecorder();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);


        videoView = findViewById(R.id.videoView);
        toggleButton = findViewById(R.id.toggleButton);
        rootLayout = findViewById(R.id.rootLayout);


    toggleButton.setOnClickListener(view -> {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                + ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {


            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.RECORD_AUDIO)) {

                toggleButton.setChecked(false);
                showQuestionPermissionUser();
            } else {

                toggleScreenShare(view);
            }
        } else{
            toggleScreenShare(view);
        }
    });

    }

    private void showQuestionPermissionUser() {
        Snackbar.make(rootLayout, "Permissions", Snackbar.LENGTH_SHORT)
                .setAction("Enabled", clickPermission())
                .show();
    }

    protected View.OnClickListener clickPermission() {
        return view -> ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO
                }, REQUEST_PERMISSION);
    }

    private void toggleScreenShare(View view) {
        if (((ToggleButton) view).isChecked()) {
            initRecord();
            recordScreen();
        } else {
            mediaRecorder.stop();
            mediaRecorder.reset();
            videoView.setVisibility(view.VISIBLE);
            videoView.setVideoURI(Uri.parse(videoUri));
            videoView.start();
        }

    }

    private void recordScreen() {
        if (mediaProjection == null) {
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
            return;
        }
        virtualDisplay = createVirtualDisplay();
        mediaRecorder.start();
    }

    private VirtualDisplay createVirtualDisplay() {
        return mediaProjection.createVirtualDisplay(TAG, _DisplayWidth, _DisplayHeight, mScreeanDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(), null, null);
    }

    private void initRecord() {
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

            videoUri = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + new StringBuilder("/RDM-")
                    .append(new SimpleDateFormat("dd-MM-yyyy-hh_mm_ss").format(new Date()))
                    .append(".mp4").toString();


            mediaRecorder.setOutputFile(videoUri);

            mediaRecorder.setVideoSize(_DisplayWidth, _DisplayHeight);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setVideoEncodingBitRate(512 * 1000);
            mediaRecorder.setVideoFrameRate(30);

            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATION.get(rotation + 90);
            mediaRecorder.setOrientationHint(orientation);

            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CODE) {
            Snackbar.make(rootLayout, "Unk Error", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (requestCode != REQUEST_CODE) {
            Snackbar.make(rootLayout, "Permission denied", Snackbar.LENGTH_SHORT).show();
            toggleButton.setChecked(false);
            return;
        }

        mediaProjectionCallback = new MediaProjectionCallback();
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        mediaProjection.registerCallback(mediaProjectionCallback, null);
        virtualDisplay = createVirtualDisplay();
        mediaRecorder.start();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION:
                if ((grantResults.length > 0) && ((grantResults[0] + grantResults[1]) == PackageManager.PERMISSION_GRANTED)) {
                    toggleButton.setChecked(true);
                    toggleScreenShare(toggleButton);
                } else {
                    toggleButton.setChecked(false);
                    showQuestionPermissionUser();
                }
                return;
        }

    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if (toggleButton.isChecked()) {
                toggleButton.setChecked(false);
                mediaRecorder.stop();
                mediaRecorder.reset();
            }

            mediaProjection = null;
            stopRecordScreen();
            super.onStop();
        }

        private void stopRecordScreen() {
            if (virtualDisplay == null) {
                return;
            }

            virtualDisplay.release();
            destroyMediaProjection();
        }

        private void destroyMediaProjection() {
            if (mediaProjection != null) {
                mediaProjection.unregisterCallback(mediaProjectionCallback);
                mediaProjection.stop();
                mediaProjection = null;
            }
        }
    }
}