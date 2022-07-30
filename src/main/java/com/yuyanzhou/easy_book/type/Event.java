package com.yuyanzhou.easy_book.type;

import com.yuyanzhou.easy_book.entity.EventEntity;
import com.yuyanzhou.easy_book.util.DateUtil;
import lombok.Data;

@Data
public class Event {
    private String id;
    private String title;
    private String description;
    private String startDate;
    private String endDate;
    private Integer creatorId;
    private User creator;
    private Integer pplLimit;
    private Integer pplCount;

    public static Event fromEntity(EventEntity eventEntity) {
        Event event = new Event();
        event.setId(eventEntity.getId().toString());
        event.setTitle(eventEntity.getTitle());
        event.setDescription(eventEntity.getDescription());
        event.setStartDate(DateUtil.formatDateInISOString(eventEntity.getStartDate()));
        event.setEndDate(DateUtil.formatDateInISOString(eventEntity.getEndDate()));
        event.setPplCount(eventEntity.getPplCount());
        event.setPplLimit(eventEntity.getPplLimit());
        event.setCreatorId(eventEntity.getCreatorId());
        return event;
    }
}
