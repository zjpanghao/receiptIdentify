package com.kunyan.entity;

import java.util.ArrayList;
import java.util.List;

public class PictureUpload {
    //private int projectId;
    private String imageBase64;

    public class ContainCheckItem {
        String value;
        int len;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public int getLen() {
            return len;
        }

        public void setLen(int len) {
            this.len = len;
        }
    }

    private List<ContainCheckItem> contains = new ArrayList<>();

    private List<String> excludes = new ArrayList<>();

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public List<ContainCheckItem> getContains() {
        return contains;
    }

    public void setContains(List<String> contains) {
        contains = new ArrayList<>();
    }

    public void setLength(int inx, int len) {
        contains.get(inx).setLen(len);
    }

    public List<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }
}
