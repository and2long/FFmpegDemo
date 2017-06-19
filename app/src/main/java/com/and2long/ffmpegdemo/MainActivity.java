package com.and2long.ffmpegdemo;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1;
    private FFmpeg ffmpeg;
    private Button runButton;
    private ProgressDialog progressDialog;
    private LinearLayout outputLayout;
    private String videoPath;
    private String audioPath;
    private String[] command;
    private String outputPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initData();
        //ffmpeg初始化。
        loadFFMpegBinary();
        initUI();
    }

    private void initData() {
        //自己准备video.mp4和audio.aac放在SD卡根目录运行测试
        videoPath = "/sdcard/video.mp4";
        audioPath = "/sdcard/audio.aac";
        outputPath = "/sdcard/output.mp4";
        //String cmd = "-y -i " + videoPath + " -i " + audioPath + " -filter_complex \"[0:a] pan=stereo|c0=1*c0|c1=1*c1 [a1], [1:a] pan=stereo|c0=1*c0|c1=1*c1 [a2],[a1][a2]amix=duration=first,pan=stereo|c0<c0+c1|c1<c2+c3,pan=mono|c0=c0+c1[a]\" -map \"[a]\" -map 0:v -c:v copy -c:a aac -strict -2 -ac 2 " + outputPath;
        String cmd = "-y -i " + videoPath + " -i " + audioPath + " -filter_complex amix=inputs=2:duration=first:dropout_transition=2 -c:v copy -c:a aac " + outputPath;
        System.out.println(cmd);
        command = cmd.split(" ");

    }

    private void initUI() {
        runButton = (Button) findViewById(R.id.run_command);
        runButton.setOnClickListener(this);
        outputLayout = (LinearLayout) findViewById(R.id.command_output);
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
    }

    /**
     * ffmpeg初始化
     */
    private void loadFFMpegBinary() {
        ffmpeg = FFmpeg.getInstance(this);
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    showUnsupportedExceptionDialog();
                }
            });
        } catch (FFmpegNotSupportedException e) {
            showUnsupportedExceptionDialog();
        }
    }

    /**
     * 初始化失败的提示
     */
    private void showUnsupportedExceptionDialog() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.device_not_supported))
                .setMessage(getString(R.string.device_not_supported_message))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.finish();
                    }
                })
                .create()
                .show();
    }

    /**
     * 点击事件
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        checkPermissionsAndRunCommand();
    }

    /**
     * 检查权限运行命令
     */
    private void checkPermissionsAndRunCommand() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //申请WRITE_EXTERNAL_STORAGE权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        } else {
            runCommand();
        }

    }

    /**
     * 运行命令
     */
    private void runCommand() {
        if (command.length != 0) {
            execFFmpegBinary(command);
        } else {
            Toast.makeText(this, getString(R.string.empty_command_toast), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 权限请求结果
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        doNext(requestCode, grantResults);
    }

    /**
     * 下一步操作
     * @param requestCode
     * @param grantResults
     */
    private void doNext(int requestCode, int[] grantResults) {
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                runCommand();
            } else {
                // Permission Denied
                Snackbar.make(runButton, "需要内存卡读写权限", Snackbar.LENGTH_INDEFINITE)
                        .setAction("OK", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                            }
                        }).show();
            }
        }
    }

    /**
     * 执行ffmpeg命令
     *
     * @param command
     */
    private void execFFmpegBinary(final String[] command) {
        try {
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    addTextViewToLayout("FAILED with output : " + s);
                }

                @Override
                public void onSuccess(String s) {
                    addTextViewToLayout("SUCCESS with output : " + s);
                }

                @Override
                public void onProgress(String s) {
                    Log.d(TAG, "Started command : ffmpeg " + command);
                    addTextViewToLayout("progress : " + s);
                    progressDialog.setMessage("Processing\n" + s);
                }

                @Override
                public void onStart() {
                    outputLayout.removeAllViews();

                    Log.d(TAG, "Started command : ffmpeg " + command);
                    progressDialog.setMessage("Processing...");
                    progressDialog.show();
                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "Finished command : ffmpeg " + command);
                    progressDialog.dismiss();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // do nothing for now
        }
    }

    /**
     * 添加输出文本
     *
     * @param text
     */
    private void addTextViewToLayout(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        outputLayout.addView(textView);
    }
}
