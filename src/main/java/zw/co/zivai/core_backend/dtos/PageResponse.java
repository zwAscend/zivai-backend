package zw.co.zivai.core_backend.dtos;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PageResponse<T> {
    List<T> items;
    int page;
    int size;
    long totalItems;
    int totalPages;
}
