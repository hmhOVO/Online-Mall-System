package com.example.utils;

import cn.hutool.core.date.DateUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;

import java.util.Date;

public class TokenUtils {

    public static final String SECRET = "TaskCollaborationSystemSecretKey";

    public static String createToken(String data, int days) {
        return JWT.create()
                .withAudience(data)
                .withExpiresAt(DateUtil.offsetDay(new Date(), days))
                .sign(Algorithm.HMAC256(SECRET));
    }

    public static String createToken(String data) {
        return createToken(data, 1);
    }

    public static boolean verify(String token) {
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException e) {
            return false;
        }
    }

    public static String getAudience(String token) {
        return JWT.decode(token).getAudience().get(0);
    }
}
