package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.CurrentUserContext;
import com.example.entity.User;
import com.example.enums.RoleEnum;
import com.example.mapper.ExportMapper;
import com.example.mapper.UserMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

@Service
public class ExportService {

    private final UserMapper userMapper;
    private final ExportMapper exportMapper;

    public ExportService(UserMapper userMapper, ExportMapper exportMapper) {
        this.userMapper = userMapper;
        this.exportMapper = exportMapper;
    }

    public void exportUsers(String role, String status, String keyword, HttpServletResponse response) throws IOException {
        QueryWrapper<User> wrapper = new QueryWrapper<User>().eq("deleted", 0).orderByDesc("created_at");
        if (role != null && !role.isBlank()) {
            wrapper.eq("role", role);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like("username", keyword).or().like("nickname", keyword));
        }
        List<User> users = userMapper.selectList(wrapper);

        String[] headers = {"ID", "用户名", "姓名", "邮箱", "手机号", "角色", "状态", "创建时间"};
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("用户列表");
            createHeaderRow(sheet, headers);

            int rowIdx = 1;
            for (User user : users) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(user.getId());
                row.createCell(1).setCellValue(user.getUsername());
                row.createCell(2).setCellValue(user.getNickname());
                row.createCell(3).setCellValue(user.getEmail());
                row.createCell(4).setCellValue(user.getPhone());
                row.createCell(5).setCellValue(formatRole(user.getRole()));
                row.createCell(6).setCellValue(formatStatus(user.getStatus()));
                row.createCell(7).setCellValue(formatDateTime(user.getCreatedAt()));
            }

            autoSizeColumns(sheet, headers.length);
            writeResponse(response, workbook, "用户列表.xlsx");
        }
    }

    public void exportProjects(String status, String keyword, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> projects = exportMapper.selectProjectsForExport(status, keyword, currentScopeUserId());

        String[] headers = {"ID", "项目名称", "项目描述", "状态", "负责人", "创建人", "开始日期", "结束日期", "创建时间"};
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("项目列表");
            createHeaderRow(sheet, headers);

            int rowIdx = 1;
            for (Map<String, Object> p : projects) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(toStringValue(p.get("id")));
                row.createCell(1).setCellValue(toStringValue(p.get("name")));
                row.createCell(2).setCellValue(toStringValue(p.get("description")));
                row.createCell(3).setCellValue(formatProjectStatus(toStringValue(p.get("status"))));
                row.createCell(4).setCellValue(toStringValue(p.get("ownerName")));
                row.createCell(5).setCellValue(toStringValue(p.get("creatorName")));
                row.createCell(6).setCellValue(toStringValue(p.get("startDate")));
                row.createCell(7).setCellValue(toStringValue(p.get("endDate")));
                row.createCell(8).setCellValue(toStringValue(p.get("createdAt")));
            }

            autoSizeColumns(sheet, headers.length);
            writeResponse(response, workbook, "项目列表.xlsx");
        }
    }

    public void exportTasks(Integer projectId, String status, String priority, Integer assigneeId, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> tasks = exportMapper.selectTasksForExport(projectId, status, priority, assigneeId, currentScopeUserId());

        String[] headers = {"ID", "所属项目", "任务标题", "优先级", "状态", "进度(%)", "组长", "创建人", "计划学时", "实际学时", "开始日期", "结束日期", "完成时间"};
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("任务列表");
            createHeaderRow(sheet, headers);

            int rowIdx = 1;
            for (Map<String, Object> t : tasks) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(toStringValue(t.get("id")));
                row.createCell(1).setCellValue(toStringValue(t.get("projectName")));
                row.createCell(2).setCellValue(toStringValue(t.get("title")));
                row.createCell(3).setCellValue(formatPriority(toStringValue(t.get("priority"))));
                row.createCell(4).setCellValue(formatTaskStatus(toStringValue(t.get("status"))));
                row.createCell(5).setCellValue(toStringValue(t.get("progress")));
                row.createCell(6).setCellValue(toStringValue(t.get("assigneeName")));
                row.createCell(7).setCellValue(toStringValue(t.get("creatorName")));
                row.createCell(8).setCellValue(toStringValue(t.get("plannedHours")));
                row.createCell(9).setCellValue(toStringValue(t.get("actualHours")));
                row.createCell(10).setCellValue(toStringValue(t.get("startDate")));
                row.createCell(11).setCellValue(toStringValue(t.get("endDate")));
                row.createCell(12).setCellValue(toStringValue(t.get("completedAt")));
            }

            autoSizeColumns(sheet, headers.length);
            writeResponse(response, workbook, "任务列表.xlsx");
        }
    }

    public void exportScores(Integer projectId, String keyword, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> rows = exportMapper.selectStudentScoresForExport(projectId, keyword, currentScopeUserId());

        String[] headers = {"项目名称", "用户名", "姓名", "任务总数", "已完成任务数", "完成率", "已评分任务数", "平均分"};
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("学生任务完成与得分");
            createHeaderRow(sheet, headers);

            int rowIdx = 1;
            for (Map<String, Object> r : rows) {
                Row row = sheet.createRow(rowIdx++);
                int total = toNumberInt(r.get("totalTasks"));
                int done = toNumberInt(r.get("doneTasks"));
                row.createCell(0).setCellValue(toStringValue(r.get("projectName")));
                row.createCell(1).setCellValue(toStringValue(r.get("username")));
                row.createCell(2).setCellValue(toStringValue(r.get("nickname")));
                row.createCell(3).setCellValue(total);
                row.createCell(4).setCellValue(done);
                row.createCell(5).setCellValue(total == 0 ? "0%" : Math.round(done * 100.0 / total) + "%");
                row.createCell(6).setCellValue(toNumberInt(r.get("scoredTaskCount")));
                row.createCell(7).setCellValue(toStringValue(r.get("averageScore")));
            }

            autoSizeColumns(sheet, headers.length);
            writeResponse(response, workbook, "学生任务完成与得分表.xlsx");
        }
    }

    public void exportPersonalWeeklyReport(HttpServletResponse response) throws IOException {
        User current = CurrentUserContext.get();
        Integer userId = current.getId();

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDateTime weekStartDt = weekStart.atStartOfDay();
        LocalDateTime weekEndDt = weekEnd.atTime(23, 59, 59);

        List<Map<String, Object>> tasks = exportMapper.selectPersonalWeekTasks(userId, weekStartDt, weekEndDt);

        String[] headers = {"ID", "所属项目", "任务标题", "优先级", "状态", "进度(%)", "计划学时", "实际学时", "开始日期", "结束日期", "完成时间", "创建人"};
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("本周任务");
            createHeaderRow(sheet, headers);

            int rowIdx = 1;
            for (Map<String, Object> t : tasks) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(toStringValue(t.get("id")));
                row.createCell(1).setCellValue(toStringValue(t.get("projectName")));
                row.createCell(2).setCellValue(toStringValue(t.get("title")));
                row.createCell(3).setCellValue(formatPriority(toStringValue(t.get("priority"))));
                row.createCell(4).setCellValue(formatTaskStatus(toStringValue(t.get("status"))));
                row.createCell(5).setCellValue(toStringValue(t.get("progress")));
                row.createCell(6).setCellValue(toStringValue(t.get("plannedHours")));
                row.createCell(7).setCellValue(toStringValue(t.get("actualHours")));
                row.createCell(8).setCellValue(toStringValue(t.get("startDate")));
                row.createCell(9).setCellValue(toStringValue(t.get("endDate")));
                row.createCell(10).setCellValue(toStringValue(t.get("completedAt")));
                row.createCell(11).setCellValue(toStringValue(t.get("creatorName")));
            }

            int summaryRow = rowIdx + 1;
            Row labelRow = sheet.createRow(summaryRow);
            labelRow.createCell(0).setCellValue("任务统计");
            CellStyle boldStyle = sheet.getWorkbook().createCellStyle();
            Font boldFont = sheet.getWorkbook().createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);
            labelRow.getCell(0).setCellStyle(boldStyle);

            long todoCount = tasks.stream().filter(t -> "TODO".equals(toStringValue(t.get("status")))).count();
            long inProgressCount = tasks.stream().filter(t -> "IN_PROGRESS".equals(toStringValue(t.get("status")))).count();
            long doneCount = tasks.stream().filter(t -> "DONE".equals(toStringValue(t.get("status")))).count();
            double totalActualHours = tasks.stream()
                    .mapToDouble(t -> {
                        String v = toStringValue(t.get("actualHours"));
                        return v.isBlank() ? 0 : Double.parseDouble(v);
                    })
                    .sum();

            Row statRow1 = sheet.createRow(summaryRow + 1);
            statRow1.createCell(0).setCellValue("任务总数");
            statRow1.createCell(1).setCellValue(tasks.size());
            statRow1.createCell(2).setCellValue("待处理");
            statRow1.createCell(3).setCellValue(todoCount);

            Row statRow2 = sheet.createRow(summaryRow + 2);
            statRow2.createCell(0).setCellValue("进行中");
            statRow2.createCell(1).setCellValue(inProgressCount);
            statRow2.createCell(2).setCellValue("已完成");
            statRow2.createCell(3).setCellValue(doneCount);

            Row statRow3 = sheet.createRow(summaryRow + 3);
            statRow3.createCell(0).setCellValue("实际总学时");
            statRow3.createCell(1).setCellValue(totalActualHours + " 学时");
            statRow3.createCell(2).setCellValue("统计周期");
            statRow3.createCell(3).setCellValue(weekStart + " 至 " + weekEnd);

            autoSizeColumns(sheet, headers.length);
            String filename = "个人任务报表_" + weekStart + "_" + weekEnd + ".xlsx";
            writeResponse(response, workbook, filename);
        }
    }

    private void createHeaderRow(Sheet sheet, String[] headers) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            CellStyle style = sheet.getWorkbook().createCellStyle();
            Font font = sheet.getWorkbook().createFont();
            font.setBold(true);
            style.setFont(font);
            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cell.setCellStyle(style);
        }
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            if (width < 3000) {
                sheet.setColumnWidth(i, 3000);
            }
        }
    }

    private void writeResponse(HttpServletResponse response, Workbook workbook, String filename) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=" + encodedFilename);
        workbook.write(response.getOutputStream());
    }

    private String toStringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private int toNumberInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return (int) Math.round(Double.parseDouble(value.toString()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 教师（TEACHER）导出时限定数据范围为其负责或参与的项目；管理员不受限。
     */
    private Integer currentScopeUserId() {
        User current = CurrentUserContext.get();
        return (current != null && RoleEnum.TEACHER.name().equals(current.getRole()))
                ? current.getId() : null;
    }

    private String formatRole(String role) {
        return switch (role) {
            case "ADMIN" -> "管理员";
            case "TEACHER" -> "指导教师";
            case "STUDENT" -> "学生";
            default -> role;
        };
    }

    private String formatStatus(String status) {
        return "ENABLED".equals(status) ? "启用" : "禁用";
    }

    private String formatProjectStatus(String status) {
        return switch (status) {
            case "NOT_STARTED" -> "未开始";
            case "IN_PROGRESS" -> "进行中";
            case "COMPLETED" -> "已完成";
            case "DELAYED" -> "已延期";
            default -> status;
        };
    }

    private String formatTaskStatus(String status) {
        return switch (status) {
            case "TODO" -> "待处理";
            case "IN_PROGRESS" -> "进行中";
            case "REVIEW" -> "审核中";
            case "DONE" -> "已完成";
            case "BLOCKED" -> "已阻塞";
            default -> status;
        };
    }

    private String formatPriority(String priority) {
        return switch (priority) {
            case "HIGH" -> "高";
            case "MEDIUM" -> "中";
            case "LOW" -> "低";
            default -> priority;
        };
    }

    private String formatDateTime(java.time.LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.toString();
    }
}
