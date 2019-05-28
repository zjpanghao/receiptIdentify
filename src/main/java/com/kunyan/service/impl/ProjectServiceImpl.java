package com.kunyan.service.impl;

import com.baidu.aip.ocr.AipOcr;
import com.google.gson.Gson;
import com.kunyan.config.DlConfig;
import com.kunyan.entity.IdentifyException;
import com.kunyan.entity.IdentifyResult;
import com.kunyan.entity.LocationOcr;
import com.kunyan.http.HttpUtil;
import com.kunyan.service.ProjectService;
import com.kunyan.service.TeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProjectServiceImpl implements ProjectService {
    private Log logger = LogFactory.getLog(ProjectServiceImpl.class);
    @Autowired
    private TeService teService;
    @Autowired
    private DlConfig dlConfig;

    private volatile BufferedImage rightImage = null;
    private volatile BufferedImage errorImage = null;

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

    private List<LocationOcr> getAllOcrs(byte [] data) throws IdentifyException {
        JSONObject jsonObject = ocrDetect(data);
        if (jsonObject.has("error_msg")) {
            throw new IdentifyException("百度服务出错:" + jsonObject.getString("error_msg"));
        }
        return getAllOcrs(jsonObject);
    }

    private List<LocationOcr> getAllOcrs(JSONObject jsonObject) {
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

    private JSONObject ocrDetect(byte[] data) {
        // 初始化一个AipOcr
        AipOcr client = OciClient.getClient();
        // 调用接口
        HashMap<String, String> map = new HashMap<>();
        //map.put("vertexes_location", "true");
        //map.put("detect_direction", "true");
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

    private String getEndNumberString(String s) {
        int i = 0;
        for (i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                break;
            }
        }

        if (i != s.length()) {
            return s.substring(i).trim();
        }
        return null;
    }

    private void drawFindRect(Graphics graphics, List<LocationOcr> locationOcrs) {
        for (LocationOcr locationOcr : locationOcrs) {
            graphics.drawRect(locationOcr.getX(), locationOcr.getY(), locationOcr.getWidth(), locationOcr.getHeight());
            graphics.drawString(locationOcr.getFindValue(), locationOcr.getX(), locationOcr.getY());
        }
    }

    private void drawGeneralRect(Graphics graphics, List<LocationOcr> locationOcrs) {
        for (LocationOcr locationOcr : locationOcrs) {
            graphics.drawRect(locationOcr.getX(), locationOcr.getY(), locationOcr.getWidth(), locationOcr.getHeight());
            graphics.drawString(locationOcr.getValue(), locationOcr.getX(), locationOcr.getY());
        }
    }

    private void mergeByLine(List<LocationOcr> ocrs) {
//      int avgHeight = 0;
//      for (LocationOcr locationOcr : ocrs) {
//          avgHeight += locationOcr.getHeight();
//      }
//      avgHeight /= ocrs.size();
        LocationOcr pre = null;
        int height = 0;
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

    @Override
    public BufferedImage checkImage(BufferedImage image) throws IOException, IdentifyException {
        long start = System.currentTimeMillis();
        // check image type
        if (image.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
            BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics graphics = newImage.createGraphics();
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
            image = newImage;
        }

        if (image.getType() != BufferedImage.TYPE_3BYTE_BGR) {
           throw  new IdentifyException("不支持的图片类型");
        }

        final int DISTENCE = 20;
        final int MIDDISTENCE = 22;
        final int MIDDLE_SPLIT = 100;
        if (rightImage == null) {
            String rightPath = dlConfig.getRightPath();
            //String errorPath = dlConfig.getErrorPath();
            logger.info(rightPath);
            InputStream inputStream = new FileInputStream(rightPath);
            if (inputStream != null) {
                rightImage = ImageIO.read(inputStream);
            }
        }
        if (image == null) {
            throw new NullPointerException();
        }
        BufferedImage bufferedImage = image;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(image.getHeight() * image.getWidth() * 3);
        ImageIO.write(image, "jpg", byteArrayOutputStream);
        logger.info("开始调用百度接口查找分隔符-----------");

        // split
        String [] spilt = {"LT#", "INT", "QTY", "Prod"};
        List<LocationOcr> allOcrs = getAllOcrs(byteArrayOutputStream.toByteArray());
        logger.info("结束调用百度接口查找分隔符-----------");
        LocationOcr splitOcr = teService.findSplitLocation(allOcrs, Arrays.asList(spilt));

        if (splitOcr == null) {
            throw new IdentifyException("找不到分隔符, 请检查图片，或更精确抓取");
        }

        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.setColor(Color.RED);
        graphics.setFont(new Font("", Font.BOLD, bufferedImage.getWidth() > 2000 ? 65 : 25));
        graphics.setStroke(new BasicStroke(8.0F));
        int middlex = splitOcr.getX() - MIDDLE_SPLIT;
        if (middlex <= 0) {
            throw new IdentifyException("找不到图片左半部分");
        }
        graphics.drawLine(middlex, 0, middlex, bufferedImage.getHeight());

        //left
        LocationOcr sellerLeft = null;
        LocationOcr sellerRight = null;
        String sellerNumber = null;
        String quantNumber = null;
        LocationOcr quantLeft= null;
        LocationOcr quantRight = null;

        List<LocationOcr> [] splitOcrs = teService.splitLocationOcrs(allOcrs,middlex);

        List<String> [] array = new ArrayList[3];
        array[0] = new ArrayList<>(Arrays.asList("SELLER PART", "QUANT"));
        array[1] = new ArrayList<>(Arrays.asList("MATERIAL CODE", "QTY"));
        array[2] = new ArrayList<>(Arrays.asList("INT SN", "QTY"));
        Map<String, String> resultMap = new HashMap<>();
        // method 0:notneed cutpicture  1:cut picture
        for (int method = 0; method < 2; method++) {
            if (method == 1) {
                BufferedImage bufferedImageleft = bufferedImage.getSubimage(0, 0, middlex, bufferedImage.getHeight());
                byteArrayOutputStream.reset();
                ImageIO.write(bufferedImageleft, "jpg", byteArrayOutputStream);
                logger.info("开始调用百度接口查找左边-----------");
                splitOcrs[0] = getAllOcrs(byteArrayOutputStream.toByteArray());
                logger.info("结束调用百度接口查找左边-----------");

                BufferedImage bufferedImageright = bufferedImage.getSubimage(middlex, 0, bufferedImage.getWidth() - middlex, bufferedImage.getHeight());
                byteArrayOutputStream.reset();
                ImageIO.write(bufferedImageright, "jpg", byteArrayOutputStream);
                logger.info("开始调用百度接口查找右边-----------");
                splitOcrs[1] = getAllOcrs(byteArrayOutputStream.toByteArray());
                logger.info("结束调用百度接口查找右边-----------");
            }
            mergeByLine(splitOcrs[0]);
            for (int i = 0; i < array.length; i++) {
                List<LocationOcr> identifyOcrs = teService.findIdentifyWords(splitOcrs[0], array[i]);
                if (identifyOcrs.size() == array[i].size()) {
                    resultMap.clear();
                    List<String> values = new ArrayList<>();
                    for (LocationOcr ocr : identifyOcrs) {
                        values.add(ocr.getFindValue());
                        resultMap.put(ocr.getFindKey(), ocr.getFindValue());
                    }
                    List<LocationOcr> compareOcrs = teService.findCompareWords(splitOcrs[1], values);
                    if (compareOcrs.size() == array[i].size()) {
                        for (LocationOcr ocr: compareOcrs) {
                            if (method == 1) {
                                ocr.setX(ocr.getX() + middlex);
                            }
                            identifyOcrs.add(ocr);
                        }
                        drawFindRect(graphics, identifyOcrs);
                        for (int j = 0; j < array[i].size(); j++) {
                            String key = array[i].get(j);
                            graphics.drawString(key + " equals :" + resultMap.get(key), middlex - MIDDISTENCE * 20, 100 * (j + 1));
                            if (rightImage != null) {
                                graphics.drawImage(rightImage, middlex + (DISTENCE - MIDDISTENCE) * 20, 100 * (j + 1), null);
                            }
                        }
                        graphics.drawString("use time:" + (System.currentTimeMillis() - start) / 1000.0 + "s", 0, image.getHeight());
                        return bufferedImage;
                    }
                }
            }
        }
       throw new IdentifyException("找不到相应数据或者结果不相等");
    }

    @Override
    public BufferedImage checkImageGeneral(BufferedImage image) throws IOException, IdentifyException {
        // check image type
        if (image.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
            BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics graphics = newImage.createGraphics();
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
            image = newImage;
        }

        if (image.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            throw  new IdentifyException("不支持的图片类型");
        }
        BufferedImage bufferedImage = image;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(image.getHeight() * image.getWidth() * 3);
        ImageIO.write(image, "jpg", byteArrayOutputStream);
        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.setColor(Color.RED);
        graphics.setFont(new Font("", Font.BOLD, bufferedImage.getWidth() > 2000 ? 65 : 25));
        graphics.setStroke(new BasicStroke(8.0F));
        List<LocationOcr> ocrs = getAllOcrs(byteArrayOutputStream.toByteArray());
        drawGeneralRect(graphics, ocrs);
        return  image;
    }
}
