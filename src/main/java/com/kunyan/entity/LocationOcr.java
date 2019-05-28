package com.kunyan.entity;

public class LocationOcr implements Comparable<LocationOcr>{
    private String value;
    private String fullValue;
    private String findValue;
    private String findKey;
    private int x;
    private int y;
    private int width;
    private int height;
    public LocationOcr(LocationOcr locationOcr) {
        x = locationOcr.getX();
        y = locationOcr.getY();
        width = locationOcr.getWidth();
        height = locationOcr.getHeight();
        value = locationOcr.getValue();
        findValue = locationOcr.getFullValue();
        fullValue = locationOcr.getFullValue();
        findKey = locationOcr.findKey;
    }

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

    public String getFindValue() {
        return findValue;
    }

    public void setFindValue(String findValue) {
        this.findValue = findValue;
    }

    public String getFindKey() {
        return findKey;
    }

    public void setFindKey(String findKey) {
        this.findKey = findKey;
    }

    @Override
    public int compareTo(LocationOcr o) {
        return Math.abs(x - o.getX()) < 5 ? y - o.getY() : x - o.getX();
    }
}
