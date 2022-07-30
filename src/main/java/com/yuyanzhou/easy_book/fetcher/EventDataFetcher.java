package com.yuyanzhou.easy_book.fetcher;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.netflix.graphql.dgs.*;
import com.netflix.graphql.dgs.context.DgsContext;
import com.yuyanzhou.easy_book.custom.AuthContext;
import com.yuyanzhou.easy_book.entity.BookingEntity;
import com.yuyanzhou.easy_book.entity.EventEntity;
import com.yuyanzhou.easy_book.fetcher.dataloader.CreatorsDataLoader;
import com.yuyanzhou.easy_book.mapper.EventEntityMapper;
import com.yuyanzhou.easy_book.mapper.UserEntityMapper;
import com.yuyanzhou.easy_book.type.Event;
import com.yuyanzhou.easy_book.type.EventInput;
import com.yuyanzhou.easy_book.type.User;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.DataLoader;

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

    @DgsQuery
    public List<Event> events() {
        QueryWrapper<EventEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().gt(EventEntity::getStartDate, new Date());

        List<EventEntity> eventEntityList = eventEntityMapper.selectList(new QueryWrapper<>());
        List<Event> eventList = eventEntityList.stream()
                .map(Event::fromEntity).collect(Collectors.toList());

        return eventList;
    }

    @DgsMutation
    public Event createEvent(@InputArgument(name = "eventInput") EventInput input, DataFetchingEnvironment dfe) {
        AuthContext authContext = DgsContext.getCustomContext(dfe);
        authContext.auth();

        EventEntity newEventEntity = EventEntity.fromEventInput(input);
        newEventEntity.setCreatorId(authContext.getUserEntity().getId());

        eventEntityMapper.insert(newEventEntity);

        Event newEvent = Event.fromEntity(newEventEntity);

        return newEvent;
    }

    @DgsData(parentType = "Event", field = "creator")
    public CompletableFuture<User> creator(DgsDataFetchingEnvironment dfe) {
        Event event = dfe.getSource();
        log.info("Fetching creator wit id: {}", event.getCreatorId());
        DataLoader<Integer, User> dataLoader = dfe.getDataLoader(CreatorsDataLoader.class);

        return dataLoader.load(event.getCreatorId());
    }

}
