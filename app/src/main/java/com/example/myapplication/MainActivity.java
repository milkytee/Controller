package com.example.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.skydroid.rcsdk.RCSDKManager;
import com.skydroid.rcsdk.KeyManager;
import com.skydroid.rcsdk.comm.CommListener;
import com.skydroid.rcsdk.common.pipeline.Pipeline;
import com.skydroid.rcsdk.PipelineManager;
import com.skydroid.rcsdk.common.error.SkyException;
import com.skydroid.rcsdk.SDKManagerCallBack;
import com.skydroid.rcsdk.key.RemoteControllerKey;
import com.skydroid.rcsdk.key.AirLinkKey;
import com.skydroid.rcsdk.common.callback.KeyListener;
import com.skydroid.rcsdk.common.callback.CompletionCallbackWith;

import com.skydroid.fpvplayer.FPVWidget;
import com.skydroid.fpvplayer.PlayerType;
import com.skydroid.fpvplayer.RtspTransport;
import com.skydroid.fpvplayer.*;
import android.widget.RelativeLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    
    // UI组件
    private TextView tvBoomAngle;
    private TextView tvStickAngle;
    private TextView tvBucketAngle;
    private TextView tvLatitude;
    private TextView tvLongitude;
    private TextView tvDigDepth;
    private TextView tvVideoLink;
    private TextView tvRcSignal;
    private TextView tvBattery;
    
    private ProgressBar progressDigDepth;
    
    private ExcavatorPostureView excavatorPostureView;
    private FPVWidget fpvWidget;
    
    private Button btnLights;
    private Button btnHorn;
    private Button btnModeSwitch;
    private Button btnHome;
    private Button btnStop;
    
    // 驾驶模式状态
    private boolean isManualMode = true;
    
    // 数据更新Handler
    private Handler handler;
    private Runnable updateRunnable;
    
    // 摇杆值更新Handler（独立更新，100ms频率）
    private Handler joystickHandler;
    private Runnable joystickUpdateRunnable;
    
    // 模拟数据
    private Random random = new Random();
    private int angleIndex = 0;
    private int angleUpdateCounter = 0; // 用于控制角度更新频率
    
    // 机械臂角度轮换数据
    private List<AngleSet> angleSets = new ArrayList<>();
    
    // UDP相关
    private Pipeline udpPipeline;
    private boolean useRealData = false; // 是否使用真实UDP数据
    private float realBoomAngle = 0f;
    private float realStickAngle = 0f;
    private float realBucketAngle = 0f;
    
    // 摇杆值
    private int ch1Value = 0; // 右摇杆左右
    private int ch2Value = 0; // 右摇杆上下
    private int ch3Value = 0; // 左摇杆上下
    private int ch4Value = 0; // 左摇杆左右
    
    // 信号强度相关
    private KeyListener<Integer> keySignalQualityListener;
    private int currentSignalStrength = 0; // 当前信号强度（0-100）
    
    // 角度数据类
    private static class AngleSet {
        float boom;
        float stick;
        float bucket;
        
        AngleSet(float boom, float stick, float bucket) {
            this.boom = boom;
            this.stick = stick;
            this.bucket = bucket;
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置全屏模式
        setFullScreenMode();
        
        setContentView(R.layout.activity_main);
        
        initViews();
        initAngleSets();
        initButtons();
        initSDK();
        startDataUpdates();
        initVideoPlayer();
    }
    
    /**
     * 设置全屏模式（隐藏状态栏和导航栏）
     */
    private void setFullScreenMode() {
        // 使用 WindowCompat 和 WindowInsetsControllerCompat 实现兼容性全屏
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        
        if (windowInsetsController != null) {
            // 隐藏状态栏和导航栏
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
            // 设置沉浸式模式，让内容可以延伸到系统栏区域
            windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }

    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // 当窗口获得焦点时，确保全屏模式
            setFullScreenMode();
        }
    }
    
    private void initViews() {
        tvBoomAngle = findViewById(R.id.tvBoomAngle);
        tvStickAngle = findViewById(R.id.tvStickAngle);
        tvBucketAngle = findViewById(R.id.tvBucketAngle);
        tvLatitude = findViewById(R.id.tvLatitude);
        tvLongitude = findViewById(R.id.tvLongitude);
        tvDigDepth = findViewById(R.id.tvDigDepth);
        tvVideoLink = findViewById(R.id.tvVideoLink);
        tvRcSignal = findViewById(R.id.tvRcSignal);
        tvBattery = findViewById(R.id.tvBattery);
        
        progressDigDepth = findViewById(R.id.progressDigDepth);
        
        excavatorPostureView = findViewById(R.id.excavatorPostureView);
        fpvWidget = findViewById(R.id.fpvWidget);
    }
    
    private void initAngleSets() {
        // 初始化几组机械臂角度数据用于轮换
        angleSets.add(new AngleSet(-30f, 45f, 10f));   // 初始位置
        angleSets.add(new AngleSet(-20f, 60f, 20f));   // 伸展位置
        angleSets.add(new AngleSet(-40f, 30f, -5f));   // 收缩位置
        angleSets.add(new AngleSet(-25f, 50f, 15f));   // 中间位置
        angleSets.add(new AngleSet(-35f, 40f, 5f));    // 另一个位置
        angleSets.add(new AngleSet(-15f, 70f, 25f));   // 最大伸展
    }
    
    /**
     * 初始化视频播放器
     */
    private void initVideoPlayer() {
        if (fpvWidget != null) {
            // 使用硬解码
            fpvWidget.setUsingMediaCodec(true);

            // 设置固定的RTSP地址
            fpvWidget.setUrl("rtsp://192.168.144.100:554/stream1");
            
            // 使用云卓播放器
            fpvWidget.setPlayerType(PlayerType.ONLY_SKY);
            
            // RTSP流TCP/UDP连接方式（自动选择）
            fpvWidget.setRtspTranstype(RtspTransport.AUTO);
            
            // 开始播放
            fpvWidget.start();
        }
    }
    
    private void initButtons() {
        btnLights = findViewById(R.id.btnLights);
        btnHorn = findViewById(R.id.btnHorn);
        btnModeSwitch = findViewById(R.id.btnModeSwitch);
        btnHome = findViewById(R.id.btnHome);
        btnStop = findViewById(R.id.btnStop);
        
        // 初始化模式切换按钮文本
        updateModeButtonText();
        
        btnLights.setOnClickListener(v -> {
            Toast.makeText(this, "亮灯已切换", Toast.LENGTH_SHORT).show();
        });
        
        btnHorn.setOnClickListener(v -> {
            Toast.makeText(this, "鸣笛已激活", Toast.LENGTH_SHORT).show();
        });
        
        btnModeSwitch.setOnClickListener(v -> {
            showModeSwitchDialog();
        });
        
        btnHome.setOnClickListener(v -> {
            Toast.makeText(this, "返回主页", Toast.LENGTH_SHORT).show();
            // 重置到初始角度
            angleIndex = 0;
            updateAngles();
        });
        
        btnStop.setOnClickListener(v -> {
            Toast.makeText(this, "紧急停止", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void showModeSwitchDialog() {
        String currentMode = isManualMode ? "手动驾驶" : "自动驾驶";
        String targetMode = isManualMode ? "自动驾驶" : "手动驾驶";
        
        new AlertDialog.Builder(this)
            .setTitle("切换驾驶模式")
            .setMessage("确定要从 " + currentMode + " 切换到 " + targetMode + " 吗？")
            .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    isManualMode = !isManualMode;
                    updateModeButtonText();
                    Toast.makeText(MainActivity.this, 
                        "已切换到" + (isManualMode ? "手动驾驶" : "自动驾驶"), 
                        Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .setCancelable(true)
            .show();
    }
    
    private void updateModeButtonText() {
        btnModeSwitch.setText(isManualMode ? "手动驾驶" : "自动驾驶");
    }
    
    private void startDataUpdates() {
        // 主数据更新Handler（1秒更新一次）
        handler = new Handler(Looper.getMainLooper());
        
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateAllData();
                handler.postDelayed(this, 1000); // 每秒更新一次
            }
        };
        
        handler.post(updateRunnable);
        
        // 摇杆值更新Handler（100ms更新一次）
        joystickHandler = new Handler(Looper.getMainLooper());
        
        joystickUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateJoystickValues(); // 只更新摇杆值
                joystickHandler.postDelayed(this, 100); // 每100ms更新一次
            }
        };
        
        joystickHandler.post(joystickUpdateRunnable);
    }
    
    private void updateAllData() {
        // 更新连接信息
        updateConnectionInfo();
        
        // 更新机械臂角度（如果使用模拟数据，则定时更新；如果使用真实数据，UDP数据到达时已更新）
        if (!useRealData) {
            updateAngles();
        }
        
        // 注意：摇杆值更新已独立到100ms循环中，不在这里更新
        
        // 更新定位信息（带小幅随机波动）
        updatePositioning();
        
        // 更新挖掘深度（带小幅随机波动）
        updateDigDepth();
    }
    
    private void updateConnectionInfo() {
        // 连接延迟: 45-60ms之间波动
        int delay = 45 + random.nextInt(15);
        tvVideoLink.setText("延迟: " + delay + "ms");
        
        // 信号强度: 使用真实数据（通过监听器更新）
        // 这里不再更新，由 keySignalQualityListener 回调更新
        
        // 电池电量: 80-90%之间波动
        int battery = 80 + random.nextInt(10);
        tvBattery.setText("电池: " + battery + "%");
    }
    
    /**
     * 更新信号强度显示
     */
    private void updateSignalDisplay() {
        if (tvRcSignal != null) {
            tvRcSignal.setText("信号强度 "+ currentSignalStrength + "%");
        }
    }
    
    private void updateAngles() {
        float boom, stick, bucket;
        
        if (useRealData) {
            // 使用真实UDP数据
            boom = realBoomAngle;
            stick = realStickAngle;
            bucket = realBucketAngle;
        } else {
            // 使用模拟数据（每3秒切换一次角度）
        angleUpdateCounter++;
        if (angleUpdateCounter >= 3) {
            angleIndex = (angleIndex + 1) % angleSets.size();
            angleUpdateCounter = 0;
        }
        
        // 轮换角度数据
        AngleSet currentSet = angleSets.get(angleIndex);
            boom = currentSet.boom;
            stick = currentSet.stick;
            bucket = currentSet.bucket;
        }
        
        // 更新视图
        excavatorPostureView.setAngles(boom, stick, bucket);
        
        // 更新文本显示
        tvBoomAngle.setText(String.format(Locale.getDefault(), "BOOM: %.2f°", boom));
        tvStickAngle.setText(String.format(Locale.getDefault(), "STICK: %.2f°", stick));
        tvBucketAngle.setText(String.format(Locale.getDefault(), "BUCKET: %.2f°", bucket));
    }
    
    private void updatePositioning() {
        // 经纬度在小范围内波动
        double lat = 31.230400 + (random.nextDouble() - 0.5) * 0.0001;
        double lng = 121.473700 + (random.nextDouble() - 0.5) * 0.0001;
        
        tvLatitude.setText(String.format(Locale.getDefault(), "LAT: %.6f N", lat));
        tvLongitude.setText(String.format(Locale.getDefault(), "LNG: %.6f E", lng));
    }
    
    private void updateDigDepth() {
        // 挖掘深度在3.0-3.5米之间波动
        double depth = 3.0 + random.nextDouble() * 0.5;
        tvDigDepth.setText(String.format(Locale.getDefault(), "%.2f m", depth));
        
        // 进度条（0-10米范围）
        int progress = (int) (depth * 100); // 转换为整数进度
        progressDigDepth.setProgress(progress);
    }
    
    /**
     * 更新摇杆值
     */
    private void updateJoystickValues() {
        KeyManager.INSTANCE.get(RemoteControllerKey.INSTANCE.getKeyChannels(), 
            new CompletionCallbackWith<int[]>() {
                @Override
                public void onSuccess(int[] value) {
                    // value 是摇杆值数组
                    if (value != null && value.length >= 4) {
                        // 减去1500作为初始值
                        ch1Value = value[0] - 1500; // 右摇杆左右
                        ch2Value = value[1] - 1500; // 右摇杆上下
                        ch3Value = value[2] - 1500; // 左摇杆上下
                        ch4Value = value[3] - 1500; // 左摇杆左右
                    }
                }

                @Override
                public void onFailure(SkyException e) {
                    Log.e("MainActivity", "摇杆值获取失败: " + (e != null ? e.getMessage() : "未知错误"));
                }
            });
    }
    
    /**
     * 初始化SDK
     */
    private void initSDK() {
        // TODO 初始化SDK,初始化一次即可
        RCSDKManager.INSTANCE.initSDK(this, new SDKManagerCallBack() {
            @Override
            public void onRcConnected() {
                Log.d("MainActivity", "遥控器连接成功");
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "遥控器连接成功", Toast.LENGTH_SHORT).show();
                });
                // 遥控器连接成功后，创建UDP管道
                createUDPPipeline();
            }
            
            @Override
            public void onRcConnectFail(SkyException e) {
                Log.e("MainActivity", "遥控器连接失败: " + (e != null ? e.getMessage() : "未知错误"));
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "遥控器连接失败", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onRcDisconnect() {
                Log.e("MainActivity", "遥控器断开连接");
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "遥控器断开连接", Toast.LENGTH_SHORT).show();
                });
            }
        });
        
        // 设置在主线程回调
        RCSDKManager.INSTANCE.setMainThreadCallBack(true);
        
        // 连接到遥控器
        RCSDKManager.INSTANCE.connectToRC();
        
        // 注册信号强度监听器
        keySignalQualityListener = new KeyListener<Integer>() {
            @Override
            public void onValueChange(Integer oldValue, Integer newValue) {
                // newValue 是信号强度百分比 (0-100)
                currentSignalStrength = newValue != null ? newValue : 0;
                // 在主线程更新UI
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateSignalDisplay();
                    }
                });
            }
        };
        KeyManager.INSTANCE.listen(AirLinkKey.INSTANCE.getKeySignalQuality(), keySignalQualityListener);
    }
    
    /**
     * 创建UDP管道
     */
    private void createUDPPipeline() {
        // 创建UDP管道：本地端口14551，发送到127.0.0.1:14552
        udpPipeline = PipelineManager.INSTANCE.createUDPPipeline(14551, "127.0.0.1", 14552);
        
        if (udpPipeline != null) {
            // 设置通信监听器
            udpPipeline.setOnCommListener(new CommListener() {
                @Override
                public void onConnectSuccess() {
                    Log.d("UDP", "UDP管道连接成功");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            useRealData = true; // 切换到使用真实数据
                            Toast.makeText(MainActivity.this, "UDP连接成功，开始接收数据", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                
                @Override
                public void onConnectFail(SkyException e) {
                    Log.e("UDP", "UDP管道连接失败: " + (e != null ? e.getMessage() : "未知错误"));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "UDP连接失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                
                @Override
                public void onDisconnect() {
                    Log.d("UDP", "UDP管道断开连接");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            useRealData = false; // 切换回模拟数据
                            Toast.makeText(MainActivity.this, "UDP断开连接", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                
                @Override
                public void onReadData(byte[] data) {
                    // 接收到的UDP数据（14字节）
                    if (data != null) {
                        Log.d("UDP", "收到数据，长度: " + data.length);
                        
                        if (data.length == 14) {
                            // 解析数据
                            IMUDataParser.parseData(data, new IMUDataParser.ParseResultCallback() {
                                @Override
                                public void onParseSuccess(float boomAngle, float stickAngle, float bucketAngle) {
                                    // 在主线程更新UI
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            realBoomAngle = boomAngle;
                                            realStickAngle = stickAngle;
                                            realBucketAngle = bucketAngle;
                                            // 立即更新角度显示
                                            updateAngles();
                                        }
                                    });
                                }
                                
                                @Override
                                public void onParseError(String error) {
                                    Log.e("UDP", "数据解析失败: " + error);
                                }
                            });
                        } else {
                            Log.w("UDP", "数据长度不正确，期望14字节，实际: " + data.length);
                        }
                    }
                }
            });
            
            // 连接UDP管道
            PipelineManager.INSTANCE.connectPipeline(udpPipeline);
        } else {
            Log.e("UDP", "创建UDP管道失败");
            Toast.makeText(this, "创建UDP管道失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 停止视频播放
        if (fpvWidget != null) {
            fpvWidget.stop();
        }
        
        // 停止主数据更新
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
        
        // 停止摇杆值更新
        if (joystickHandler != null && joystickUpdateRunnable != null) {
            joystickHandler.removeCallbacks(joystickUpdateRunnable);
        }
        
        // 断开UDP管道
        if (udpPipeline != null) {
            PipelineManager.INSTANCE.disconnectPipeline(udpPipeline);
            udpPipeline = null;
        }
        
        // 断开遥控器连接
        RCSDKManager.INSTANCE.disconnectRC();
        
        // 取消信号强度监听
        if (keySignalQualityListener != null) {
            KeyManager.INSTANCE.cancelListen(keySignalQualityListener);
            keySignalQualityListener = null;
        }
    }
}
