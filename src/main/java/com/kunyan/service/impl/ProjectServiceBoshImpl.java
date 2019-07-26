package com.kunyan.service.impl;

import com.baidu.aip.ocr.AipOcr;
import com.kunyan.config.DlConfig;
import com.kunyan.entity.*;
import com.kunyan.ocr.OcrUtil;
import com.kunyan.service.ProjectService;
import com.kunyan.service.TeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

@Component
public class ProjectServiceBoshImpl implements ProjectService {
    private Log logger = LogFactory.getLog(ProjectServiceBoshImpl.class);
    @Autowired
    private TeService teService;
    @Autowired
    private DlConfig dlConfig;

    private volatile BufferedImage rightImage = null;

    private JSONObject ocrDetect(byte[] data) {
        AipOcr client = OciClient.getClient();
        HashMap<String, String> map = new HashMap<>();
        JSONObject res = client.accurateGeneral(data, map);
        return res;
    }


    @Override
    public PictureItem checkImage(BufferedImage image) throws IOException, IdentifyException {
        long start = System.currentTimeMillis();
        PictureItem pictureItem = new PictureItem();
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
            if (Files.isReadable(Paths.get(rightPath))) {
                logger.info(rightPath);
                InputStream inputStream = new FileInputStream(rightPath);
                if (inputStream != null) {
                    rightImage = ImageIO.read(inputStream);
                }
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
        String [] spilt = {"主包装", "包装号", "材料", "材料数量"};
        java.util.List<LocationOcr> allOcrs = teService.getAllOcrs(byteArrayOutputStream.toByteArray());
        logger.info("结束调用百度接口查找分隔符-----------");
        LocationOcr splitOcr = teService.findSplitLocation(allOcrs, Arrays.asList(spilt));

        if (splitOcr == null) {
            //throw new IdentifyException("找不到分隔符, 请检查图片，或更精确抓取");
            logger.error("找不到分隔符, 请检查图片，或更精确抓取");
            return null;
        }

        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.setColor(Color.RED);
        graphics.setFont(new Font("", Font.BOLD, bufferedImage.getWidth() > 2000 ? 65 : 25));
        graphics.setStroke(new BasicStroke(8.0F));
        int middlex = splitOcr.getX() - MIDDLE_SPLIT;
        if (middlex <= 0) {
            //throw new IdentifyException("找不到图片左半部分");
            logger.error("找不到图片左半部分");
            return null;
        }
        graphics.drawLine(middlex, 0, middlex, bufferedImage.getHeight());
        final int SEARCH_MODE_NUM = 1;
        Map<String, String> chToeng = new HashMap<>();
        chToeng.put("包装号", "Package number");
        chToeng.put("材料数量", "Quantity");
        List<LocationOcr> [] splitOcrs = teService.splitLocationOcrs(allOcrs, middlex);
        java.util.List<String>[] arrayFindDown = new ArrayList[SEARCH_MODE_NUM];
        arrayFindDown[0] = new ArrayList<>(Arrays.asList("包装号", "材料数量"));
        java.util.List<String>[] arrayFindReg = new ArrayList[SEARCH_MODE_NUM];
        arrayFindReg[0] = new ArrayList<>(Arrays.asList("[0-9]", " PC"));
        java.util.List<List<Integer> >  arrayFindRegDirection = new ArrayList(SEARCH_MODE_NUM);
        arrayFindRegDirection.add(new ArrayList<>(Arrays.asList(0, 1)));
        // method 0:notneed cutpicture  1:cut picture
        for (int method = 0; method < 2; method++) {
            if (method == 1) {
                BufferedImage bufferedImageleft = bufferedImage.getSubimage(0, 0, middlex, bufferedImage.getHeight());
                byteArrayOutputStream.reset();
                ImageIO.write(bufferedImageleft, "jpg", byteArrayOutputStream);
                logger.info("开始调用bai接口查找左边-----------");
                splitOcrs[1] = teService.getAllOcrs(byteArrayOutputStream.toByteArray());
                logger.info("结束调用baidu接口查找左边-----------");

                BufferedImage bufferedImageright = bufferedImage.getSubimage(middlex, 0, bufferedImage.getWidth() - middlex, bufferedImage.getHeight());
                byteArrayOutputStream.reset();
                ImageIO.write(bufferedImageright, "jpg", byteArrayOutputStream);
                logger.info("开始调用接口查找右边-----------");
                splitOcrs[0] = teService.getAllOcrs(byteArrayOutputStream.toByteArray());
                logger.info("结束调用百度接口查找右边-----------");
            }
            //mergeByLine(splitOcrs[1]);
            for (int i = 0; i < arrayFindDown.length; i++) {
                Map<String, LocationOcr> identifyOcrs = teService.findIdentifyWordsDownNearest(splitOcrs[0], arrayFindDown[i]);
                Set<String> checkValues = new HashSet<>();
                if (identifyOcrs.size() == arrayFindDown[i].size()) {
//                    resultMap.clear();
                    java.util.List<OcrFindItem> values = new ArrayList<>();
                    for (Map.Entry<String, LocationOcr> ocr : identifyOcrs.entrySet()) {
                        checkValues.add(ocr.getValue().getValue());
                        ocr.getValue().setFindValue(ocr.getValue().getValue());
                        ocr.getValue().setKey(new OcrFindItem(ocr.getKey(), ocr.getKey()));
                        if (method == 1) {
                            ocr.getValue().setX(ocr.getValue().getX() + middlex);
                        }
                        int inx = 0;
                        for (String downKey: arrayFindDown[i]) {
                            if (downKey.equals(ocr.getKey())) {
                                if (arrayFindRegDirection.get(i).get(inx) == 0) {
                                    OcrFindItem ocrFindItem = new OcrFindItem(ocr.getKey(), arrayFindReg[i].get(inx) + ocr.getValue().getValue());
                                    values.add(ocrFindItem);
                                } else {
                                    OcrFindItem ocrFindItem = new OcrFindItem(ocr.getKey(), ocr.getValue().getValue() + arrayFindReg[i].get(inx));
                                    values.add(ocrFindItem);
                                }
                            }
                            inx++;
                        }
                    }

                    List<LocationOcr> compareOcrs = teService.findIdentifyWords(splitOcrs[1], values);
                    if (compareOcrs.size() == arrayFindDown[i].size()) {
                        List<LocationOcr> findOcrs = new ArrayList<>(identifyOcrs.values());
                        for (LocationOcr locationOcr : compareOcrs) {
                            logger.info("开始精准识别:" + locationOcr.getValue());
                            LocationOcr reIdentify = teService.reIdentify(bufferedImage, locationOcr);
                            logger.info("结束精准识别:" + reIdentify.getValue());
                            reIdentify.setFindValue(OcrUtil.getNumberString(reIdentify.getValue()));
                            reIdentify.setKey(locationOcr.getKey());
                            if (i == 0) {
                                if (reIdentify.getFindKey().contains(arrayFindReg[i].get(1)) && checkValues.contains(reIdentify.getFindValue())) {
                                    findOcrs.add(reIdentify);
                                } else if (reIdentify.getFindKey().contains(arrayFindReg[i].get(0)) && reIdentify.getFindValue().length() > 1 && checkValues.contains(reIdentify.getFindValue().substring(1))) {
                                    findOcrs.add(reIdentify);
                                }
                            } else {
                                if (checkValues.contains(reIdentify.getFindValue())) {
                                    findOcrs.add(reIdentify);
                                }
                            }
                        }

                        OcrUtil.drawFindRect(graphics, findOcrs);
                        if (findOcrs.size() != 2 * arrayFindDown[i].size()) {
                            return null;
                        }
                        Map<String, String> resultMap = new HashMap<>();
                        for (int k = 0; k < arrayFindDown[i].size(); k++) {
                            resultMap.put(arrayFindDown[i].get(k), identifyOcrs.get(arrayFindDown[i].get(k)).getFindValue());
                        }
                        int j = 0;
                        for (Map.Entry<String, LocationOcr> entry : identifyOcrs.entrySet()) {
                            String key = entry.getKey();
                            graphics.drawString(chToeng.get(key) + " equals :" + entry.getValue().getValue(), middlex - MIDDISTENCE * 20, 100 * (j + 1));
                            if (rightImage != null) {
                                graphics.drawImage(rightImage, middlex + (DISTENCE - MIDDISTENCE) * 20, 100 * (j + 1), null);
                            }
                            j++;
                        }
                        pictureItem.setErrorCode(0);
                        byteArrayOutputStream.reset();
                        ImageIO.write(bufferedImage, "jpg", byteArrayOutputStream);
                        pictureItem.setErrorCode(0);
                        List<LocationOcr> left = new ArrayList<>();
                        List<LocationOcr> right = new ArrayList<>();
                        for (LocationOcr locationOcr : findOcrs) {
                            if (locationOcr.getX() < middlex) {
                                left.add(locationOcr);
                            } else {
                                right.add(locationOcr);
                            }
                        }
                        pictureItem.setLeftItems(left);
                        pictureItem.setRightItems(right);
                        pictureItem.setMiddleX(middlex);
                        pictureItem.setImageBase64(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));
                        return pictureItem;
                    }
                }
            }
        }
        //throw new IdentifyException("找不到相应数据或者结果不相等");
        return null;
    }

    @Override
    public BufferedImage checkImageGeneral(BufferedImage image, PictureUpload pictureUpload) throws IOException, IdentifyException {
        return null;
    }
}
