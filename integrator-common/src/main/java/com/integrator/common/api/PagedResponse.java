package com.integrator.common.api;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder(builderMethodName = "pagedBuilder")
@EqualsAndHashCode(callSuper = true)
public class PagedResponse<T> extends ApiResponse<List<T>> {
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public static <T> PagedResponse<T> success(List<T> data,
            int pageNumber,
            int pageSize,
            long totalElements,
            int totalPages,
            boolean last) {

        return PagedResponse.<T>pagedBuilder()
                .success(true)
                .data(data)
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(last)
                .build();
    }

    public static <T> PagedResponse<T> success(List<T> data,
            int pageNumber,
            int pageSize,
            long totalElements,
            int totalPages,
            boolean last,
            String correlationId) {

        return PagedResponse.<T>pagedBuilder()
                .success(true)
                .data(data)
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(last)
                .correlationId(correlationId)
                .build();
    }
}
