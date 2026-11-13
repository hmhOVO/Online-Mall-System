package com.example.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TokenUtilsTest {

    @Test
    public void verifyValidToken_shouldReturnTrue() {
        String token = TokenUtils.createToken("1-STUDENT", 1);
        Assertions.assertTrue(TokenUtils.verify(token), "正常签发的 token 应校验通过");
        Assertions.assertEquals("1-STUDENT", TokenUtils.getAudience(token), "解析出的身份应与签发时一致");
    }

    @Test
    public void verifyTamperedToken_shouldReturnFalse() {
        String token = TokenUtils.createToken("1-STUDENT", 1);
        String tampered = token.substring(0, token.length() - 1) + "x";
        Assertions.assertFalse(TokenUtils.verify(tampered), "被篡改的 token 不应通过校验");
    }

    @Test
    public void verifyExpiredToken_shouldReturnFalse() {
        String expired = TokenUtils.createToken("1-STUDENT", -1);
        Assertions.assertFalse(TokenUtils.verify(expired), "过期 token 不应通过校验");
    }
}
