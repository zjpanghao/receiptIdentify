package com.kunyan.entity;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class IdentifyResult {
    @SerializedName("error_code")
    private int errorCode;
    @SerializedName("results")
    private List<IdentifyItem> identifyItemList;

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public List<IdentifyItem> getIdentifyItemList() {
        return identifyItemList;
    }

    public void setIdentifyItemList(List<IdentifyItem> identifyItemList) {
        this.identifyItemList = identifyItemList;
    }
}
