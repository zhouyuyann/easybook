package com.yuyanzhou.easy_book.fetcher;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.netflix.graphql.dgs.*;
import com.netflix.graphql.dgs.context.DgsContext;
import com.yuyanzhou.easy_book.custom.AuthContext;
import com.yuyanzhou.easy_book.entity.BookingEntity;
import com.yuyanzhou.easy_book.entity.EventEntity;
import com.yuyanzhou.easy_book.entity.UserEntity;
import com.yuyanzhou.easy_book.mapper.BookingEntityMapper;
import com.yuyanzhou.easy_book.mapper.EventEntityMapper;
import com.yuyanzhou.easy_book.mapper.UserEntityMapper;
import com.yuyanzhou.easy_book.type.*;
import com.yuyanzhou.easy_book.util.TokenUtil;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@DgsComponent
@RequiredArgsConstructor
public class UserDataFetcher {
    private final UserEntityMapper userEntityMapper;
    private final EventEntityMapper eventEntityMapper;
    private final BookingEntityMapper bookingEntityMapper;
    private final PasswordEncoder passwordEncoder;

    @DgsQuery
    public List<User> users(DataFetchingEnvironment dfe) {
        AuthContext authContext = DgsContext.getCustomContext(dfe);
        authContext.auth();
        List<UserEntity> userEntityList = userEntityMapper.selectList(null);
        List<User> userList = userEntityList.stream()
                .map(User::fromEntity)
                .collect(Collectors.toList());
        return userList;
    }

    @DgsMutation
    public User createUser(@InputArgument UserInput userInput) {
        ensureUserNotExists(userInput);

        UserEntity newUserEntity = new UserEntity();
        newUserEntity.setEmail(userInput.getEmail());
        newUserEntity.setPassword(passwordEncoder.encode(userInput.getPassword()));

        userEntityMapper.insert(newUserEntity);

        User newUser = User.fromEntity(newUserEntity);
        newUser.setPassword(null);

        return newUser;
    }

    @DgsQuery
    public AuthData login(@InputArgument LoginInput loginInput) {
        UserEntity userEntity = this.findUserByEmail(loginInput.getEmail());
        if (userEntity == null) {
            throw new RuntimeException("account not exists");
        }
        boolean match = passwordEncoder.matches(loginInput.getPassword(), userEntity.getPassword());
        if (!match) {
            throw new RuntimeException("password incorrect");
        }

        String token = TokenUtil.signToken(userEntity.getId(), 1);

        AuthData authData = new AuthData()
                .setUserId(userEntity.getId())
                .setToken(token)
                .setTokenExpiration(1);

        return authData;
    }


    @DgsData(parentType = "User", field = "bookings")
    public List<Booking> bookings(DgsDataFetchingEnvironment dfe) {
        User user = dfe.getSource();
        QueryWrapper<BookingEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BookingEntity::getUserId, user.getId());
        List<BookingEntity> bookingEntityList = bookingEntityMapper.selectList(queryWrapper);
        List<Booking> bookings = bookingEntityList.stream()
                .map(Booking::fromEntity)
                .collect(Collectors.toList());
        return bookings;
    }

    @DgsData(parentType = "User", field = "createdEvents")
    public List<Event> createdEvents(DgsDataFetchingEnvironment dfe) {
        User user = dfe.getSource();
        QueryWrapper<EventEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(EventEntity::getCreatorId, user.getId());
        List<EventEntity> eventEntityList = eventEntityMapper.selectList(queryWrapper);
        List<Event> eventList = eventEntityList.stream()
                .map(Event::fromEntity)
                .collect(Collectors.toList());
        return eventList;
    }

    private void ensureUserNotExists(UserInput userInput) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(UserEntity::getEmail, userInput.getEmail());
        if (userEntityMapper.selectCount(queryWrapper) >= 1) {
            throw new RuntimeException("email already exists");
        }
    }

    private UserEntity findUserByEmail(String email) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(UserEntity::getEmail, email);
        return userEntityMapper.selectOne(queryWrapper);
    }
}
