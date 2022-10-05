package com.ecsoft.zyymaintain;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.ecsoft.zyymaintain.config.GlobalConfiguration;
import com.ecsoft.zyymaintain.database.DbSettingsService;
import com.ecsoft.zyymaintain.network.util.OKHTTPUtil;
import com.ecsoft.zyymaintain.ui.list.adapter.entity.OrderStatusPO;
import com.ecsoft.zyymaintain.ui.list.adapter.entity.WorkOrderPO;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkOrderDetailActivity extends AppCompatActivity {

    // 定义组件
    private TextView  tvTitle;
    private TextView  tvCreateUser;
    private TextView  tvCreateTime;
    private TextView  tvEndTime;
    private TextView  tvOrderStatus;
    private TextView  tvContent;
    private ImageView ivOrderImg;
    private Spinner   spStatusModify;
    private Button    btnExamine;
    private LinearLayout llBack;

    private List<OrderStatusPO> orderStatuses;
    private String[]            orderStatusesNameArray;
    private Integer             orderStatusNowSelected;

    private ProgressDialog pdExamineOrderStatusRequest;

    // 定义Handler what常量
    private static final int HANDLER_GET_USERNAME_SUCCESS      = 0x10001;
    private static final int HANDLER_GET_USERNAME_FAILED       = 0x10002;
    private static final int HANDLER_GET_STATUS_SUCCESS        = 0x20001;
    private static final int HANDLER_GET_STATUS_FAILED         = 0x20002;
    private static final int HANDLER_GET_ORDER_STATUS_SUCCESS  = 0x30001;
    private static final int HANDLER_GET_ORDER_STATUS_FAILED   = 0x30002;
    private static final int HANDLER_GET_EXAMINE_SUCCESS       = 0x40001;
    private static final int HANDLER_GET_EXAMINE_FAILED        = 0x40002;

    // 定义内部使用的对象
    private WorkOrderPO workOrderPO; // 当前需要详细显示的工单对象


    // 创建Handler消息队列对象
    private Handler handler = new Handler(new Handler.Callback() {
        @SuppressLint("SetTextI18n")
        @Override
        public boolean handleMessage(@NonNull Message message) {

            switch (message.what){ // 判断what
                case HANDLER_GET_USERNAME_SUCCESS:{ // 用户名获取成功
                    Bundle data = message.getData();
                    tvCreateUser.setText("创建用户：" +data.getString("userName"));
                    break;
                }
                case HANDLER_GET_USERNAME_FAILED:{
                    tvCreateUser.setText("创建用户：未知用户");
                    break;
                }
                case HANDLER_GET_STATUS_SUCCESS:{
                    Bundle data = message.getData();
                    tvOrderStatus.setText(data.getString("statusName"));
                    break;
                }
                case HANDLER_GET_STATUS_FAILED:{
                    tvOrderStatus.setText("未知状态");
                    break;
                }
                case HANDLER_GET_ORDER_STATUS_SUCCESS:{
                    // 创建适配器对象
                    ArrayAdapter arrayAdapter = new ArrayAdapter(WorkOrderDetailActivity.this, android.R.layout.simple_spinner_dropdown_item,orderStatusesNameArray);
                    spStatusModify.setAdapter(arrayAdapter);
                    break;
                }
                case HANDLER_GET_ORDER_STATUS_FAILED:{
                    Toast.makeText(WorkOrderDetailActivity.this, "工单状态请求错误", Toast.LENGTH_SHORT).show();
                }
                case HANDLER_GET_EXAMINE_SUCCESS:{ // 审批修改正确
                    pdExamineOrderStatusRequest.dismiss(); // 关闭请求加载框
                    Bundle data = message.getData();
                    Toast.makeText(WorkOrderDetailActivity.this,"完成状态审批，已通知所有上线用户", Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                }
                case HANDLER_GET_EXAMINE_FAILED:{ // 审批修改错误
                    pdExamineOrderStatusRequest.dismiss(); // 关闭请求加载框
                    Bundle data = message.getData();
                    new AlertDialog.Builder(WorkOrderDetailActivity.this)
                            .setTitle("发生错误")
                            .setMessage(data.getString("msg"))
                            .setCancelable(false)
                            .setPositiveButton("我知道了",null)
                            .show();
                }
            }
            return false;
        }
    });
// 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_order_detail);

        initViewComponent();
        initViewComponentData();
        initViewComponentEvent();
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }
    }
