package com.ecsoft.zyymaintain;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.ecsoft.zyymaintain.config.GlobalConfiguration;
import com.ecsoft.zyymaintain.database.CopyDBUtils;
import com.ecsoft.zyymaintain.database.DbSettingsService;
import com.ecsoft.zyymaintain.network.LoginService;
import com.ecsoft.zyymaintain.service.NotificationService;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.time.Instant;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    /*

36x36 (0.75x)      - 低密度 (ldpi)
48x48（1.0x 基准）  - 中密度 (mdpi)
72x72 (1.5x) - 高密度 (hdpi)
96x96 (2.0x) - 超高密度 (xhdpi)
144x144 (3.0x) - 超超高密度 (xxhdpi)
192x192 (4.0x) - 超超超高密度 (xxxhdpi)

     */

    private ConstraintLayout clMainActivity;
    private Button       btnLogin;
    private EditText     etUsername;
    private EditText     etPassword;
    private CheckBox     cbKeepLogin;
    private LinearLayout llNetworkConfig;

    private static final int HANDLER_LOGGING       = 0x0001;
    private static final int HANDLER_LOGIN_SUCCESS = 0x0002;
    private static final int HANDLER_AUTO_SUCCESS  = 0x0003;
    private static final int HANDLER_LOGIN_FAILED  = 0x1001;
    private static final int HANDLER_AUTO_FAILED   = 0x1002;

    private ProgressDialog loggingProgressDialog;

    // 壁纸：https://img.xjh.me/random_img.php

    // 定义一个Handler
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch (message.what){
                case HANDLER_LOGGING:{
                    loggingProgressDialog = new ProgressDialog(MainActivity.this);
                    loggingProgressDialog.setMessage("登录请求中....");
                    loggingProgressDialog.setCancelable(false);
                    loggingProgressDialog.show();
                    break;
                }
                case HANDLER_LOGIN_SUCCESS:{
                    Toast.makeText(MainActivity.this, message.getData().getString("msg"), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this,WorkOrderManageActivity.class);
                    loggingProgressDialog.dismiss();
                    startActivity(intent);
                    finish();
                    break;
                }
                case HANDLER_LOGIN_FAILED:{
                    Bundle data = message.getData(); // 获取传递参数
                    String messageText = data.getString("data"); // 获取对话框Message文本
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("登录失败")
                            .setMessage(messageText)
                            .setCancelable(false)
                            .setPositiveButton("重新输入",null)
                            .show();
                    loggingProgressDialog.dismiss();
                    break;
                }
                case HANDLER_AUTO_FAILED:{
                    Toast.makeText(MainActivity.this, "账号可能在别处登录，请重新登录！", Toast.LENGTH_SHORT).show();
                    loggingProgressDialog.dismiss();
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("警告")
                            .setMessage("您好，您的账号可能在别处登录，如果不是您亲自作为，请注意及时联系管理员修改账号密码")
                            .setCancelable(false)
                            .setPositiveButton("我以知晓",null)
                            .show();
                    break;
                }
                case HANDLER_AUTO_SUCCESS:{
                    loggingProgressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "自动登录成功", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this,WorkOrderManageActivity.class);
                    startActivity(intent);
                    finish();
                    break;
                }
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isFirstRun(); // 判断是否为第一次启动
        initViewComponent();
        initViewComponentData();
        initViewComponentEvent();
        isAutoLogin(); // 判断是否自动登录
        initService(); // 加载服务
        loadBK(); // 加载登录背景
        initGlobalConfiguration(); // 加载全局配置
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }
    }

    private void initService() {
        Intent intent = new Intent(MainActivity.this, NotificationService.class);
        Bundle bundle = new Bundle();
        DbSettingsService dbSettingsService = new DbSettingsService(MainActivity.this);
        bundle.putString("userName",dbSettingsService.getSettings("tokenUser"));
        bundle.putString("userToken",dbSettingsService.getSettings("loginToken"));
        bundle.putString("userId",dbSettingsService.getSettings("uid"));
        intent.putExtra("data",bundle);
        startService(intent);
    }

    private void initGlobalConfiguration() {
        DbSettingsService dbSettingsService = new DbSettingsService(MainActivity.this);
        if (dbSettingsService.getSettings("serverAddress") != null && dbSettingsService.getSettings("serverPort") != null) {
            // 设置全局变量
            GlobalConfiguration.serverUrl = "http://" + dbSettingsService.getSettings("serverAddress").trim() + ":" +dbSettingsService.getSettings("serverPort").trim();
            GlobalConfiguration.webSocket = "ws://" + dbSettingsService.getSettings("serverAddress").trim() + ":" +dbSettingsService.getSettings("serverPort").trim();
        } else {
            // 启动网络设置Activity
            Toast.makeText(this, "未配置有效网络环境，请设置", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this,NetworkConfigurationActivity.class);
            startActivity(intent);
        }
    }

    private void initViewComponent() {
        clMainActivity = findViewById(R.id.cl_main_activity);
        btnLogin = findViewById(R.id.btn_login);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        cbKeepLogin = findViewById(R.id.cb_keep_login);
        llNetworkConfig = findViewById(R.id.ll_network_config);
    }
    private void isAutoLogin(){
        DbSettingsService settingsService = new DbSettingsService(MainActivity.this);
        String isKeepLogin = settingsService.getSettings("keepLogin");
        if (isKeepLogin.equals("true")){ // 自动登录为真
            loggingProgressDialog = new ProgressDialog(MainActivity.this);
            loggingProgressDialog.setMessage("自动登录中....");
            loggingProgressDialog.setCancelable(false);
            loggingProgressDialog.show();
            // 自动登录逻辑
            String lastLoginTimeStr = settingsService.getSettings("lastLogin");
            Instant lastLoginTime   = Instant.parse(lastLoginTimeStr);
            Date lastLoginDateTime = Date.from(lastLoginTime);
            Date nowTime         = new Date();
            int year                = lastLoginDateTime.getYear();
            int month               = lastLoginDateTime.getMonth();
            int day                 = lastLoginDateTime.getDay();
            int ss =  nowTime.getYear();
            int ss2 = nowTime.getMonth();
            System.out.println(ss+ss2);
            if (year != nowTime.getYear() || month != nowTime.getMonth()){
                // 年份或者月份不一致，登录失效
                settingsService.setSettings("keepLogin","false");
                settingsService.setSettings("token",null);
                Toast.makeText(this, "登录失效请重新登录", Toast.LENGTH_SHORT).show();
                loggingProgressDialog.dismiss();
                cbKeepLogin.setChecked(true);
                etUsername.setText(settingsService.getSettings("tokenUser"));
            } else if (day - nowTime.getYear() >=10){
                // 上次记录登录时间大于10天，登录失效
                settingsService.setSettings("keepLogin","false");
                settingsService.setSettings("token",null);
                Toast.makeText(this, "登录失效请重新登录", Toast.LENGTH_SHORT).show();
                loggingProgressDialog.dismiss();
                cbKeepLogin.setChecked(true);
                etUsername.setText(settingsService.getSettings("tokenUser"));
            } else {
                // 登录有效，进入Token令牌交换校验

                String loginToken = settingsService.getSettings("loginToken");
                String tokenUser  = settingsService.getSettings("tokenUser");
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        LoginService loginService = new LoginService();
                        String responseText = loginService.tokenCheck(tokenUser, loginToken);
                        JSONTokener tokener = new JSONTokener(responseText);
                        try {
                            JSONObject responseJson = (JSONObject) tokener.nextValue();
                            if (responseJson.getString("code").equals("200")) {
                                JSONObject authResult = responseJson.getJSONObject("checks");
                                DbSettingsService dbSettingsService = new DbSettingsService(MainActivity.this);
                                dbSettingsService.setSettings("uid",String.valueOf(authResult.getInt("tokenUID")));
                                mHandler.sendEmptyMessage(HANDLER_AUTO_SUCCESS);
                            } else {
                                mHandler.sendEmptyMessage(HANDLER_AUTO_FAILED);

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();

            }
        }
    }
    private void initViewComponentData(){
        DbSettingsService settingsService = new DbSettingsService(MainActivity.this);
        etUsername.setText(settingsService.getSettings("rememberUser"));
    }
    private void initViewComponentEvent() {
        // 设置登录按钮的单击监听事件当执行登录操作
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!etUsername.getText().toString().equals("") && !etPassword.getText().toString().equals("")){
                    DbSettingsService settingsService = new DbSettingsService(MainActivity.this);
                    settingsService.setSettings("rememberUser",etUsername.getText().toString());
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mHandler.sendEmptyMessage(HANDLER_LOGGING);
                            LoginService loginService = new LoginService();
                            String responseText = loginService.doLogin(etUsername.getText().toString(), etPassword.getText().toString());
                            JSONTokener tokener = new JSONTokener(responseText);
                            try {
                                JSONObject responseJson = (JSONObject) tokener.nextValue();
                                JSONObject authResult = responseJson.getJSONObject("authResult");
                                if (authResult.getBoolean("isLogin")){ // 校验登录是否成功
                                    // 判断Token是否为空
                                    if (!authResult.getString("token").equals("")){
                                        // 判断是否保持登录
                                        if (cbKeepLogin.isChecked()){
                                            // 保持登录
                                            // 写出数据
                                            settingsService.setSettings("keepLogin","true");
                                            // 写出服务器返回的令牌值
                                            settingsService.setSettings("uid",authResult.getString("uid"));
                                            settingsService.setSettings("loginToken",authResult.getString("token"));

                                            Instant nowTime = Instant.now();
                                            // 写出令牌链接的用户名
                                            settingsService.setSettings("lastLogin",nowTime.toString());
                                            settingsService.setSettings("tokenUser",etUsername.getText().toString());
                                        } else {
                                            // 不保持登录
                                            DbSettingsService settingsService = new DbSettingsService(MainActivity.this);
                                            settingsService.setSettings("keepLogin","false");
                                            settingsService.setSettings("loginToken",authResult.getString("token"));

                                        }
                                        Message msg = new Message();
                                        Bundle bundle = new Bundle();
                                        bundle.putString("msg","用户登录成功");
                                        msg.setData(bundle);
                                        msg.what = HANDLER_LOGIN_SUCCESS;
                                        mHandler.sendMessage(msg);
                                    } else {
                                        Message msg = new Message();
                                        msg.what = HANDLER_LOGIN_FAILED;
                                        Bundle bundle = new Bundle();
                                        bundle.putString("data","对不起，登录失败\n"+"失败原因：服务器未发送有效Token");
                                        msg.setData(bundle);
                                        mHandler.sendMessage(msg);
                                    }
                                } else {
                                    Message msg = new Message();
                                    msg.what = HANDLER_LOGIN_FAILED;
                                    Bundle bundle = new Bundle();
                                    bundle.putString("data","对不起，登录失败\n"+"失败原因："+responseJson.getString("msg"));
                                    msg.setData(bundle);
                                    mHandler.sendMessage(msg);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    thread.start();
                } else {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("提示")
                            .setMessage("用户名密码不完整")
                            .setPositiveButton("重新输入",null)
                            .show();
                }


            }
        });
        // 设置网络设置按钮的单击监听时间
        llNetworkConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 创建跳转网络配置意图
                Intent intent = new Intent(MainActivity.this,NetworkConfigurationActivity.class);
                startActivity(intent);// 开启Activity
            }// 81115722
        });
    }
    private void isFirstRun(){
        String fileStr = "/data/data/" + getPackageName() + "/data.db";
        File file = new File(fileStr);
        if (!file.exists()){ // 数据库文件不存在复制数据库
            CopyDBUtils.copyDbFile(MainActivity.this,"data.db");
            Intent intent = new Intent(MainActivity.this,WelcomeActivity.class);
            startActivity(intent);
        }
    }
    private void loadBK(){
        // 使用Glide支撑库加载
    }

}