package com.playvora.playvora_api.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {

    private List<T> records;

    private long count;

    private int totalPages;

    private int currentPage;

    private Integer prevPage;

    private Integer nextPage;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long unReadCount;

}
