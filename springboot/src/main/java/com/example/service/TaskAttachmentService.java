package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.common.CurrentUserContext;
import com.example.entity.FileResource;
import com.example.entity.Task;
import com.example.entity.TaskAttachment;
import com.example.exception.CustomerException;
import com.example.mapper.FileResourceMapper;
import com.example.mapper.TaskAttachmentMapper;
import com.example.mapper.TaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class TaskAttachmentService {

    private final TaskAttachmentMapper taskAttachmentMapper;
    private final FileResourceMapper fileResourceMapper;
    private final TaskMapper taskMapper;
    private final FileStorageService fileStorageService;

    public TaskAttachmentService(TaskAttachmentMapper taskAttachmentMapper,
                                  FileResourceMapper fileResourceMapper,
                                  TaskMapper taskMapper,
                                  FileStorageService fileStorageService) {
        this.taskAttachmentMapper = taskAttachmentMapper;
        this.fileResourceMapper = fileResourceMapper;
        this.taskMapper = taskMapper;
        this.fileStorageService = fileStorageService;
    }

    public List<TaskAttachment> listByTaskId(Integer taskId) {
        return taskAttachmentMapper.selectWithFileByTaskId(taskId);
    }

    @Transactional
    public TaskAttachment upload(Integer taskId, MultipartFile file) {
        Task task = taskMapper.selectById(taskId);
        if (task == null || task.getDeleted() == 1) {
            throw new CustomerException("任务不存在");
        }

        String filename = fileStorageService.store(file);
        String originalName = file.getOriginalFilename();
        
        FileResource fileResource = new FileResource();
        fileResource.setProjectId(task.getProjectId());
        fileResource.setFilename(filename);
        fileResource.setOriginalName(originalName);
        fileResource.setFilePath(fileStorageService.getFilePath(filename));
        fileResource.setFileSize(file.getSize());
        fileResource.setMimeType(file.getContentType());
        fileResource.setBusinessType("TASK_ATTACHMENT");
        fileResource.setUploaderId(CurrentUserContext.get().getId());
        fileResource.setRefCount(1);
        fileResourceMapper.insert(fileResource);

        TaskAttachment attachment = new TaskAttachment();
        attachment.setTaskId(taskId);
        attachment.setFileId(fileResource.getId());
        taskAttachmentMapper.insert(attachment);

        attachment.setFileName(fileResource.getOriginalName());
        attachment.setFilePath(fileStorageService.getPreviewUrl(filename));
        attachment.setFileSize(fileResource.getFileSize());
        attachment.setMimeType(fileResource.getMimeType());
        attachment.setUploaderId(fileResource.getUploaderId());
        return attachment;
    }

    @Transactional
    public void delete(Integer taskId, Integer attachmentId) {
        LambdaQueryWrapper<TaskAttachment> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TaskAttachment::getId, attachmentId)
                .eq(TaskAttachment::getTaskId, taskId)
                .eq(TaskAttachment::getDeleted, 0);
        TaskAttachment attachment = taskAttachmentMapper.selectOne(wrapper);
        
        if (attachment == null) {
            throw new CustomerException("附件不存在");
        }

        LambdaUpdateWrapper<TaskAttachment> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(TaskAttachment::getId, attachmentId)
                .set(TaskAttachment::getDeleted, 1);
        taskAttachmentMapper.update(null, updateWrapper);

        fileResourceMapper.updateRefCount(attachment.getFileId(), -1);
    }

    @Transactional
    public void deleteByTaskId(Integer taskId) {
        LambdaQueryWrapper<TaskAttachment> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TaskAttachment::getTaskId, taskId)
                .eq(TaskAttachment::getDeleted, 0);
        List<TaskAttachment> attachments = taskAttachmentMapper.selectList(wrapper);

        for (TaskAttachment attachment : attachments) {
            fileResourceMapper.updateRefCount(attachment.getFileId(), -1);
        }

        LambdaUpdateWrapper<TaskAttachment> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(TaskAttachment::getTaskId, taskId)
                .set(TaskAttachment::getDeleted, 1);
        taskAttachmentMapper.update(null, updateWrapper);
    }
}