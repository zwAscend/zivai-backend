package zw.co.zivai.core_backend.common.configs;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.hwc")
public class HuaweiOcrProperties {
    private String ak;
    private String sk;
    private String projectId;
    private String ocrEndpoint;
    private String host;
    private boolean forceTrailingSlash = true;
    private int httpTimeoutSeconds = 120;
    private final GeneralText generalText = new GeneralText();

    @Getter
    @Setter
    public static class GeneralText {
        private boolean detectDirection = true;
        private boolean quickMode = false;
        private long maxOriginalFileSizeBytes = 7L * 1024L * 1024L;
        private long maxEncodedImageBytes = 2_500_000L;
        private int maxImageWidth = 2200;
        private int maxImageHeight = 2200;
        private float jpegQuality = 0.72f;
        private float minJpegQuality = 0.50f;
        private int adaptiveResizePercent = 85;
        private int maxAdaptivePasses = 6;
        private int timeoutRetries = 1;
        private long retryBackoffMs = 600L;
        private String rasterOutputFormat = "jpg";
        private int pdfRenderDpi = 200;
        private int maxPdfPages = 50;
        private String pdfRenderImageFormat = "jpg";
        private List<String> allowedFormats = new ArrayList<>(List.of(
            "jpeg", "jpg", "png", "bmp", "gif", "tiff", "tif", "webp", "pcx", "ico", "pdf", "psd"
        ));
    }
}
