package com.kunyan.service.impl;

import com.baidu.aip.ocr.AipOcr;
import com.kunyan.config.DlConfig;
import com.kunyan.entity.IdentifyException;
import com.kunyan.entity.LocationOcr;
import com.kunyan.entity.PictureUpload;
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
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

import static com.kunyan.ocr.OcrUtil.mergeByLine;

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
            mergeByLine(splitOcrs[1]);
            for (int i = 0; i < arrayFindDown.length; i++) {
                Map<String, LocationOcr> identifyOcrs = teService.findIdentifyWordsDownNearest(splitOcrs[0], arrayFindDown[i]);
                if (identifyOcrs.size() == arrayFindDown[i].size()) {
//                    resultMap.clear();
                    java.util.List<String> values = new ArrayList<>();
                    for (Map.Entry<String, LocationOcr> ocr : identifyOcrs.entrySet()) {
                        ocr.getValue().setFindValue(ocr.getValue().getValue());
                        if (method == 1) {
                            ocr.getValue().setX(ocr.getValue().getX() + middlex);
                        }
                        int inx = 0;
                        for (String downKey: arrayFindDown[i]) {
                            if (downKey.equals(ocr.getKey())) {
                                if (arrayFindRegDirection.get(i).get(inx) == 0) {
                                    values.add(arrayFindReg[i].get(inx) + ocr.getValue().getValue());
                                } else {
                                    values.add(ocr.getValue().getValue() + arrayFindReg[i].get(inx));
                                }
                            }
                            inx++;
                        }
                    }

                    List<LocationOcr> compareOcrs = teService.findIdentifyWords(splitOcrs[1], values);
                    if (compareOcrs.size() == arrayFindDown[i].size()) {
                        List<LocationOcr> findOcrs = new ArrayList<>(identifyOcrs.values());
                        for (LocationOcr ocr: compareOcrs) {
                            ocr.setFindValue(OcrUtil.getNumberString(ocr.getValue()));
                            findOcrs.add(ocr);
                        }
                        OcrUtil.drawFindRect(graphics, findOcrs);
                        int j = 0;
                        for (Map.Entry<String, LocationOcr> entry : identifyOcrs.entrySet()) {
                            String key = entry.getKey();
                            graphics.drawString(key + " equals :" + entry.getValue().getValue(), middlex - MIDDISTENCE * 20, 100 * (j + 1));
                            if (rightImage != null) {
                                graphics.drawImage(rightImage, middlex + (DISTENCE - MIDDISTENCE) * 20, 100 * (j + 1), null);
                            }
                            j++;
                        }
                        return bufferedImage;
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
