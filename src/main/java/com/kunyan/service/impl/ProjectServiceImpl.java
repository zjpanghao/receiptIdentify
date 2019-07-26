package com.kunyan.service.impl;

import com.kunyan.config.DlConfig;
import com.kunyan.entity.*;
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
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.awt.image.BufferedImage.TYPE_3BYTE_BGR;

@Service
public class ProjectServiceImpl implements ProjectService {
    private Log logger = LogFactory.getLog(ProjectServiceImpl.class);
    @Autowired
    private TeService teService;
    @Autowired
    private DlConfig dlConfig;

    private volatile BufferedImage rightImage = null;

    @Override
    public PictureItem checkImage(BufferedImage image) throws IOException, IdentifyException {
        long start = System.currentTimeMillis();
        PictureItem pictureItem = new PictureItem();
        // check image type
        if (image.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
            BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), TYPE_3BYTE_BGR);
            Graphics graphics = newImage.createGraphics();
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
            image = newImage;
        }

        if (image.getType() != TYPE_3BYTE_BGR) {
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
        graphics.setStroke(new BasicStroke(3.0F));
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
                List<OcrFindItem> ocrFindItems = new ArrayList<>();
                for (int k = 0; k < array[i].size(); k++) {
                    OcrFindItem ocrFindItem = new OcrFindItem(array[i].get(k), array[i].get(k));
                    ocrFindItems.add(ocrFindItem);
                }
                List<LocationOcr> identifyOcrs = teService.findIdentifyWords(splitOcrs[0], ocrFindItems);
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
                        OcrUtil.drawGeneralRectRight(graphics, identifyOcrs);
                        for (int j = 0; j < array[i].size(); j++) {
                            String key = array[i].get(j);
                            graphics.drawString(key + " equals :" + resultMap.get(key), middlex - MIDDISTENCE * 20, 100 * (j + 1));
                            if (rightImage != null) {
                                graphics.drawImage(rightImage, middlex + (DISTENCE - MIDDISTENCE) * 20, 100 * (j + 1), null);
                            }
                        }
                        graphics.drawString("use time:" + (System.currentTimeMillis() - start) / 1000.0 + "s", 0, image.getHeight());
                        for (int k = 0; k < array[i].size(); k++) {
                            List<String> items = array[i];
                            resultMap.put(items.get(k), resultMap.get(items.get(k)));
                        }

                        pictureItem.setErrorCode(0);
                        byteArrayOutputStream.reset();
                        ImageIO.write(bufferedImage, "jpg", byteArrayOutputStream);
                        pictureItem.setErrorCode(0);
                        List<LocationOcr> left = new ArrayList<>();
                        List<LocationOcr> right = new ArrayList<>();
                        for (LocationOcr locationOcr : identifyOcrs) {
                            if (locationOcr.getX() < middlex) {
                                left.add(locationOcr);
                            } else {
                                right.add(locationOcr);
                            }
                        }
                        pictureItem.setLeftItems(left);
                        pictureItem.setRightItems(right);
                        pictureItem.setImageBase64(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));
                        return pictureItem;
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
            BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), TYPE_3BYTE_BGR);
            Graphics graphics = newImage.createGraphics();
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
            image = newImage;
        }

        if (image.getType() != TYPE_3BYTE_BGR) {
            throw  new IdentifyException("不支持的图片类型");
        }
        BufferedImage bufferedImage = new BufferedImage(image.getWidth(), image.getHeight(), TYPE_3BYTE_BGR);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(image.getHeight() * image.getWidth() * 3);

        ImageIO.write(image, "jpg", byteArrayOutputStream);
        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());

        graphics.setColor(Color.RED);
        graphics.setFont(new Font("", Font.ROMAN_BASELINE, bufferedImage.getWidth() > 2000 ? 35 : 9));
        graphics.setStroke(new BasicStroke(3.0F));

        List<LocationOcr> ocrs = teService.getAllOcrs(byteArrayOutputStream.toByteArray());
        Iterator<LocationOcr> ocrIterator = ocrs.iterator();
        while (ocrIterator.hasNext()) {
            LocationOcr locationOcr = ocrIterator.next();
            boolean find = false;
            for (PictureUpload.ContainCheckItem contain : pictureUpload.getContains()) {
                Pattern identify = Pattern.compile(contain.getValue(), Pattern.CASE_INSENSITIVE);
                Matcher matcher = identify.matcher(locationOcr.getValue());
                if (matcher.matches()) {
                    int inx = matcher.start();
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
        OcrUtil.drawGeneralRectIn(graphics, ocrs);
        ImageIO.write(bufferedImage, "jpg", new File("d:\\test.jpg"));

        return  bufferedImage;
    }
}
