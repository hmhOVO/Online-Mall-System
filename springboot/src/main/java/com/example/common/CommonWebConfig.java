package com.example.common;

import com.example.interceptor.AdminInterceptor;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class CommonWebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CommonWebConfig.class);

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    @Value("${file.upload.url-prefix:/upload/}")
    private String urlPrefix;

    @Resource
    private AdminInterceptor adminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
                log.info("创建上传目录：{}", dir);
            } catch (IOException e) {
                throw new RuntimeException("无法创建上传目录: " + dir, e);
            }
        }
        String location = "file:///" + dir.toString().replace("\\", "/");
        if (!location.endsWith("/")) {
            location += "/";
        }
        log.info("静态资源映射：{} -> {}", urlPrefix + "**", location);
        registry.addResourceHandler(urlPrefix + "**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}
