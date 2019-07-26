package com.kunyan.ocr;

import com.baidu.aip.ocr.AipOcr;
import com.kunyan.entity.IdentifyException;
import com.kunyan.entity.LocationOcr;
import com.kunyan.service.impl.OciClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;

public class OcrUtil {

    public static void drawFindRect(Graphics graphics, List<LocationOcr> locationOcrs) {
        Color color = graphics.getColor();
        for (LocationOcr locationOcr : locationOcrs) {
            graphics.setColor(Color.GREEN);
            graphics.drawRect(locationOcr.getX(), locationOcr.getY(), locationOcr.getWidth(), locationOcr.getHeight());
            graphics.setColor(Color.RED);
            graphics.drawString(locationOcr.getFindValue(), locationOcr.getX(), locationOcr.getY());
        }
        graphics.setColor(color);
    }

    public static  void drawGeneralRect(Graphics graphics, List<LocationOcr> locationOcrs) {
        for (LocationOcr locationOcr : locationOcrs) {
            graphics.drawRect(locationOcr.getX(), locationOcr.getY(), locationOcr.getWidth(), locationOcr.getHeight());
            graphics.drawString(locationOcr.getValue(), locationOcr.getX(), locationOcr.getY());
        }
    }

    public static  void drawGeneralRectIn(Graphics graphics, List<LocationOcr> locationOcrs) {
        for (LocationOcr locationOcr : locationOcrs) {
            graphics.setColor(Color.GREEN);
            graphics.drawRect(locationOcr.getX(), locationOcr.getY(), locationOcr.getWidth(), locationOcr.getHeight());
            graphics.setColor(Color.RED);
            graphics.drawString(locationOcr.getValue(), locationOcr.getX(), locationOcr.getY() + locationOcr.getHeight() / 2);
        }
    }

    public static  void drawGeneralRectRight(Graphics graphics, List<LocationOcr> locationOcrs) {
        for (LocationOcr locationOcr : locationOcrs) {
            graphics.setColor(Color.GREEN);
            graphics.drawRect(locationOcr.getX(), locationOcr.getY(), locationOcr.getWidth(), locationOcr.getHeight());
            graphics.setColor(Color.RED);
            graphics.drawString(locationOcr.getValue(), locationOcr.getX() + locationOcr.getWidth(), locationOcr.getY() + locationOcr.getHeight() / 2);
        }
    }

    public static void mergeByLine(List<LocationOcr> ocrs) {
        LocationOcr pre = null;
        Iterator<LocationOcr> it = ocrs.iterator();
        while (it.hasNext()) {
            LocationOcr locationOcr = it.next();
            if (pre != null && Math.abs(locationOcr.getY() - pre.getY())< pre.getHeight() / 2 && Math.abs(locationOcr.getX() - pre.getX()) <pre.getWidth()) {
                pre.setValue(pre.getValue() + locationOcr.getValue());
                pre.setWidth((locationOcr.getX() - pre.getX()) + locationOcr.getWidth());
                it.remove();
            } else {
                pre = locationOcr;
            }
        }
    }

    public static JSONObject ocrDetect(byte[] data) {
        AipOcr client = OciClient.getClient();
        HashMap<String, String> map = new HashMap<>();
        JSONObject res = client.accurateGeneral(data, map);
        return res;
    }


    public static  List<LocationOcr> getAllOcrs(byte [] data) throws IdentifyException {
        JSONObject jsonObject = ocrDetect(data);
        if (jsonObject.has("error_msg")) {
            throw new IdentifyException("百度服务出错:" + jsonObject.getString("error_msg"));
        }
        List<LocationOcr> locationOcrs = new ArrayList<>();
        JSONArray wordsResult = jsonObject.getJSONArray("words_result");
        for (int i = 0; i < wordsResult.length(); i++) {
            JSONObject wordResult = wordsResult.getJSONObject(i);
            String name = wordResult.getString("words");
            JSONObject location = wordResult.getJSONObject("location");
            int top = location.getInt("top");
            int width = location.getInt("width");
            int left = location.getInt("left");
            int height = location.getInt("height");
            LocationOcr locationOcr = new LocationOcr(name, left, top, width, height);
            locationOcrs.add(locationOcr);
        }
        Collections.sort(locationOcrs);
        return locationOcrs;
    }

    public static  String getNumberString(String s) {
        int i = 0;
        int start = -1;
        int end = s.length();
        for (i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                start = i;
                break;
            }
        }

        if (start == -1) {
            return null;
        }

        for (; i < s.length(); i++) {
            if (s.charAt(i) < '0' || s.charAt(i) > '9') {
                end = i;
                break;
            }
        }
        return  s.substring(start, end);
    }

    public static String md5(byte [] data) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(data);
            return new BigInteger(1, md.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
           return null;
        }
    }
}
