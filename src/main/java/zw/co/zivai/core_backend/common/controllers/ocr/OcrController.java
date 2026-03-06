package zw.co.zivai.core_backend.common.controllers.ocr;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.common.dtos.ocr.GeneralTextOcrResponse;
import zw.co.zivai.core_backend.common.services.ocr.HuaweiOcrService;

@RestController
@RequestMapping({ "/api/ocr", "/ocr" })
@RequiredArgsConstructor
public class OcrController {
    private final HuaweiOcrService huaweiOcrService;

    @PostMapping(value = "/general-text", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<GeneralTextOcrResponse> recognizeGeneralText(
        @RequestPart(value = "files", required = false) List<MultipartFile> files,
        @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        List<MultipartFile> inputFiles = new ArrayList<>();
        if (files != null) {
            inputFiles.addAll(files);
        }
        if (file != null) {
            inputFiles.add(file);
        }
        return huaweiOcrService.recognizeGeneralText(inputFiles);
    }
}
