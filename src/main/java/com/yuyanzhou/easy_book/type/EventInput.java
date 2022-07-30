package com.yuyanzhou.easy_book.type;

import lombok.Data;

@Data
public class EventInput {
    private String title;
    private String description;
    private String startDate;
    private String endDate;
    private Integer pplLimit;
}
