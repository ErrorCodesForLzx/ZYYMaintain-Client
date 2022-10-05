package com.ecsoft.zyymaintain.ui.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.textclassifier.TextClassification;
import android.widget.Button;
import android.widget.TextView;

import com.ecsoft.zyymaintain.MainActivity;
import com.ecsoft.zyymaintain.NetworkConfigurationActivity;
import com.ecsoft.zyymaintain.R;
import com.ecsoft.zyymaintain.database.DbSettingsService;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link UserManageFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserManageFragment extends Fragment {

    private View thisView;

    private TextView tvUserName;
    private Button   btnNetworkConfiguration;
    private Button   btnSafetyExit;

    public UserManageFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment UserManageFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static UserManageFragment newInstance(String param1, String param2) {
        UserManageFragment fragment = new UserManageFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        thisView = inflater.inflate(R.layout.fragment_user_manage, container, false);
        initViewComponent();
        initViewComponentEvent();
        initViewComponentData();
        return thisView;
    }

    private void initViewComponent() {
        tvUserName              = thisView.findViewById(R.id.tv_user_name);
        btnNetworkConfiguration = thisView.findViewById(R.id.btn_network_configuration);
        btnSafetyExit           = thisView.findViewById(R.id.btn_safety_exit);

    }
    private void initViewComponentEvent() {
        btnSafetyExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { // 执行安全退出逻辑
                new AlertDialog.Builder(thisView.getContext())
                        .setTitle("提示")
                        .setMessage("您是否要在本客户端安全退出登录？")
                        .setCancelable(true)
                        .setNegativeButton("否",null)
                        .setPositiveButton("是", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                DbSettingsService dbSettingsService = new DbSettingsService(thisView.getContext());
                                // 将登录信息修改为空
                                dbSettingsService.setSettings("isLogin","false");
                                dbSettingsService.setSettings("loginToken",null);
                                dbSettingsService.setSettings("keepLogin","false");
                                dbSettingsService.setSettings("tokenUser",null);
                                new AlertDialog.Builder(thisView.getContext())
                                        .setTitle("提示")
                                        .setMessage("该用户已经安全从本客户端退出，请重新登录!")
                                        .setCancelable(false)
                                        .setPositiveButton("结束程序", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                // 杀死该程序进程
                                                android.os.Process.killProcess(android.os.Process.myPid());
                                            }
                                        })
                                        .show();
                            }
                        })
                        .show();
            }
        });
        btnNetworkConfiguration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(thisView.getContext(), NetworkConfigurationActivity.class);
                startActivity(intent);
            }
        });
    }
    private void initViewComponentData() {
        DbSettingsService dbSettingsService = new DbSettingsService(thisView.getContext());
        tvUserName.setText(dbSettingsService.getSettings("tokenUser"));
    }


}