package com.ecsoft.zyymaintain;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.transition.Explode;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.ecsoft.zyymaintain.config.GlobalConfiguration;
import com.ecsoft.zyymaintain.network.util.OKHTTPUtil;
import com.ecsoft.zyymaintain.ui.list.adapter.ListWorkOrderAdapter;
import com.ecsoft.zyymaintain.ui.list.adapter.entity.WorkOrderPO;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OrderQueryActivity extends AppCompatActivity {

    private LinearLayout llBack;          // 回退组件
    private EditText     etKeyWord;       // 关键字输入框组件
    private RecyclerView rvWorkOrderList; // 创建工单列表
    private ImageView    ivQueryOrder;    // 创建查询工单图片按钮

    private static final int ACTIVITY_REQUEST_ORDER_DETAIL = 0x10002; // 工单详情状态Activity请求码

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {

            switch (message.what){
                case 0x0001:{
                    Bundle bundle = message.getData();
                    List<WorkOrderPO> datas   = (List<WorkOrderPO>) bundle.getSerializable("datas");
                    Boolean           hasData = bundle.getBoolean("hasData");

                    ListWorkOrderAdapter listWorkOrderAdapter;
                    // 创建适配器对象
                    if (!hasData){ // 判断是否有数据
                        // 没有数据显示没有数据的界面
                        datas.add(new WorkOrderPO());
                        listWorkOrderAdapter = new ListWorkOrderAdapter(OrderQueryActivity.this,datas,false);
                    } else { // 有数据加载数据
                        listWorkOrderAdapter = new ListWorkOrderAdapter(OrderQueryActivity.this,datas,true);

                    }
                    // 为创建的适配器注册项目单击事件
                    listWorkOrderAdapter.setOnItemClickListener(new ListWorkOrderAdapter.OnItemClickListener() {
                        @Override
                        public void onItemClicked(int position, View clickedView, WorkOrderPO entityData) {
                            // 实现工单详情跳转业务
                            Intent intent = new Intent(OrderQueryActivity.this, WorkOrderDetailActivity.class);
                            Bundle data = new Bundle();
                            data.putSerializable("entityData",entityData);// 将实体数据对象传递给详细工单界面
                            intent.putExtra("data",data);
                            startActivityForResult(intent,ACTIVITY_REQUEST_ORDER_DETAIL); // 启动Activity

                        }
                    });

                    // 将创建好的适配器应用到列表
                    rvWorkOrderList.setAdapter(listWorkOrderAdapter);
                    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(OrderQueryActivity.this);
                    linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL); // 设置垂直流式布局管理器
                    rvWorkOrderList.setLayoutManager(linearLayoutManager);
                    break;
                }
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_query);
        // 设置过渡
        getWindow().setEnterTransition( new Explode(  ) );
        getWindow().setExitTransition( new Explode(  ) );

        setViewComponent();
        setViewComponentEvent();

        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }
    }

    private void setViewComponent() {
        llBack          = findViewById(R.id.ll_back);
        etKeyWord       = findViewById(R.id.et_key_word);
        rvWorkOrderList = findViewById(R.id.rv_work_order_list);
        ivQueryOrder    = findViewById(R.id.iv_query_order);
    }

    private void setViewComponentEvent() {
        llBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent intent = new Intent(OrderQueryActivity.this,WorkOrderManageActivity.class);
//                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(OrderQueryActivity.this,etKeyWord,"search_share").toBundle());
                finish(); // 结束
            }
        });
        ivQueryOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadViewData(); // 加载数据
            }
        });
        etKeyWord.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (i == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_UP){ // 当用户按下回车（Search）
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS); // 关闭软键盘

                    loadViewData();// 加载数据

                }
                return false;
            }
        });
    }

    private void loadViewData() {
        // 发送 HTTP 请求，获得数据
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                HashMap<String, String> params = new HashMap<>();
                params.put("keyWord",etKeyWord.getText().toString());
                String responseText = OKHTTPUtil.sendGet(GlobalConfiguration.serverUrl + "/api/record/queryMaintenance",params);
                // 解析获取到的数据到JSON
                JSONTokener tokener = new JSONTokener(responseText);
                try {
                    JSONObject responseJson = (JSONObject) tokener.nextValue();
                    // 创建对象集合
                    ArrayList<WorkOrderPO> datas = new ArrayList<>();
                    JSONArray records = responseJson.getJSONArray("records"); // 获取JSON Array对象
                    for (int i = 0;i<=records.length()-1;i++){ // 遍历 json array对象
                        // datas.set(i, (WorkOrderPO) records.get(i));
                        WorkOrderPO itemEntity = new WorkOrderPO(); // 定义每一项的数据实体对象
                        JSONObject itemJsonObj = records.getJSONObject(i);
                        itemEntity.setRid(itemJsonObj.getInt("rid"));
                        // "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
                        itemEntity.setStartTime(dateFormat.parse(itemJsonObj.getString("startTime")));
                        if (!(itemJsonObj.getString("endTime").equals("null"))){
                            itemEntity.setEndTime(dateFormat.parse(itemJsonObj.getString("endTime")));
                        }
                        itemEntity.setTitle(itemJsonObj.getString("title"));

                        itemEntity.setContent(itemJsonObj.getString("content"));
                        itemEntity.setCurrentStatus(itemJsonObj.getInt("currentStatus"));
                        itemEntity.setCreateUser(itemJsonObj.getLong("createUser"));
                        itemEntity.setImgUrl(itemJsonObj.getString("imgUrl"));
                        datas.add(itemEntity); // 将创建好的每一项添加到数据集合内
                    }

                    // 请求以及数据处理成功，发送消息到队列
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("datas",datas);
                    bundle.putBoolean("hasData",responseJson.getInt("number")>0);
                    Message msg = new Message();
                    msg.setData(bundle);
                    msg.what = 0x0001;
                    handler.sendMessage(msg);
                } catch (JSONException | ParseException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();

    }


}