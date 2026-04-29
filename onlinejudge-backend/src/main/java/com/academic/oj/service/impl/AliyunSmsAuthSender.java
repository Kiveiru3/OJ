package com.academic.oj.service.impl;

import com.academic.oj.common.ResultCode;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.config.SmsProperties;
import com.academic.oj.dto.VerificationCodeDTO;
import com.academic.oj.service.SmsSender;
import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.teaopenapi.models.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "sms", name = "provider", havingValue = "aliyun-auth")
public class AliyunSmsAuthSender implements SmsSender {

    private final SmsProperties.AliyunAuth properties;
    private final Client client;

    public AliyunSmsAuthSender(SmsProperties smsProperties) throws Exception {
        this.properties = smsProperties.getAliyunAuth();
        fillAccessKeyFallback(smsProperties.getAliyun());
        validateConfig();
        Config config = new Config()
                .setAccessKeyId(properties.getAccessKeyId())
                .setAccessKeySecret(properties.getAccessKeySecret());
        config.endpoint = properties.getEndpoint();
        this.client = new Client(config);
    }

    @Override
    public void sendVerificationCode(String phone, String code) {
        throw new UnsupportedOperationException("Aliyun SMS authentication manages verification codes");
    }

    @Override
    public boolean managesVerification() {
        return true;
    }

    @Override
    public VerificationCodeDTO sendManagedVerificationCode(String phone) {
        try {
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setCountryCode(properties.getCountryCode())
                    .setPhoneNumber(phone)
                    .setSignName(properties.getSignName())
                    .setTemplateCode(properties.getTemplateCode())
                    .setTemplateParam(properties.getTemplateParam())
                    .setCodeLength(toLong(properties.getCodeLength()))
                    .setValidTime(toLong(properties.getValidTime()))
                    .setDuplicatePolicy(toLong(properties.getDuplicatePolicy()))
                    .setInterval(toLong(properties.getInterval()))
                    .setCodeType(toLong(properties.getCodeType()))
                    .setReturnVerifyCode(false)
                    .setAutoRetry(toLong(properties.getAutoRetry()));
            if (StringUtils.hasText(properties.getSchemeName())) {
                request.setSchemeName(properties.getSchemeName());
            }

            SendSmsVerifyCodeResponse response = client.sendSmsVerifyCode(request);
            String responseCode = response.getBody() == null ? null : response.getBody().getCode();
            if (!"OK".equals(responseCode)) {
                String message = response.getBody() == null ? "unknown error" : response.getBody().getMessage();
                log.warn("Aliyun SMS auth send failed, phone={}, code={}, message={}",
                        maskPhone(phone), responseCode, message);
                throw new BusinessException(ResultCode.ERROR.getCode(), "SMS verification send failed: " + message);
            }
            log.info("Aliyun SMS auth sent, phone={}, requestId={}", maskPhone(phone), response.getBody().getRequestId());
            return new VerificationCodeDTO(phone, properties.getValidTime(), null);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Aliyun SMS auth request failed, phone={}", maskPhone(phone), ex);
            throw new BusinessException(ResultCode.ERROR.getCode(), "SMS verification send failed: " + ex.getMessage());
        }
    }

    @Override
    public void verifyManagedCode(String phone, String code) {
        try {
            CheckSmsVerifyCodeRequest request = new CheckSmsVerifyCodeRequest()
                    .setCountryCode(properties.getCountryCode())
                    .setPhoneNumber(phone)
                    .setVerifyCode(code)
                    .setCaseAuthPolicy(toLong(properties.getCaseAuthPolicy()));
            if (StringUtils.hasText(properties.getSchemeName())) {
                request.setSchemeName(properties.getSchemeName());
            }

            CheckSmsVerifyCodeResponse response = client.checkSmsVerifyCode(request);
            String responseCode = response.getBody() == null ? null : response.getBody().getCode();
            String verifyResult = response.getBody() == null || response.getBody().getModel() == null
                    ? null
                    : response.getBody().getModel().getVerifyResult();
            if (!"OK".equals(responseCode) || !"PASS".equals(verifyResult)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Invalid verification code");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Aliyun SMS auth check failed, phone={}", maskPhone(phone), ex);
            throw new BusinessException(ResultCode.ERROR.getCode(), "SMS verification check failed: " + ex.getMessage());
        }
    }

    private void fillAccessKeyFallback(SmsProperties.Aliyun aliyun) {
        if (!StringUtils.hasText(properties.getAccessKeyId())) {
            properties.setAccessKeyId(aliyun.getAccessKeyId());
        }
        if (!StringUtils.hasText(properties.getAccessKeySecret())) {
            properties.setAccessKeySecret(aliyun.getAccessKeySecret());
        }
    }

    private void validateConfig() {
        if (!StringUtils.hasText(properties.getAccessKeyId())
                || !StringUtils.hasText(properties.getAccessKeySecret())
                || !StringUtils.hasText(properties.getSignName())
                || !StringUtils.hasText(properties.getTemplateCode())) {
            throw new BusinessException(ResultCode.ERROR.getCode(),
                    "Aliyun SMS authentication is enabled but access key, gifted sign name, or gifted template code is missing");
        }
    }

    private Long toLong(Integer value) {
        return value == null ? null : value.longValue();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "unknown";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
