package zw.co.zivai.core_backend.common.services.ocr;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.exception.ConnectionException;
import com.huaweicloud.sdk.core.exception.RequestTimeoutException;
import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.huaweicloud.sdk.core.http.HttpConfig;
import com.huaweicloud.sdk.ocr.v1.OcrClient;
import com.huaweicloud.sdk.ocr.v1.model.GeneralTextRequestBody;
import com.huaweicloud.sdk.ocr.v1.model.RecognizeGeneralTextRequest;
import com.huaweicloud.sdk.ocr.v1.model.RecognizeGeneralTextResponse;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.common.configs.HuaweiOcrProperties;
import zw.co.zivai.core_backend.common.dtos.ocr.GeneralTextOcrResponse;
import zw.co.zivai.core_backend.common.exceptions.BadRequestException;
import zw.co.zivai.core_backend.common.exceptions.UpstreamServiceTimeoutException;
import zw.co.zivai.core_backend.common.exceptions.UpstreamServiceUnavailableException;

@Service
@RequiredArgsConstructor
public class HuaweiOcrService {
    private static final String OCR_ENGINE = "huawei-ocr-general-text";

    private final HuaweiOcrProperties properties;
    private final ObjectMapper objectMapper;

    private volatile OcrClient ocrClient;

    public List<GeneralTextOcrResponse> recognizeGeneralText(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("At least one file is required.");
        }

        List<MultipartFile> nonEmptyFiles = files.stream()
            .filter(Objects::nonNull)
            .filter(f -> !f.isEmpty())
            .toList();

        if (nonEmptyFiles.isEmpty()) {
            throw new BadRequestException("At least one non-empty file is required.");
        }

