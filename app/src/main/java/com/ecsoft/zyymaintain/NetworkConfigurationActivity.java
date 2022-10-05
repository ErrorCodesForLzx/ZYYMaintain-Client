package com.ecsoft.zyymaintain;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.ecsoft.zyymaintain.database.DbSettingsService;

public class NetworkConfigurationActivity extends AppCompatActivity {

    private EditText etServerAddress;
    private EditText etServerPort;
    private Button   btnApplySettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_configuration);

        initViewComponent();     // 将组件注册到视图
        initViewComponentEvent();// 加载组件的事件
        initViewComponentData(); // 加载组件数据
    }



    private void initViewComponent(){
        etServerAddress  = findViewById(R.id.et_server_address);
        etServerPort     = findViewById(R.id.et_server_port);
        btnApplySettings = findViewById(R.id.btn_apply_settings);
    }
    private void initViewComponentData() {
        DbSettingsService dbSettingsService = new DbSettingsService(NetworkConfigurationActivity.this);
        if (dbSettingsService.getSettings("serverAddress") != null) {
            etServerAddress.setText(dbSettingsService.getSettings("serverAddress").trim());
        }
        if (dbSettingsService.getSettings("serverPort") != null) {
            etServerPort.setText(dbSettingsService.getSettings("serverPort").trim());
        }
    }
    private void initViewComponentEvent() {
        btnApplySettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { // 当更新设置按钮被单击
                DbSettingsService dbSettingsService = new DbSettingsService(NetworkConfigurationActivity.this);
                if (etServerAddress.getText().toString().equals("") || etServerPort.getText().toString().equals("")){
                    // 如果服务器地址和端口有一个为空
                    new AlertDialog.Builder(NetworkConfigurationActivity.this)
                            .setTitle("提示")
                            .setMessage("请完整输入有效的服务器地址和端口!")
                            .setPositiveButton("知道了",null)
                            .setCancelable(false)
                            .show();
                } else {
                    dbSettingsService.setSettings("serverAddress",etServerAddress.getText().toString());
                    dbSettingsService.setSettings("serverPort",etServerPort.getText().toString());
                    new AlertDialog.Builder(NetworkConfigurationActivity.this)
                            .setTitle("提示")
                            .setMessage("服务器地址设置成功，请重启软件!")
                            .setPositiveButton("关闭软件", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // 杀死该程序进程
                                    android.os.Process.killProcess(android.os.Process.myPid());
                                }
                            })
                            .setCancelable(false)
                            .show();
                }

            }
        });
    }
}