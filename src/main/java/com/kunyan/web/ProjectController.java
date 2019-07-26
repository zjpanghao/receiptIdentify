package com.kunyan.web;

import com.google.gson.Gson;
import com.kunyan.entity.IdentifyException;
import com.kunyan.entity.PictureItem;
import com.kunyan.entity.PictureUpload;
import com.kunyan.service.ProjectService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Controller
@EnableAspectJAutoProxy
public class ProjectController {
    private final Log logger = LogFactory.getLog(ProjectController.class);
    @Autowired
    @Qualifier("projectServiceBoshImpl")
    private ProjectService projectServiceBosh;

    @Autowired
    @Qualifier("projectServiceImpl")
    private ProjectService projectService;

    @RequestMapping("/identify")
    @ResponseBody
    public PictureItem identifyProject(@RequestBody String body ) {
        long start = System.currentTimeMillis();
        logger.info("start call identify-------------" + start);
        PictureUpload pictureUpload = new Gson().fromJson(body, PictureUpload.class);
        PictureItem pictureItem = new PictureItem();
        pictureItem.setErrorCode(-3);
        try {
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(pictureUpload.getImageBase64())));
            logger.info("image type:" + bufferedImage.getType());
            if (bufferedImage == null) {
                pictureItem.setErrorMsg("图片读取错误");
                return pictureItem;
            }

            PictureItem result = projectServiceBosh.checkImage(bufferedImage);
            if (result == null) {
                result = projectService.checkImage(bufferedImage);
            }
            if (result == null) {
                pictureItem.setErrorCode(-5);
                pictureItem.setErrorMsg("没有解析到对应数据信息");
                return pictureItem;
            }
            return result;
        } catch (IdentifyException e) {
            pictureItem.setErrorCode(-1);
            pictureItem.setErrorMsg(e.getMsg());
            logger.error("识别异常" + "error:" + e.getMsg());
        } catch (IOException e) {
            e.printStackTrace();
            pictureItem.setErrorCode(-2);
            logger.error("识别错误 io异常" + "error:" + e.getMessage());
            pictureItem.setErrorMsg("识别接口异常，检查图片信息");
        } catch (Exception e) {
            pictureItem.setErrorCode(-3);
            pictureItem.setErrorMsg("服务出现异常");
            logger.error("识别错误 异常:" + "error:" + e.getMessage());
        } finally {
            logger.info("end call identify-------------" + System.currentTimeMillis());
        }
        return pictureItem;
    }

    @RequestMapping("/identifyGeneral")
    @ResponseBody
    public PictureItem identifyGeneral(@RequestBody String body ) {
        logger.info("start call identify-------------" + System.currentTimeMillis());
        PictureUpload pictureUpload = new Gson().fromJson(body, PictureUpload.class);
        PictureItem pictureItem = new PictureItem();
        pictureItem.setErrorCode(-3);
        try {
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(pictureUpload.getImageBase64())));
            logger.info("image type:" + bufferedImage.getType());
            if (bufferedImage == null) {
                pictureItem.setErrorMsg("图片读取错误");
                return pictureItem;
            }

            BufferedImage result = projectService.checkImageGeneral(bufferedImage, pictureUpload);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(body.length());
            ImageIO.write(result, "jpg", byteArrayOutputStream);
            pictureItem.setErrorCode(0);
            pictureItem.setImageBase64(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));
            return pictureItem;
        } catch (IdentifyException e) {
            pictureItem.setErrorCode(-1);
            pictureItem.setErrorMsg(e.getMsg());
            logger.error("识别异常" + "error:" + e.getMsg());
        } catch (IOException e) {
            e.printStackTrace();
            pictureItem.setErrorCode(-2);
            logger.error("识别错误 io异常" + "error:" + e.getMessage());
            pictureItem.setErrorMsg("识别接口异常，检查图片信息");
        } catch (Exception e) {
            pictureItem.setErrorCode(-3);
            pictureItem.setErrorMsg("服务出现异常");
            logger.error("识别错误 异常:" + "error:" + e.getMessage());
        } finally {
            logger.info("end call identify-------------" + System.currentTimeMillis());
        }
        return pictureItem;
    }

}