        List<GeneralTextOcrResponse> responses = new ArrayList<>();
        for (MultipartFile file : nonEmptyFiles) {
            responses.addAll(processSingleFile(file));
        }
        return responses;
    }

    private List<GeneralTextOcrResponse> processSingleFile(MultipartFile file) {
        validateFileSize(file);
        Set<String> allowedFormats = normalizeAllowedFormats(properties.getGeneralText().getAllowedFormats());
        String extension = resolveFileExtension(file.getOriginalFilename());

        if (extension == null || !allowedFormats.contains(extension)) {
            throw new BadRequestException(
                "Unsupported file format. Allowed formats: " + String.join(", ", allowedFormats)
            );
        }

        if ("pdf".equals(extension)) {
            return processPdf(file);
        }

        String base64 = prepareRasterBase64(file, extension);
        GeneralTextOcrResponse response = recognizeBase64(
            base64,
            null,
            safeFileName(file),
            extension,
            1,
            1
        );
        return List.of(response);
    }

    private List<GeneralTextOcrResponse> processPdf(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream(); PDDocument document = PDDocument.load(inputStream)) {
            int totalPages = document.getNumberOfPages();
            if (totalPages <= 0) {
                throw new BadRequestException("Uploaded PDF has no pages.");
            }
            int maxPdfPages = properties.getGeneralText().getMaxPdfPages();
            if (maxPdfPages > 0 && totalPages > maxPdfPages) {
                throw new BadRequestException(
                    "PDF has " + totalPages + " pages. Max allowed pages: " + maxPdfPages + "."
                );
            }

            PDFRenderer renderer = new PDFRenderer(document);
            List<GeneralTextOcrResponse> responses = new ArrayList<>(totalPages);
            float renderDpi = Math.max(72, properties.getGeneralText().getPdfRenderDpi());
            String renderImageFormat = normalizePdfRenderImageFormat(properties.getGeneralText().getPdfRenderImageFormat());

            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, renderDpi, ImageType.RGB);
                BufferedImage optimized = downscaleIfNeeded(image);
                String base64 = encodeBufferedImageToBase64(optimized, renderImageFormat);

                GeneralTextOcrResponse response = recognizeBase64(
                    base64,
                    null,
                    safeFileName(file),
                    "pdf",
                    pageIndex + 1,
                    totalPages
                );
                responses.add(response);
            }
            return responses;
        } catch (IOException e) {
            throw new BadRequestException("Failed to parse PDF file.");
        }
    }

    private GeneralTextOcrResponse recognizeBase64(
        String imageBase64,
        String imageUrl,
        String fileName,
        String fileFormat,
        Integer pageNumber,
        Integer totalPages
    ) {
        GeneralTextRequestBody body = new GeneralTextRequestBody()
            .withQuickMode(properties.getGeneralText().isQuickMode())
            .withDetectDirection(properties.getGeneralText().isDetectDirection());

        if (imageBase64 != null) {
            body.withImage(imageBase64);
        } else if (imageUrl != null) {
            body.withUrl(imageUrl);
        } else {
            throw new BadRequestException("Either imageBase64 or imageUrl is required.");
        }

        RecognizeGeneralTextRequest sdkRequest = new RecognizeGeneralTextRequest().withBody(body);

        try {
            RecognizeGeneralTextResponse sdkResponse = invokeWithRetry(sdkRequest);
            JsonNode raw = objectMapper.valueToTree(sdkResponse);
            return mapResponse(raw, fileName, fileFormat, pageNumber, totalPages);
        } catch (ServiceResponseException e) {
            int statusCode = e.getHttpStatusCode();
            String baseMessage = "Huawei OCR error"
                + (statusCode > 0 ? " (" + statusCode + ")" : "")
                + (e.getErrorCode() != null ? " [" + e.getErrorCode() + "]" : "")
                + (e.getErrorMsg() != null ? ": " + e.getErrorMsg() : "");
            if (statusCode >= 400 && statusCode < 500) {
                throw new BadRequestException(baseMessage);
            }
            throw new UpstreamServiceUnavailableException(baseMessage, e);
        }
    }

    private RecognizeGeneralTextResponse invokeWithRetry(RecognizeGeneralTextRequest request)
        throws ServiceResponseException {
        int retries = Math.max(0, properties.getGeneralText().getTimeoutRetries());
        int maxAttempts = retries + 1;
        long backoffMs = Math.max(0L, properties.getGeneralText().getRetryBackoffMs());

        ConnectionException lastConnectionException = null;
        RequestTimeoutException lastTimeoutException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return getClient().recognizeGeneralText(request);
            } catch (ConnectionException ex) {
                lastConnectionException = ex;
                if (attempt == maxAttempts) {
                    break;
                }
                sleepBeforeRetry(backoffMs, attempt);
            } catch (RequestTimeoutException ex) {
                lastTimeoutException = ex;
                if (attempt == maxAttempts) {
                    break;
                }
                sleepBeforeRetry(backoffMs, attempt);
            }
        }

        if (lastTimeoutException != null) {
            throw new UpstreamServiceTimeoutException(
                "Huawei OCR request timed out. Try a smaller file or fewer pages.",
                lastTimeoutException
            );
        }

        throw new UpstreamServiceUnavailableException(
            "Huawei OCR service is currently unreachable.",
            lastConnectionException
        );
    }

    private void sleepBeforeRetry(long backoffMs, int attempt) {
        if (backoffMs <= 0) {
            return;
        }
        long delay = backoffMs * attempt;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private OcrClient getClient() {
        OcrClient client = ocrClient;
        if (client != null) {
            return client;
        }
        synchronized (this) {
            if (ocrClient == null) {
                ocrClient = buildClient();
            }
            return ocrClient;
        }
    }

    private OcrClient buildClient() {
        String ak = requireProperty(properties.getAk(), "app.hwc.ak");
        String sk = requireProperty(properties.getSk(), "app.hwc.sk");
        String projectId = requireProperty(properties.getProjectId(), "app.hwc.project-id");
        String endpoint = resolveEndpoint();

        BasicCredentials credentials = new BasicCredentials()
            .withAk(ak)
            .withSk(sk)
            .withProjectId(projectId);

        HttpConfig httpConfig = HttpConfig.getDefaultHttpConfig();
        if (properties.getHttpTimeoutSeconds() > 0) {
            httpConfig.withTimeout(properties.getHttpTimeoutSeconds());
        }

        return OcrClient.newBuilder()
            .withHttpConfig(httpConfig)
            .withCredential(credentials)
            .withEndpoint(endpoint)
            .build();
    }

    private String resolveEndpoint() {
        String endpoint = trimToNull(properties.getOcrEndpoint());
        if (endpoint == null) {
            String host = requireProperty(properties.getHost(), "app.hwc.host");
            endpoint = "https://" + host;
        }
        if (properties.isForceTrailingSlash() && !endpoint.endsWith("/")) {
            endpoint = endpoint + "/";
        }

        // The Huawei SDK already prefixes API paths with "/" (for example "/v2/...").
        // Keep endpoint slash-free at the end to avoid "//v2/..." requests.
        while (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint;
    }

    private void validateFileSize(MultipartFile file) {
        long maxSizeBytes = properties.getGeneralText().getMaxOriginalFileSizeBytes();
        if (maxSizeBytes > 0 && file.getSize() > maxSizeBytes) {
            throw new BadRequestException("File is too large. Max allowed size is " + maxSizeBytes + " bytes.");
        }
    }

    private String encodeToBase64(MultipartFile file) {
        byte[] rawBytes = readBytes(file);
        validateEncodedPayloadSize(rawBytes.length);
        return Base64.getEncoder().encodeToString(rawBytes);
    }

    private String prepareRasterBase64(MultipartFile file, String extension) {
        BufferedImage source = readImage(file);
        if (source == null) {
            return encodeToBase64(file);
        }
        BufferedImage optimized = downscaleIfNeeded(source);
        String outputFormat = normalizeRasterOutputFormat(properties.getGeneralText().getRasterOutputFormat(), extension);
        return encodeBufferedImageToBase64(optimized, outputFormat);
    }

    private BufferedImage readImage(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return ImageIO.read(inputStream);
        } catch (IOException e) {
            return null;
        }
    }

    private BufferedImage downscaleIfNeeded(BufferedImage image) {
        int maxWidth = Math.max(64, properties.getGeneralText().getMaxImageWidth());
        int maxHeight = Math.max(64, properties.getGeneralText().getMaxImageHeight());

        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= maxWidth && height <= maxHeight) {
            return image;
        }

        double widthRatio = (double) maxWidth / width;
        double heightRatio = (double) maxHeight / height;
        double scale = Math.min(widthRatio, heightRatio);

        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        return resizeImage(image, targetWidth, targetHeight);
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Failed to read uploaded file.");
        }
    }

    private String encodeBufferedImageToBase64(BufferedImage image, String format) {
        byte[] encoded = encodeBufferedImageWithAdaptiveConstraints(image, format);
        return Base64.getEncoder().encodeToString(encoded);
    }

    private byte[] encodeBufferedImageWithAdaptiveConstraints(BufferedImage image, String format) {
        long maxEncodedImageBytes = properties.getGeneralText().getMaxEncodedImageBytes();
        int maxPasses = Math.max(1, properties.getGeneralText().getMaxAdaptivePasses());
        int resizePercent = Math.max(60, Math.min(95, properties.getGeneralText().getAdaptiveResizePercent()));

        String workingFormat = format;
        BufferedImage workingImage = image;
        float quality = properties.getGeneralText().getJpegQuality();
        float minQuality = Math.max(0.35f, Math.min(0.95f, properties.getGeneralText().getMinJpegQuality()));
        byte[] encoded = null;

        for (int pass = 1; pass <= maxPasses; pass++) {
            encoded = encodeBufferedImage(workingImage, workingFormat, quality);
            if (maxEncodedImageBytes <= 0 || encoded.length <= maxEncodedImageBytes) {
                return encoded;
            }

            if (pass == maxPasses) {
                break;
            }

            if (!"jpg".equals(workingFormat) && !"jpeg".equals(workingFormat)) {
                workingFormat = "jpg";
            } else {
                quality = Math.max(minQuality, quality - 0.08f);
            }

            BufferedImage shrunk = resizeByPercent(workingImage, resizePercent);
            if (shrunk.getWidth() == workingImage.getWidth() && shrunk.getHeight() == workingImage.getHeight()) {
                break;
            }
            workingImage = shrunk;
        }

        if (maxEncodedImageBytes > 0 && encoded != null && encoded.length > maxEncodedImageBytes) {
            throw new BadRequestException(
                "Image payload is too large after optimization. Reduce image quality/resolution or split the PDF."
            );
        }
        return encoded == null ? new byte[0] : encoded;
    }

    private byte[] encodeBufferedImage(BufferedImage image, String format, float jpegQuality) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if ("jpg".equals(format) || "jpeg".equals(format)) {
                writeJpegWithQuality(image, outputStream, jpegQuality);
            } else {
                boolean written = ImageIO.write(image, format, outputStream);
                if (!written) {
                    throw new BadRequestException("Unable to encode image format: " + format + ".");
                }
            }
            byte[] encoded = outputStream.toByteArray();
            if (encoded.length == 0) {
                throw new BadRequestException("Encoded image is empty.");
            }
            return encoded;
        } catch (IOException e) {
            throw new BadRequestException("Failed to encode image for OCR.");
        }
    }

    private void writeJpegWithQuality(BufferedImage image, ByteArrayOutputStream outputStream, float configuredQuality)
        throws IOException {
        float quality = Math.max(0.35f, Math.min(0.95f, configuredQuality));
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            ImageIO.write(image, "jpg", outputStream);
            return;
        }

        ImageWriter writer = writers.next();
        try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(outputStream)) {
            writer.setOutput(ios);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(quality);
            }
            writer.write(null, new javax.imageio.IIOImage(image, null, null), writeParam);
            ios.flush();
        } finally {
            writer.dispose();
        }
    }

    private BufferedImage resizeByPercent(BufferedImage image, int resizePercent) {
        double scale = resizePercent / 100.0d;
        int targetWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int targetHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));
        return resizeImage(image, targetWidth, targetHeight);
    }

    private BufferedImage resizeImage(BufferedImage image, int targetWidth, int targetHeight) {
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return resized;
    }

    private void validateEncodedPayloadSize(int payloadBytes) {
        long maxEncodedImageBytes = properties.getGeneralText().getMaxEncodedImageBytes();
        if (maxEncodedImageBytes > 0 && payloadBytes > maxEncodedImageBytes) {
            throw new BadRequestException(
                "Image payload exceeds configured limit (" + maxEncodedImageBytes + " bytes)."
            );
        }
    }

    private String normalizePdfRenderImageFormat(String configuredFormat) {
        String format = trimToNull(configuredFormat);
        if (format == null) {
            return "jpg";
        }
        String normalized = format.toLowerCase();
        return switch (normalized) {
            case "jpg", "jpeg", "png", "bmp", "gif" -> normalized;
            default -> "jpg";
        };
    }

    private String normalizeRasterOutputFormat(String configuredFormat, String fallbackExtension) {
        String format = trimToNull(configuredFormat);
        if (format == null) {
            format = fallbackExtension;
        }
        String normalized = format.toLowerCase();
        return switch (normalized) {
            case "jpg", "jpeg", "png", "bmp", "gif" -> normalized;
            default -> "jpg";
        };
    }

    private String safeFileName(MultipartFile file) {
        String originalFilename = trimToNull(file.getOriginalFilename());
        if (originalFilename != null) {
            return originalFilename;
        }
        return "uploaded-file";
    }

    private String resolveFileExtension(String fileName) {
        String normalized = trimToNull(fileName);
        if (normalized == null) {
            return null;
        }
        int dotIdx = normalized.lastIndexOf('.');
        if (dotIdx < 0 || dotIdx == normalized.length() - 1) {
            return null;
        }
        return normalized.substring(dotIdx + 1).trim().toLowerCase();
    }

    private Set<String> normalizeAllowedFormats(List<String> formats) {
        if (formats == null || formats.isEmpty()) {
            return Set.of("jpeg", "jpg", "png", "bmp", "gif", "tiff", "tif", "webp", "pcx", "ico", "pdf", "psd");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : formats) {
            String format = trimToNull(value);
            if (format != null) {
                normalized.add(format.toLowerCase());
            }
        }
        if (normalized.isEmpty()) {
            normalized.add("jpeg");
            normalized.add("jpg");
            normalized.add("png");
            normalized.add("bmp");
            normalized.add("gif");
            normalized.add("tiff");
            normalized.add("tif");
            normalized.add("webp");
            normalized.add("pcx");
            normalized.add("ico");
            normalized.add("pdf");
            normalized.add("psd");
        }
        return normalized;
    }

    private GeneralTextOcrResponse mapResponse(
        JsonNode raw,
        String fileName,
        String fileFormat,
        Integer pageNumber,
        Integer totalPages
    ) {
        JsonNode resultNode = getField(raw, "result");
        JsonNode wordsNode = getField(resultNode, "words_block_list", "wordsBlockList");
        JsonNode markdownNode = getField(resultNode, "markdown_result", "markdownResult");
        JsonNode wordsCountNode = getField(resultNode, "words_block_count", "wordsBlockCount");

        String fullText = buildFullText(markdownNode, wordsNode);
        Double averageConfidence = computeAverageConfidence(wordsNode);
        int wordsBlockCount = wordsCountNode != null && wordsCountNode.isNumber()
            ? wordsCountNode.asInt()
            : (wordsNode != null && wordsNode.isArray() ? wordsNode.size() : 0);

        GeneralTextOcrResponse response = new GeneralTextOcrResponse();
        response.setFileName(fileName);
        response.setFileFormat(fileFormat);
        response.setPageNumber(pageNumber);
        response.setTotalPages(totalPages);
        response.setEngine(OCR_ENGINE);
        response.setFullText(fullText);
        response.setAverageConfidence(averageConfidence);
        response.setWordsBlockCount(wordsBlockCount);
        return response;
    }

    private String buildFullText(JsonNode markdownNode, JsonNode wordsNode) {
        if (markdownNode != null && markdownNode.isTextual() && !markdownNode.asText().isBlank()) {
            return markdownNode.asText();
        }
        if (wordsNode == null || !wordsNode.isArray()) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        for (JsonNode block : wordsNode) {
            JsonNode words = getField(block, "words");
            if (words != null && words.isTextual() && !words.asText().isBlank()) {
                lines.add(words.asText());
            }
        }
        return String.join("\n", lines);
    }

    private Double computeAverageConfidence(JsonNode wordsNode) {
        if (wordsNode == null || !wordsNode.isArray()) {
            return null;
        }
        List<Double> confidences = new ArrayList<>();
        for (JsonNode block : wordsNode) {
            JsonNode confidence = getField(block, "confidence");
            if (confidence != null && confidence.isNumber()) {
                confidences.add(confidence.asDouble());
            }
        }
        if (confidences.isEmpty()) {
            return null;
        }
        return confidences.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.averagingDouble(Double::doubleValue));
    }

    private JsonNode getField(JsonNode node, String... fieldNames) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode candidate = node.get(fieldName);
            if (candidate != null && !candidate.isMissingNode()) {
                return candidate;
            }
        }
        return null;
    }

    private String requireProperty(String value, String name) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalStateException(name + " must be configured.");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
