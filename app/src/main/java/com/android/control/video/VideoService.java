package com.android.control.video;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.alex.livertmppushsdk.FdkAacEncode;
import com.alex.livertmppushsdk.RtmpSessionManager;
import com.alex.livertmppushsdk.SWVideoEncoder;
import com.android.control.MainActivity;
import com.android.control.R;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by dulong on 2017/7/26.
 */

public class VideoService extends Service {


    private final static int ID_RTMP_PUSH_START = 100;
    private final static int ID_RTMP_PUSH_EXIT = 101;
    private final int WIDTH_DEF = 480;
    private final int HEIGHT_DEF = 640;
    private final int FRAMERATE_DEF = 20;
    private final int BITRATE_DEF = 800 * 1000;

    private final int SAMPLE_RATE_DEF = 22050;
    private final int CHANNEL_NUMBER_DEF = 2;

    private final String LOG_TAG = "MainActivity";
    private final boolean DEBUG_ENABLE = false;
    public SurfaceView _mSurfaceView = null;
    PowerManager.WakeLock _wakeLock;
    private String _rtmpUrl;
    private DataOutputStream _outputStream = null;
    private AudioRecord _AudioRecorder = null;
    private byte[] _RecorderBuffer = null;
    private FdkAacEncode _fdkaacEnc = null;
    private int _fdkaacHandle = 0;
    private Camera _mCamera = null;
    private boolean _bIsFront = true;
    private SWVideoEncoder _swEncH264 = null;
    private int _iDegrees = 0;

    private int _iRecorderBufferSize = 0;

    private Button _SwitchCameraBtn = null;

    private boolean _bStartFlag = false;

    private int _iCameraCodecType = android.graphics.ImageFormat.NV21;

    private byte[] _yuvNV21 = new byte[WIDTH_DEF * HEIGHT_DEF * 3 / 2];
    private byte[] _yuvEdit = new byte[WIDTH_DEF * HEIGHT_DEF * 3 / 2];

    private RtmpSessionManager _rtmpSessionMgr = null;

    private Queue<byte[]> _YUVQueue = new LinkedList<byte[]>();
    private Lock _yuvQueueLock = new ReentrantLock();// ��YUV�

