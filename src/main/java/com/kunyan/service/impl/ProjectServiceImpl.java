package com.kunyan.service.impl;

import com.kunyan.config.DlConfig;
import com.kunyan.entity.IdentifyException;
import com.kunyan.entity.LocationOcr;
import com.kunyan.entity.PictureUpload;
import com.kunyan.ocr.OcrUtil;
import com.kunyan.service.ProjectService;
import com.kunyan.service.TeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

@Service
public class ProjectServiceImpl implements ProjectService {
    private Log logger = LogFactory.getLog(ProjectServiceImpl.class);
    @Autowired
    private TeService teService;
    @Autowired
    private DlConfig dlConfig;

    private volatile BufferedImage rightImage = null;

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
            if (Files.isReadable(Paths.get(rightPath))) {
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
        String [] spilt = {"LT#", "INT", "QTY", "Prod"};
        List<LocationOcr> allOcrs = teService.getAllOcrs(byteArrayOutputStream.toByteArray());
        logger.info("结束调用百度接口查找分隔符-----------");
        LocationOcr splitOcr = teService.findSplitLocation(allOcrs, Arrays.asList(spilt));

        if (splitOcr == null) {
            return null;
        }

        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.setColor(Color.RED);
        graphics.setFont(new Font("", Font.BOLD, bufferedImage.getWidth() > 2000 ? 65 : 25));
        graphics.setStroke(new BasicStroke(8.0F));
        int middlex = splitOcr.getX() - MIDDLE_SPLIT;
        if (middlex <= 0) {
            //throw new IdentifyException("找不到图片左半部分");
            return null;
        }
        graphics.drawLine(middlex, 0, middlex, bufferedImage.getHeight());

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
                splitOcrs[0] = teService.getAllOcrs(byteArrayOutputStream.toByteArray());
                logger.info("结束调用百度接口查找左边-----------");

                BufferedImage bufferedImageright = bufferedImage.getSubimage(middlex, 0, bufferedImage.getWidth() - middlex, bufferedImage.getHeight());
                byteArrayOutputStream.reset();
                ImageIO.write(bufferedImageright, "jpg", byteArrayOutputStream);
                logger.info("开始调用百度接口查找右边-----------");
                splitOcrs[1] = teService.getAllOcrs(byteArrayOutputStream.toByteArray());
                logger.info("结束调用百度接口查找右边-----------");
            }
            OcrUtil.mergeByLine(splitOcrs[0]);
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
                        OcrUtil.drawFindRect(graphics, identifyOcrs);
                        for (int j = 0; j < array[i].size(); j++) {
                            String key = array[i].get(j);
                            graphics.drawString(key + " equals :" + resultMap.get(key), middlex - MIDDISTENCE * 20, 100 * (j + 1));
                            if (rightImage != null) {
                                graphics.drawImage(rightImage, middlex + (DISTENCE - MIDDISTENCE) * 20, 100 * (j + 1), null);
                            }
                        }
                       // graphics.drawString("use time:" + (System.currentTimeMillis() - start) / 1000.0 + "s", 0, image.getHeight());
                        return bufferedImage;
                    }
                }
            }
        }
      return null;
    }

    @Override
    public BufferedImage checkImageGeneral(BufferedImage image, PictureUpload pictureUpload) throws IOException, IdentifyException {
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
        graphics.setFont(new Font("", Font.ROMAN_BASELINE, bufferedImage.getWidth() > 2000 ? 65 : 18));
        graphics.setStroke(new BasicStroke(3.0F));
        List<LocationOcr> ocrs = teService.getAllOcrs(byteArrayOutputStream.toByteArray());
        Iterator<LocationOcr> ocrIterator = ocrs.iterator();
        while (ocrIterator.hasNext()) {
            LocationOcr locationOcr = ocrIterator.next();
            boolean find = false;
            for (PictureUpload.ContainCheckItem contain : pictureUpload.getContains()) {
                int inx = locationOcr.getValue().indexOf(contain.getValue());
                if (inx != -1) {
                    find = true;
                    if (contain.getLen() > 0) {
                        locationOcr.setValue(locationOcr.getValue().substring(inx, locationOcr.getValue().length() < inx + contain.getLen() ? locationOcr.getValue().length() : inx + contain.getLen()));
                    }
                    break;
                }
            }
            if (!find && pictureUpload.getContains().size() > 0) {
                ocrIterator.remove();
            } else {
                for (String exclude : pictureUpload.getExcludes()) {
                    if (locationOcr.getValue().contains(exclude)) {
                        ocrIterator.remove();
                        break;
                    }
                }
            }
        }
        OcrUtil.drawGeneralRect(graphics, ocrs);
        return  image;
    }
}
