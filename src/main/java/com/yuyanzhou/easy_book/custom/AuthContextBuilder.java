package com.yuyanzhou.easy_book.custom;


import com.netflix.graphql.dgs.context.DgsCustomContextBuilder;
import com.netflix.graphql.dgs.context.DgsCustomContextBuilderWithRequest;
import com.yuyanzhou.easy_book.entity.UserEntity;
import com.yuyanzhou.easy_book.mapper.UserEntityMapper;
import com.yuyanzhou.easy_book.util.TokenUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@Data
@AllArgsConstructor
@Component
@Slf4j
public class AuthContextBuilder implements DgsCustomContextBuilderWithRequest {
    private final UserEntityMapper userEntityMapper;
    static String AUTHORIZATION_HEADER = "Authorization";


    @Override
    public Object build(@Nullable Map map, @Nullable HttpHeaders httpHeaders, @Nullable WebRequest webRequest) {
        log.info("Building auth context...");
        AuthContext authContext = new AuthContext();
        if (!httpHeaders.containsKey(AUTHORIZATION_HEADER)) {
            log.info("User is not authenticated");
            return authContext;
        }
        String authorization = httpHeaders.getFirst(AUTHORIZATION_HEADER);
        String token = authorization.replace("Bearer ", "");
        Integer userId;
        try {
            userId = TokenUtil.verifyToken(token);
        } catch (Exception e) {
            log.warn("failed token validation",e);
            authContext.setTokenInvalid(true);
            return authContext;
        }
        UserEntity userEntity = userEntityMapper.selectById(userId);
        if (userEntity == null){
            authContext.setTokenInvalid(true);
            return authContext;
        }
        authContext.setUserEntity(userEntity);
        log.info("Build auth context complete...");
        return authContext;


    }
}
