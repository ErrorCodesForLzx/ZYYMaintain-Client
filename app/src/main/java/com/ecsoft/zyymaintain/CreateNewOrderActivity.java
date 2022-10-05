package com.ecsoft.zyymaintain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.ecsoft.zyymaintain.config.GlobalConfiguration;
import com.ecsoft.zyymaintain.database.DbSettingsService;
import com.ecsoft.zyymaintain.network.util.OKHTTPUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CreateNewOrderActivity extends AppCompatActivity {

    private LinearLayout llBack;
    private EditText     etOrderTitle;
    private EditText     etOrderContent;
    private TextView     tvWordLimit;
    private TextView     tvClearPic;
    private ImageView    ivUploadPic;
    private Button       btnCommitWorkOrder;

    private ProgressDialog pdUpdatingPic ;
    private ProgressDialog pdSendWorkOrder;

    private boolean upload_hasPic = false;
    private String  upload_picUrl = "";

    private static final int INTENT_REQ_CAPTURE_IMAGE = 0x10001; // 拍照请求码
    private static final int HANDLER_UPLOAD_STATUS_SUCCESS = 0x10001; // 图片上传成功
    private static final int HANDLER_UPLOAD_STATUS_FAILED  = 0x10002; // 图片上传失败
    private static final int HANDLER_SEND_WORK_ORDER_SUCCESS = 0x20001; // 工单上传成功
    private static final int HANDLER_SEND_WORK_ORDER_FAILED  = 0x20002; // 工单上传失败

    // 定义Handler方法
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch (message.what){
                case HANDLER_UPLOAD_STATUS_SUCCESS:{
                    pdUpdatingPic.dismiss();
                    Toast.makeText(CreateNewOrderActivity.this, "图片上传成功 (1/1)", Toast.LENGTH_SHORT).show();
                    Bundle data = message.getData();
                    int fid = data.getInt("fid");
                    // 将fid和服务器请求API连接获得图片地址
                    String imgUrl = GlobalConfiguration.serverUrl + "/api/file/getPic?fid="+fid;
                    upload_picUrl = imgUrl;
                    // 使用Glide显示网络图片
                    Glide.with(CreateNewOrderActivity.this)
                            .load(imgUrl)
                            .into(ivUploadPic);
                    upload_hasPic = true;
                    tvClearPic.setVisibility(View.VISIBLE); // 显示清除图片标签
                    break;
                }
                case HANDLER_UPLOAD_STATUS_FAILED:{
                    pdUpdatingPic.dismiss();
                    Bundle data = message.getData();
                    AlertDialog alertDialog = new AlertDialog.Builder(CreateNewOrderActivity.this)
                            .setTitle("发生错误")
                            .setMessage(data.getString("msg"))
                            .setNegativeButton("我知道了",null)
                            .show();
                    break;
                }
                case HANDLER_SEND_WORK_ORDER_SUCCESS:{
                    Toast.makeText(CreateNewOrderActivity.this, "工单添加成功，请耐心等待处理", Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                }
                case HANDLER_SEND_WORK_ORDER_FAILED:{
                    Bundle data = message.getData();
                    new AlertDialog.Builder(CreateNewOrderActivity.this)
                            .setTitle("发生错误")
                            .setMessage(data.getString("msg"))
                            .setPositiveButton("我知道了",null)
                            .setCancelable(false)
                            .show();
                }

            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_order);

        initViewComponent();      // 注册组件到对象
        initViewComponentEvent(); // 加载组件事件
        initViewComponentData();  // 加载组件数据和状态
    }

    private void initViewComponentData() {
        tvClearPic.setVisibility(View.GONE); // 清除图片标签默认为隐藏状态
    }

    private void initViewComponent() {
        llBack             = findViewById(R.id.ll_back);
        etOrderTitle       = findViewById(R.id.et_order_title);
        etOrderContent     = findViewById(R.id.et_order_content);
        ivUploadPic        = findViewById(R.id.iv_upload_pic);
        tvWordLimit        = findViewById(R.id.tv_word_limit);
        tvClearPic         = findViewById(R.id.tv_clear_pic);
        btnCommitWorkOrder = findViewById(R.id.btn_commit_work_order);
    }
    private void initViewComponentEvent() {
        llBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        ivUploadPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 判断是否有上传图片
                if (upload_hasPic){
                    Toast.makeText(CreateNewOrderActivity.this, "图片已经上传成功", Toast.LENGTH_SHORT).show();
                } else {
                    // 当上传图片按钮被单击
                    new  AlertDialog.Builder(CreateNewOrderActivity.this)
                            .setTitle("请选择一张图片")
                            .setMessage("你可以在相机或者图片库中选择！(单击空白处取消)")
                            .setPositiveButton("拍照", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                    // 调用系统API相机拍照
                                    Intent intent = new Intent();// 创建照相意图对象
                                    intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE); // 设置拍照意图
                                    intent.addCategory(Intent.CATEGORY_DEFAULT);// 添加为默认意图分类
                                    File outPutFile = new File(getExternalCacheDir(),"uploadFile.jpg");

                                    try {
                                        if (outPutFile.exists()){ // 判断文件是否存在
                                            outPutFile.delete(); // 文件存在删除存在的文件
                                        }
                                        outPutFile.createNewFile(); // 创建新文件
                                        // 创建输出文件
                                        Uri outPutFileImageUri;
                                        // 判断Android系统API版本，决定是用什么操作
                                        if (Build.VERSION.SDK_INT >= 24){ // 如果是新版本的安卓系统环境
                                            // 使用 提供者 类型提供的API
                                            outPutFileImageUri = FileProvider.getUriForFile(CreateNewOrderActivity.this,"com.ecsoft.zyymaintain.fileprovider",outPutFile);
                                        } else {
                                            outPutFileImageUri = Uri.fromFile(outPutFile);
                                        }

                                        intent.putExtra(MediaStore.EXTRA_OUTPUT, outPutFileImageUri);
                                        startActivityForResult(intent, INTENT_REQ_CAPTURE_IMAGE);
                                    } catch (IOException e) {
                                        Toast.makeText(CreateNewOrderActivity.this, "文件系统API未接收到系统调度的有效返回，程序结束...", Toast.LENGTH_SHORT).show();
                                        e.printStackTrace();
                                        finish();
                                    }
                                }
                            })
                            .setNegativeButton("相册", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // 功能暂时未实现
                                    Toast.makeText(CreateNewOrderActivity.this, "您的设备不支持，请先使用拍照功能", Toast.LENGTH_SHORT).show();


                                    // 调用系统API相机拍照
                                    Intent intent = new Intent();// 创建照相意图对象
                                    intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE); // 设置拍照意图
                                    intent.addCategory(Intent.CATEGORY_DEFAULT);// 添加为默认意图分类
                                    File outPutFile = new File(getExternalCacheDir(),"uploadFile.jpg");

                                    try {
                                        if (outPutFile.exists()){ // 判断文件是否存在
                                            outPutFile.delete(); // 文件存在删除存在的文件
                                        }
                                        outPutFile.createNewFile(); // 创建新文件
                                        // 创建输出文件
                                        Uri outPutFileImageUri;
                                        // 判断Android系统API版本，决定是用什么操作
                                        if (Build.VERSION.SDK_INT >= 24){ // 如果是新版本的安卓系统环境
                                            // 使用 提供者 类型提供的API
                                            outPutFileImageUri = FileProvider.getUriForFile(CreateNewOrderActivity.this,"com.ecsoft.zyymaintain.fileprovider",outPutFile);
                                        } else {
                                            outPutFileImageUri = Uri.fromFile(outPutFile);
                                        }

                                        intent.putExtra(MediaStore.EXTRA_OUTPUT, outPutFileImageUri);
                                        startActivityForResult(intent, INTENT_REQ_CAPTURE_IMAGE);
                                    } catch (IOException e) {
                                        Toast.makeText(CreateNewOrderActivity.this, "文件系统API未接收到系统调度的有效返回，程序结束...", Toast.LENGTH_SHORT).show();
                                        e.printStackTrace();
                                        finish();
                                    }
                                }
                            })
                            .setCancelable(true)
                            .show();
                }


            }
        });
        etOrderContent.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                int length = etOrderContent.length();// 获取大小
                tvWordLimit.setText("("+length+"/255)字");
                return false;
            }
        });
        etOrderContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @SuppressLint("SetTextI18n")
            @Override
            public void afterTextChanged(Editable editable) {
                String inputMsg = editable.toString().trim();
                int length = etOrderContent.length();// 获取大小
                tvWordLimit.setText("("+length+"/255)字");
            }
        });
        tvClearPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(CreateNewOrderActivity.this)
                        .setMessage("是否清除图片？")
                        .setTitle("提示")
                        .setPositiveButton("清除", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                upload_hasPic = false;
                                ivUploadPic.setImageResource(R.drawable.ic_icon_add_pic);
                                tvClearPic.setVisibility(View.GONE); // 设置显示状态为隐藏
                            }
                        })
                        .setNegativeButton("取消",null)
                        .setCancelable(false)
                        .show();
            }

        });
        btnCommitWorkOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 显示提示框
                new AlertDialog.Builder(CreateNewOrderActivity.this)
                        .setTitle("提示")
                        .setMessage("为了避免不必要的麻烦，工单上传后不可更改请谨慎填写，您确认要继续吗？")
                        .setPositiveButton("继续上传", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                pdSendWorkOrder = new ProgressDialog(CreateNewOrderActivity.this);
                                pdSendWorkOrder.setMessage("正在提交并上传数据中...");
                                pdSendWorkOrder.setCancelable(false);
                                pdSendWorkOrder.show();
                                // 编写提交工单逻辑
                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Map<String,String> params = new HashMap<>();
                                        DbSettingsService settingsService = new DbSettingsService(CreateNewOrderActivity.this);
                                        params.put("userToken", settingsService.getSettings("loginToken"));
                                        params.put("title",etOrderTitle.getText().toString());
                                        params.put("content",etOrderContent.getText().toString());
                                        // 判断是否存在图片
                                        if (upload_hasPic) params.put("imgUrl",upload_picUrl);
                                        // 发送HTTP请求
                                        String responseText = OKHTTPUtil.sendGet(GlobalConfiguration.serverUrl + "/api/record/reportMaintenance", params);
                                        // 将文本转换为Json对象
                                        JSONTokener tokener = new JSONTokener(responseText);
                                        try {
                                            JSONObject responseJSON = (JSONObject) tokener.nextValue();
                                            if (responseJSON.getInt("code") == 200) {
                                                Message msg = new Message();
                                                msg.what = HANDLER_SEND_WORK_ORDER_SUCCESS;
                                                Bundle data = new Bundle();
                                                data.putString("msg",responseJSON.getString("msg"));
                                                msg.setData(data);
                                                handler.sendMessage(msg);
                                            } else {
                                                Message msg = new Message();
                                                msg.what = HANDLER_SEND_WORK_ORDER_FAILED;
                                                Bundle data = new Bundle();
                                                data.putString("msg",responseJSON.getString("msg"));
                                                msg.setData(data);
                                                handler.sendMessage(msg);
                                                // 结束消息

                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                    }
                                });
                                thread.start();
                            }
                        })
                        .setNegativeButton("返回编写",null)
                        .setCancelable(false)
                        .show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 判断请求码
        if (requestCode == INTENT_REQ_CAPTURE_IMAGE){ // 如果请求码是捕获图片，判断为图片捕获的结果
            if (resultCode == Activity.RESULT_OK){ // 如果返回OK代表已经完成拍照
                File outPutFile = new File(getExternalCacheDir(),"uploadFile.jpg");
                if (!outPutFile.exists()){ // 判断文件是否存在
                    Toast.makeText(CreateNewOrderActivity.this, "对不起，应用未获取到图片文件...", Toast.LENGTH_SHORT).show();
                    finish();
                } else { // 文件存在
                    // 存在进行下一步逻辑
                    pdUpdatingPic = new ProgressDialog(CreateNewOrderActivity.this);
                    pdUpdatingPic.setCancelable(false);
                    pdUpdatingPic.setMessage("图片上传中...");
                    pdUpdatingPic.show(); // 显示对话框
                    // 启动上传线程
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String responseText = OKHTTPUtil.upLoadImage(GlobalConfiguration.serverUrl + "/api/file/upload", outPutFile);
                            JSONTokener tokener = new JSONTokener(responseText);
                            try {
                                JSONObject responseJSON = (JSONObject) tokener.nextValue();
                                // 判断响应结果
                                if (responseJSON.getInt("code") == 200) {
                                    // 如果响应码为200，代表请求上传成功
                                    Message msg = new Message();
                                    Bundle bundle = new Bundle();
                                    msg.what = HANDLER_UPLOAD_STATUS_SUCCESS;
                                    bundle.putString("msg",responseJSON.getString("msg")); // 将错误原因添加到消息队列
                                    bundle.putInt("fid",responseJSON.getInt("fid")); // 图片ID
                                    msg.setData(bundle);
                                    handler.sendMessage(msg); // 发送成功码到消息队列
                                } else {
                                    // 否则上传失败
                                    Message msg = new Message();
                                    Bundle bundle = new Bundle();
                                    msg.what = HANDLER_UPLOAD_STATUS_FAILED;
                                    msg.setData(bundle);
                                    handler.sendMessage(msg);
                                }
                            } catch (JSONException e) {
                                // 发生错误退出
                                Message msg = new Message();
                                Bundle bundle = new Bundle();
                                bundle.putString("msg","程序发生无法修补的核心错误，结束中..."); // 将错误原因添加到消息队列
                                msg.what = HANDLER_UPLOAD_STATUS_FAILED;
                                msg.setData(bundle);
                                handler.sendMessage(msg); // 发生错误信息到消息队列
                                e.printStackTrace(); // 打印错误堆栈跟踪
                            }
                        }
                    });
                    thread.start();
                }

            } else {
                // 用户取消拍照或者发生异常
                Toast.makeText(this, "用户取消了本次拍照操作", Toast.LENGTH_SHORT).show();
            }
        }
    }



}