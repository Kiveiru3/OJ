package com.academic.oj.service.impl;

import com.academic.oj.common.ResultCode;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.config.SmsProperties;
import com.academic.oj.service.SmsSender;
import com.alibaba.fastjson2.JSON;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "sms", name = "provider", havingValue = "aliyun")
public class AliyunSmsSender implements SmsSender {

    private final SmsProperties.Aliyun properties;
    private final Client client;

    public AliyunSmsSender(SmsProperties smsProperties) throws Exception {
        this.properties = smsProperties.getAliyun();
        validateConfig();
        Config config = new Config()
                .setAccessKeyId(properties.getAccessKeyId())
                .setAccessKeySecret(properties.getAccessKeySecret());
        config.endpoint = properties.getEndpoint();
        this.client = new Client(config);
    }

    @Override
    public void sendVerificationCode(String phone, String code) {
        try {
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(properties.getSignName())
                    .setTemplateCode(properties.getTemplateCode())
                    .setTemplateParam(JSON.toJSONString(Map.of("code", code)));
            SendSmsResponse response = client.sendSms(request);
            String responseCode = response.getBody() == null ? null : response.getBody().getCode();
            if (!"OK".equals(responseCode)) {
                String message = response.getBody() == null ? "unknown error" : response.getBody().getMessage();
                log.warn("Aliyun SMS send failed, phone={}, code={}, message={}", maskPhone(phone), responseCode, message);
                throw new BusinessException(ResultCode.ERROR.getCode(), "SMS send failed: " + message);
            }
            log.info("Aliyun SMS sent, phone={}, bizId={}", maskPhone(phone), response.getBody().getBizId());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Aliyun SMS request failed, phone={}", maskPhone(phone), ex);
            throw new BusinessException(ResultCode.ERROR.getCode(), "SMS send failed: " + ex.getMessage());
        }
    }

    private void validateConfig() {
        if (!StringUtils.hasText(properties.getAccessKeyId())
                || !StringUtils.hasText(properties.getAccessKeySecret())
                || !StringUtils.hasText(properties.getSignName())
                || !StringUtils.hasText(properties.getTemplateCode())) {
            throw new BusinessException(ResultCode.ERROR.getCode(),
                    "Aliyun SMS is enabled but access key, sign name, or template code is missing");
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "unknown";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
