package com.android.control;

import android.Manifest.permission;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.android.control.base.BaseActivity;
import com.android.control.utils.SaveUtils;
import com.android.control.video.VideoService;

import cn.jpush.android.api.JPushInterface;

public class MainActivity extends BaseActivity implements View.OnClickListener {
    public static final String RTMPURL_MESSAGE = "com.android.control.rtmpurl";
    private EditText _rtmpUrlEditText = null;
    private static final String _rtmpUrlDefault = "rtmp://192.168.0.44:1935/live/12345678";
    private View.OnClickListener _startRtmpPushOnClickedEvent = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            if (!isHavePermission()) return;
            Intent i = new Intent(MainActivity.this, VideoService.class);
            String rtmpUrl = _rtmpUrlEditText.getText().toString();
            i.putExtra(MainActivity.RTMPURL_MESSAGE, rtmpUrl);
            startService(i);

        }
    };
    private EditText et_userName;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this).setCancelable(true);
                        builder.setTitle("设置").setMessage("权限请求").setNegativeButton(android.R.string.cancel, null);
                        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null)));
                            }
                        }).show();
                    } else Toast.makeText(this, "权限请求被拒绝！", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Intent i = new Intent(MainActivity.this, VideoService.class);
            String rtmpUrl = _rtmpUrlEditText.getText().toString();
            i.putExtra(MainActivity.RTMPURL_MESSAGE, rtmpUrl);
            startService(i);
        } else super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void InitUI() {
        _rtmpUrlEditText = findViewById(R.id.rtmpUrleditText);
        _rtmpUrlEditText.setText(_rtmpUrlDefault);
        findViewById(R.id.startRtmpButton).setOnClickListener(_startRtmpPushOnClickedEvent);
        final EditText et_jpushID = findViewById(R.id.et_jpushID);
        findViewById(R.id.btn_getID).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                et_jpushID.setText(JPushInterface.getRegistrationID(MainActivity.this));
            }
        });
        et_userName = findViewById(R.id.et_userName);
        findViewById(R.id.btn_userName).setOnClickListener(this);
        et_userName.setText(SaveUtils.getUserName(this));
        final CheckBox cb_hint = findViewById(R.id.cb_hint);
        cb_hint.setChecked(SaveUtils.getIsHint(this));
        cb_hint.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SaveUtils.saveIsHint(MainActivity.this, isChecked);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        InitUI();
    }

    public boolean isHavePermission() {
        if (Build.VERSION.SDK_INT < 23) return true;
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(MainActivity.this, permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(MainActivity.this, permission.WAKE_LOCK) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(MainActivity.this, permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(MainActivity.this, permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(MainActivity.this, permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(MainActivity.this, permission.INTERNET) == PackageManager.PERMISSION_DENIED
                ) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    permission.WRITE_EXTERNAL_STORAGE,
                    permission.RECORD_AUDIO,
                    permission.WAKE_LOCK,
                    permission.READ_PHONE_STATE,
                    permission.READ_EXTERNAL_STORAGE,
                    permission.CAMERA,
                    permission.INTERNET
            }, 0);
            return false;
        } else return true;

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_userName:
                String userName = et_userName.getText().toString();
                if (userName.isEmpty()) {
                    Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(this, SaveUtils.saveUserName(this, userName) ? "设置成功" : "设置失败", Toast.LENGTH_SHORT).show();

                break;
        }
    }
}
