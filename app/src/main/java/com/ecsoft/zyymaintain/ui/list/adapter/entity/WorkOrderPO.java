package com.ecsoft.zyymaintain.ui.list.adapter.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.Date;

public class WorkOrderPO implements Serializable {
    private int rid;
    private Date startTime;
    private Date endTime;
    private String title;
    private String content;
    private Integer currentStatus;
    private Long createUser;
    private String imgUrl;
    private static final long serialVersionUID=1L;

    public WorkOrderPO() {
    }

    public WorkOrderPO(int rid, Date startTime, Date endTime, String title, String content, Integer currentStatus, Long createUser, String imgUrl) {
        this.rid = rid;
        this.startTime = startTime;
        this.endTime = endTime;
        this.title = title;
        this.content = content;
        this.currentStatus = currentStatus;
        this.createUser = createUser;
        this.imgUrl = imgUrl;
    }

    protected WorkOrderPO(Parcel in) {
        rid = in.readInt();
        title = in.readString();
        content = in.readString();
        if (in.readByte() == 0) {
            currentStatus = null;
        } else {
            currentStatus = in.readInt();
        }
        if (in.readByte() == 0) {
            createUser = null;
        } else {
            createUser = in.readLong();
        }
        imgUrl = in.readString();
    }


    public int getRid() {
        return rid;
    }

    public void setRid(int rid) {
        this.rid = rid;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(Integer currentStatus) {
        this.currentStatus = currentStatus;
    }

    public Long getCreateUser() {
        return createUser;
    }

    public void setCreateUser(Long createUser) {
        this.createUser = createUser;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

//    @Override
//    public int describeContents() {
//        return 0;
//    }
//
//    @Override
//    public void writeToParcel(Parcel parcel, int i) {
////        private Date startTime;
////        private Date endTime;
////        private String title;
////        private String content;
////        private Integer currentStatus;
////        private Long createUser;
////        private String imgUrl;
//        parcel.writeSerializable(startTime);
//        parcel.writeSerializable(endTime);
//        parcel.writeString(title);
//        parcel.writeString(content);
//        parcel.writeInt(currentStatus);
//        parcel.writeLong(createUser);
//        parcel.writeString(imgUrl);
//    }
}
