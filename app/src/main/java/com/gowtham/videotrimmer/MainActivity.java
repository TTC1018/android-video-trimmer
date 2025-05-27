package com.gowtham.videotrimmer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.Statistics;
import com.arthenica.mobileffmpeg.StatisticsCallback;
import com.cocosw.bottomsheet.BottomSheet;
import com.google.android.material.snackbar.Snackbar;
import com.gowtham.library.utils.CompressOption;
import com.gowtham.library.utils.FileUtils;
import com.gowtham.library.utils.LogMessage;
import com.gowtham.library.utils.TrimType;
import com.gowtham.library.utils.TrimVideo;
import com.gowtham.library.utils.TrimmerUtils;

import java.io.File;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private VideoView videoView;
    private MediaController mediaController;
    private EditText edtFixedGap, edtMinGap, edtMinFrom, edtMAxTo;
    private int trimType;

    private int YOUR_VIDEO_DURATION = 10000;

    ActivityResultLauncher<Intent> videoTrimResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK &&
                        result.getData() != null) {
                    Uri uri = Uri.parse(TrimVideo.getTrimmedVideoPath(result.getData()));
                    Log.d(TAG, "Trimmed path:: " + uri);

                    long intendedTrimmedDurationMs = TrimVideo.getIntendedTrimmedDurationMs(result.getData());

                    if (intendedTrimmedDurationMs <= 0) {
                        Log.w(TAG, "Intended trimmed duration not available or invalid. Progress might be inaccurate.");
                        // 대체값으로 실제 생성된 파일의 길이를 읽어오거나 기본값을 사용할 수 있습니다.
                        // 하지만 FFmpeg 처리 중에는 파일이 완전히 쓰이지 않았을 수 있어 이상적이지 않습니다.
                        intendedTrimmedDurationMs = TrimmerUtils.getDurationMillis(MainActivity.this, uri);
                        if (intendedTrimmedDurationMs <= 0) {
                            intendedTrimmedDurationMs = 10000; // 최후의 기본값 (예: 10초)
                            Log.e(TAG, "Could not determine trimmed video duration. Using default for progress.");
                        }
                    }

                    final long effectiveTotalDuration = intendedTrimmedDurationMs;

                    Config.resetStatistics();
                    Config.enableStatisticsCallback(new StatisticsCallback() {
                        @Override
                        public void apply(Statistics statistics) {
                            float progress = 0;
                            if (effectiveTotalDuration > 0) {
                                // statistics.getTime()은 FFmpeg이 현재까지 처리한 시간 (ms)
                                progress = ((float) statistics.getTime() / effectiveTotalDuration) * 100;
                            }
                            // UI 표시를 위해 진행률을 0-100% 사이로 제한
                            progress = Math.max(0f, Math.min(progress, 100f));

                            Snackbar.make(videoView.getRootView(), "Progressed " + String.format(Locale.US, "%.0f", progress) + "%", Snackbar.LENGTH_SHORT).show();

                            // 진행률이 거의 100%에 도달하면 비디오 재생 준비
                            if (progress >= 99.9f) { // 부동 소수점 비교를 위해 임계값 사용
                                videoView.setMediaController(mediaController);
                                videoView.setVideoURI(uri);
                                videoView.requestFocus();
                                // 필요하다면 여기서 통계 콜백 비활성화
                                // Config.disableStatisticsCallback();
                            }
                        }
                    });
                    videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mediaController.setAnchorView(videoView);
                            videoView.start();
                        }
                    });

                    String filepath = String.valueOf(uri);
                    File file = new File(filepath);
                    long length = file.length();
                    Log.d(TAG, "Video size:: " + (length / 1024));
                } else
                    LogMessage.v("videoTrimResultLauncher data is null");
            });

    ActivityResultLauncher<Intent> takeOrSelectVideoResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK &&
                        result.getData() != null) {
                    Intent data = result.getData();
                    //check video duration if needed
           /*        if (TrimmerUtils.getDuration(this,data.getData())<=30){
                    Toast.makeText(this,"Video should be larger than 30 sec",Toast.LENGTH_SHORT).show();
                    return;
                }*/
                    if (data.getData() != null) {
                        LogMessage.v("Video path:: " + data.getData());
                        openTrimActivity(String.valueOf(data.getData()));
                    } else {
                        Toast.makeText(this, "video uri is null", Toast.LENGTH_SHORT).show();
                    }
                } else
                    LogMessage.v("takeVideoResultLauncher data is null");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        videoView = findViewById(R.id.video_view);
        edtFixedGap = findViewById(R.id.edt_fixed_gap);
        edtMinGap = findViewById(R.id.edt_min_gap);
        edtMinFrom = findViewById(R.id.edt_min_from);
        edtMAxTo = findViewById(R.id.edt_max_to);
        mediaController = new MediaController(this);


        findViewById(R.id.btn_default_trim).setOnClickListener(this);
        findViewById(R.id.btn_fixed_gap).setOnClickListener(this);
        findViewById(R.id.btn_min_gap).setOnClickListener(this);
        findViewById(R.id.btn_min_max_gap).setOnClickListener(this);
    }

    private void openTrimActivity(String data) {
        if (trimType == 0) {
            TrimVideo.activity(data)
                    .setCompressOption(new CompressOption()) //pass empty constructor for default compress option
                    .setLocal(this.getResources().getConfiguration().getLocales().get(0).getLanguage())
                    .start(this, videoTrimResultLauncher);
        } else if (trimType == 1) {
            TrimVideo.activity(data)
                    .setTrimType(TrimType.FIXED_DURATION)
                    .setFixedDuration(getEdtValueLong(edtFixedGap))
                    .setLocal("ar")
                    .setLocal("ko-rKR")
                    .setLocal("ko")
                    .start(this, videoTrimResultLauncher);
        } else if (trimType == 2) {
            TrimVideo.activity(data)
                    .setTrimType(TrimType.MIN_DURATION)
                    .setLocal("ar")
                    .setLocal("ko-rKR")
                    .setLocal("ko")
                    .setMinDuration(getEdtValueLong(edtMinGap))
                    .start(this, videoTrimResultLauncher);
        } else {
            TrimVideo.activity(data)
                    .setTrimType(TrimType.MIN_MAX_DURATION)
                    .setLocal("ar")
                    .setLocal("ko-rKR")
                    .setLocal("ko")
                    .setMinToMax(getEdtValueLong(edtMinFrom), getEdtValueLong(edtMAxTo))
                    .start(this, videoTrimResultLauncher);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_default_trim) {
            onDefaultTrimClicked();
        } else if (v.getId() == R.id.btn_fixed_gap) {
            onFixedTrimClicked();
        } else if (v.getId() == R.id.btn_min_gap) {
            onMinGapTrimClicked();
        } else if (v.getId() == R.id.btn_min_max_gap) {
            onMinToMaxTrimClicked();
        }
    }

    private void onDefaultTrimClicked() {
        trimType = 0;
        if (checkCamStoragePer())
            showVideoOptions();
    }

    private void onFixedTrimClicked() {
        trimType = 1;
        if (isEdtTxtEmpty(edtFixedGap))
            Toast.makeText(this, "Enter fixed gap duration", Toast.LENGTH_SHORT).show();
        else if (checkCamStoragePer())
            showVideoOptions();
    }

    private void onMinGapTrimClicked() {
        trimType = 2;
        if (isEdtTxtEmpty(edtMinGap))
            Toast.makeText(this, "Enter min gap duration", Toast.LENGTH_SHORT).show();
        else if (checkCamStoragePer())
            showVideoOptions();
    }


    private void onMinToMaxTrimClicked() {
        trimType = 3;
        if (isEdtTxtEmpty(edtMinFrom))
            Toast.makeText(this, "Enter min gap duration", Toast.LENGTH_SHORT).show();
        else if (isEdtTxtEmpty(edtMAxTo))
            Toast.makeText(this, "Enter max gap duration", Toast.LENGTH_SHORT).show();
        else if (checkCamStoragePer())
            showVideoOptions();
    }

    public void showVideoOptions() {
        try {
            BottomSheet.Builder builder = getBottomSheet();
            builder.sheet(R.menu.menu_video);
            builder.listener(item -> {
                if (R.id.action_take == item.getItemId())
                    captureVideo();
                else
                    openVideo();
                return false;
            });
            builder.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BottomSheet.Builder getBottomSheet() {
        return new BottomSheet.Builder(this).title(R.string.txt_option);
    }

    public void captureVideo() {
        try {
            Intent intent = new Intent("android.media.action.VIDEO_CAPTURE");
            intent.putExtra("android.intent.extra.durationLimit", 30);
            takeOrSelectVideoResultLauncher.launch(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openVideo() {
        try {
            Intent intent = new Intent();
            intent.setType("video/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            takeOrSelectVideoResultLauncher.launch(Intent.createChooser(intent, "Select Video"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (isPermissionOk(grantResults))
            showVideoOptions();
    }

    private boolean isEdtTxtEmpty(EditText editText) {
        return editText.getText().toString().trim().isEmpty();
    }

    private long getEdtValueLong(EditText editText) {
        return Long.parseLong(editText.getText().toString().trim());
    }

    private boolean checkCamStoragePer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            return checkPermission(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED, Manifest.permission.ACCESS_MEDIA_LOCATION);
        else
            return checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA);
    }

    private boolean checkPermission(String... permissions) {
        boolean allPermitted = false;
        for (String permission : permissions) {
            allPermitted = (ContextCompat.checkSelfPermission(this, permission)
                    == PackageManager.PERMISSION_GRANTED);
            if (!allPermitted)
                break;
        }
        if (allPermitted)
            return true;
        ActivityCompat.requestPermissions(this, permissions,
                220);
        return false;
    }

    private boolean isPermissionOk(int... results) {
        boolean isAllGranted = true;
        for (int result : results) {
            if (PackageManager.PERMISSION_GRANTED != result) {
                isAllGranted = false;
                break;
            }
        }
        return isAllGranted;
    }
}