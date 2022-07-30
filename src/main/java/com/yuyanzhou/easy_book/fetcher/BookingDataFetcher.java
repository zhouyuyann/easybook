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
import com.yuyanzhou.easy_book.type.Booking;
import com.yuyanzhou.easy_book.type.Event;
import com.yuyanzhou.easy_book.type.User;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@DgsComponent
@RequiredArgsConstructor
public class BookingDataFetcher {
    private final BookingEntityMapper bookingEntityMapper;
    private final EventEntityMapper eventEntityMapper;
    private final UserEntityMapper userEntityMapper;

    @DgsQuery
    public List<Booking> bookings(DataFetchingEnvironment dfe) {
        AuthContext authContext = DgsContext.getCustomContext(dfe);
        authContext.auth();

        QueryWrapper<BookingEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BookingEntity::getUserId, authContext.getUserEntity().getId());
        List<Booking> bookings = bookingEntityMapper.selectList(queryWrapper)
                .stream()
                .map(Booking::fromEntity)
                .collect(Collectors.toList());
        return bookings;
    }

    @DgsMutation
    public Event cancelBooking(@InputArgument(name = "bookingId") String bookingIdString,
                               DataFetchingEnvironment dfe) {
        AuthContext authContext = DgsContext.getCustomContext(dfe);
        authContext.auth();

        Integer bookingId = Integer.parseInt(bookingIdString);
        BookingEntity bookingEntity = bookingEntityMapper.selectById(bookingId);
        if (bookingEntity == null) {
            throw new RuntimeException(String.format("Booking with id %s does not exist", bookingIdString));
        }

        Integer userId = bookingEntity.getUserId();
        UserEntity userEntity = authContext.getUserEntity();
        if (!userEntity.getId().equals(userId)) {
            throw new RuntimeException("You are not allowed to cancel other people's booking!");
        }
        bookingEntityMapper.deleteById(bookingId);

        Integer eventId = bookingEntity.getEventId();
        EventEntity eventEntity = eventEntityMapper.selectById(eventId);
        eventEntity.setPplCount(eventEntity.getPplCount()-1);
        eventEntityMapper.updateById(eventEntity);
        Event event = Event.fromEntity(eventEntity);
        return event;
    }

    @DgsMutation
    public Booking bookEvent(@InputArgument String eventId, DataFetchingEnvironment dfe) {
        AuthContext authContext = DgsContext.getCustomContext(dfe);
        authContext.auth();

        UserEntity userEntity = authContext.getUserEntity();
        EventEntity eventEntity = eventEntityMapper.selectById(Integer.parseInt(eventId));

        List<Booking> bookings = this.bookings(dfe);
        for (Booking booking : bookings) {
            if (booking.getEventId() == Integer.parseInt(eventId)){
                throw new RuntimeException("user already book this event");
            }
        }

        BookingEntity bookingEntity = new BookingEntity();
        Integer pplCount = eventEntity.getPplCount();
        Integer pplLimit = eventEntity.getPplLimit();
        if (pplCount+1 <= pplLimit) {
            bookingEntity.setUserId(userEntity.getId());
            bookingEntity.setEventId(Integer.parseInt(eventId));
            bookingEntity.setCreatedAt(new Date());
            bookingEntity.setUpdatedAt(new Date());

            bookingEntityMapper.insert(bookingEntity);
            eventEntity.setPplCount(pplCount+1);
            eventEntityMapper.updateById(eventEntity);
        }

        Booking booking = Booking.fromEntity(bookingEntity);

        return booking;
    }

    @DgsData(parentType = "Booking", field = "user")
    public User user(DgsDataFetchingEnvironment dfe) {
        Booking booking = dfe.getSource();
        UserEntity userEntity = userEntityMapper.selectById(booking.getUserId());
        User user = User.fromEntity(userEntity);
        return user;
    }


    @DgsData(parentType = "Booking", field = "event")
    public Event event(DgsDataFetchingEnvironment dfe) {
        Booking booking = dfe.getSource();
        EventEntity eventEntity = eventEntityMapper.selectById(booking.getEventId());
        Event event = Event.fromEntity(eventEntity);
        return event;
    }
}
