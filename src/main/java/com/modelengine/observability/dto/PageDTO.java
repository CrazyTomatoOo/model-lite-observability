package com.modelengine.observability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic pagination DTO.
 *
 * @param <T> the type of data in the page
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageDTO<T> {

    private List<T> data;
    private Integer page;
    private Integer size;
    private Long total;
    private Integer pages;

    /**
     * Creates a PageDTO from the given parameters.
     *
     * @param data  the page content
     * @param page  the current page number (1-based)
     * @param size  the page size
     * @param total the total number of elements
     * @param <T>   the type of data
     * @return a new PageDTO instance
     */
    public static <T> PageDTO<T> of(List<T> data, int page, int size, long total) {
        int pages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return PageDTO.<T>builder()
                .data(data)
                .page(page)
                .size(size)
                .total(total)
                .pages(pages)
                .build();
    }

    /**
     * Creates a PageDTO from a PaginationRequest.
     *
     * @param data    the page content
     * @param request the pagination request
     * @param total   the total number of elements
     * @param <T>     the type of data
     * @return a new PageDTO instance
     */
    public static <T> PageDTO<T> of(List<T> data, PaginationRequest request, long total) {
        return of(data, request.getPage(), request.getSize(), total);
    }
}
