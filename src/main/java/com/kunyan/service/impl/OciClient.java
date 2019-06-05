package com.kunyan.service.impl;

import com.baidu.aip.ocr.AipOcr;
import org.springframework.beans.factory.annotation.Value;

public class OciClient {
    static volatile boolean  config = false;

    static class OciClientHolder {
        @Value("baidu.appId")
        static private String appId;
        @Value("baidu.apiKey")
        static private String apiKey;

        @Value("baidu.secretKey")
        static private String secretKey;
        static AipOcr client = new AipOcr(appId, "MVyEclIpQWgGDTyMns3BcXsl", "gMgZruU45gpT5d5Fq3lk0YXUXbhzs7QB");
    }

    public static AipOcr getClient() {
        if (config) {
            OciClientHolder.client.setConnectionTimeoutInMillis(2000);
            OciClientHolder.client.setSocketTimeoutInMillis(60000);
            config = true;
        }
        return OciClientHolder.client;
    }
}