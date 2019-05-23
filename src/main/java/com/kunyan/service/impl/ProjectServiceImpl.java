package com.kunyan.service.impl;

import com.baidu.aip.ocr.AipOcr;
import com.google.gson.Gson;
import com.kunyan.entity.IdentifyException;
import com.kunyan.entity.IdentifyResult;
import com.kunyan.entity.LocationOcr;
import com.kunyan.http.HttpUtil;
import com.kunyan.service.ProjectService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProjectServiceImpl implements ProjectService {
    private Log logger = LogFactory.getLog(ProjectServiceImpl.class);

    public LocationOcr ocrRegDetect(JSONObject res, String regEx) {
        JSONArray wordsResult = res.getJSONArray("words_result");
        // String s = res.toString(2);
        Pattern pattern = Pattern.compile(regEx);
        for (int i = 0; i < wordsResult.length(); i++) {
            JSONObject wordResult = wordsResult.getJSONObject(i);
            String word = wordResult.getString("words");
            //JSONObject location = wordResult.getJSONObject("location");
            Matcher matcher = pattern.matcher(word);
            if (matcher.find()) {
                JSONObject location = wordResult.getJSONObject("location");
                int top = location.getInt("top");
                int width = location.getInt("width");
                int left = location.getInt("left");
                int height = location.getInt("height");
                LocationOcr locationOcr = new LocationOcr(matcher.group(), left, top, width, height);
                locationOcr.setFullValue(word);
                return locationOcr;
            }
        }
        return null;
    }

    private JSONObject ocrDetect(byte[] data) {
        // 初始化一个AipOcr
        AipOcr client = OciClient.getClient();
        // 调用接口
        HashMap<String, String> map = new HashMap<>();
        //map.put("vertexes_location", "true");
        map.put("detect_direction", "true");
        JSONObject res = client.accurateGeneral(data, map);
        return res;
    }

    public List<LocationOcr> ocrRegDetectAll(JSONObject res, String regEx) {
        List<LocationOcr> ocrs = new ArrayList<>();
        JSONArray wordsResult = res.getJSONArray("words_result");
        // String s = res.toString(2);
        Pattern pattern = Pattern.compile(regEx);
        for (int i = 0; i < wordsResult.length(); i++) {
            JSONObject wordResult = wordsResult.getJSONObject(i);
            String word = wordResult.getString("words");
            //JSONObject location = wordResult.getJSONObject("location");
            Matcher matcher = pattern.matcher(word);
            if (matcher.find()) {
                JSONObject location = wordResult.getJSONObject("location");
                int top = location.getInt("top");
                int width = location.getInt("width");
                int left = location.getInt("left");
                int height = location.getInt("height");
                LocationOcr locationOcr = new LocationOcr(matcher.group(), left, top, width, height);
                ocrs.add(locationOcr);
            }
        }
        return ocrs;
    }

    @Override
    public BufferedImage checkImage(BufferedImage image) throws IOException, IdentifyException {
        if (image == null) {
            throw new NullPointerException();
        }
        BufferedImage bufferedImage = image;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(image.getHeight() * image.getWidth() * 3);
        ImageIO.write(image, "jpg", byteArrayOutputStream);
        logger.info("开始调用百度接口查找分隔符-----------");
        JSONObject jsonObject = ocrDetect(byteArrayOutputStream.toByteArray());
        logger.info("结束调用百度接口查找分隔符-----------");
        if (jsonObject.has("error_msg")) {
            throw new IdentifyException("百度服务出错:" + jsonObject.getString("error_msg"));
        }

        // split
        String [] spilt = {"LT#", "INT", "QTY"};
        LocationOcr code1 = null;
        for (String name : spilt) {
            code1 = ocrRegDetect(jsonObject, name);
            if (code1 != null) {
                break;
            }
        }
        if (code1 == null) {
            throw new IdentifyException("找不到分隔符, 请检查图片，或更精确抓取");
        }
        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.setColor(Color.RED);
        graphics.setFont(new Font("", Font.BOLD, 35));
        graphics.setStroke(new BasicStroke(8.0F));
        int middlex = code1.getX() - 50;
        if (middlex <= 0) {
            throw new IdentifyException("找不到图片左半部分");
        }
        graphics.drawLine(middlex, 0, middlex, bufferedImage.getHeight());

        //left
        BufferedImage bufferedImageleft = bufferedImage.getSubimage(0, 0, middlex, bufferedImage.getHeight());
        byteArrayOutputStream.reset();
        ImageIO.write(bufferedImageleft, "jpg", byteArrayOutputStream);
        logger.info("开始调用百度接口查找左边-----------");
        jsonObject = ocrDetect(byteArrayOutputStream.toByteArray());
        logger.info("结束调用百度接口查找左边-----------");
        LocationOcr seller = ocrRegDetect(jsonObject, "SELLER PART");
        if (seller == null) {
            throw new IdentifyException("找不到seller part");
        }
        graphics.drawRect(seller.getX(), seller.getY(), seller.getWidth(), seller.getHeight());
        int i = 0;
        for (i = 0; i < seller.getFullValue().length(); i++) {
            char c = seller.getFullValue().charAt(i);
            if (c >= '0' && c <= '9') {
                break;
            }
        }

        String sellerNumberLeft = "";
        if (i != seller.getFullValue().length()) {
            sellerNumberLeft = seller.getFullValue().substring(i).trim();
        }
        graphics.drawString(sellerNumberLeft, seller.getX(), seller.getY());
        LocationOcr quant = ocrRegDetect(jsonObject, "QUANT");
        if (quant == null) {
            throw new IdentifyException("找不到quant");
        }
        graphics.drawRect(quant.getX(), quant.getY(), quant.getWidth(), quant.getHeight());
        i = 0;
        for (i = 0; i < quant.getFullValue().length(); i++) {
            char c = quant.getFullValue().charAt(i);
            if (c >= '0' && c <= '9') {
                break;
            }
        }
        String quantNumberLeft = "";
        if (i != quant.getFullValue().length()) {
            quantNumberLeft = quant.getFullValue().substring(i).trim();
        }
        graphics.drawString(quantNumberLeft, quant.getX(), quant.getY());

        // right
        BufferedImage bufferedImageright = bufferedImage.getSubimage(middlex, 0, bufferedImage.getWidth() - middlex, bufferedImage.getHeight());
        byteArrayOutputStream.reset();
        ImageIO.write(bufferedImageright, "jpg", byteArrayOutputStream);
        logger.info("开始调用百度接口查找右边-----------");
        jsonObject = ocrDetect(byteArrayOutputStream.toByteArray());
        logger.info("结束调用百度接口查找右边-----------");
        // draw seller number
        List<LocationOcr> sellerRights = ocrRegDetectAll(jsonObject, sellerNumberLeft);
        LocationOcr leftest = null;
        if (sellerRights.size() > 0) {
            for (LocationOcr locationOcr : sellerRights) {
                if (leftest == null || locationOcr.getX() < leftest.getX()) {
                    leftest = locationOcr;
                }
            }
            graphics.drawRect(leftest.getX() + middlex, leftest.getY(), leftest.getWidth(), leftest.getHeight());
        }
        // draw quant number
        List<LocationOcr> quantRights = ocrRegDetectAll(jsonObject, quantNumberLeft);
        leftest = null;
        if (quantRights.size() > 0) {
            for (LocationOcr locationOcr : quantRights) {
                if (leftest == null || locationOcr.getX() < leftest.getX()) {
                    leftest = locationOcr;
                }
            }
            graphics.drawRect(leftest.getX() + middlex, leftest.getY(), leftest.getWidth(), leftest.getHeight());
        }
        if (sellerRights.size() > 0) {
            graphics.drawString("Seller No  equal:" + sellerNumberLeft, middlex, 100);
        }
        if (quantRights.size() > 0) {
            graphics.drawString("Quant equal :" + quantNumberLeft, middlex, 200);
        }
        return bufferedImage;
    }
}
