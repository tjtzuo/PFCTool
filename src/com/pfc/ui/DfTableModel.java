/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfc.ui;

import com.pfc.xml.DataFlash;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Joseph
 */
public class DfTableModel extends AbstractTableModel {
    boolean DEBUG = false;
    String[] columnNames = {java.util.ResourceBundle.getBundle("com/pfc/ui/Bundle").getString("NAME"), java.util.ResourceBundle.getBundle("com/pfc/ui/Bundle").getString("VALUE"), java.util.ResourceBundle.getBundle("com/pfc/ui/Bundle").getString("UNIT")};
    Object[][] data = {};

    public DfTableModel(List<DataFlash> dfList) {
//        columnNames = new String[]{"Name", "Value", "Unit"};
        if (dfList != null) {
            int count = dfList.size();
            data = new String[count][columnNames.length];
            for (int i = 0; i < count; i++) {
                DataFlash df = dfList.get(i);
                data[i][0] = df.getName();
                data[i][1] = df.getType().equals("-") ? "-" : "";
                data[i][2] = df.getUnit();
            }
        }
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return data.length;
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        return data[row][col];
    }

    /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell.  If we didn't implement this method,
         * then the last column would contain text ("true"/"false"),
         * rather than a check box.
     */
    @Override
    public Class getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    /*
         * Don't need to implement this method unless your table's
         * editable.
     */
    @Override
    public boolean isCellEditable(int row, int col) {
        //Note that the data/cell address is constant,
        //no matter where the cell appears onscreen.
        return (col == 1 && !data[row][1].toString().equals("-"));
    }

    /*
         * Don't need to implement this method unless your table's
         * data can change.
     */
    @Override
    public void setValueAt(Object value, int row, int col) {
        if (DEBUG) {
            System.out.println("Setting value at " + row + "," + col
                    + " to " + value
                    + " (an instance of "
                    + value.getClass() + ")");
        }

        data[row][col] = value;
        fireTableCellUpdated(row, col);

        if (DEBUG) {
            System.out.println("New value of data:");
            printDebugData();
        }
    }

    private void printDebugData() {
        int numRows = getRowCount();
        int numCols = getColumnCount();

        for (int i = 0; i < numRows; i++) {
            System.out.print("    row " + i + ":");
            for (int j = 0; j < numCols; j++) {
                System.out.print("  " + data[i][j]);
            }
            System.out.println();
        }
        System.out.println("--------------------------");
    }
}
