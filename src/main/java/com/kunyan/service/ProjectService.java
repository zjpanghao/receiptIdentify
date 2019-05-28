package com.kunyan.service;
import com.kunyan.entity.IdentifyException;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

public interface ProjectService {
    BufferedImage checkImage(BufferedImage image) throws IOException, IdentifyException;
    BufferedImage checkImageGeneral(BufferedImage image) throws IOException, IdentifyException;
}
