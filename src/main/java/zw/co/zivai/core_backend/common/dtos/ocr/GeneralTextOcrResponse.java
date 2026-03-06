package zw.co.zivai.core_backend.common.dtos.ocr;

import lombok.Data;

@Data
public class GeneralTextOcrResponse {
    private String fileName;
    private String fileFormat;
    private Integer pageNumber;
    private Integer totalPages;
    private String engine;
    private String fullText;
    private Double averageConfidence;
    private Integer wordsBlockCount;
}
