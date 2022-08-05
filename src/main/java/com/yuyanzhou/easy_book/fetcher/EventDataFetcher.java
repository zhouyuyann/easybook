package com.yuyanzhou.easy_book.fetcher;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.netflix.graphql.dgs.*;
import com.netflix.graphql.dgs.context.DgsContext;
import com.yuyanzhou.easy_book.custom.AuthContext;
import com.yuyanzhou.easy_book.entity.BookingEntity;
import com.yuyanzhou.easy_book.entity.EventEntity;
import com.yuyanzhou.easy_book.entity.UserEntity;
import com.yuyanzhou.easy_book.fetcher.dataloader.BookerDataLoader;
import com.yuyanzhou.easy_book.fetcher.dataloader.CreatorsDataLoader;
import com.yuyanzhou.easy_book.mapper.EventEntityMapper;
import com.yuyanzhou.easy_book.mapper.UserEntityMapper;
import com.yuyanzhou.easy_book.mapper.BookingEntityMapper;
import com.yuyanzhou.easy_book.type.Event;
import com.yuyanzhou.easy_book.type.EventInput;
import com.yuyanzhou.easy_book.type.User;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.DataLoader;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@DgsComponent
@RequiredArgsConstructor
public class EventDataFetcher {

    private final EventEntityMapper eventEntityMapper;
    private final UserEntityMapper userEntityMapper;
    private final BookingEntityMapper bookingEntityMapper;

    @DgsQuery
    public List<Event> events() {
        QueryWrapper<EventEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().apply("start_date >= current_timestamp");
        List<EventEntity> eventEntityList = eventEntityMapper.selectList(queryWrapper);
        List<Event> eventList = eventEntityList.stream()
                .map(Event::fromEntity).collect(Collectors.toList());

        return eventList;
    }

    @DgsMutation
    public Event createEvent(@InputArgument(name = "eventInput") EventInput input, DataFetchingEnvironment dfe) {
        AuthContext authContext = DgsContext.getCustomContext(dfe);
        authContext.auth();

        EventEntity newEventEntity = EventEntity.fromEventInput(input);
        log.info(Boolean.toString(newEventEntity.getStartDate().before(new Date())));
        newEventEntity.setCreatorId(authContext.getUserEntity().getId());

        eventEntityMapper.insert(newEventEntity);

        Event newEvent = Event.fromEntity(newEventEntity);

        return newEvent;
    }

    @DgsMutation
    public Event deleteEvent(@InputArgument(name = "eventId") String eventIdString,
                               DataFetchingEnvironment dfe) {
        AuthContext authContext = DgsContext.getCustomContext(dfe);
        authContext.auth();

        Integer eventId = Integer.parseInt(eventIdString);
        EventEntity eventEntity = eventEntityMapper.selectById(eventId);
        if (eventEntity == null) {
            throw new RuntimeException(String.format("event with id %s does not exist", eventIdString));
        }

        Integer userId = eventEntity.getCreatorId();
        UserEntity userEntity = authContext.getUserEntity();
        if (!userEntity.getId().equals(userId)) {
            throw new RuntimeException("You are not allowed to delete other people's event");
        }
        QueryWrapper<BookingEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BookingEntity::getEventId, eventId);
        List<BookingEntity> bookingEntityList = bookingEntityMapper.selectList(queryWrapper);
        for (BookingEntity bookingEntity : bookingEntityList){
            bookingEntityMapper.deleteById(bookingEntity.getId());
        }
        eventEntityMapper.deleteById(eventId);
        Event event = Event.fromEntity(eventEntity);
        return event;
    }

    @DgsData(parentType = "Event", field = "creator")
    public CompletableFuture<User> creator(DgsDataFetchingEnvironment dfe) {
        Event event = dfe.getSource();
        log.info("Fetching creator wit id: {}", event.getCreatorId());
        DataLoader<Integer, User> dataLoader = dfe.getDataLoader(CreatorsDataLoader.class);

        return dataLoader.load(event.getCreatorId());
    }

     @DgsData(parentType = "Event", field = "booker")
    public CompletableFuture<User> booker(DgsDataFetchingEnvironment dfe) {
        Event event = dfe.getSource();
        log.info("Fetching event wit id: {}", event.getId());

        DataLoader<Integer, User> dataLoader = dfe.getDataLoader(BookerDataLoader.class);

        return dataLoader.load(Integer.parseInt(event.getId()));
    }



}
