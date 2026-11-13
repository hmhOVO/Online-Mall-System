package com.example.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.common.TaskConstants;
import com.example.entity.SysConfig;
import com.example.entity.Task;
import com.example.mapper.TaskMapper;
import com.example.service.NotificationService;
import com.example.service.SysConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class OverdueReminderJob {

    private static final Logger log = LoggerFactory.getLogger(OverdueReminderJob.class);

    private final TaskMapper taskMapper;
    private final NotificationService notificationService;
    private final SysConfigService sysConfigService;

    public OverdueReminderJob(TaskMapper taskMapper, NotificationService notificationService,
                              SysConfigService sysConfigService) {
        this.taskMapper = taskMapper;
        this.notificationService = notificationService;
        this.sysConfigService = sysConfigService;
    }

    /**
     * 每天早上 9 点扫描逾期/临期任务并推送通知
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void remind() {
        int warningDays = getWarningDays();
        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusDays(warningDays);

        LambdaQueryWrapper<Task> wrapper = Wrappers.lambdaQuery();
        wrapper.ne(Task::getStatus, TaskConstants.STATUS_DONE)
                .le(Task::getEndDate, warningDate)
                .eq(Task::getDeleted, 0)
                .orderByAsc(Task::getEndDate);
        List<Task> tasks = taskMapper.selectList(wrapper);

        for (Task task : tasks) {
            if (task.getAssigneeId() == null) continue;
            String title;
            String content;
            if (task.getEndDate().isBefore(today)) {
                title = "任务已逾期";
                content = String.format("任务「%s」已于 %s 逾期，请及时处理", task.getTitle(), task.getEndDate());
            } else {
                title = "任务即将到期";
                content = String.format("任务「%s」将于 %s 到期，请提前安排", task.getTitle(), task.getEndDate());
            }
            String targetUrl = "/project/" + task.getProjectId() + "/board";
            try {
                notificationService.send(task.getAssigneeId(), "OVERDUE", title, content, targetUrl);
            } catch (Exception e) {
                log.warn("逾期通知发送失败，任务 {}", task.getId(), e);
            }
        }
        log.info("逾期提醒任务扫描完成，共 {} 条", tasks.size());
    }

    private int getWarningDays() {
        try {
            SysConfig config = sysConfigService.getByKey("overdue.warning.days");
            if (config != null && config.getConfigValue() != null) {
                return Integer.parseInt(config.getConfigValue().trim());
            }
        } catch (Exception e) {
            log.warn("读取逾期预警阈值失败，使用默认值 3", e);
        }
        return 3;
    }
}
