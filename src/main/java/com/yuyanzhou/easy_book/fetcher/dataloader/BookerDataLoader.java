package com.yuyanzhou.easy_book.fetcher.dataloader;


import com.netflix.graphql.dgs.DgsDataLoader;
import com.yuyanzhou.easy_book.mapper.BookingEntityMapper;
import com.yuyanzhou.easy_book.mapper.UserEntityMapper;
import com.yuyanzhou.easy_book.type.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.BatchLoader;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@DgsDataLoader(name="booker")
@RequiredArgsConstructor
@Slf4j
public class BookerDataLoader implements BatchLoader<Integer, User> {
    private final BookingEntityMapper bookingEntityMapper;
    private final UserEntityMapper userEntityMapper;

    @Override
    public CompletionStage<List<User>> load(List<Integer> eventIds) {
        List<Integer> userIds = bookingEntityMapper.selectBatchIds(eventIds)
                        .stream().map(bookingEntity ->  bookingEntity.getUserId())
                        .collect(Collectors.toList());

        return CompletableFuture.supplyAsync(
                () -> userEntityMapper.selectBatchIds(userIds)
                        .stream().map(userEntity ->  User.fromEntity(userEntity))
                        .collect(Collectors.toList()));
    }
}
