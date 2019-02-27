/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfc.ui;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author Joseph
 */
public class BitsColorRenderer implements TableCellRenderer {
    public static final DefaultTableCellRenderer DEFAULT_RENDERER = new DefaultTableCellRenderer();

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = DEFAULT_RENDERER.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, row, column);

        if (column < 3) {
            c.setBackground(Color.WHITE);
        } else {
            int val = Integer.decode((String) table.getValueAt(row, 2));
            if ((val & (1<<(10-column))) != 0) {
//                c.setBackground(Color.RED);
                    c.setBackground(Color.PINK);
            } else {
                if (((String)value).equals("RSVD"))
                    c.setBackground(Color.LIGHT_GRAY);
                else
//                    c.setBackground(new Color(0, 192, 0));
                    c.setBackground(new Color(175, 255, 175));
            }
        }

        return c;
    }

}
