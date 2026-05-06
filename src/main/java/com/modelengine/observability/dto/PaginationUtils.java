package com.modelengine.observability.dto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * In-memory pagination utilities.
 * <p>
 * Provides manual pagination and sorting without Spring Data dependencies.
 */
public class PaginationUtils {

    /**
     * Sort specification parsed from a string.
     *
     * @param field     the field name to sort by
     * @param direction the sort direction ("asc" or "desc")
     */
    public record Sort(String field, String direction) {
        public boolean isAscending() {
            return "asc".equalsIgnoreCase(direction);
        }
    }

    private PaginationUtils() {
        // utility class
    }

    /**
     * Parses a sort parameter in {@code field,asc|desc} format.
     * <p>
     * Defaults to {@code instanceName,desc} when the input is null or blank.
     *
     * @param sortParam the sort parameter string (e.g. "instanceName,desc")
     * @return the parsed Sort record
     */
    public static Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return new Sort("instanceName", "desc");
        }
        String[] parts = sortParam.split(",");
        String field = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim() : "desc";
        if (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction)) {
            direction = "desc";
        }
        return new Sort(field, direction);
    }

    /**
     * Paginates a list in-memory using the given request.
     * <p>
     * No comparator means items are returned in their original order.
     *
     * @param items   the full list of items
     * @param request the pagination request (page, size)
     * @param <T>     the type of items
     * @return a PageDTO with the requested page of results
     */
    public static <T> PageDTO<T> paginate(List<T> items, PaginationRequest request) {
        return paginate(items, request, (Comparator<T>) null);
    }

    /**
     * Paginates and sorts a list in-memory.
     *
     * @param items      the full list of items
     * @param request    the pagination request (page, size)
     * @param comparator optional comparator for sorting (null = original order)
     * @param <T>        the type of items
     * @return a PageDTO with the requested page of results
     */
    public static <T> PageDTO<T> paginate(List<T> items, PaginationRequest request,
                                          Comparator<T> comparator) {
        List<T> working = new ArrayList<>(items != null ? items : List.of());

        if (comparator != null) {
            working.sort(comparator);
        }

        long total = working.size();
        int page = request.getPage();
        int size = request.getSize();
        int fromIndex = (page - 1) * size;

        if (fromIndex >= total) {
            return PageDTO.of(List.of(), page, size, total);
        }

        int toIndex = Math.min(fromIndex + size, (int) total);
        return PageDTO.of(working.subList(fromIndex, toIndex), page, size, total);
    }
}
