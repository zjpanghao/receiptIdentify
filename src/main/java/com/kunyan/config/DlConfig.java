package com.kunyan.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DlConfig {
    @Value("${picture.right}")
    private String rightPath;
    @Value("${picture.error}")
    private String errorPath;

    public String getRightPath() {
        return rightPath;
    }

    public void setRightPath(String rightPath) {
        this.rightPath = rightPath;
    }

    public String getErrorPath() {
        return errorPath;
    }

    public void setErrorPath(String errorPath) {
        this.errorPath = errorPath;
    }
}
