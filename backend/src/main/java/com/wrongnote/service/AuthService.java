package com.wrongnote.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrongnote.config.WechatConfig;
import com.wrongnote.entity.User;
import com.wrongnote.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final WechatConfig wechatConfig;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 小程序登录：用 code 换 openid，自动注册/返回 userId
     */
    public Map<String, Object> login(String code, String nickname, String avatarUrl) {
        // 1. code2Session
        String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                wechatConfig.getAppId(), wechatConfig.getAppSecret(), code);

        String resp;
        try {
            resp = restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            log.error("调用微信 code2Session 失败", e);
            throw new RuntimeException("微信登录失败: " + e.getMessage());
        }

        try {
            JsonNode node = objectMapper.readTree(resp);
            if (node.has("errcode") && node.get("errcode").asInt() != 0) {
                throw new RuntimeException("微信登录失败: " + node.get("errmsg").asText());
            }
            String openid = node.get("openid").asText();

            // 2. 查或建 user
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getOpenid, openid);
            User user = userMapper.selectOne(wrapper);

            if (user == null) {
                user = new User();
                user.setOpenid(openid);
                user.setNickname(nickname);
                user.setAvatarUrl(avatarUrl);
                userMapper.insert(user);
                log.info("新用户注册, userId={}, openid={}", user.getId(), openid);
            }

            return Map.of(
                    "userId", user.getId(),
                    "openid", user.getOpenid(),
                    "nickname", user.getNickname() != null ? user.getNickname() : ""
            );
        } catch (Exception e) {
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException("解析微信登录响应失败: " + resp, e);
        }
    }
}