//
    private void initViewComponentEvent() {
        llBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish(); // 关闭当前界面
            }
        });
        spStatusModify.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                OrderStatusPO item = orderStatuses.get(i); // 获取当前单击的实体对象
                orderStatusNowSelected = item.getSid();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        btnExamine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new AlertDialog.Builder(WorkOrderDetailActivity.this)
                        .setTitle("提示")
                        .setMessage("您确定要修改当前状态吗？")
                        .setPositiveButton("确认修改", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                pdExamineOrderStatusRequest = new ProgressDialog(WorkOrderDetailActivity.this);
                                pdExamineOrderStatusRequest.setMessage("审批状态正在提交到服务器...");
                                pdExamineOrderStatusRequest.setCancelable(false);
                                pdExamineOrderStatusRequest.show();
                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        DbSettingsService dbSettingsService = new DbSettingsService(WorkOrderDetailActivity.this);
                                        Map<String,String> params = new HashMap<>();
                                        params.put("rid",String.valueOf(workOrderPO.getRid()));
                                        params.put("sid",String.valueOf(orderStatusNowSelected));
                                        params.put("userToken",dbSettingsService.getSettings("loginToken"));
                                        String responseText = OKHTTPUtil.sendGet(GlobalConfiguration.serverUrl + "/api/record/examineMaintenance", params);
                                        JSONTokener tokener = new JSONTokener(responseText);
                                        try {
                                            JSONObject responseJSON = (JSONObject) tokener.nextValue();
                                            if (responseJSON.getInt("code") == 200) { // 状态审批成功
                                                Message message = new Message();
                                                message.what = HANDLER_GET_EXAMINE_SUCCESS;
                                                handler.sendMessage(message); // 发送成功消息到消息队列
                                            } else if (responseJSON.getInt("code") == 205){ // 用户权限不足
                                                Message message = new Message();
                                                message.what = HANDLER_GET_EXAMINE_FAILED;
                                                Bundle bundle = new Bundle();
                                                bundle.putString("msg",responseJSON.getString("msg"));
                                                message.setData(bundle);
                                                handler.sendMessage(message); // 发送错误消息到消息队列
                                            } else if (responseJSON.getInt("code") == 405){ // 失效的用户token
                                                Message message = new Message();
                                                message.what = HANDLER_GET_EXAMINE_FAILED;
                                                Bundle bundle = new Bundle();
                                                bundle.putString("msg",responseJSON.getString("msg"));
                                                message.setData(bundle);
                                                handler.sendMessage(message); // 发送错误消息到消息队列
                                            } else {
                                                Message message = new Message();
                                                message.what = HANDLER_GET_EXAMINE_FAILED;
                                                Bundle bundle = new Bundle();
                                                bundle.putString("msg","状态审批失败，服务器内部异常");
                                                message.setData(bundle);
                                                handler.sendMessage(message); // 发送错误消息到消息队列
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                                thread.start();
                            }
                        })
                        .setNegativeButton("取消",null)
                        .setCancelable(false)
                        .show();


            }
        });
    }


    private void initViewComponent() {
        tvTitle        = findViewById(R.id.tv_work_order_title);
        tvCreateUser   = findViewById(R.id.tv_work_order_create_user);
        tvCreateTime   = findViewById(R.id.tv_work_order_create_time);
        tvEndTime      = findViewById(R.id.tv_work_order_end_time);
        tvOrderStatus  = findViewById(R.id.tv_work_order_status);
        tvContent      = findViewById(R.id.tv_content);
        ivOrderImg     = findViewById(R.id.iv_order_img);
        spStatusModify = findViewById(R.id.sp_status_modify);
        btnExamine     = findViewById(R.id.btn_examine);
        llBack         = findViewById(R.id.ll_back);
    }
    @SuppressLint("SetTextI18n")
    private void initViewComponentData() {
        Intent intent = getIntent(); // 获取传递的意图对象
        Bundle data   = intent.getBundleExtra("data"); // 获取绑定对象
        this.workOrderPO = (WorkOrderPO) data.getSerializable("entityData");
        // 将数据应用到组件上
        tvTitle       .setText(workOrderPO.getTitle());
        tvCreateUser  .setText("正在加载用户名...");
        Date startTime = workOrderPO.getStartTime(); // 获取到Date时间对象
        Calendar cal = Calendar.getInstance(); // 创建日历对象
        cal.setTime(startTime); // 设置日历时间
        tvCreateTime  .setText("创建时间：" + cal.get(Calendar.YEAR)+"年"+cal.get(Calendar.MONTH)+"月"+cal.get(Calendar.DAY_OF_MONTH)+"日 "+cal.get(Calendar.HOUR_OF_DAY)+":"+cal.get(Calendar.SECOND));
        if (workOrderPO.getEndTime() != null){ // 如果结束时间为空，代表还没有完成
            Date endTime = workOrderPO.getEndTime();
            cal.setTime(endTime); // 重新设置日历时间
            tvEndTime.setText("完成时间：" +  cal.get(Calendar.YEAR)+"年"+cal.get(Calendar.MONTH)+"月"+cal.get(Calendar.DAY_OF_MONTH)+"日 "+cal.get(Calendar.HOUR_OF_DAY)+":"+cal.get(Calendar.SECOND));
        } else {
            tvEndTime.setText("完成时间：未完成");
        }
        tvOrderStatus .setText("未知状态");
        tvContent     .setText(workOrderPO.getContent());
        if (workOrderPO.getImgUrl() != null){ // 判断是否有图片
            // 加载图片
            Glide.with(WorkOrderDetailActivity.this)
                    .load(workOrderPO.getImgUrl())
                    .into(ivOrderImg);
        }
        // 开启一个线程进行获取状态集合
        Thread threadGetStatus = new Thread(new Runnable() {
            @Override
            public void run() {
                String responseText = OKHTTPUtil.sendGet(GlobalConfiguration.serverUrl + "/api/status/getAllStatus", new HashMap<>());
                JSONTokener tokener = new JSONTokener(responseText);
                try {
                    JSONObject jsonObject = (JSONObject) tokener.nextValue();
                    if (jsonObject.getInt("code") == 200) {
                        JSONArray status = jsonObject.getJSONArray("status"); // 获取状态集合
                        orderStatuses = new ArrayList<>();
                        orderStatusesNameArray = new String[status.length()]; // 创建状态名字数组，用于适配器
                        for (int i=0;i <= status.length()-1;i++){ // 遍历每一个集合
                            JSONObject statusItem = status.getJSONObject(i); // 获取每一项
                            OrderStatusPO itemPO = new OrderStatusPO(); // 创建实体类型
                            itemPO.setSid(statusItem.getInt("sid"));
                            itemPO.setSname(statusItem.getString("sname"));
                            orderStatuses.add(itemPO); // 添加
                            orderStatusesNameArray[i] = itemPO.getSname(); // 添加名称
                            if (i == 0){
                                orderStatusNowSelected = itemPO.getSid(); // 默认使用第一个的值
                            }
                        }
                        // 发送消息队列
                        handler.sendEmptyMessage(HANDLER_GET_ORDER_STATUS_SUCCESS);
                    } else {
                        // 发送错误消息
                        handler.sendEmptyMessage(HANDLER_GET_ORDER_STATUS_FAILED);
                    }
                } catch (JSONException e) {
                    handler.sendEmptyMessage(HANDLER_GET_ORDER_STATUS_FAILED);
                    e.printStackTrace();
                }


            }
        });
        threadGetStatus.start();

        // 分别开启两个线程进行获取用户名和状态
        Thread threadGetUserName = new Thread(new Runnable() {
            @Override
            public void run() {
                HashMap<String,String> params =  new HashMap<>();
                params.put("uid",workOrderPO.getCreateUser().toString());
                String responseText = OKHTTPUtil.sendGet(GlobalConfiguration.serverUrl + "/api/user/getUserName", params);
                JSONTokener tokener = new JSONTokener(responseText);
                try {
                    JSONObject responseJson = (JSONObject) tokener.nextValue();
                    Bundle data = new Bundle();
                    if (responseJson.getInt("code") == 200) {
                        data.putString("userName",responseJson.getString("userName"));
                    } else {
                        data.putString("userName","未知用户名");
                    }
                    Message msg = new Message();// 创建消息对象
                    msg.what = HANDLER_GET_USERNAME_SUCCESS; // 设置消息类型
                    msg.setData(data); // 传递数据
                    handler.sendMessage(msg);
                } catch (JSONException e) {
                    // 发生异常发送错误到消息处理队列
                    handler.sendEmptyMessage(HANDLER_GET_USERNAME_FAILED);
                    e.printStackTrace();
                }


            }
        });
        Thread threadGetStatusName = new Thread(new Runnable() {
            @Override
            public void run() {
                HashMap<String,String> params =  new HashMap<>();
                params.put("sid",workOrderPO.getCurrentStatus().toString());
                String responseText = OKHTTPUtil.sendGet(GlobalConfiguration.serverUrl + "/api/status/getStatus", params);
                JSONTokener tokener = new JSONTokener(responseText);
                try {
                    JSONObject responseJson = (JSONObject) tokener.nextValue();
                    Bundle data = new Bundle();
                    if (responseJson.getInt("code") == 200) {
                        data.putString("statusName",responseJson.getString("statusName"));
                    } else {
                        data.putString("statusName","未知状态");
                        data.putString("MSG","CTY");
                    }
                    Message msg = new Message();// 创建消息对象
                    msg.what = HANDLER_GET_STATUS_SUCCESS; // 设置消息类型
                    msg.setData(data); // 传递数据
                    handler.sendMessage(msg);
                } catch (JSONException e) {
                    // 发生异常发送错误到消息处理队列
                    handler.sendEmptyMessage(HANDLER_GET_STATUS_FAILED);
                    e.printStackTrace();
                }

            }
        });
        // 开启线程
        threadGetUserName  .start();
        threadGetStatusName.start();
    }

    /**
     *
     * @param mContext
     * @param serviceName
     * 是包名+服务的类名（例如：com.example.testbackstage.TestService）
     * @return true代表正在运行，false代表服务没有正在运行
     */

}