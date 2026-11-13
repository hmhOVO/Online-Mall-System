package com.example.service;

import com.example.common.FileType;
import com.example.exception.CustomerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp",
            "pdf",
            "doc", "docx", "xls", "xlsx", "ppt", "pptx"
    );
    private static final Set<String> DOCUMENT_MIME_TYPES = Set.of(
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    );

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    @Value("${file.upload.url-prefix:/upload/}")
    private String urlPrefix;

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomerException("上传文件不能为空");
        }
        validateFile(file);
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            String original = sanitizeFilename(file.getOriginalFilename());
            String ext = extractExtension(original);
            String filename = UUID.randomUUID().toString().replace("-", "") + (ext.isEmpty() ? "" : "." + ext);
            Path target = dir.resolve(filename);
            file.transferTo(target);
            return filename;
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new CustomerException("文件保存失败：" + e.getMessage());
        }
    }

    public void validateFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomerException("文件大小不能超过 20MB");
        }
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            throw new CustomerException("文件名不能为空");
        }
        String ext = extractExtension(original).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new CustomerException("不支持的文件类型，仅支持图片、PDF、Word、Excel、PPT");
        }
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new CustomerException("无法识别文件类型");
        }
        if (!isAllowedContentType(contentType, ext)) {
            throw new CustomerException("文件类型与扩展名不一致或不在白名单内");
        }
    }

    private boolean isAllowedContentType(String contentType, String ext) {
        String lower = contentType.toLowerCase(Locale.ROOT);
        if (lower.startsWith("image/")) {
            return Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp").contains(ext);
        }
        if ("pdf".equals(ext)) {
            return lower.contains("pdf");
        }
        return DOCUMENT_MIME_TYPES.contains(lower);
    }

    public FileType resolveFileType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return FileType.UNKNOWN;
        }
        String lower = mimeType.toLowerCase(Locale.ROOT);
        if (lower.startsWith("image/")) {
            return FileType.IMAGE;
        }
        if (lower.contains("pdf")) {
            return FileType.PDF;
        }
        if (DOCUMENT_MIME_TYPES.contains(lower)) {
            return FileType.DOCUMENT;
        }
        return FileType.UNKNOWN;
    }

    public boolean isImage(String mimeType) {
        return resolveFileType(mimeType) == FileType.IMAGE;
    }

    public boolean isDocument(String mimeType) {
        FileType type = resolveFileType(mimeType);
        return type == FileType.DOCUMENT || type == FileType.PDF;
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "";
        }
        String name = filename.replaceAll("[^a-zA-Z0-9.\\u4e00-\\u9fa5_-]", "_");
        if (name.length() > 200) {
            name = name.substring(name.length() - 200);
        }
        return name;
    }

    public String getFilePath(String filename) {
        return Paths.get(uploadDir).toAbsolutePath().normalize().resolve(filename).toString();
    }

    public String getPreviewUrl(String filename) {
        return urlPrefix + filename;
    }

    public Path getPath(String filename) {
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir)) {
            throw new CustomerException("非法的文件路径");
        }
        return target;
    }

    public void delete(String filename) {
        try {
            Files.deleteIfExists(getPath(filename));
        } catch (IOException e) {
            log.warn("删除旧文件失败：{}", filename, e);
        }
    }

    public Path getConvertedPdfPath(String filename) {
        return getPath(filename + ".pdf");
    }

    public String getConvertedPdfFilename(String filename) {
        return filename + ".pdf";
    }
}
