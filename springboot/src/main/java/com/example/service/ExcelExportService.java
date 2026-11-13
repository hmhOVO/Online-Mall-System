package com.example.service;

import com.example.common.CurrentUserContext;
import com.example.entity.Project;
import com.example.entity.Task;
import com.example.entity.User;
import com.example.mapper.DashboardMapper;
import com.example.mapper.ProjectMapper;
import com.example.mapper.TaskMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ExcelExportService {

    private final DashboardMapper dashboardMapper;
    private final TaskMapper taskMapper;
    private final ProjectMapper projectMapper;

    public ExcelExportService(DashboardMapper dashboardMapper, TaskMapper taskMapper, ProjectMapper projectMapper) {
        this.dashboardMapper = dashboardMapper;
        this.taskMapper = taskMapper;
        this.projectMapper = projectMapper;
    }

    public byte[] exportProjectReport(Integer projectId) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                throw new RuntimeException("项目不存在");
            }

            Sheet overviewSheet = workbook.createSheet("项目概览");
            createProjectOverviewSheet(workbook, overviewSheet, project);

            Sheet taskSheet = workbook.createSheet("任务列表");
            createTaskListSheet(workbook, taskSheet, projectId);

            Sheet memberSheet = workbook.createSheet("成员统计");
            createMemberStatsSheet(workbook, memberSheet, projectId);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    public byte[] exportPersonalReport(Integer userId) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            if (userId == null) {
                userId = CurrentUserContext.get().getId();
            }

            Sheet overviewSheet = workbook.createSheet("个人工作概览");
            createPersonalOverviewSheet(workbook, overviewSheet, userId);

            Sheet taskSheet = workbook.createSheet("任务明细");
            createPersonalTaskSheet(workbook, taskSheet, userId);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void createProjectOverviewSheet(Workbook workbook, Sheet sheet, Project project) {
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("项目报告");
        titleCell.setCellStyle(createTitleStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        int rowNum = 2;
        rowNum = addLabelValueRow(sheet, rowNum++, "项目名称", project.getName(), workbook);
        rowNum = addLabelValueRow(sheet, rowNum++, "项目描述", project.getDescription(), workbook);
        rowNum = addLabelValueRow(sheet, rowNum++, "项目标签", project.getTags(), workbook);
        rowNum = addLabelValueRow(sheet, rowNum++, "项目状态", getStatusLabel(project.getStatus()), workbook);
        rowNum = addLabelValueRow(sheet, rowNum++, "创建时间", formatDateTime(project.getCreatedAt()), workbook);
        rowNum = addLabelValueRow(sheet, rowNum++, "开始日期", formatDate(project.getStartDate()), workbook);
        rowNum = addLabelValueRow(sheet, rowNum++, "截止日期", formatDate(project.getEndDate()), workbook);

        for (int i = 0; i < 4; i++) {
            sheet.setColumnWidth(i, 5000);
        }
    }

    private void createTaskListSheet(Workbook workbook, Sheet sheet, Integer projectId) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"任务名称", "优先级", "状态", "组长ID", "开始日期", "截止日期", "进度(%)", "实际学时"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(createHeaderStyle(workbook));
        }

        List<Task> tasks = taskMapper.selectByProjectId(projectId);
        int rowNum = 1;
        for (Task task : tasks) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(task.getTitle() != null ? task.getTitle() : "");
            row.createCell(1).setCellValue(getPriorityLabel(task.getPriority()));
            row.createCell(2).setCellValue(getStatusLabel(task.getStatus()));
            row.createCell(3).setCellValue(task.getAssigneeId() != null ? task.getAssigneeId().toString() : "待认领");
            row.createCell(4).setCellValue(formatDate(task.getStartDate()));
            row.createCell(5).setCellValue(formatDate(task.getEndDate()));
            row.createCell(6).setCellValue(task.getProgress() != null ? task.getProgress() : 0);
            row.createCell(7).setCellValue(task.getActualHours() != null ? task.getActualHours() : 0);
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 4000);
        }
    }

    private void createMemberStatsSheet(Workbook workbook, Sheet sheet, Integer projectId) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"成员名称", "总任务数", "完成任务数", "待办任务数", "实际学时"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(createHeaderStyle(workbook));
        }

        List<Map<String, Object>> memberStats = dashboardMapper.selectMemberStatsByProject(projectId);
        int rowNum = 1;
        for (Map<String, Object> stat : memberStats) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(safeToString(stat.get("nickname")));
            row.createCell(1).setCellValue(toInt(stat.get("totalTasks")));
            row.createCell(2).setCellValue(toInt(stat.get("completedTasks")));
            row.createCell(3).setCellValue(toInt(stat.get("todoTasks")));
            row.createCell(4).setCellValue(toDouble(stat.get("actualHours")));
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 4000);
        }
    }

    private int toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double toDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void createPersonalOverviewSheet(Workbook workbook, Sheet sheet, Integer userId) {
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("个人任务报表");
        titleCell.setCellStyle(createTitleStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        int rowNum = 2;
        rowNum = addLabelValueRow(sheet, rowNum++, "用户ID", userId.toString(), workbook);
        rowNum = addLabelValueRow(sheet, rowNum++, "统计日期", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")), workbook);

        try {
            Map<String, Object> personal = dashboardMapper.getPersonalStats(userId);
            if (personal != null) {
                rowNum = addLabelValueRow(sheet, rowNum++, "参与项目数", safeToString(personal.get("projectCount")), workbook);
                rowNum = addLabelValueRow(sheet, rowNum++, "完成任务数", safeToString(personal.get("completedTaskCount")), workbook);
                rowNum = addLabelValueRow(sheet, rowNum++, "待办任务数", safeToString(personal.get("todoCount")), workbook);
                rowNum = addLabelValueRow(sheet, rowNum++, "逾期任务数", safeToString(personal.get("overdueCount")), workbook);
                rowNum = addLabelValueRow(sheet, rowNum++, "任务完成率", safeToString(personal.get("completionRate")) + "%", workbook);
            }
        } catch (Exception e) {
        }

        for (int i = 0; i < 4; i++) {
            sheet.setColumnWidth(i, 5000);
        }
    }

    private void createPersonalTaskSheet(Workbook workbook, Sheet sheet, Integer userId) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"任务名称", "优先级", "状态", "开始日期", "截止日期", "进度(%)"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(createHeaderStyle(workbook));
        }

        List<Map<String, Object>> tasks = dashboardMapper.selectRecentTasks(userId);
        int rowNum = 1;
        for (Map<String, Object> task : tasks) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(safeToString(task.get("title")));
            row.createCell(1).setCellValue(getPriorityLabel(safeToString(task.get("priority"))));
            row.createCell(2).setCellValue(getStatusLabel(safeToString(task.get("status"))));
            row.createCell(3).setCellValue(formatDate(task.get("startDate")));
            row.createCell(4).setCellValue(formatDate(task.get("endDate")));
            Object progress = task.get("progress");
            row.createCell(5).setCellValue(progress instanceof Number ? ((Number) progress).intValue() : 0);
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 4000);
        }
    }

    private int addLabelValueRow(Sheet sheet, int rowNum, String label, String value, Workbook workbook) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(createLabelStyle(workbook));

        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value != null ? value : "");
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 1, 3));
        return rowNum + 1;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createLabelStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private String formatDate(Object date) {
        if (date == null) {
            return "";
        }
        if (date instanceof LocalDate) {
            return ((LocalDate) date).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        return date.toString();
    }

    private String formatDateTime(Object date) {
        if (date == null) {
            return "";
        }
        if (date instanceof LocalDateTime) {
            return ((LocalDateTime) date).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return date.toString();
    }

    private String safeToString(Object obj) {
        if (obj == null) {
            return "";
        }
        return obj.toString();
    }

    private String getStatusLabel(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case "TODO" -> "待办";
            case "IN_PROGRESS" -> "进行中";
            case "REVIEW" -> "审核中";
            case "DONE" -> "已完成";
            default -> status;
        };
    }

    private String getPriorityLabel(String priority) {
        if (priority == null) return "普通";
        return switch (priority) {
            case "HIGH" -> "高";
            case "MEDIUM" -> "中";
            case "LOW" -> "低";
            default -> priority;
        };
    }
}
