/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfc.ui;

import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;
import javax.swing.table.AbstractTableModel;
import org.ini4j.Ini;
import org.ini4j.IniPreferences;

/**
 *
 * @author Joseph
 */
public class ChemTableModel extends AbstractTableModel {
    boolean DEBUG = false;
    String[] columnNames = {"ID", "Manufacturer", "Model", "Description"};
    Object[][] data = {};

    public ChemTableModel() {
        int count = 0;
        try {
            Ini ini = new Ini(new File("../Chemistry/Chem.ini"));
            Preferences prefs = new IniPreferences(ini);
//        int num = Integer.parseInt(ini.get("Chemistry", "NumChemFiles"));
            int num = prefs.node("Chemistry").getInt("NumChemFiles", 0);
            for (Integer i = 1; i <= num; i++) {
                Preferences section = prefs.node(i.toString() + "_Chem");
//            System.out.println(section);
//            try {
//                for (String key : section.keys())
//                    System.out.println(key + ": "+ section.get(key, null));
//            } catch (BackingStoreException ex) {
//                System.err.println(ex);
//            }
//                System.out.println("ChemID : " + section.get("ChemID", null));
/*
                System.out.print("ChemID : ");
                String sChemID = section.get("ChemID", null);
                if (sChemID != null) {
                    String path = "../Chemistry/" + sChemID + ".chm";
                    byte[] buf = new byte[256];
                    if (DllEntry.dec128(path, buf) == 256) {
                        System.out.println(String.format("%02X%02X", buf[208], buf[209]));
                    
                    }
                }
*/
//                System.out.println("Description : " + section.get("Description", null));
                int n = section.getInt("NumKnownCells", 0);
//                for (Integer j = 1; j <= n; j++) {
//                    System.out.println(section.get(j.toString(), null));
//                }
                count += n;
            }
            data = new Object[count][columnNames.length];
            int k = 0;
            for (Integer i = 1; i <= num; i++) {
                Preferences section = prefs.node(i.toString() + "_Chem");
                String sChemID = section.get("ChemID", null),
                        sDesc = section.get("Description", null);
                int n = section.getInt("NumKnownCells", 0);
                for (Integer j = 1; j <= n; j++) {
                    data[k][0] = sChemID;
                    data[k][1] = "";
                    data[k][2] = "";
                    data[k][3] = sDesc;
                    String str = section.get(j.toString(), null);
                    if (str != null) {
                        String[] ss = str.split(":");
                        data[k][1] = ss[0];
                        if (ss.length > 1)
                            data[k][2] = ss[1];
                    }
                    k++;
                }
            }
        } catch (IOException ex) {
            System.err.println(ex);
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
*/
}
