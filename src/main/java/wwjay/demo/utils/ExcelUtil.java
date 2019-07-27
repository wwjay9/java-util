package wwjay.demo.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * excel工具
 *
 * @author wwj
 */
@SuppressWarnings({"unused", "WeakerAccess", "SpellCheckingInspection"})
public class ExcelUtil {

    private static final DataFormatter DATA_FORMATTER = new DataFormatter();
    public static final int SEARCH_UP = 1;
    public static final int SEARCH_RIGHT = 2;
    public static final int SEARCH_DOWN = 3;
    public static final int SEARCH_LEFT = 4;

    private ExcelUtil() {
    }

    /**
     * 向单元格写数据
     *
     * @param sheet   工作表
     * @param value   值
     * @param row     行索引
     * @param col     列索引
     * @param spanRow 合并的行数
     */
    public static void writeCellValue(Sheet sheet, Object value, int row, int col, int spanRow) {
        // 只有value不为null，并且是合并单元格的左上角单元格时才写入数据
        if (value == null) {
            return;
        }
        if (getMergedCell(sheet, row, col) != null && !testMergedFirstCell(sheet, row, col)) {
            return;
        }
        Cell cell = sheet.getRow(row).getCell(col);

        // 设置合适的单元格格式
        if (value instanceof Number) {
            cell.setCellValue(Double.parseDouble(value.toString()));
        } else {
            cell.setCellValue(value.toString());
        }

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
        if (insertNumber <= 0) {
            return;
        }
        // 插入位置的行
        Row sourceRow = sheet.getRow(startRow);
        // 如果插入的行不存在则创建新行
        if (sourceRow == null) {
            sourceRow = sheet.createRow(insertNumber);
        }
        // 从插入行开始到最后一行向下移动
        // TODO poi4.x版本中shiftRows方法存在bug，https://bz.apache.org/bugzilla/show_bug.cgi?id=57423
        sheet.shiftRows(startRow, sheet.getLastRowNum(), insertNumber, true, false);

        // 填充移动后留下的空行
        for (int i = startRow; i < startRow + insertNumber; i++) {
            Row row = sheet.createRow(i);
            row.setHeightInPoints(sourceRow.getHeightInPoints());
            short lastCellNum = sourceRow.getLastCellNum();
            for (int x = 0; x < lastCellNum; x++) {
                Cell cell = row.createCell(x);
                cell.setCellStyle(sourceRow.getCell(x).getCellStyle());
            }
        }
    }

    /**
     * 在工作表中清除startRow到endRow之间的所有数据(包含开始行和结束行)并保留单元格原格式
     *
     * @param sheet    工作表
     * @param startRow 开始行
     * @param endRow   结束行
     */
    public static void cleanData(Sheet sheet, int startRow, int endRow) {
        if (endRow < startRow) {
            throw new InvalidParameterException("开始行必须小于结束行");
        }
        for (int i = startRow; i <= endRow; i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                for (int y = 0; y <= row.getLastCellNum(); y++) {
                    Cell cell = row.getCell(y);
                    if (cell != null) {
                        cell.setBlank();
                    }
                }
            }
        }
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
        if (lastRow < firstRow || lastCol < firstCol) {
            throw new InvalidParameterException("参数不正确");
        }
        for (int i = firstRow; i <= lastRow; i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                for (int y = firstCol; y <= lastCol; y++) {
                    Cell cell = row.getCell(y);
                    if (cell != null) {
                        cell.setBlank();
                    }
                }
            }
        }
    }

    /**
     * 在工作表中删除startRow到endRow之间的所有行(包含开始行和结束行)
     *
     * @param sheet    工作表
     * @param startRow 开始行
     * @param endRow   结束行
     */
    public static void remove(Sheet sheet, int startRow, int endRow) {
        if (endRow < startRow) {
            throw new InvalidParameterException("开始行必须小于结束行");
        }
        for (int i = startRow; i <= endRow; i++) {
            remove(sheet, startRow);
        }
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
     * @param sheet 工作表
     * @param row   行索引
     * @param col   列索引
     * @return 合并单元格的范围
     */
    public static CellRangeAddress getMergedCell(Sheet sheet, int row, int col) {
        if (sheet == null || row < 0 || col < 0) {
            throw new InvalidParameterException("参数错误");
        }
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
        if (sheet == null || row < 0 || col < 0) {
            throw new InvalidParameterException("参数错误");
        }
        return Optional.ofNullable(sheet.getMergedRegions())
                .map(cras -> cras.stream().anyMatch(cra -> cra.getFirstRow() == row && cra.getFirstColumn() == col))
                .orElse(false);
    }

    /**
     * 获取单元格的值，包括合并单元格的
     *
     * @param sheet 工作表
     * @param row   行索引
     * @param col   列索引
     * @return 单元格的值
     */
    public static String getCellValue(Sheet sheet, int row, int col) {
        if (sheet == null || row < 0 || col < 0) {
            return null;
        }
        List<CellRangeAddress> craList = sheet.getMergedRegions();
        if (sheet.getRow(row) == null) {
            return null;
        }
        Cell cell = sheet.getRow(row).getCell(col);
        // WPS的合并单元格只有左上角的单元格有值
        if (cell == null) {
            return Optional.ofNullable(craList)
                    .map(mergedRegions -> {
                        for (CellRangeAddress cra : mergedRegions) {
                            if (cra.isInRange(row, col)) {
                                Cell firstCell = sheet.getRow(cra.getFirstRow()).getCell(cra.getFirstColumn());
                                if (firstCell != null) {
                                    return DATA_FORMATTER.formatCellValue(firstCell);
                                }
                            }
                        }
                        return null;
                    })
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
    }

    /**
     * 从左上角开始搜索首个关键字附近的值
     *
     * @param sheet     工作表
     * @param keyword   关键字
     * @param direction 方向,1:上、2:右、3:下、4:左
     * @return 关键字附近的值
     */
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
    public static Cell searchCell(Sheet sheet, String keyword) {
        if (sheet == null || !StringUtils.hasText(keyword)) {
            return null;
        }
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                for (int y = 0; y <= row.getLastCellNum(); y++) {
                    if (Objects.equals(keyword, getCellValue(sheet, i, y))) {
                        return sheet.getRow(i).getCell(y);
                    }
                }
            }
        }
        return null;
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
        byte[] file;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            workbook.write(bos);
            file = bos.toByteArray();
            bos.close();
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return file;
    }
}
