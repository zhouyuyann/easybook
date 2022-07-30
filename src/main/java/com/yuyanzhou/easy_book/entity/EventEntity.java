package com.yuyanzhou.easy_book.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyanzhou.easy_book.type.EventInput;
import com.yuyanzhou.easy_book.util.DateUtil;
import lombok.Data;

import java.util.Date;

@Data
@TableName(value = "tb_event")
public class EventEntity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String title;
    private String description;
    private Date startDate;
    private Date endDate;
    private Integer creatorId;
    private Integer pplLimit;
    private Integer pplCount;

    public static EventEntity fromEventInput(EventInput input) {
        if (DateUtil.convertISOStringToDate(input.getEndDate()).before(DateUtil.convertISOStringToDate(input.getStartDate()))){
            throw new IllegalArgumentException("invalid date");
        }
        EventEntity eventEntity = new EventEntity();
        eventEntity.setTitle(input.getTitle());
        eventEntity.setDescription(input.getDescription());
        eventEntity.setStartDate(DateUtil.convertISOStringToDate(input.getStartDate()));
        eventEntity.setEndDate(DateUtil.convertISOStringToDate(input.getEndDate()));
        eventEntity.setPplLimit(input.getPplLimit());
        eventEntity.setPplCount(0);
        return eventEntity;
    }
}
