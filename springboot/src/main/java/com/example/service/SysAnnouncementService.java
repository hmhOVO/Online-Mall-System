package com.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.CurrentUserContext;
import com.example.entity.SysAnnouncement;
import com.example.entity.User;
import com.example.enums.RoleEnum;
import com.example.exception.CustomerException;
import com.example.mapper.SysAnnouncementMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SysAnnouncementService {

    private final SysAnnouncementMapper announcementMapper;

    public SysAnnouncementService(SysAnnouncementMapper announcementMapper) {
        this.announcementMapper = announcementMapper;
    }

    public Page<SysAnnouncement> listAnnouncements(Integer pageNum, Integer pageSize,
                                                   String status, String type, String keyword) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        if (!RoleEnum.ADMIN.name().equals(current.getRole())) {
            throw new CustomerException("仅管理员可管理公告");
        }

        int offset = (pageNum - 1) * pageSize;
        var list = announcementMapper.selectAnnouncements(status, type, keyword, offset, pageSize);
        int total = announcementMapper.countAnnouncements(status, type, keyword);

        Page<SysAnnouncement> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(list);
        return page;
    }

    public Page<SysAnnouncement> listPublished(Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        var list = announcementMapper.selectPublishedAnnouncements(offset, pageSize);
        int total = announcementMapper.countPublishedAnnouncements();

        Page<SysAnnouncement> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(list);
        return page;
    }

    public SysAnnouncement getDetail(Integer id) {
        SysAnnouncement announcement = announcementMapper.selectById(id);
        if (announcement == null || announcement.getDeleted() == 1) {
            throw new CustomerException("公告不存在");
        }
        return announcement;
    }

    public void createAnnouncement(SysAnnouncement announcement) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        if (!RoleEnum.ADMIN.name().equals(current.getRole())) {
            throw new CustomerException("仅管理员可发布公告");
        }

        announcement.setPublisherId(current.getId());
        if (announcement.getIsTop() == null) {
            announcement.setIsTop(0);
        }
        if (announcement.getStatus() == null) {
            announcement.setStatus("DRAFT");
        }
        if ("PUBLISHED".equals(announcement.getStatus())) {
            announcement.setPublishTime(LocalDateTime.now());
        }

        announcementMapper.insert(announcement);
    }

    public void updateAnnouncement(Integer id, SysAnnouncement announcement) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        if (!RoleEnum.ADMIN.name().equals(current.getRole())) {
            throw new CustomerException("仅管理员可编辑公告");
        }

        SysAnnouncement existing = announcementMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new CustomerException("公告不存在");
        }

        announcement.setId(id);
        if ("PUBLISHED".equals(announcement.getStatus())
                && !"PUBLISHED".equals(existing.getStatus())) {
            announcement.setPublishTime(LocalDateTime.now());
        }

        announcementMapper.updateById(announcement);
    }

    public void deleteAnnouncement(Integer id) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        if (!RoleEnum.ADMIN.name().equals(current.getRole())) {
            throw new CustomerException("仅管理员可删除公告");
        }

        SysAnnouncement existing = announcementMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new CustomerException("公告不存在");
        }

        announcementMapper.deleteById(id);
    }

    public void publishAnnouncement(Integer id) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        if (!RoleEnum.ADMIN.name().equals(current.getRole())) {
            throw new CustomerException("仅管理员可发布公告");
        }

        SysAnnouncement existing = announcementMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new CustomerException("公告不存在");
        }

        existing.setStatus("PUBLISHED");
        existing.setPublishTime(LocalDateTime.now());
        announcementMapper.updateById(existing);
    }

    public void offlineAnnouncement(Integer id) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        if (!RoleEnum.ADMIN.name().equals(current.getRole())) {
            throw new CustomerException("仅管理员可下线公告");
        }

        SysAnnouncement existing = announcementMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new CustomerException("公告不存在");
        }

        existing.setStatus("OFFLINE");
        announcementMapper.updateById(existing);
    }

    public void toggleTop(Integer id) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        if (!RoleEnum.ADMIN.name().equals(current.getRole())) {
            throw new CustomerException("仅管理员可操作置顶");
        }

        SysAnnouncement existing = announcementMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new CustomerException("公告不存在");
        }

        existing.setIsTop(existing.getIsTop() == 1 ? 0 : 1);
        announcementMapper.updateById(existing);
    }
}
