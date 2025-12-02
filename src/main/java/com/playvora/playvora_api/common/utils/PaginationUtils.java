package com.playvora.playvora_api.common.utils;

import com.playvora.playvora_api.common.dto.PaginatedResponse;
import org.springframework.data.domain.Page;

public class PaginationUtils {

    /**
     * Converts a Spring Data Page to PaginatedResponse
     * 
     * @param page The Spring Data Page object
     * @param <T> The type of items in the page
     * @return PaginatedResponse containing the paginated data
     */
    public static <T> PaginatedResponse<T> toPaginatedResponse(Page<T> page) {
        int currentPage = page.getNumber();
        int totalPages = page.getTotalPages();
        
        return PaginatedResponse.<T>builder()
                .records(page.getContent())
                .count(page.getTotalElements())
                .totalPages(totalPages)
                .currentPage(currentPage)
                .prevPage(currentPage > 0 ? currentPage - 1 : null)
                .nextPage(currentPage < totalPages - 1 ? currentPage + 1 : null)
                .build();
    }
}


