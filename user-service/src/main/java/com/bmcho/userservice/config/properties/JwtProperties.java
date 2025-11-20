package com.bmcho.userservice.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String privateKey;
    private String publicKey;

    private Expire expire = new Expire();

    @Getter
    @Setter
    public static class Expire {
        private int accessToken;
        private int refreshToken;
    }

    public String getPrivateKey() {
        return this.privateKey
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            // 2. 줄바꿈/공백 제거
            .replaceAll("\\s+", "");
    }

    public String getPublicKey() {
        return this.publicKey
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            // 2. 줄바꿈/공백 제거
            .replaceAll("\\s+", "");
    }
}