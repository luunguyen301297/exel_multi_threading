package com.example.demo.utils;

import com.example.demo.model.FileItem;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@SuppressWarnings({"unused","Duplicates"})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExcelUtils {

    static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    static String TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    static String TYPE_MACRO = "application/vnd.ms-excel.sheet.macroEnabled.12";

    public static boolean hasExcelFormat(MultipartFile file) {
        return TYPE.equals(file.getContentType()) || TYPE_MACRO.equalsIgnoreCase(file.getContentType());
    }

    public static void generateFileHeader(String filePath, String title,
                                          List<String> fileDescriptions, List<String> headers) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            int rowNum = 0;

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setFontHeightInPoints((short) 20);
            titleFont.setBold(true);
            titleStyle.setFont(titleFont);

            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            titleCell.setCellStyle(titleStyle);

            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, headers.size() - 1));

            for (String description : fileDescriptions) {
                Row descriptionRow = sheet.createRow(rowNum);
                Cell descriptionCell = descriptionRow.createCell(0);
                descriptionCell.setCellValue(description);
                sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, headers.size() - 1));
                rowNum++;
            }

            Row headerRow = sheet.createRow(rowNum);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
            log.info("Excel file written successfully at: {}", filePath);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static FileItem loadFileAsResource(String filePath) {
        try {
            Path file = Paths.get(filePath);
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(file));
            return FileItem.builder()
                    .fileName(file.getFileName().toString())
                    .resource(resource)
                    .build();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static void writeWorkBookToFile(Workbook workbook, String filename) {
        try (FileOutputStream fileOut = new FileOutputStream(filename)) {
            workbook.write(fileOut);
        } catch (IOException e) {
            log.error("Error writing to file: {}", e.getMessage());
        }
    }

    public static void cleanUp(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", path);
        }
    }

    public static <T> List<String> extractHeadersFromObjects(Class<T> t) {
        List<String> headers = new ArrayList<>();
        headers.add("NO");
        Field[] fields = t.getDeclaredFields();
        for (Field field : fields) {
            headers.add(field.getName().toUpperCase());
        }
        return headers;
    }

    public static <T> void mapObjectToRow(Row row, T t, int headerLastRowIndex, int dataRowStartIndex) {
        int column = 0;
        row.createCell(column++).setCellValue(dataRowStartIndex - headerLastRowIndex - 1);

        Field[] fields = t.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(t);
                if (value != null) {
                    switch (value.getClass().getSimpleName()) {
                        case "LocalDateTime" ->
                                row.createCell(column++).setCellValue(((LocalDateTime) value).format(DATE_FORMATTER));
                        case "LocalDate" ->
                                row.createCell(column++).setCellValue(((LocalDate) value).format(DATE_FORMATTER));
                        case "OffsetDateTime" ->
                                row.createCell(column++).setCellValue(((OffsetDateTime) value).format(DATE_FORMATTER));
                        case "Date" ->
                                row.createCell(column++).setCellValue(SIMPLE_DATE_FORMAT.format((Date) value));
                        case "String" ->
                                row.createCell(column++).setCellValue((String) value);
                        case "Integer", "Double", "Float", "Long", "Short" ->
                                row.createCell(column++).setCellValue(((Number) value).doubleValue());
                        case "BigDecimal" ->
                                row.createCell(column++).setCellValue(((BigDecimal) value).doubleValue());
                        case "Boolean" ->
                                row.createCell(column++).setCellValue((Boolean) value);
                        default ->
                                row.createCell(column++).setCellValue(value.toString());
                    }
                } else {
                    row.createCell(column++).setCellValue("");
                }
            } catch (IllegalAccessException e) {
                log.error("Error accessing field: {}", field.getName(), e);
            }
        }
    }

    public static boolean isValidExcelFile(MultipartFile file) {
        return Objects.equals(file.getContentType(), TYPE);
    }

}
