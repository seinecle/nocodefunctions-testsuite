/*
 * Copyright Clement Levallois 2021-2023. License Attribution 4.0 Intertnational (CC BY 4.0)
 */
package net.clementlevallois.nocodeapp.testingsuite.utils;

import com.monitorjbl.xlsx.StreamingReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.clementlevallois.importers.model.CellRecord;
import net.clementlevallois.importers.model.ColumnModel;
import net.clementlevallois.importers.model.SheetModel;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import static org.apache.poi.ss.usermodel.CellType.BLANK;
import static org.apache.poi.ss.usermodel.CellType.BOOLEAN;
import static org.apache.poi.ss.usermodel.CellType.ERROR;
import static org.apache.poi.ss.usermodel.CellType.FORMULA;
import static org.apache.poi.ss.usermodel.CellType.NUMERIC;
import static org.apache.poi.ss.usermodel.CellType.STRING;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 *
 * @author LEVALLOIS
 */
public class ExcelReader {

    public static List<SheetModel> readExcelFile(Path pathToExcelFile) throws FileNotFoundException, IOException {

        List<SheetModel> sheets = new ArrayList();

        InputStream is = Files.newInputStream(pathToExcelFile);

        try (Workbook wb = StreamingReader.builder()
                .rowCacheSize(100) // number of rows to keep in memory (defaults to 10)
                .bufferSize(4096) // buffer size to use when reading InputStream to file (defaults to 1024)
                .open(is)) {
            int sheetNumber = 0;
            for (Sheet sheet : wb) {
                sheetNumber++;
                List<ColumnModel> headerNames = new ArrayList();
                SheetModel sheetModel = new SheetModel();
                sheetModel.setName(sheet.getSheetName());
                int rowNumber = 0;

                int leftiestColumnIndex = -1;

                for (Row r : sheet) {
                    if (rowNumber == 0) {
                        for (Cell cell : r) {
                            if (cell == null) {
                                continue;
                            }
                            int columnIndex = cell.getColumnIndex();
                            int rowIndex = cell.getRowIndex();

                            // this condition has for effect to store permanently the column index of the first column non empty on the left - which might not always be the one at index 0
                            if ((leftiestColumnIndex == -1) && columnIndex > leftiestColumnIndex) {
                                leftiestColumnIndex = columnIndex;
                            }

                            String cellStringValue = ExcelReader.returnStringValue(cell);

                            // adding the first line as a header
                            ColumnModel cmHeader = new ColumnModel(String.valueOf(columnIndex), cellStringValue);
                            headerNames.add(cmHeader);

                            CellRecord cellRecord = new CellRecord(rowIndex, columnIndex, cellStringValue);
                            sheetModel.addCellRecord(cellRecord);
                        }
                        sheetModel.setTableHeaderNames(headerNames);
                    }
                    rowNumber++;

                    for (Cell cell : r) {
                        if (cell == null) {
                            continue;
                        }

                        int columnIndex = cell.getColumnIndex();
                        int rowIndex = cell.getRowIndex();

                        String returnStringValue = ExcelReader.returnStringValue(cell);

                        CellRecord cellRecord = new CellRecord(rowIndex, columnIndex, returnStringValue);
                        sheetModel.addCellRecord(cellRecord);
                    }
                }
                sheets.add(sheetModel);
            }
        }
        return sheets;
    }

    public static String returnStringValue(Cell cell) {
        CellType cellType = cell.getCellType();

        switch (cellType) {
            case NUMERIC -> {
                try {
                    double doubleVal = cell.getNumericCellValue();
                    if (doubleVal == (int) doubleVal) {
                        int value = Double.valueOf(doubleVal).intValue();
                        return String.valueOf(value);
                    } else {
                        return String.valueOf(doubleVal);
                    }
                } catch (java.lang.NumberFormatException e) {
                    System.out.println("error reading double value in cell");
                    System.out.println("error: " + e.getMessage());
                    System.out.println("returning 0");
                    return "0";
                }
            }
            case STRING -> {
                return cell.getStringCellValue();
            }
            case ERROR -> {
                return String.valueOf(cell.getErrorCellValue());
            }
            case BLANK -> {
                return "";
            }
            case FORMULA -> {
                return cell.getCellFormula();
            }
            case BOOLEAN -> {
                return String.valueOf(cell.getBooleanCellValue());
            }
        }
        return "error decoding string value of the cell";
    }

}
