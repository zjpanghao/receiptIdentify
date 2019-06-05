package com.kunyan.service;
import com.kunyan.entity.IdentifyException;
import com.kunyan.entity.PictureUpload;

import java.awt.image.BufferedImage;
import java.io.IOException;

public interface ProjectService {
    BufferedImage checkImage(BufferedImage image) throws IOException, IdentifyException;
    BufferedImage checkImageGeneral(BufferedImage image, PictureUpload pictureUpload) throws IOException, IdentifyException;
}
