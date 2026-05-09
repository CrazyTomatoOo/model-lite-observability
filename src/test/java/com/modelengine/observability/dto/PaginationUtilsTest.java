package com.modelengine.observability.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PaginationUtilsTest {

    private static Validator validator;
    private static ValidatorFactory factory;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }

    // ───────── Parsing ─────────

    @Test
    void parseSortNullDefaultsToInstanceNameDesc() {
        PaginationUtils.Sort sort = PaginationUtils.parseSort(null);
        assertEquals("instanceName", sort.field());
        assertEquals("desc", sort.direction());
        assertFalse(sort.isAscending());
    }

    @Test
    void parseSortBlankDefaultsToInstanceNameDesc() {
        PaginationUtils.Sort sort = PaginationUtils.parseSort("   ");
        assertEquals("instanceName", sort.field());
        assertEquals("desc", sort.direction());
    }

    @Test
    void parseSortWithFieldAndAsc() {
        PaginationUtils.Sort sort = PaginationUtils.parseSort("name,asc");
        assertEquals("name", sort.field());
        assertEquals("asc", sort.direction());
        assertTrue(sort.isAscending());
    }

    @Test
    void parseSortWithFieldOnlyDefaultsDesc() {
        PaginationUtils.Sort sort = PaginationUtils.parseSort("priority");
        assertEquals("priority", sort.field());
        assertEquals("desc", sort.direction());
    }

    @Test
    void parseSortInvalidDirectionDefaultsDesc() {
        PaginationUtils.Sort sort = PaginationUtils.parseSort("name,invalid");
        assertEquals("name", sort.field());
        assertEquals("desc", sort.direction());
    }

    // ───────── Validation: page ─────────

    @Test
    void pageZeroIsRejected() {
        PaginationRequest req = PaginationRequest.builder().page(0).size(20).build();
        Set<ConstraintViolation<PaginationRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "page=0 should be rejected by @Min(1)");
    }

    @Test
    void pageNegativeIsRejected() {
        PaginationRequest req = PaginationRequest.builder().page(-1).size(20).build();
        Set<ConstraintViolation<PaginationRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "page=-1 should be rejected by @Min(1)");
    }

    // ───────── Validation: size ─────────

    @Test
    void sizeZeroIsRejected() {
        PaginationRequest req = PaginationRequest.builder().page(1).size(0).build();
        Set<ConstraintViolation<PaginationRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "size=0 should be rejected by @Min(1)");
    }

    @Test
    void sizeNegativeIsRejected() {
        PaginationRequest req = PaginationRequest.builder().page(1).size(-1).build();
        Set<ConstraintViolation<PaginationRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "size=-1 should be rejected by @Min(1)");
    }

    @Test
    void sizeOverMaxIsRejected() {
        PaginationRequest req = PaginationRequest.builder().page(1).size(101).build();
        Set<ConstraintViolation<PaginationRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "size=101 should be rejected by @Max(100)");
    }

    // ───────── Validation: sortDirection ─────────

    @Test
    void sortDirectionInvalidIsRejected() {
        PaginationRequest req = PaginationRequest.builder().page(1).size(20)
                .sortDirection("invalid").build();
        Set<ConstraintViolation<PaginationRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "sortDirection=invalid should be rejected");
    }

    @Test
    void sortDirectionAscIsValid() {
        PaginationRequest req = PaginationRequest.builder().page(1).size(20)
                .sortDirection("asc").build();
        Set<ConstraintViolation<PaginationRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "sortDirection=asc should be valid");
    }

    // ───────── Default values ─────────

    @Test
    void defaultValuesAreValid() {
        PaginationRequest req = new PaginationRequest();
        Set<ConstraintViolation<PaginationRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "default request should be valid");
        assertEquals(1, req.getPage());
        assertEquals(20, req.getSize());
        assertEquals("desc", req.getSortDirection());
    }

    @Test
    void builderDefaultsAreValid() {
        PaginationRequest req = PaginationRequest.builder().build();
        Set<ConstraintViolation<PaginationRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "builder default request should be valid");
        assertEquals(1, req.getPage());
        assertEquals(20, req.getSize());
        assertEquals("desc", req.getSortDirection());
    }

    // ───────── Paginate: edge cases ─────────

    @Test
    void paginateEmptyList() {
        PaginationRequest req = PaginationRequest.builder().page(1).size(20).build();
        PageDTO<String> result = PaginationUtils.paginate(List.of(), req);
        assertTrue(result.getRecords().isEmpty());
        assertEquals(0, result.getTotal());
        assertEquals(0, result.getPages());
        assertEquals(1, result.getPage());
        assertEquals(20, result.getSize());
    }

    @Test
    void paginateNullList() {
        PaginationRequest req = PaginationRequest.builder().page(1).size(20).build();
        PageDTO<String> result = PaginationUtils.paginate(null, req);
        assertTrue(result.getRecords().isEmpty());
        assertEquals(0, result.getTotal());
    }

    @Test
    void paginateSingleRecord() {
        PaginationRequest req = PaginationRequest.builder().page(1).size(20).build();
        PageDTO<String> result = PaginationUtils.paginate(List.of("only"), req);
        assertEquals(1, result.getRecords().size());
        assertEquals("only", result.getRecords().getFirst());
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getPages());
    }

    @Test
    void paginateExactBoundary() {
        // 55 items, size=20, page=3 → 15 items (last page)
        List<Integer> items = java.util.stream.IntStream.rangeClosed(1, 55)
                .boxed().toList();
        PaginationRequest req = PaginationRequest.builder().page(3).size(20).build();
        PageDTO<Integer> result = PaginationUtils.paginate(items, req);
        assertEquals(15, result.getRecords().size());
        assertEquals(41, result.getRecords().getFirst()); // items 41-55
        assertEquals(55, result.getRecords().getLast());
        assertEquals(55, result.getTotal());
        assertEquals(3, result.getPages()); // ceil(55/20) = 3
    }

    @Test
    void paginateFirstPage() {
        List<Integer> items = java.util.stream.IntStream.rangeClosed(1, 55)
                .boxed().toList();
        PaginationRequest req = PaginationRequest.builder().page(1).size(20).build();
        PageDTO<Integer> result = PaginationUtils.paginate(items, req);
        assertEquals(20, result.getRecords().size());
        assertEquals(1, result.getRecords().getFirst());
        assertEquals(20, result.getRecords().getLast());
    }

    @Test
    void paginatePageBeyondTotalReturnsEmpty() {
        List<Integer> items = java.util.stream.IntStream.rangeClosed(1, 10)
                .boxed().toList();
        PaginationRequest req = PaginationRequest.builder().page(5).size(20).build();
        PageDTO<Integer> result = PaginationUtils.paginate(items, req);
        assertTrue(result.getRecords().isEmpty());
        assertEquals(10, result.getTotal());
        assertEquals(5, result.getPage());
    }

    // ───────── Paginate with sorting ─────────

    @Test
    void paginateWithComparatorSortsData() {
        List<String> items = List.of("zebra", "apple", "monkey", "banana");
        PaginationRequest req = PaginationRequest.builder().page(1).size(10).build();
        PageDTO<String> result = PaginationUtils.paginate(items, req, Comparator.naturalOrder());
        assertEquals(List.of("apple", "banana", "monkey", "zebra"), result.getRecords());
    }

    @Test
    void paginateWithComparatorRespectsPage() {
        List<Integer> items = java.util.stream.IntStream.rangeClosed(1, 10)
                .boxed().toList();
        PaginationRequest req = PaginationRequest.builder().page(2).size(3).build();
        // Reverse order: 10, 9, 8, 7, 6, 5, 4, 3, 2, 1
        // Page 2 (size 3): indices 3..5 → 7, 6, 5
        PageDTO<Integer> result = PaginationUtils.paginate(items, req, Comparator.reverseOrder());
        assertEquals(List.of(7, 6, 5), result.getRecords());
    }

    @Test
    void paginateNoComparatorPreservesOriginalOrder() {
        List<String> items = List.of("c", "a", "b");
        PaginationRequest req = PaginationRequest.builder().page(1).size(10).build();
        PageDTO<String> result = PaginationUtils.paginate(items, req);
        assertEquals(List.of("c", "a", "b"), result.getRecords());
    }
}
