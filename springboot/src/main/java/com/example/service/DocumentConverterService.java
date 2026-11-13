package com.example.service;

import com.example.exception.CustomerException;
import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.office.LocalOfficeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Service
public class DocumentConverterService {

    private static final Logger log = LoggerFactory.getLogger(DocumentConverterService.class);

    private static final List<String> OFFICE_MIME_KEYWORDS = Arrays.asList(
            "msword",
            "wordprocessingml",
            "excel",
            "spreadsheetml",
            "powerpoint",
            "presentationml"
    );

    @Value("${jodconverter.office.home:}")
    private String officeHome;

    @Value("${jodconverter.office.ports:2002,2003,2004}")
    private List<Integer> officePorts;

    @Value("${jodconverter.office.max-tasks-per-process:10}")
    private int maxTasksPerProcess;

    private volatile OfficeManager officeManager;
    private volatile DocumentConverter converter;
    private volatile boolean initialized = false;
    private volatile String initError = null;

    public boolean isConvertible(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        String lower = mimeType.toLowerCase();
        if (lower.contains("pdf")) {
            return true;
        }
        return OFFICE_MIME_KEYWORDS.stream().anyMatch(lower::contains);
    }

    public boolean isOfficeDocument(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        String lower = mimeType.toLowerCase();
        return OFFICE_MIME_KEYWORDS.stream().anyMatch(lower::contains);
    }

    public Path convertToPdf(Path sourcePath, String mimeType) {
        if (!isConvertible(mimeType)) {
            throw new CustomerException("当前文件类型不支持转换为 PDF 预览");
        }
        if (mimeType.toLowerCase().contains("pdf")) {
            return sourcePath;
        }
        ensureConverterReady();

        Path pdfPath = sourcePath.resolveSibling(sourcePath.getFileName().toString() + ".pdf");
        File sourceFile = sourcePath.toFile();
        File targetFile = pdfPath.toFile();

        if (targetFile.exists() && targetFile.length() > 0) {
            log.info("PDF 已存在，跳过转换: {}", pdfPath);
            return pdfPath;
        }

        try {
            converter.convert(sourceFile).to(targetFile).execute();
            log.info("文档转 PDF 成功: {} -> {}", sourcePath, pdfPath);
            return pdfPath;
        } catch (OfficeException e) {
            log.error("文档转 PDF 失败: {}", sourcePath, e);
            throw new CustomerException("文档预览转换失败，请确认已安装 LibreOffice 并配置正确：" + e.getMessage());
        }
    }

    private synchronized void ensureConverterReady() {
        if (initialized) {
            if (initError != null) {
                throw new CustomerException("LibreOffice 未正确初始化：" + initError);
            }
            return;
        }
        try {
            LocalOfficeManager.Builder builder = LocalOfficeManager.builder()
                    .portNumbers(officePorts.stream().mapToInt(Integer::intValue).toArray())
                    .maxTasksPerProcess(maxTasksPerProcess);
            if (officeHome != null && !officeHome.isBlank()) {
                builder.officeHome(officeHome);
            }
            officeManager = builder.build();
            officeManager.start();
            converter = LocalConverter.builder().officeManager(officeManager).build();
            initialized = true;
            log.info("LibreOffice 转换服务初始化成功");
        } catch (Exception e) {
            initError = e.getMessage();
            initialized = true;
            log.error("LibreOffice 转换服务初始化失败", e);
            throw new CustomerException("LibreOffice 未正确安装或配置：" + e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        if (officeManager != null && officeManager.isRunning()) {
            try {
                officeManager.stop();
                log.info("LibreOffice 转换服务已关闭");
            } catch (OfficeException e) {
                log.error("关闭 LibreOffice 转换服务失败", e);
            }
        }
    }
}
