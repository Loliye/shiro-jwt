package com.mikufans.shiro.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.mikufans.shiro.exception.CustomExcetption;
import com.mikufans.shiro.model.common.Constant;
import com.mikufans.shiro.util.common.Base64ConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil
{
    private static String accessTokenExpireTime;
    private static String encryptJWTKey;


    @Value("${accessTokenExpireTime}")
    public void setAccessTokenExpireTime(String accessTokenExpireTime)
    {
        JwtUtil.accessTokenExpireTime = accessTokenExpireTime;
    }

    @Value("${encryptJWTKey}")
    public void setEncryptJWTKey(String encryptJWTKey)
    {
        JwtUtil.encryptJWTKey = encryptJWTKey;
    }

    /**
     * 校验token是否正确
     *
     * @param token
     * @return
     */
    public static boolean verify(String token)
    {
        try
        {
            // 帐号加JWT私钥解密
            String secret = getClaim(token, Constant.ACCOUNT) + Base64ConvertUtil.decode(encryptJWTKey);

            Algorithm algorithm = Algorithm.HMAC256(secret);

            JWTVerifier verifier = JWT.require(algorithm).build();

            DecodedJWT jwt = verifier.verify(token);
            return true;

        } catch (UnsupportedEncodingException e)
        {
            log.error("JWTToken认证解密出现UnsupportedEncodingException异常:" + e.getMessage());
            throw new CustomExcetption("JWTToken认证解密出现UnsupportedEncodingException异常:" + e.getMessage());
        }
    }


    /**
     * 获取token中信息  无需要secret解密也能获得
     *
     * @param token
     * @param claim
     * @return
     */
    public static String getClaim(String token, String claim)
    {
        try
        {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim(claim).asString();
        } catch (JWTDecodeException e)
        {
            log.error("解密Token中的公共信息出现JWTDecodeException异常:" + e.getMessage());
            throw new CustomExcetption("解密Token中的公共信息出现JWTDecodeException异常:" + e.getMessage());
        }
    }

    /**
     * 生成签名
     * @param account
     * @param currentTimeMills
     * @return 返回加密后的token
     */
    public static String sign(String account,String currentTimeMills)
    {
        try
        {
            String secret=account+Base64ConvertUtil.decode(encryptJWTKey);

            Date date=new Date(System.currentTimeMillis()+Long.parseLong(accessTokenExpireTime)*1000);

            Algorithm algorithm=Algorithm.HMAC256(secret);

            return JWT.create()
                .withClaim("account", account)
                .withClaim("currentTimeMillis", currentTimeMills)
                .withExpiresAt(date)
                .sign(algorithm);
        } catch (UnsupportedEncodingException e)
        {
            log.error("JWTToken加密出现UnsupportedEncodingException异常:" + e.getMessage());
            throw new CustomExcetption("JWTToken加密出现UnsupportedEncodingException异常:" + e.getMessage());
        }
    }


}