    private Thread _h264EncoderThread = null;
    private Runnable _h264Runnable = new Runnable() {
        @Override
        public void run() {
            while (!_h264EncoderThread.interrupted() && _bStartFlag) {
                int iSize = _YUVQueue.size();
                if (iSize > 0) {
                    _yuvQueueLock.lock();
                    byte[] yuvData = _YUVQueue.poll();
                    if (iSize > 9) {
                        Log.i(LOG_TAG, "###YUV Queue len=" + _YUVQueue.size() + ", YUV length=" + yuvData.length);
                    }

                    _yuvQueueLock.unlock();
                    if (yuvData == null) {
                        continue;
                    }

                    if (_bIsFront) {
                        _yuvEdit = _swEncH264.YUV420pRotate270(yuvData, HEIGHT_DEF, WIDTH_DEF);
                    } else {
                        _yuvEdit = _swEncH264.YUV420pRotate90(yuvData, HEIGHT_DEF, WIDTH_DEF);
                    }
                    byte[] h264Data = _swEncH264.EncoderH264(_yuvEdit);
                    if (h264Data != null) {
                        _rtmpSessionMgr.InsertVideoData(h264Data);
                        if (DEBUG_ENABLE) {
                            try {
                                _outputStream.write(h264Data);
                                int iH264Len = h264Data.length;
                                //Log.i(LOG_TAG, "Encode H264 len="+iH264Len);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            _YUVQueue.clear();
        }
    };
    private Thread _AacEncoderThread = null;
    private Runnable _aacEncoderRunnable = new Runnable() {
        @Override
        public void run() {
            DataOutputStream outputStream = null;
            if (DEBUG_ENABLE) {
                File saveDir = Environment.getExternalStorageDirectory();
                String strFilename = saveDir + "/aaa.aac";
                try {
                    outputStream = new DataOutputStream(new FileOutputStream(strFilename));
                } catch (FileNotFoundException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

            long lSleepTime = SAMPLE_RATE_DEF * 16 * 2 / _RecorderBuffer.length;

            while (!_AacEncoderThread.interrupted() && _bStartFlag) {
                int iPCMLen = _AudioRecorder.read(_RecorderBuffer, 0, _RecorderBuffer.length); // Fill buffer
                if ((iPCMLen != _AudioRecorder.ERROR_BAD_VALUE) && (iPCMLen != 0)) {
                    if (_fdkaacHandle != 0) {
                        byte[] aacBuffer = _fdkaacEnc.FdkAacEncode(_fdkaacHandle, _RecorderBuffer);
                        if (aacBuffer != null) {
                            long lLen = aacBuffer.length;

                            _rtmpSessionMgr.InsertAudioData(aacBuffer);
                            //Log.i(LOG_TAG, "fdk aac length="+lLen+" from pcm="+iPCMLen);
                            if (DEBUG_ENABLE) {
                                try {
                                    outputStream.write(aacBuffer);
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } else {
                    Log.i(LOG_TAG, "######fail to get PCM data");
                }
                try {
                    Thread.sleep(lSleepTime / 10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.i(LOG_TAG, "AAC Encoder Thread ended ......");
        }
    };
    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            Bundle b = msg.getData();
            int ret;
            switch (msg.what) {
                case ID_RTMP_PUSH_START: {
                    Start();
                    break;
                }
            }
        }
    };
    private View view;
    private Camera.PreviewCallback _previewCallback = new Camera.PreviewCallback() {

        @Override
        public void onPreviewFrame(byte[] YUV, Camera currentCamera) {
            if (!_bStartFlag) {
                return;
            }

            boolean bBackCameraFlag = true;

            byte[] yuv420 = null;

            if (_iCameraCodecType == android.graphics.ImageFormat.YV12) {
                yuv420 = new byte[YUV.length];
                _swEncH264.swapYV12toI420_Ex(YUV, yuv420, HEIGHT_DEF, WIDTH_DEF);
            } else if (_iCameraCodecType == android.graphics.ImageFormat.NV21) {
                yuv420 = _swEncH264.swapNV21toI420(YUV, HEIGHT_DEF, WIDTH_DEF);
            }

            if (yuv420 == null) {
                return;
            }
            if (!_bStartFlag) {
                return;
            }
            _yuvQueueLock.lock();
            if (_YUVQueue.size() > 1) {
                _YUVQueue.clear();
            }
            _YUVQueue.offer(yuv420);
            _yuvQueueLock.unlock();
        }
    };
    private View.OnClickListener _stopOnClickedEvent = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (_mCamera != null) {
                try {
                    _mCamera.setPreviewCallback(null);
                    _mCamera.setPreviewDisplay(null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                _mCamera.stopPreview();
                _mCamera.release();
                _mCamera = null;
            }
            if (_bStartFlag) {
                Stop();
            }
            stopService(new Intent(VideoService.this, VideoService.class));
            getWindowManager().removeView(VideoService.this.view);
        }
    };
    private View.OnClickListener _switchCameraOnClickedEvent = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            if (_mCamera == null) {
                return;
            }
            _mCamera.setPreviewCallback(null);
            _mCamera.stopPreview();
            _mCamera.release();
            _mCamera = null;

            if (_bIsFront) {
                _mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            } else {
                _mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            }
            _bIsFront = !_bIsFront;
            InitCamera();
        }
    };

    private int getDispalyRotation() {
        int i = getWindowManager().getDefaultDisplay().getRotation();
        switch (i) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }

    private int getDisplayOritation(int degrees, int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int result = 0;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    public void InitCamera() {
        Camera.Parameters p = _mCamera.getParameters();

        Camera.Size prevewSize = p.getPreviewSize();
        Log.i(LOG_TAG, "Original Width:" + prevewSize.width + ", height:" + prevewSize.height);

        List<Camera.Size> PreviewSizeList = p.getSupportedPreviewSizes();
        List<Integer> PreviewFormats = p.getSupportedPreviewFormats();
        Log.i(LOG_TAG, "Listing all supported preview sizes");
        for (Camera.Size size : PreviewSizeList) {
            Log.i(LOG_TAG, "  w: " + size.width + ", h: " + size.height);
        }

        Log.i(LOG_TAG, "Listing all supported preview formats");
        Integer iNV21Flag = 0;
        Integer iYV12Flag = 0;
        for (Integer yuvFormat : PreviewFormats) {
            Log.i(LOG_TAG, "preview formats:" + yuvFormat);
            if (yuvFormat == android.graphics.ImageFormat.YV12) {
                iYV12Flag = android.graphics.ImageFormat.YV12;
            }
            if (yuvFormat == android.graphics.ImageFormat.NV21) {
                iNV21Flag = android.graphics.ImageFormat.NV21;
            }
        }

        if (iNV21Flag != 0) {
            _iCameraCodecType = iNV21Flag;
        } else if (iYV12Flag != 0) {
            _iCameraCodecType = iYV12Flag;
        }
        p.setPreviewSize(HEIGHT_DEF, WIDTH_DEF);
        p.setPreviewFormat(_iCameraCodecType);
        p.setPreviewFrameRate(FRAMERATE_DEF);

        _mCamera.setDisplayOrientation(_iDegrees);
        p.setRotation(_iDegrees);
        _mCamera.setPreviewCallback(_previewCallback);
        _mCamera.setParameters(p);
        try {
            _mCamera.setPreviewDisplay(_mSurfaceView.getHolder());
        } catch (Exception e) {
            return;
        }
        _mCamera.cancelAutoFocus();//只有加上了这一句，才会自动对焦。

        _mCamera.startPreview();
    }

    public WindowManager getWindowManager() {
        return (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
    }

    private void Start() {
        if (DEBUG_ENABLE) {
            File saveDir = Environment.getExternalStorageDirectory();
            String strFilename = saveDir + "/aaa.h264";
            Log.i("dl--", "Start: " + strFilename);
            try {
                _outputStream = new DataOutputStream(new FileOutputStream(strFilename));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
//_rtmpSessionMgr.Start("rtmp://175.25.23.34/live/12345678");
        //_rtmpSessionMgr.Start("rtmp://192.168.0.110/live/12345678");
        _rtmpSessionMgr = new RtmpSessionManager();
        _rtmpSessionMgr.Start(_rtmpUrl);

        int iFormat = _iCameraCodecType;
        _swEncH264 = new SWVideoEncoder(WIDTH_DEF, HEIGHT_DEF, FRAMERATE_DEF, BITRATE_DEF);
        _swEncH264.start(iFormat);

        _bStartFlag = true;

        _h264EncoderThread = new Thread(_h264Runnable);
        _h264EncoderThread.setPriority(Thread.MAX_PRIORITY);
        _h264EncoderThread.start();

        _AudioRecorder.startRecording();
        _AacEncoderThread = new Thread(_aacEncoderRunnable);
        _AacEncoderThread.setPriority(Thread.MAX_PRIORITY);
        _AacEncoderThread.start();
    }

    private void Stop() {
        _bStartFlag = false;

        _AacEncoderThread.interrupt();
        _h264EncoderThread.interrupt();

        _AudioRecorder.stop();
        _swEncH264.stop();

        _rtmpSessionMgr.Stop();

        _yuvQueueLock.lock();
        _YUVQueue.clear();
        _yuvQueueLock.unlock();

        if (DEBUG_ENABLE) {
            if (_outputStream != null) {
                try {
                    _outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void InitAudioRecord() {
        _iRecorderBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_DEF,
                AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        _AudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE_DEF, AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, _iRecorderBufferSize);
        _RecorderBuffer = new byte[_iRecorderBufferSize];

        _fdkaacEnc = new FdkAacEncode();
        _fdkaacHandle = _fdkaacEnc.FdkAacInit(SAMPLE_RATE_DEF, CHANNEL_NUMBER_DEF);
    }

    private void RtmpStartMessage() {
        Message msg = new Message();
        msg.what = ID_RTMP_PUSH_START;
        Bundle b = new Bundle();
        b.putInt("ret", 0);
        msg.setData(b);
        mHandler.sendMessage(msg);
    }

    private void InitAll() {
        WindowManager wm = this.getWindowManager();

        int width = wm.getDefaultDisplay().getWidth();
        int height = wm.getDefaultDisplay().getHeight();
        int iNewWidth = (int) (height * 3.0 / 4.0);

        RelativeLayout rCameraLayout = (RelativeLayout) view.findViewById(R.id.cameraRelative);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        int iPos = width - iNewWidth;
        layoutParams.setMargins(iPos, 0, 0, 0);

        _mSurfaceView = (SurfaceView) this.view.findViewById(R.id.surfaceViewEx);
        _mSurfaceView.getHolder().setFixedSize(HEIGHT_DEF, WIDTH_DEF);
        _mSurfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        _mSurfaceView.getHolder().setKeepScreenOn(true);
        _mSurfaceView.getHolder().addCallback(new SurceCallBack());
        _mSurfaceView.setLayoutParams(layoutParams);

        InitAudioRecord();

        _SwitchCameraBtn = (Button) view.findViewById(R.id.SwitchCamerabutton);
        _SwitchCameraBtn.setOnClickListener(_switchCameraOnClickedEvent);
        view.findViewById(R.id.btn_close).setOnClickListener(_stopOnClickedEvent);

        RtmpStartMessage();//开始推流
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        _wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.service_video, null);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        //设置View默认的摆放位置
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        //设置window type
        layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        //设置背景为透明
        layoutParams.format = PixelFormat.RGBA_8888;
        //注意该属性的设置很重要，FLAG_NOT_FOCUSABLE使浮动窗口不获取焦点,若不设置该属性，屏幕的其它位置点击无效，应为它们无法获取焦点
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        //设置视图的显示位置，通过WindowManager更新视图的位置其实就是改变(x,y)的值
        layoutParams.height = 300;
        layoutParams.width = 300;
        if (Build.VERSION.SDK_INT >= 23) {
            if (Settings.canDrawOverlays(getApplicationContext())) {
                windowManager.addView(view, layoutParams);
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivity(intent);
                stopService(new Intent(getApplicationContext(), VideoService.class));
            }
        } else {
            windowManager.addView(view, layoutParams);
        }


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        _rtmpUrl = intent.getStringExtra(MainActivity.RTMPURL_MESSAGE);
        InitAll();
        return START_NOT_STICKY;//结束后不会自动启动服务
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("dl--", "onBind: ");
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("dl--", "onDestroy: ");
        if (_mCamera != null) {
            try {
                _mCamera.setPreviewCallback(null);
                _mCamera.setPreviewDisplay(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            _mCamera.stopPreview();
            _mCamera.release();
            _mCamera = null;
        }
        if (_bStartFlag) {
            Stop();
        }
        try {
            getWindowManager().removeView(VideoService.this.view);
        } catch (IllegalArgumentException e) {
            view = null;
        }

    }

    private final class SurceCallBack implements SurfaceHolder.Callback {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            _mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
                        InitCamera();
                        camera.cancelAutoFocus();//只有加上了这一句，才会自动对焦。
                    }
                }
            });
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            _iDegrees = getDisplayOritation(getDispalyRotation(), 0);
            if (_mCamera != null) {
                InitCamera();
                return;
            }

            _mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            InitCamera();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    }
}
