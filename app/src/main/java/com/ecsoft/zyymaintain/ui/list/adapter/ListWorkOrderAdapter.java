package com.ecsoft.zyymaintain.ui.list.adapter;


import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.ecsoft.zyymaintain.R;
import com.ecsoft.zyymaintain.ui.list.adapter.entity.WorkOrderPO;

import java.util.List;

public class ListWorkOrderAdapter extends RecyclerView.Adapter<ListWorkOrderAdapter.ViewHolder> {

    private Context           context;
    private Boolean           hasData;
    private List<WorkOrderPO> listData;



    // 定义监听器内部接口
    public interface OnItemClickListener{
        void onItemClicked(int position,View clickedView,WorkOrderPO entityData);
    }

    // 定义监听器接口变量
    private OnItemClickListener onItemClickListener;
    // 设置监听器接口变量的setter方法
    public void setOnItemClickListener(OnItemClickListener listener){
        this.onItemClickListener = listener;
    }


    public ListWorkOrderAdapter(Context context, List<WorkOrderPO> listData,Boolean hasData) {
        this.context   = context;
        this.listData  = listData;
        this.hasData   = hasData;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (hasData){ // 如果有数据，加载数据列表
            View inflate = LayoutInflater.from(context).inflate(R.layout.item_work_order_list, parent, false);
            return new ViewHolder(inflate);
        } else { // 没有数据，加载提示界面
            View inflate = LayoutInflater.from(context).inflate(R.layout.item_nothing_to_show, parent, false);
            return new ViewHolder(inflate);
        }

    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        // 获取相应角标的数据实体对象
        if (hasData){ // 如果有数据才加载
            // 加载组件数据
            WorkOrderPO item = listData.get(position);
            holder.tvIndex.setText(Integer.toString(position+1));
            holder.tvTitle.setText(item.getTitle());
            switch (item.getCurrentStatus()) {
                case 1:{
                    holder.tvStatus.setText("未处理");
                    holder.tvStatus.setTextColor(context.getResources().getColor(R.color.red));
                    break;
                }
                case 2:{
                    holder.tvStatus.setText("处理中");
                    holder.tvStatus.setTextColor(context.getResources().getColor(R.color.yellow));
                    break;
                }
                case 3:{
                    holder.tvStatus.setText("处理完成");
                    holder.tvStatus.setTextColor(context.getResources().getColor(R.color.green));
                    break;
                }
            }
            // 加载组件事件
            // 注册跟视图的单击事件，当根被单击的时候调用自定义事件
            holder.clWorkOrderListItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // 判断是否用户注册了自定义事件，注册才执行
                    if (onItemClickListener != null){
                        // 程序内部调用事件监听接口的触发器，完成事件的触发
                        onItemClickListener.onItemClicked(position,view, listData.get(position));
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return listData.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        // 注册布局
        public ConstraintLayout clWorkOrderListItem;
        // 注册各个需要控制的组件
        public TextView tvTitle;
        public TextView tvStatus;
        public TextView tvIndex;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            if (hasData){ // 如果有数据才加载
                clWorkOrderListItem = itemView.findViewById(R.id.cl_work_order_list_item);
                tvIndex = itemView.findViewById(R.id.tv_index);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvStatus = itemView.findViewById(R.id.tv_status);
            }
        }
    }

}
