package com.otaliastudios.transcoder.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.otaliastudios.transcoder.Transcoder;
import com.otaliastudios.transcoder.TranscoderListener;
import com.otaliastudios.transcoder.engine.TrackStatus;
import com.otaliastudios.transcoder.internal.Logger;
import com.otaliastudios.transcoder.strategy.DefaultAudioStrategy;
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy;
import com.otaliastudios.transcoder.strategy.OutputStrategy;
import com.otaliastudios.transcoder.strategy.RemoveTrackStrategy;
import com.otaliastudios.transcoder.strategy.size.AspectRatioResizer;
import com.otaliastudios.transcoder.strategy.size.FractionResizer;
import com.otaliastudios.transcoder.strategy.size.PassThroughResizer;
import com.otaliastudios.transcoder.validator.DefaultValidator;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Future;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;


public class TranscoderActivity extends AppCompatActivity implements
        TranscoderListener,
        RadioGroup.OnCheckedChangeListener {

    private static final String TAG = "DemoApp";
    private static final Logger LOG = new Logger(TAG);

    private static final String FILE_PROVIDER_AUTHORITY = "com.otaliastudios.transcoder.demo.fileprovider";
    private static final int REQUEST_CODE_PICK = 1;
    private static final int PROGRESS_BAR_MAX = 1000;

    private RadioGroup mAudioChannelsGroup;
    private RadioGroup mVideoFramesGroup;
    private RadioGroup mVideoResolutionGroup;
    private RadioGroup mVideoAspectGroup;
    private RadioGroup mVideoRotationGroup;
    private RadioGroup mSpeedGroup;

    private ProgressBar mProgressView;
    private TextView mButtonView;

    private boolean mIsTranscoding;
    private Future<Void> mTranscodeFuture;
    private Uri mTranscodeInputUri;
    private File mTranscodeOutputFile;
    private long mTranscodeStartTime;
    private OutputStrategy mTranscodeVideoStrategy;
    private OutputStrategy mTranscodeAudioStrategy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.setLogLevel(Logger.LEVEL_VERBOSE);
        setContentView(R.layout.activity_transcoder);

        mButtonView = findViewById(R.id.button);
        mButtonView.setOnClickListener(v -> {
            if (!mIsTranscoding) {
                startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("video/*"), REQUEST_CODE_PICK);
            } else {
                mTranscodeFuture.cancel(true);
            }
        });
        setIsTranscoding(false);

        mProgressView = findViewById(R.id.progress);
        mProgressView.setMax(PROGRESS_BAR_MAX);

        mAudioChannelsGroup = findViewById(R.id.channels);
        mVideoFramesGroup = findViewById(R.id.frames);
        mVideoResolutionGroup = findViewById(R.id.resolution);
        mVideoAspectGroup = findViewById(R.id.aspect);
        mVideoRotationGroup = findViewById(R.id.rotation);
        mSpeedGroup = findViewById(R.id.speed);

        mAudioChannelsGroup.setOnCheckedChangeListener(this);
        mVideoFramesGroup.setOnCheckedChangeListener(this);
        mVideoResolutionGroup.setOnCheckedChangeListener(this);
        mVideoAspectGroup.setOnCheckedChangeListener(this);
        syncParameters();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        syncParameters();
    }

    private void syncParameters() {
        int channels;
        switch (mAudioChannelsGroup.getCheckedRadioButtonId()) {
            case R.id.channels_mono: channels = 1; break;
            case R.id.channels_stereo: channels = 2; break;
            default: channels = DefaultAudioStrategy.AUDIO_CHANNELS_AS_IS;
        }
        mTranscodeAudioStrategy = new DefaultAudioStrategy(channels);

        int frames;
        switch (mVideoFramesGroup.getCheckedRadioButtonId()) {
            case R.id.frames_24: frames = 24; break;
            case R.id.frames_30: frames = 30; break;
            case R.id.frames_60: frames = 60; break;
            default: frames = DefaultVideoStrategy.DEFAULT_FRAME_RATE;
        }
        float fraction;
        switch (mVideoResolutionGroup.getCheckedRadioButtonId()) {
            case R.id.resolution_half: fraction = 0.5F; break;
            case R.id.resolution_third: fraction = 1F / 3F; break;
            default: fraction = 1F;
        }
        float aspectRatio;
        switch (mVideoAspectGroup.getCheckedRadioButtonId()) {
            case R.id.aspect_169: aspectRatio = 16F / 9F; break;
            case R.id.aspect_43: aspectRatio = 4F / 3F; break;
            case R.id.aspect_square: aspectRatio = 1F; break;
            default: aspectRatio = 0F;
        }
        mTranscodeVideoStrategy = new DefaultVideoStrategy.Builder()
                .addResizer(aspectRatio > 0 ? new AspectRatioResizer(aspectRatio) : new PassThroughResizer())
                .addResizer(new FractionResizer(fraction))
                .frameRate(frames)
                .build();
    }

    private void setIsTranscoding(boolean isTranscoding) {
        mIsTranscoding = isTranscoding;
        mButtonView.setText(mIsTranscoding ? "Cancel Transcoding" : "Select Video & Transcode");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {
            mTranscodeInputUri = data.getData();
            transcode();
        }
    }

    private void transcode() {
        // Create a temporary file for output.
        try {
            File outputDir = new File(getExternalFilesDir(null), "outputs");
            //noinspection ResultOfMethodCallIgnored
            outputDir.mkdir();
            mTranscodeOutputFile = File.createTempFile("transcode_test", ".mp4", outputDir);
            LOG.i("Transcoding into " + mTranscodeOutputFile);
        } catch (IOException e) {
            LOG.e("Failed to create temporary file.", e);
            Toast.makeText(this, "Failed to create temporary file.", Toast.LENGTH_LONG).show();
            return;
        }

        int rotation;
        switch (mVideoRotationGroup.getCheckedRadioButtonId()) {
            case R.id.rotation_90: rotation = 90; break;
            case R.id.rotation_180: rotation = 180; break;
            case R.id.rotation_270: rotation = 270; break;
            default: rotation = 0;
        }

        float speed;
        switch (mSpeedGroup.getCheckedRadioButtonId()) {
            case R.id.speed_05x: speed = 0.5F; break;
            case R.id.speed_2x: speed = 2F; break;
            default: speed = 1F;
        }

        // Launch the transcoding operation.
        mTranscodeStartTime = SystemClock.uptimeMillis();
        setIsTranscoding(true);
        mTranscodeFuture = Transcoder.into(mTranscodeOutputFile.getAbsolutePath())
                .setDataSource(this, mTranscodeInputUri)
                .setListener(this)
                .setAudioOutputStrategy(mTranscodeAudioStrategy)
                .setVideoOutputStrategy(mTranscodeVideoStrategy)
                .setRotation(rotation)
                .setSpeed(speed)
                .transcode();
    }

    @Override
    public void onTranscodeProgress(double progress) {
        if (progress < 0) {
            mProgressView.setIndeterminate(true);
        } else {
            mProgressView.setIndeterminate(false);
            mProgressView.setProgress((int) Math.round(progress * PROGRESS_BAR_MAX));
        }
    }

    @Override
    public void onTranscodeCompleted(int successCode) {
        if (successCode == Transcoder.SUCCESS_TRANSCODED) {
            LOG.w("Transcoding took " + (SystemClock.uptimeMillis() - mTranscodeStartTime) + "ms");
            onTranscodeFinished(true, "Transcoded file placed on " + mTranscodeOutputFile);
            Uri uri = FileProvider.getUriForFile(TranscoderActivity.this,
                    FILE_PROVIDER_AUTHORITY,
                    mTranscodeOutputFile);
            startActivity(new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "video/mp4")
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        } else if (successCode == Transcoder.SUCCESS_NOT_NEEDED) {
            // TODO: Not sure this works
            LOG.i("Transcoding was not needed.");
            onTranscodeFinished(true, "Transcoding not needed, source file not touched.");
            startActivity(new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(mTranscodeInputUri, "video/mp4")
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        }
    }

    @Override
    public void onTranscodeCanceled() {
        onTranscodeFinished(false, "Transcoder canceled.");
    }

    @Override
    public void onTranscodeFailed(@NonNull Throwable exception) {
        onTranscodeFinished(false, "Transcoder error occurred. " + exception.getMessage());
    }

    private void onTranscodeFinished(boolean isSuccess, String toastMessage) {
        mProgressView.setIndeterminate(false);
        mProgressView.setProgress(isSuccess ? PROGRESS_BAR_MAX : 0);
        setIsTranscoding(false);
        Toast.makeText(TranscoderActivity.this, toastMessage, Toast.LENGTH_LONG).show();
    }

}
