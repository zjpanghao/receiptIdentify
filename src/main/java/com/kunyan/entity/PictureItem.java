package com.kunyan.entity;

import java.util.ArrayList;
import java.util.List;

public class PictureItem {
    private int errorCode;
    private String errorMsg;
    private String imageBase64;
    private int middleX;
    private List<LocationOcr> leftItems = new ArrayList<>();
    private List<LocationOcr> rightItems = new ArrayList<>();

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public List<LocationOcr> getLeftItems() {
        return leftItems;
    }

    public void setLeftItems(List<LocationOcr> leftItems) {
        this.leftItems = leftItems;
    }

    public List<LocationOcr> getRightItems() {
        return rightItems;
    }

    public void setRightItems(List<LocationOcr> rightItems) {
        this.rightItems = rightItems;
    }

    public int getMiddleX() {
        return middleX;
    }

    public void setMiddleX(int middleX) {
        this.middleX = middleX;
    }
}
