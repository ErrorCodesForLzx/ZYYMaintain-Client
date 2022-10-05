package com.ecsoft.zyymaintain.ui.fragment;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.ecsoft.zyymaintain.CreateNewOrderActivity;
import com.ecsoft.zyymaintain.OrderQueryActivity;
import com.ecsoft.zyymaintain.R;
import com.ecsoft.zyymaintain.WorkOrderDetailActivity;
import com.ecsoft.zyymaintain.config.GlobalConfiguration;
import com.ecsoft.zyymaintain.network.util.OKHTTPUtil;
import com.ecsoft.zyymaintain.ui.list.adapter.ListWorkOrderAdapter;
import com.ecsoft.zyymaintain.ui.list.adapter.entity.OrderStatusPO;
import com.ecsoft.zyymaintain.ui.list.adapter.entity.WorkOrderPO;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WorkOrderManageFragment#} factory method to
 * create an instance of this fragment.
 */
public class WorkOrderManageFragment extends Fragment {

    private View thisView;

    private LinearLayout llQueryOrder;      // 创建工单查询组件
    private Button       btnCreateNewOrder; // 创建新建工单按钮组件
    private Spinner      spQueryStatus;     // 创建状态查询控件
    private RecyclerView rvWorkOrderList;

    private List<OrderStatusPO> orderStatuses;
    private String[]            orderStatusesNameArray;

    private static final int ACTIVITY_REQUEST_COMMIT_WORK_ORDER    = 0x10001; // 提交工单Activity请求码
    private static final int ACTIVITY_REQUEST_ORDER_DETAIL         = 0x10002; // 工单详情状态Activity请求码
    private static final int HANDLER_GET_ORDER_STATUS_SUCCESS      = 0x10001;
    private static final int HANDLER_GET_ORDER_STATUS_FAILED       = 0x10002;

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
                        listWorkOrderAdapter = new ListWorkOrderAdapter(thisView.getContext(),datas,false);
                    } else { // 有数据加载数据
                        listWorkOrderAdapter = new ListWorkOrderAdapter(thisView.getContext(),datas,true);

                    }
                    // 为创建的适配器注册项目单击事件
                    listWorkOrderAdapter.setOnItemClickListener(new ListWorkOrderAdapter.OnItemClickListener() {
                        @Override
                        public void onItemClicked(int position, View clickedView, WorkOrderPO entityData) {
                            // 实现工单详情跳转业务
                            Intent intent = new Intent(thisView.getContext(), WorkOrderDetailActivity.class);
                            Bundle data = new Bundle();
                            data.putSerializable("entityData",entityData);// 将实体数据对象传递给详细工单界面
                            intent.putExtra("data",data);
                            startActivityForResult(intent,ACTIVITY_REQUEST_ORDER_DETAIL); // 启动Activity
                        }
                    });

                    // 将创建好的适配器应用到列表
                    rvWorkOrderList.setAdapter(listWorkOrderAdapter);
                    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(thisView.getContext());
                    linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL); // 设置垂直流式布局管理器
                    rvWorkOrderList.setLayoutManager(linearLayoutManager);
                    break;
                }
                case HANDLER_GET_ORDER_STATUS_SUCCESS:{
                    // 创建数组适配器
                    ArrayAdapter adapter = new ArrayAdapter(thisView.getContext(), android.R.layout.simple_spinner_dropdown_item,orderStatusesNameArray);
                    spQueryStatus.setAdapter(adapter);
                    break;
                }
                case HANDLER_GET_ORDER_STATUS_FAILED:{
                    Toast.makeText(thisView.getContext(), "状态分类获取失败", Toast.LENGTH_SHORT).show();
                    break;
                }
            }
            return false;
        }
    });





    public WorkOrderManageFragment() {
        // Required empty public constructor
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initViewData();
    }

    private void initViewData() {
        // 发送 HTTP 请求，获得数据
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String responseText = OKHTTPUtil.sendGet(GlobalConfiguration.serverUrl + "/api/record/showAllMaintenance", new HashMap<>());
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
        Thread statusGetThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String responseText = OKHTTPUtil.sendGet(GlobalConfiguration.serverUrl + "/api/status/getAllStatus", new HashMap<>());
                JSONTokener tokener = new JSONTokener(responseText);
                try {
                    JSONObject jsonObject = (JSONObject) tokener.nextValue();
                    if (jsonObject.getInt("code") == 200) {
                        JSONArray status = jsonObject.getJSONArray("status"); // 获取状态集合
                        orderStatuses = new ArrayList<>();
                        orderStatusesNameArray = new String[status.length()+1]; // 创建状态名字数组，用于适配器
                        orderStatusesNameArray[0] = "全部筛选";
                        for (int i=0;i <= status.length()-1;i++){ // 遍历每一个集合
                            JSONObject statusItem = status.getJSONObject(i); // 获取每一项
                            OrderStatusPO itemPO = new OrderStatusPO(); // 创建实体类型
                            itemPO.setSid(statusItem.getInt("sid"));
                            itemPO.setSname(statusItem.getString("sname"));
                            orderStatuses.add(itemPO); // 添加
                            orderStatusesNameArray[i+1] = itemPO.getSname(); // 添加名称
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
        statusGetThread.start();

    }

    private void initViewComponent() {
        rvWorkOrderList   = thisView.findViewById(R.id.rv_work_order_list);
        llQueryOrder      = thisView.findViewById(R.id.ll_query_order);
        btnCreateNewOrder = thisView.findViewById(R.id.btn_create_new_order);
        spQueryStatus     = thisView.findViewById(R.id.sp_query_status);
    }

    private void initViewComponentEvent() {
        llQueryOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 跳转到工单搜索界面
                Intent intent = new Intent(thisView.getContext(), OrderQueryActivity.class);
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(getActivity(),(View) llQueryOrder,"search_share").toBundle());
            }
        });
        btnCreateNewOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { // 跳转到工单创建界面
                Intent intent = new Intent(thisView.getContext(), CreateNewOrderActivity.class);
                // startActivity(intent);
                startActivityForResult(intent,ACTIVITY_REQUEST_COMMIT_WORK_ORDER);
            }
        });
        spQueryStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0) { // 0代表为全局筛选
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String responseText = OKHTTPUtil.sendGet(GlobalConfiguration.serverUrl + "/api/record/showAllMaintenance", new HashMap<>());
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
                } else {
                    OrderStatusPO orderStatusPO = orderStatuses.get(i-1);
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            HashMap<String, String> params = new HashMap<>();// 创建参数集合
                            params.put("sid",orderStatusPO.getSid().toString());
                            String responseText = OKHTTPUtil.sendGet(GlobalConfiguration.serverUrl + "/api/record/queryMaintenanceByID",params);
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

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_work_order_manage, container, false);
        thisView = view;
        initViewComponent();
        initViewComponentEvent();
        return view;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case ACTIVITY_REQUEST_COMMIT_WORK_ORDER:{ // 如果是添加工单Activity返回则刷新数据
                // 重新加载数据
                initViewData();
                break;
            }
            case ACTIVITY_REQUEST_ORDER_DETAIL:{
                initViewData();
                break;
            }

        }
    }
}