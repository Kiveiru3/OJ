package com.academic.oj.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {
    private String provider = "mock";
    private Aliyun aliyun = new Aliyun();
    private AliyunAuth aliyunAuth = new AliyunAuth();

    @Data
    public static class Aliyun {
        private String accessKeyId;
        private String accessKeySecret;
        private String endpoint = "dysmsapi.aliyuncs.com";
        private String signName;
        private String templateCode;
    }

    @Data
    public static class AliyunAuth {
        private String accessKeyId;
        private String accessKeySecret;
        private String endpoint = "dypnsapi.aliyuncs.com";
        private String schemeName;
        private String countryCode = "86";
        private String signName;
        private String templateCode;
        private String templateParam = "{\"code\":\"##code##\",\"min\":\"5\"}";
        private Integer codeLength = 6;
        private Integer validTime = 300;
        private Integer duplicatePolicy = 1;
        private Integer interval = 60;
        private Integer codeType = 1;
        private Integer autoRetry = 1;
        private Integer caseAuthPolicy = 1;
    }
}
