package com.kunyan.entity;

public class LocationOcr {
    private String value;
    private String fullValue;
    private int x;
    private int y;
    private int width;
    private int height;

    public LocationOcr(String value, int x, int y, int width, int height) {
        this.value = value;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getFullValue() {
        return fullValue;
    }

    public void setFullValue(String fullValue) {
        this.fullValue = fullValue;
    }
}
