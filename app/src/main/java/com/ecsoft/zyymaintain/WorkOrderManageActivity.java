package com.ecsoft.zyymaintain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import com.ecsoft.zyymaintain.database.DbSettingsService;
import com.ecsoft.zyymaintain.service.NotificationService;
import com.ecsoft.zyymaintain.ui.fragment.UserManageFragment;
import com.ecsoft.zyymaintain.ui.fragment.WorkOrderManageFragment;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class WorkOrderManageActivity extends AppCompatActivity {

    private ViewPager vpContent;
    private TabLayout tlTab;

    private String[] tabItem        = new String[]{"工单","用户"};
    private Fragment[] itemFragment = new Fragment[]{new WorkOrderManageFragment(),new UserManageFragment()};
    private Integer[] itemDrawable  = new Integer[]{R.drawable.ic_tab_item_work_order,R.drawable.ic_tab_item_user_card};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_order_manage);
        initViewComponent();
        initViewComponentEvent();
        // initViewData();
        if (!isServiceRunning(WorkOrderManageActivity.this,"com.ecsoft.zyymaintain.service.NotificationService")){
            Intent intent = new Intent(WorkOrderManageActivity.this, NotificationService.class);
            Bundle bundle = new Bundle();
            DbSettingsService dbSettingsService = new DbSettingsService(WorkOrderManageActivity.this);
            bundle.putString("userName",dbSettingsService.getSettings("tokenUser"));
            bundle.putString("userToken",dbSettingsService.getSettings("loginToken"));
            bundle.putString("userId",dbSettingsService.getSettings("uid"));
            intent.putExtra("data",bundle);
            startService(intent);
        }
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }
    }



    private void initViewComponent() {
        vpContent = findViewById(R.id.vp_content);
        tlTab     = findViewById(R.id.tl_tab);
    }

    private void initViewComponentEvent() {
        vpContent.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {

            @Nullable
            @Override
            public CharSequence getPageTitle(int position) {

                return tabItem[position % tabItem.length];
            }


            @NonNull
            @Override
            public Fragment getItem(int position) {
                return itemFragment[position];
            }

            @Override
            public int getCount() {
                return itemFragment.length;
            }
        });
        tlTab.setupWithViewPager(vpContent);

    }

    /**
     * 用来判断服务是否运行.
     * @param mContext dd
     * @param className 判断的服务名字
     * @return true 在运行 false 不在运行
     */
    public static boolean isServiceRunning(Context mContext, String className) {
        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager)
                mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList
                = activityManager.getRunningServices(30);
        if (!(serviceList.size()>0)) {
            return false;
        }
        for (int i=0; i<serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals(className) == true) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }

    private void initViewData(){
        for (int i = 0; i <= itemDrawable.length-1; i++){ // 遍历数组
            // 解析XML文件到View对象
            View inflatedItemView = View.inflate(WorkOrderManageActivity.this, R.layout.item_tab_view, null);
            // 根据遍历的对象，应用视图组件属性
            ImageView ivItemIcon = inflatedItemView.findViewById(R.id.iv_item_icon); // 获取图标
            TextView  tvItemName = inflatedItemView.findViewById(R.id.tv_item_name); // 获取标题
            ivItemIcon.setImageResource(itemDrawable[i]); // 根据遍历游标，设置图片资源
            tvItemName.setText(tabItem[i]);
            tlTab.addTab(tlTab.newTab().setCustomView(inflatedItemView));
        }
    }
}