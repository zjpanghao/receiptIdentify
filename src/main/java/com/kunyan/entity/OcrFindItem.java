package com.kunyan.entity;

public class OcrFindItem {
    private String key;
    private String findKey;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getFindKey() {
        return findKey;
    }

    public void setFindKey(String findKey) {
        this.findKey = findKey;
    }

    public OcrFindItem(String key, String findKey) {
        this.key = key;
        this.findKey = findKey;
    }
}
