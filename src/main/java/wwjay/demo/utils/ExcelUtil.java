package wwjay.demo.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbookFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * excel工具
 *
 * @author wwj
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ExcelUtil {

    public static final int SEARCH_UP = 1;
    public static final int SEARCH_RIGHT = 2;
    public static final int SEARCH_DOWN = 3;
    public static final int SEARCH_LEFT = 4;
    private static final DataFormatter DATA_FORMATTER = new DataFormatter();
    private static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_FORMAT);

    private ExcelUtil() {
    }

    /**
     * 创建一个简单的Excel表格，并将表格写入临时文件中
     *
     * @param data 需要写入的数据
     * @param <T>  表格中的表头根据对象字段的{@link ExcelProperty}注解生成，
     *             所有带有{@link ExcelProperty}注解的字段都将写入表格中
     * @return 临时文件路径
     */
    public static <T> Path writeToTempFile(List<T> data) {
        Assert.notEmpty(data, "数据不能为空");
        List<ColumnProperty> columnProperty = getExcelProperty(data.get(0).getClass());
        Assert.notEmpty(columnProperty, "没有需要写入的字段");

        Workbook workbook = XSSFWorkbookFactory.createWorkbook();
        Sheet sheet = workbook.createSheet();

        // 写入表头
        Row headerRow = sheet.createRow(0);
        IntStream.range(0, columnProperty.size()).forEachOrdered(i -> {
            ColumnProperty property = columnProperty.get(i);
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(property.getHeaderName());
            Integer colWidth = property.getColWidth();
            if (colWidth != null) {
                sheet.setColumnWidth(i, colWidth);
            }
        });

        // 写入行数据
        IntStream.range(0, data.size()).forEachOrdered(i -> {
            T t = data.get(i);
            BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(t);
            Row row = sheet.createRow(i + 1);
            IntStream.range(0, columnProperty.size()).forEachOrdered(j -> {
                ColumnProperty cp = columnProperty.get(j);
                Object value = beanWrapper.getPropertyValue(cp.getFieldName());
                Cell cell = row.createCell(j);
                setCellValue(cell, value);
            });
        });
        return writeToTempFile(workbook);
    }

    /**
     * 将Workbook写入临时文件
     *
     * @param workbook 表格
     * @return 临时文件目录
     */
    public static Path writeToTempFile(Workbook workbook) {
        Path excelFilePath;
        try {
            excelFilePath = Files.createTempFile("excel", ".xlsx");
        } catch (IOException e) {
            throw new RuntimeException("创建临时文件失败", e);
        }
        try (workbook; FileOutputStream os = new FileOutputStream(excelFilePath.toFile())) {
            workbook.write(os);
        } catch (IOException e) {
            throw new RuntimeException("写入Excel失败", e);
        }
        return excelFilePath;
    }

    /**
     * 向单元格写数据，只有value不为null，并且是合并单元格的左上角单元格时才写入数据
     *
     * @param cell  单元格
     * @param value 值
     */
    public static void writeCellValue(Cell cell, Object value) {
        Assert.notNull(cell, "单元格不能为空");
        writeCellValue(cell.getSheet(), value, cell.getRowIndex(), cell.getColumnIndex());
    }

    /**
     * 向单元格写数据，只有value不为null，并且是合并单元格的左上角单元格时才写入数据
     *
     * @param sheet 工作表
     * @param value 值
     * @param row   行索引
     * @param col   列索引
     */
    public static void writeCellValue(Sheet sheet, Object value, int row, int col) {
        writeCellValue(sheet, value, row, col, 0);
    }

    /**
     * 向单元格写数据，只有value不为null，并且是合并单元格的左上角单元格时才写入数据
     *
     * @param sheet   工作表
     * @param value   值
     * @param row     行索引
     * @param col     列索引
     * @param spanRow 合并的行数
     */
    public static void writeCellValue(Sheet sheet, Object value, int row, int col, int spanRow) {
        if (value == null) {
            return;
        }
        if (getMergedCell(sheet, row, col) != null && !testMergedFirstCell(sheet, row, col)) {
            return;
        }
        Cell cell = sheet.getRow(row).getCell(col);

        // 设置合适的单元格格式
        setCellValue(cell, value);

        // 添加合并单元格
        if (spanRow > 1) {
            sheet.addMergedRegion(new CellRangeAddress(row, row + spanRow - 1, col, col));
        }
    }

    /**
     * 插入行
     *
     * @param sheet        工作表
     * @param startRow     插入的起始行
     * @param insertNumber 插入的行数
     */
    public static void insertRow(Sheet sheet, int startRow, int insertNumber) {
        Assert.isTrue(insertNumber > 0, "插入的行数必须大于0");
        // 插入位置的行，如果插入的行不存在则创建新行
        Row sourceRow = Optional.ofNullable(sheet.getRow(startRow)).orElseGet(() -> sheet.createRow(insertNumber));
        // 从插入行开始到最后一行向下移动
        sheet.shiftRows(startRow, sheet.getLastRowNum(), insertNumber, true, false);

        // 填充移动后留下的空行
        IntStream.range(startRow, startRow + insertNumber).forEachOrdered(i -> {
            Row row = sheet.createRow(i);
            row.setHeightInPoints(sourceRow.getHeightInPoints());
            short lastCellNum = sourceRow.getLastCellNum();
            IntStream.range(0, lastCellNum).forEachOrdered(j -> {
                Cell cell = row.createCell(j);
                cell.setCellStyle(sourceRow.getCell(j).getCellStyle());
            });
        });
    }

    /**
     * 在工作表中清除startRow到endRow之间的所有数据(包含开始行和结束行)并保留单元格原格式
     *
     * @param sheet    工作表
     * @param startRow 开始行
     * @param endRow   结束行
     */
    public static void cleanData(Sheet sheet, int startRow, int endRow) {
        Assert.isTrue(endRow >= startRow, "结束行必须大于等于开始行");
        IntStream.rangeClosed(startRow, endRow)
                .mapToObj(sheet::getRow)
                .filter(Objects::nonNull)
                .forEachOrdered(row ->
                        IntStream.rangeClosed(0, row.getLastCellNum())
                                .mapToObj(row::getCell)
                                .filter(Objects::nonNull)
                                .forEachOrdered(Cell::setBlank));
    }

    /**
     * 在工作表中删除指定单元格区间的数据(包含左上角和右下角单元格)并保留单元格原格式
     *
     * @param sheet    工作表
     * @param firstRow 左上角单元格的行
     * @param firstCol 左上角单元格的列
     * @param lastRow  右下角单元格的行
     * @param lastCol  右下角单元格的列
     */
    public static void cleanData(Sheet sheet, int firstRow, int firstCol, int lastRow, int lastCol) {
        Assert.isTrue(lastRow >= firstRow && lastCol >= firstCol, "参数不正确");
        IntStream.rangeClosed(firstRow, lastRow)
                .mapToObj(sheet::getRow)
                .filter(Objects::nonNull)
                .forEachOrdered(row ->
                        IntStream.rangeClosed(firstCol, lastCol)
                                .mapToObj(row::getCell)
                                .filter(Objects::nonNull)
                                .forEachOrdered(Cell::setBlank));
    }

    /**
     * 在工作表中删除startRow到endRow之间的所有行(包含开始行和结束行)
     *
     * @param sheet    工作表
     * @param startRow 开始行
     * @param endRow   结束行
     */
    public static void remove(Sheet sheet, int startRow, int endRow) {
        Assert.isTrue(endRow >= startRow, "结束行必须大于等于开始行");
        IntStream.rangeClosed(startRow, endRow).forEachOrdered(i -> remove(sheet, startRow));
    }

    /**
     * 从工作表中删除指定的行
     *
     * @param sheet    工作表
     * @param rowIndex 行索引
     */
    public static void remove(Sheet sheet, int rowIndex) {
        int lastRow = sheet.getLastRowNum();
        if (rowIndex >= 0 && rowIndex < lastRow) {
            sheet.shiftRows(rowIndex + 1, lastRow, -1);
        }
        if (rowIndex == lastRow) {
            Row removingRow = sheet.getRow(rowIndex);
            if (removingRow != null) {
                sheet.removeRow(removingRow);
            }
        }
    }

    /**
     * 获取合并单元格的范围地址，如果不是合并单元格返回null
     *
     * @param cell 单元格
     * @return 合并单元格的范围
     */
    @Nullable
    public static CellRangeAddress getMergedCell(Cell cell) {
        return getMergedCell(cell.getSheet(), cell.getRowIndex(), cell.getColumnIndex());
    }

    /**
     * 获取合并单元格的范围地址，如果不是合并单元格返回null
     *
     * @param sheet 工作表
     * @param row   行索引
     * @param col   列索引
     * @return 合并单元格的范围
     */
    @Nullable
    public static CellRangeAddress getMergedCell(Sheet sheet, int row, int col) {
        Assert.isTrue(sheet != null && row >= 0 && col >= 0, "参数错误");
        for (CellRangeAddress range : sheet.getMergedRegions()) {
            if (range.isInRange(row, col)) {
                return range;
            }
        }
        return null;
    }

    /**
     * 测试指定单元格是否是所属合并单元格的左上角单元格
     *
     * @param sheet 工作表
     * @param row   行索引
     * @param col   列索引
     * @return 如果是所在合并单元格的左上角单元格时返回true，否则返回false
     */
    public static boolean testMergedFirstCell(Sheet sheet, int row, int col) {
        Assert.isTrue(sheet != null && row >= 0 && col >= 0, "参数错误");
        return Optional.ofNullable(sheet.getMergedRegions())
                .map(mr -> mr.stream().anyMatch(cra -> cra.getFirstRow() == row && cra.getFirstColumn() == col))
                .orElse(false);
    }

    /**
     * 拆分cell所在的合并单元格
     *
     * @param cell 单元格
     */
    public static void splitCell(Cell cell) {
        Assert.notNull(cell, "单元格不能为空");
        Sheet sheet = cell.getSheet();
        IntStream.range(0, sheet.getNumMergedRegions())
                .filter(i -> sheet.getMergedRegion(i).isInRange(cell))
                .forEachOrdered(sheet::removeMergedRegion);
    }

    /**
     * 获取单元格的值，包括合并单元格的
     *
     * @param cell 单元格
     * @return 单元格的值
     */
    @Nullable
    public static String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        return getCellValue(cell.getSheet(), cell.getRowIndex(), cell.getColumnIndex());
    }

    /**
     * 获取单元格的值，包括合并单元格的
     *
     * @param sheet 工作表
     * @param row   行索引
     * @param col   列索引
     * @return 单元格的值
     */
    @Nullable
    public static String getCellValue(Sheet sheet, int row, int col) {
        Assert.isTrue(sheet != null && row >= 0 && col >= 0, "参数错误");
        List<CellRangeAddress> craList = sheet.getMergedRegions();
        if (sheet.getRow(row) == null) {
            return null;
        }
        Cell cell = sheet.getRow(row).getCell(col);
        // WPS的合并单元格只有左上角的单元格有值
        if (cell == null) {
            return Optional.ofNullable(craList).stream()
                    .flatMap(List::stream)
                    .filter(cra -> cra.isInRange(row, col))
                    .findFirst()
                    .map(cra -> sheet.getRow(cra.getFirstRow()).getCell(cra.getFirstColumn()))
                    .map(DATA_FORMATTER::formatCellValue)
                    .orElse(null);
        }
        String value = DATA_FORMATTER.formatCellValue(cell);
        // Microsoft Office 的合并单元格值为""
        if (Objects.equals(value, "")) {
            for (CellRangeAddress cra : craList) {
                if (cra.isInRange(cell)) {
                    value = DATA_FORMATTER.formatCellValue(sheet.getRow(cra.getFirstRow()).getCell(cra.getFirstColumn()));
                }
            }
        }
        return value;
    }

    /**
     * 插入求和公式
     *
     * @param sheet       工作表
     * @param formulaRow  公式单元格所在行索引
     * @param formulaCol  公式单元格所在列索引
     * @param sumStartRow 求和的开始行
     * @param sumEndRow   求和的结束行
     */
    public static void insertSumFormula(Sheet sheet, int formulaRow, int formulaCol, int sumStartRow, int sumEndRow) {
        // 修改公式
        Cell sumCell = sheet.getRow(formulaRow).getCell(formulaCol);
        String startAddress = sheet.getRow(sumStartRow).getCell(formulaCol).getAddress().toString();
        String endAddress = sheet.getRow(sumEndRow).getCell(formulaCol).getAddress().toString();
        String cellFormula = "SUM(" + startAddress + ":" + endAddress + ")";
        sumCell.setCellFormula(cellFormula);
        // 插入公式以后让公式生效
        sheet.getWorkbook().getCreationHelper().createFormulaEvaluator().evaluateAll();
    }

    /**
     * 从左上角开始搜索首个关键字附近的值
     *
     * @param sheet     工作表
     * @param keyword   关键字
     * @param direction 方向,1:上、2:右、3:下、4:左
     * @return 关键字附近的值
     */
    @Nullable
    public static String searchNearby(Sheet sheet, String keyword, int direction) {
        if (sheet == null || !StringUtils.hasText(keyword)) {
            return null;
        }
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                for (int y = 0; y <= row.getLastCellNum(); y++) {
                    if (Objects.equals(keyword, getCellValue(sheet, i, y))) {
                        if (direction == SEARCH_UP) {
                            return getCellValue(sheet, i - 1, y);
                        } else if (direction == SEARCH_RIGHT) {
                            return getCellValue(sheet, i, y + 1);
                        } else if (direction == SEARCH_DOWN) {
                            return getCellValue(sheet, i + 1, y);
                        } else if (direction == SEARCH_LEFT) {
                            return getCellValue(sheet, i, y - 1);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 搜索首个关键字的单元格
     *
     * @param sheet   工作表
     * @param keyword 关键字
     * @return 单元格
     */
    @Nullable
    public static Cell searchCell(Sheet sheet, String keyword) {
        if (sheet == null || !StringUtils.hasText(keyword)) {
            return null;
        }
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                for (int y = 0; y <= row.getLastCellNum(); y++) {
                    Cell cell = row.getCell(y);
                    if (Objects.equals(keyword, getCellValue(cell))) {
                        return cell;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 将单元格设置为水平对齐
     *
     * @param cell 单元格
     */
    public static void setHorizontalAlignment(Cell cell) {
        CellStyle cellStyle = cell.getSheet().getWorkbook().createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cell.setCellStyle(cellStyle);
    }

    /**
     * 将Workbook转换成byte[]，并关闭流
     *
     * @param workbook 工作表
     * @return 字节数组
     */
    public static byte[] workbookToByteArray(Workbook workbook) {
        if (workbook == null) {
            return null;
        }
        try (workbook; ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            workbook.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("workbook转换失败", e);
        }
    }

    /**
     * 自动根据value类型设置合适的单元格值
     *
     * @param cell  单元格
     * @param value 单元格值
     */
    private static void setCellValue(Cell cell, Object value) {
        if (cell == null || value == null) {
            return;
        }
        if (value instanceof Number) {
            cell.setCellValue(Double.parseDouble(value.toString()));
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Date) {
            cell.setCellValue(new SimpleDateFormat(DEFAULT_FORMAT).format((Date) value));
        } else if (value instanceof LocalDateTime) {
            cell.setCellValue(((LocalDateTime) value).format(FORMATTER));
        } else if (value instanceof LocalDate) {
            cell.setCellValue((LocalDate) value);
        } else if (value instanceof Calendar) {
            cell.setCellValue((Calendar) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }

    /**
     * 获取申明了ExcelProperty注解字段的值
     */
    private static <T> List<ColumnProperty> getExcelProperty(Class<T> clazz) {
        return Stream.of(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(ExcelProperty.class))
                .map(field -> {
                    String name = field.getName();
                    ExcelProperty property = field.getAnnotation(ExcelProperty.class);
                    return new ColumnProperty(name, property.value(), property.width());
                })
                .collect(Collectors.toList());
    }

    private static class ColumnProperty {

        private String fieldName;
        private String headerName;
        private short colWidth;

        private ColumnProperty(String fieldName, String headerName, short colWidth) {
            Assert.isTrue(colWidth < 255, "列宽最大不能超过255个字符");
            this.fieldName = fieldName;
            this.headerName = headerName;
            this.colWidth = colWidth;
        }

        private String getFieldName() {
            return fieldName;
        }

        private String getHeaderName() {
            return headerName;
        }

        private Integer getColWidth() {
            return colWidth > -1 ? colWidth * 256 : null;
        }
    }
}
