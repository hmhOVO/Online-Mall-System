package com.example.controller;

import com.example.common.CurrentUserContext;
import com.example.common.ProjectRoleLevel;
import com.example.annotation.OperationLog;
import com.example.annotation.RequirePermission;
import com.example.common.Result;
import com.example.entity.FileResource;
import com.example.entity.User;
import com.example.enums.RoleEnum;
import com.example.exception.CustomerException;
import com.example.mapper.FileResourceMapper;
import com.example.service.DocumentConverterService;
import com.example.service.FileStorageService;
import com.example.service.ProjectService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/file")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private static final String BUSINESS_PROJECT_FILE = "PROJECT_FILE";

    @Resource
    private FileResourceMapper fileResourceMapper;

    @Resource
    private FileStorageService fileStorageService;

    @Resource
    private ProjectService projectService;

    @Resource
    private DocumentConverterService documentConverterService;

    @RequirePermission("project.view")
    @GetMapping("/{id}/download")
    public void download(@PathVariable Integer id, HttpServletResponse response) {
        FileResource file = fileResourceMapper.selectById(id);
        if (file == null || file.getDeleted() != null && file.getDeleted() == 1) {
            throw new CustomerException("文件不存在");
        }

        checkPermission(file);

        Path path = fileStorageService.getPath(file.getFilename());
        if (!Files.exists(path)) {
            throw new CustomerException("文件已丢失");
        }

        String originalName = file.getOriginalName();
        if (originalName == null || originalName.isEmpty()) {
            originalName = file.getFilename();
        }

        response.setContentType(file.getMimeType() != null ? file.getMimeType() : "application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodeFilename(originalName) + "\"");

        try (var in = Files.newInputStream(path);
             var out = response.getOutputStream()) {
            long fileSize = file.getFileSize() != null ? file.getFileSize() : Files.size(path);
            response.setContentLengthLong(fileSize);
            in.transferTo(out);
            out.flush();
        } catch (IOException e) {
            log.error("文件下载失败: {}", path, e);
            throw new CustomerException("文件下载失败");
        }
    }

    @RequirePermission("project.view")
    @GetMapping("/{id}/preview")
    public void preview(@PathVariable Integer id, HttpServletResponse response) {
        FileResource file = fileResourceMapper.selectById(id);
        if (file == null || file.getDeleted() != null && file.getDeleted() == 1) {
            throw new CustomerException("文件不存在");
        }

        checkPermission(file);

        Path path = fileStorageService.getPath(file.getFilename());
        if (!Files.exists(path)) {
            throw new CustomerException("文件已丢失");
        }

        String mimeType = file.getMimeType() != null ? file.getMimeType() : "";
        Path previewPath = path;
        String previewContentType = file.getMimeType() != null ? file.getMimeType() : "application/octet-stream";

        // Office 文档使用转换后的 PDF 进行预览
        if (documentConverterService.isOfficeDocument(mimeType)) {
            previewPath = documentConverterService.convertToPdf(path, mimeType);
            previewContentType = "application/pdf";
        }

        response.setContentType(previewContentType);
        response.setHeader("Content-Disposition", "inline");

        try (var in = Files.newInputStream(previewPath);
             var out = response.getOutputStream()) {
            response.setContentLengthLong(Files.size(previewPath));
            in.transferTo(out);
            out.flush();
        } catch (IOException e) {
            log.error("文件预览失败: {}", previewPath, e);
            throw new CustomerException("文件预览失败");
        }
    }

    @RequirePermission("project.view")
    @GetMapping("/{id}/info")
    public Result info(@PathVariable Integer id) {
        FileResource file = fileResourceMapper.selectById(id);
        if (file == null || file.getDeleted() != null && file.getDeleted() == 1) {
            throw new CustomerException("文件不存在");
        }
        checkPermission(file);
        return Result.success(file);
    }

    /**
     * 项目公共文件列表：项目成员均可查看。
     */
    @RequirePermission("project.view")
    @GetMapping("/project/{projectId}")
    public Result listProjectFiles(@PathVariable Integer projectId) {
        checkProjectRead(projectId);
        List<FileResource> files = fileResourceMapper.selectProjectFiles(projectId, BUSINESS_PROJECT_FILE);
        for (FileResource file : files) {
            file.setPreviewUrl(fileStorageService.getPreviewUrl(file.getFilename()));
        }
        return Result.success(files);
    }

    /**
     * 上传项目公共文件：仅指导教师或管理员。
     */
    @OperationLog("上传项目文件")
    @PostMapping("/project/{projectId}/upload")
    public Result uploadProjectFile(@PathVariable Integer projectId,
                                    @RequestParam("file") MultipartFile file) {
        User current = checkProjectManage(projectId);
        String filename = fileStorageService.store(file);

        FileResource resource = new FileResource();
        resource.setProjectId(projectId);
        resource.setFilename(filename);
        resource.setOriginalName(file.getOriginalFilename());
        resource.setFilePath(fileStorageService.getFilePath(filename));
        resource.setFileSize(file.getSize());
        resource.setMimeType(file.getContentType());
        resource.setBusinessType(BUSINESS_PROJECT_FILE);
        resource.setRefCount(1);
        resource.setIsPublic(true);
        resource.setUploaderId(current.getId());
        fileResourceMapper.insert(resource);

        resource.setPreviewUrl(fileStorageService.getPreviewUrl(filename));
        resource.setUploaderName(current.getNickname() != null ? current.getNickname() : current.getUsername());
        return Result.success(resource);
    }

    @OperationLog("删除文件")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        FileResource file = fileResourceMapper.selectById(id);
        if (file == null || file.getDeleted() != null && file.getDeleted() == 1) {
            throw new CustomerException("文件不存在");
        }
        checkDeletePermission(file);
        fileResourceMapper.deleteById(id);
        return Result.success();
    }

    private void checkPermission(FileResource file) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }

        if (file.getUploaderId() != null && file.getUploaderId().equals(current.getId())) {
            return;
        }

        // 项目文件必须是项目成员才可访问，isPublic 仅代表“项目内公共”，不对全系统开放
        if (file.getProjectId() != null) {
            ProjectRoleLevel level = projectService.getUserProjectRole(file.getProjectId(), current.getId());
            if (level != null && level != ProjectRoleLevel.NONE) {
                return;
            }
            throw new CustomerException("无权限访问该文件");
        }

        Boolean isPublic = file.getIsPublic();
        if (isPublic != null && isPublic) {
            return;
        }

        throw new CustomerException("无权限访问该文件");
    }

    private void checkProjectRead(Integer projectId) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        if (RoleEnum.ADMIN.name().equals(current.getRole())) {
            return;
        }
        if (projectService.getUserProjectRole(projectId, current.getId()) == ProjectRoleLevel.NONE) {
            throw new CustomerException("无权限查看该项目文件");
        }
    }

    private User checkProjectManage(Integer projectId) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        if (RoleEnum.ADMIN.name().equals(current.getRole())) {
            return current;
        }
        if (projectService.getUserProjectRole(projectId, current.getId()) != ProjectRoleLevel.OWNER) {
            throw new CustomerException("仅指导教师可上传项目文件");
        }
        return current;
    }

    private void checkDeletePermission(FileResource file) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        if (RoleEnum.ADMIN.name().equals(current.getRole())) {
            return;
        }
        if (file.getUploaderId() != null && file.getUploaderId().equals(current.getId())) {
            return;
        }
        if (file.getProjectId() != null
                && projectService.getUserProjectRole(file.getProjectId(), current.getId()) == ProjectRoleLevel.OWNER) {
            return;
        }
        throw new CustomerException("无权限删除该文件");
    }

    private String encodeFilename(String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        return encoded.replace("+", "%20");
    }
}
