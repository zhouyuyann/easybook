package com.yuyanzhou.easy_book.custom;


import com.yuyanzhou.easy_book.entity.UserEntity;
import lombok.Data;

@Data
public class AuthContext {
    private UserEntity userEntity;
    private boolean tokenInvalid;

    public void auth(){
        if (tokenInvalid) {
            throw new RuntimeException("invalid token");
        }
        if (userEntity == null) {
            throw new RuntimeException("please login");
        }
    }
}
