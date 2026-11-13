package com.lxw.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "game.jwt")
public class JwtProperties {

    /** 签名密钥 */
    private String secretKey = "GameStatsSecretKey2024GameStatsSecretKey2024";

    /** 过期时间（毫秒），默认 7 天 */
    private Long ttl = 7 * 24 * 60 * 60 * 1000L;

    /** token 在请求头中的名称 */
    private String tokenName = "token";
}
