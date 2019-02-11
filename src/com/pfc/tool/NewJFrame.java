package com.pfc.tool;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import com.pfc.lib.DllEntry;
import com.pfc.lib.UsbSmb;
import com.pfc.ui.*;
import com.pfc.xml.BitField;
import com.pfc.xml.SBS;
import com.pfc.xml.sax.SBSHandler;
import com.pfc.xml.Cluster;
import com.pfc.xml.DataFlash;
import com.pfc.xml.sax.BitsHandler;
import com.pfc.xml.sax.DataFlashHandler;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

/**
 *
 * @author Joseph
 */
public class NewJFrame extends javax.swing.JFrame {
    boolean DEBUG = false;
    public static UsbSmb usbSmb;
    SbsTableModel sbsTableModel;
    BitsTableModel bitsTableModel;
    List<SBS> sbsList;
    List<BitField> bfList;
    List<Cluster> clusterList;
    List<JTable> clusterTableList;
    byte[] dfBuf = new byte[2048];
    JFileChooser fc;
    int socCmd;
    int stradr;
    boolean bNewSBS = false;

    /**
     * Creates new form NewJFrame
     */
    public NewJFrame() {
        socCmd = bNewSBS ? 0x2C: 0x0d;
        stradr = bNewSBS ? 0x7600 : 0xB000;
        SBSHandler sbsHandler = new SBSHandler();
        BitsHandler bitsHandler = new BitsHandler();
        DataFlashHandler dfHandler = new DataFlashHandler();
        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            SAXParser saxParser = saxParserFactory.newSAXParser();
            saxParser.parse(new File("SBS.xml"), sbsHandler);
            saxParser.parse(new File("DataFlash.xml"), dfHandler);
            saxParser.parse(new File("Bits.xml"), bitsHandler);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            System.out.println(e);
        }

        //Get SBS list
        sbsList = sbsHandler.getSBSList();
        if (DEBUG) {
            //print sbs information
            sbsList.forEach((sbs) -> {
                System.out.println(sbs);
            });
        }
        sbsTableModel = new SbsTableModel(sbsList);

        //Get BitField list
        bfList = bitsHandler.getBitFieldList();
        if (DEBUG) {
            //print bf information
            bfList.forEach((bf) -> {
                System.out.println(bf);
            });
        }
        bitsTableModel = new BitsTableModel(bfList);

        initComponents();

        // Set custom color renderer
        BitsColorRenderer colorRenderer = new BitsColorRenderer();
        jTableBits.setDefaultRenderer(String.class, colorRenderer);
        
        clusterTableList = new ArrayList<>();
        //Get DataFlashCluster list
        clusterList = dfHandler.getDFClusterList();
        for (Cluster cluster : clusterList) {
            //Get DataFlash list
            List<DataFlash> dfList = cluster.getDFList();
            if (DEBUG) {
                //print dataflash information
                System.out.println(cluster);
                dfList.forEach((df) -> {
                    System.out.println(df);
                });
            }
            DfTableModel dfTableModel = new DfTableModel(dfList);
            JTable jTableCluster = new JTable();
            jTableCluster.setModel(dfTableModel);
            JScrollPane jScrollPaneCluster = new JScrollPane();
            jScrollPaneCluster.setViewportView(jTableCluster);
            jTabbedPaneDataFlash.addTab(cluster.getName(), jScrollPaneCluster);
            clusterTableList.add(jTableCluster);
        }
        
        refreshSBS();

        //Create a file chooser
        fc = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("DataFlash Files (*.ifi)", "ifi");
        fc.setFileFilter(filter);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPaneMain = new javax.swing.JScrollPane();
        jTabbedPaneMain = new javax.swing.JTabbedPane();
        jPanelSBS = new javax.swing.JPanel();
        jScrollPaneSBS = new javax.swing.JScrollPane();
        jTableSBS = new javax.swing.JTable();
        jButtonRefresh = new javax.swing.JButton();
        jProgressBarSOC = new javax.swing.JProgressBar();
        jScrollPaneBits = new javax.swing.JScrollPane();
        jTableBits = new javax.swing.JTable();
        jLabelSOC = new javax.swing.JLabel();
        jPanelDataFlash = new javax.swing.JPanel();
        jTabbedPaneDataFlash = new javax.swing.JTabbedPane();
        jButtonReadAll = new javax.swing.JButton();
        jButtonWriteAll = new javax.swing.JButton();
        jButtonDefault = new javax.swing.JButton();
        jButtonExport = new javax.swing.JButton();
        jButtonImport = new javax.swing.JButton();
        jProgressBar1 = new javax.swing.JProgressBar();
        jPanelCommand = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jTabbedPaneMain.setToolTipText("");
        jTabbedPaneMain.setFont(new java.awt.Font("Dialog", 0, 20)); // NOI18N
        jTabbedPaneMain.setPreferredSize(new java.awt.Dimension(720, 540));

        jPanelSBS.setPreferredSize(new java.awt.Dimension(635, 415));

        jTableSBS.setModel(sbsTableModel);
        jTableSBS.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        jTableSBS.getTableHeader().setReorderingAllowed(false);
        jScrollPaneSBS.setViewportView(jTableSBS);

        jButtonRefresh.setText("Refresh");
        jButtonRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRefreshActionPerformed(evt);
            }
        });

        jProgressBarSOC.setStringPainted(true);

        jTableBits.setModel(bitsTableModel);
        jTableBits.setToolTipText(null);
        jTableBits.setRowSelectionAllowed(false);
        jTableBits.getTableHeader().setReorderingAllowed(false);
        jScrollPaneBits.setViewportView(jTableBits);

        jLabelSOC.setText("SOC :");

        javax.swing.GroupLayout jPanelSBSLayout = new javax.swing.GroupLayout(jPanelSBS);
        jPanelSBS.setLayout(jPanelSBSLayout);
        jPanelSBSLayout.setHorizontalGroup(
            jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSBSLayout.createSequentialGroup()
                .addGroup(jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelSBSLayout.createSequentialGroup()
                        .addGap(35, 35, 35)
                        .addGroup(jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelSBSLayout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(jLabelSOC)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jProgressBarSOC, javax.swing.GroupLayout.PREFERRED_SIZE, 395, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanelSBSLayout.createSequentialGroup()
                                .addComponent(jScrollPaneSBS, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jButtonRefresh))))
                    .addGroup(jPanelSBSLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPaneBits, javax.swing.GroupLayout.PREFERRED_SIZE, 599, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(106, Short.MAX_VALUE))
        );
        jPanelSBSLayout.setVerticalGroup(
            jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSBSLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonRefresh)
                    .addComponent(jScrollPaneSBS, javax.swing.GroupLayout.PREFERRED_SIZE, 290, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jProgressBarSOC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelSOC))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 7, Short.MAX_VALUE)
                .addComponent(jScrollPaneBits, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPaneMain.addTab("SBS", jPanelSBS);

        jTabbedPaneDataFlash.setTabPlacement(javax.swing.JTabbedPane.LEFT);
        jTabbedPaneDataFlash.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N

        jButtonReadAll.setText("Read All");
        jButtonReadAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReadAllActionPerformed(evt);
            }
        });

        jButtonWriteAll.setText("Write All");
        jButtonWriteAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonWriteAllActionPerformed(evt);
            }
        });

        jButtonDefault.setText("Default");
        jButtonDefault.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDefaultActionPerformed(evt);
            }
        });

        jButtonExport.setText("Export");
        jButtonExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExportActionPerformed(evt);
            }
        });

        jButtonImport.setText("Import");
        jButtonImport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonImportActionPerformed(evt);
            }
        });

        jProgressBar1.setMaximum(2048);

        javax.swing.GroupLayout jPanelDataFlashLayout = new javax.swing.GroupLayout(jPanelDataFlash);
        jPanelDataFlash.setLayout(jPanelDataFlashLayout);
        jPanelDataFlashLayout.setHorizontalGroup(
            jPanelDataFlashLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelDataFlashLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelDataFlashLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPaneDataFlash, javax.swing.GroupLayout.PREFERRED_SIZE, 519, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanelDataFlashLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jProgressBar1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanelDataFlashLayout.createSequentialGroup()
                            .addComponent(jButtonReadAll)
                            .addGap(32, 32, 32)
                            .addComponent(jButtonWriteAll)
                            .addGap(34, 34, 34)
                            .addComponent(jButtonDefault)
                            .addGap(36, 36, 36)
                            .addComponent(jButtonExport)
                            .addGap(38, 38, 38)
                            .addComponent(jButtonImport))))
                .addContainerGap(186, Short.MAX_VALUE))
        );
        jPanelDataFlashLayout.setVerticalGroup(
            jPanelDataFlashLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelDataFlashLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPaneDataFlash, javax.swing.GroupLayout.PREFERRED_SIZE, 434, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanelDataFlashLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonReadAll)
                    .addComponent(jButtonWriteAll)
                    .addComponent(jButtonDefault)
                    .addComponent(jButtonExport)
                    .addComponent(jButtonImport))
                .addGap(18, 18, 18)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPaneMain.addTab("DataFlash", jPanelDataFlash);

        javax.swing.GroupLayout jPanelCommandLayout = new javax.swing.GroupLayout(jPanelCommand);
        jPanelCommand.setLayout(jPanelCommandLayout);
        jPanelCommandLayout.setHorizontalGroup(
            jPanelCommandLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 715, Short.MAX_VALUE)
        );
        jPanelCommandLayout.setVerticalGroup(
            jPanelCommandLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 548, Short.MAX_VALUE)
        );

        jTabbedPaneMain.addTab("Command", jPanelCommand);

        jScrollPaneMain.setViewportView(jTabbedPaneMain);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPaneMain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPaneMain, javax.swing.GroupLayout.DEFAULT_SIZE, 594, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRefreshActionPerformed
        refreshSBS();
    }//GEN-LAST:event_jButtonRefreshActionPerformed

    public void refreshSBS() {
        for (int i = 0; i < jTableSBS.getRowCount(); i++) {
            String str = "";
            SBS sbs = sbsList.get(i);
            String format = sbs.getFormat();
            int cmd = sbs.getCmd();
            if (format.equals("String")) {
                if (bNewSBS) {
                    byte[] pByte = new byte[1];
                    if (usbSmb.readByte(cmd - 1, pByte)) {
                    //if (usbSmb.readByte(cmd, pByte)) {
                        int len = Math.min(Byte.toUnsignedInt(pByte[0]), sbs.getSize());
                        byte[] name = new byte[len];
                        if (usbSmb.readBytes(cmd, len, name)) {
                        //if (usbSmb.readBytes(cmd + 1, len, name)) {
                            str = new String(name, 0, len);
                        }
                    }
                } else {
                    int size = sbs.getSize();
                    byte[] pByte = new byte[size + 1];
                    if (usbSmb.readBytes(cmd, size + 1, pByte)) {
                        str = new String(pByte, 1, Math.min(Byte.toUnsignedInt(pByte[0]), size));
                    }
                }
            }
            else {
                short[] pwValue = new short[1];
//                if (usbSmb.ReadWord(Byte.decode("0x"+cmd), pwValue)) {
                if (usbSmb.readWord(cmd, pwValue)) {
                    switch (sbs.getFormat()) {
                        case "Hex":
//                            str = "0x" + Integer.toHexString(pwValue[0]);
                            str = String.format("0x%1$04X", pwValue[0]);
                            break;
                        case "Date":
                            str = String.format("%1$d-%2$d-%3$d",
                                    1980+(pwValue[0]>>9), (pwValue[0]>>5)&0xF, pwValue[0]&0x1F);
                            break;
                        case "Temp":
                            str = String.format("%1$5.1f", ((float)pwValue[0]-2731.5)/10.0);
                            break;
                        case "UInt":
                            str = Integer.toString(Short.toUnsignedInt(pwValue[0]));
                            break;
                        case "Int":
                            str = Integer.toString(pwValue[0]);
                            break;
                        default:
                    }
                    if (cmd == socCmd) {
                        jProgressBarSOC.setValue(pwValue[0]);
                    }
                }
            }
            jTableSBS.setValueAt(str, i, 2);
        }
        
        int i = 0;
        for (BitField bf : bfList){
            short[] pwValue = new short[1];
            if (usbSmb.readWord(bf.getCmd(), pwValue)) {
                int size = bf.getSize();
                for (int j = 0; j < size; j++) {
                    byte val = (byte)(pwValue[0] >> ((size-1-j)*8));
                    String str = String.format("0x%1$02X", val);
                    jTableBits.setValueAt(str, i++, 2);
                }
            }
        }
    }

    private void jButtonReadAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonReadAllActionPerformed
        jProgressBar1.setValue(0);
        new Thread() {
            @Override
            public void run() {
                final int retry_end = 3;
                int retry_count;
                if (bNewSBS) {
                    for (retry_count = 0; retry_count < retry_end; retry_count++) {
                        if (usbSmb.writeByte(0x61, 0)) {
                            break;
                        }
                        try {
                            sleep(10);
                        } catch (InterruptedException ex) {
                        }
                    }
                    if (retry_count == retry_end) {
                        return;
                    }
                }
                int index;
                for (index = 0; index < 8; index++) {
                    for (retry_count = 0; retry_count < retry_end; retry_count++) {
                        if (bNewSBS) {
                            if (usbSmb.writeByteVerify(0x3e, index))
                                break;
                        } else {
                            if (usbSmb.writeWordVerify(0x77, index))
                                break;
                        }
                        try {
                            sleep(10);
                        } catch (InterruptedException ex) {
                        }
                    }
                    if (retry_count == retry_end)
                        break;
                    for (retry_count = 0; retry_count < retry_end; retry_count++) {
                        byte[] sector_buf = new byte[256];
                        if (usbSmb.readDataFlashSector(bNewSBS, sector_buf)) {
                            System.arraycopy(sector_buf, 0, dfBuf, index * 256, sector_buf.length);
                            break;
                        }
                        try {
                            sleep(10);
                        } catch (InterruptedException ex) {
                        }
                    }
                    if (retry_count == retry_end)
                        break;
                    
                    jProgressBar1.setValue(256 * (index + 1));
                }
                if (index == 8) {
                    refreshDataFlash();
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Failed !!!",
                            "Read DataFlash",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.start();
    }//GEN-LAST:event_jButtonReadAllActionPerformed

    private void jButtonWriteAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonWriteAllActionPerformed
        updateDataFlash();
        jProgressBar1.setValue(0);
        new Thread() {
            @Override
            public void run() {
                final int retry_end = 3;
                int retry_count;
                if (bNewSBS) {
                    for (retry_count = 0; retry_count < retry_end; retry_count++) {
                        if (usbSmb.writeByte(0x61, 0)) {
                            break;
                        }
                        try {
                            sleep(10);
                        } catch (InterruptedException ex) {
                        }
                    }
                    if (retry_count == retry_end) {
                        return;
                    }
                }

                int index;
                for (index = 0; index < 8; index++) {
                    for (retry_count = 0; retry_count < retry_end; retry_count++) {
                        if (bNewSBS) {
                            if (usbSmb.writeByteVerify(0x3e, index)) {
                                break;
                            }
                        } else {
                            if (usbSmb.writeWordVerify(0x77, index)) {
                                break;
                            }
                        }
                        try {
                            sleep(10);
                        } catch (InterruptedException ex) {
                        }
                    }
                    if (retry_count == retry_end) {
                        break;
                    }

                    for (retry_count = 0; retry_count < retry_end; retry_count++) {
                        byte[] sector_buf = new byte[256];
                        System.arraycopy(dfBuf, index * 256, sector_buf, 0, sector_buf.length);
                        if (usbSmb.writeDataFlashSector(bNewSBS, sector_buf)) {
                            break;
                        }
                        try {
                            sleep(10);
                        } catch (InterruptedException ex) {
                        }
                    }
                    if (retry_count == retry_end) {
                        break;
                    }
                    
                    jProgressBar1.setValue(256 * (index + 1));
                }

                if (index == 8) {
                    JOptionPane.showMessageDialog(null,
                            "Success",
                            "Write DataFlash",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    usbSmb.writeWord(0, 0x43);
                    JOptionPane.showMessageDialog(null,
                            "Failed !!!",
                            "Write DataFlash",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.start();
        
    }//GEN-LAST:event_jButtonWriteAllActionPerformed

    private void jButtonDefaultActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDefaultActionPerformed
        int len = DllEntry.dec128("Default.ifi", dfBuf);
        if (len == 2048) {
            refreshDataFlash();
        }
    }//GEN-LAST:event_jButtonDefaultActionPerformed

    private void jButtonExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonExportActionPerformed
        int returnVal = fc.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            String path = file.getPath();
            System.out.println("Saving: " + path);
//            Path p = Paths.get(path);
            updateDataFlash();
            try {
//                Files.write(p, dfBuf, StandardOpenOption.CREATE_NEW);
                FileOutputStream f = new FileOutputStream(file);
                f.write(dfBuf);
                if (DllEntry.cod128(path)) {
                    JOptionPane.showMessageDialog(this, "SUCCESS");
                }
            } catch (IOException ex) {
                System.out.println(ex);
            }
        }
    }//GEN-LAST:event_jButtonExportActionPerformed

    private void jButtonImportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonImportActionPerformed
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            String path = file.getPath();//.getAbsolutePath();
            System.out.println("Opening: " + path);
            if (DllEntry.dec128(path, dfBuf) == 2048) {
                refreshDataFlash();
            }
        }
    }//GEN-LAST:event_jButtonImportActionPerformed
    
    public void refreshDataFlash() {
        int i = 0;
        for (Cluster cluster : clusterList) {
            List<DataFlash> dfList = cluster.getDFList();
            JTable jTableCluster = clusterTableList.get(i++);
            for (int j = 0; j < jTableCluster.getRowCount(); j++) {
                DataFlash df = dfList.get(j);
                int value = 0, addr = df.getStartAdr() - stradr;
                String str = "", type = df.getType();
                switch (type) {
                    case "S1":
                        value = dfBuf[addr];
                        break;
                    case "U1":
                        value = Byte.toUnsignedInt(dfBuf[addr]);
                        break;
                    case "S2":
                        value = (dfBuf[addr] << 8)
                                | Byte.toUnsignedInt(dfBuf[addr + 1]);
                        break;
                    case "U2":
                        value = (Byte.toUnsignedInt(dfBuf[addr]) << 8)
                                | Byte.toUnsignedInt(dfBuf[addr + 1]);
                        break;
                    case "S4":
                    case "U4":
                        value = (dfBuf[addr] << 24)
                                | (Byte.toUnsignedInt(dfBuf[addr + 1]) << 16)
                                | (Byte.toUnsignedInt(dfBuf[addr + 2]) << 8)
                                | Byte.toUnsignedInt(dfBuf[addr + 3]);
                        break;
                    case "string":
                        byte len = dfBuf[addr];
                        if (len > 0 && len < 32) {
                            str = new String(dfBuf, addr + 1, len);
                        }
                        break;
                    case "-":
                        str = "-";
                        break;
                    default:
                        assert (false);
                }
                if (df.getUnit().equals("hex")) {
                    str = "0x" + Integer.toHexString(value);
                } else if (df.getUnit().equals("date")) {
                    str = String.format("%1$d-%2$d-%3$d", 1980+(value>>9), (value>>5)&0xF, value&0x1F);
                } else {
                    switch (type) {
                        case "S1":
                        case "U1":
                        case "S2":
                        case "U2":
                        case "S4":
                            str = Integer.toString(value);
                            break;
                        case "U4":
                            str = Integer.toUnsignedString(value);
                            break;
                        default:
                    }
                }
                jTableCluster.setValueAt(str, j, 1);
            }
        }
    }

    public void updateDataFlash() {
        int i = 0;
        for (JTable jTableCluster : clusterTableList) {
            List<DataFlash> dfList = clusterList.get(i++).getDFList();
            for (int j = 0; j < jTableCluster.getRowCount(); j++) {
                DataFlash df = dfList.get(j);
                int addr = df.getStartAdr() - stradr;
                String type = df.getType(), str = (String) jTableCluster.getValueAt(j, 1);
                if (type.equals("string")) {
                    byte[] bytes = str.getBytes();
                    dfBuf[addr] = (byte) Math.min(bytes.length, 10);
                    System.arraycopy(bytes, 0, dfBuf, addr + 1, dfBuf[addr]);
                } else if (type.equals("-")) {
                } else {
                    int value = 0;
                    try {
                        if (df.getUnit().equals("hex")) {
                            if (str.length() > 2) {
                                value = Integer.parseUnsignedInt(str.substring(2), 16);
                            }
                        } else if (df.getUnit().equals("date")) {
                            int dash1 = str.indexOf('-'), dash2 = str.lastIndexOf('-'),
                                year = Integer.parseInt(str.substring(0, dash1)),
                                month = Integer.parseInt(str.substring(dash1 + 1, dash2)),
                                day = Integer.parseInt(str.substring(dash2 + 1));
                            value = ((year - 1980) << 9) + (month << 5) + day;
                        } else {
                            value = Integer.parseInt(str);
                        }
                    } catch (NumberFormatException e) {
                        System.out.println(e);
                    }
                    switch (type) {
                        case "S1":
                        case "U1":
                            dfBuf[addr] = (byte) value;
                            break;
                        case "S2":
                        case "U2":
                            dfBuf[addr] = (byte) (value >> 8);
                            dfBuf[addr + 1] = (byte) value;
                            break;
                        case "S4":
                        case "U4":
                            dfBuf[addr] = (byte) (value >> 24);
                            dfBuf[addr + 1] = (byte) (value >> 16);
                            dfBuf[addr + 2] = (byte) (value >> 8);
                            dfBuf[addr + 3] = (byte) value;
                            break;
                        default:
                            assert (false);
                    }
                }
            }
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//                if ("Nimbus".equals(info.getName())) {
//                if ("Windows Classic".equals(info.getName())) {
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(NewJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NewJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NewJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NewJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        usbSmb = new UsbSmb();
        System.out.println("USB SMBus Version: " + Integer.toHexString(usbSmb.getVersion()));

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new NewJFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonDefault;
    private javax.swing.JButton jButtonExport;
    private javax.swing.JButton jButtonImport;
    private javax.swing.JButton jButtonReadAll;
    private javax.swing.JButton jButtonRefresh;
    private javax.swing.JButton jButtonWriteAll;
    private javax.swing.JLabel jLabelSOC;
    private javax.swing.JPanel jPanelCommand;
    private javax.swing.JPanel jPanelDataFlash;
    private javax.swing.JPanel jPanelSBS;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JProgressBar jProgressBarSOC;
    private javax.swing.JScrollPane jScrollPaneBits;
    private javax.swing.JScrollPane jScrollPaneMain;
    private javax.swing.JScrollPane jScrollPaneSBS;
    private javax.swing.JTabbedPane jTabbedPaneDataFlash;
    private javax.swing.JTabbedPane jTabbedPaneMain;
    private javax.swing.JTable jTableBits;
    private javax.swing.JTable jTableSBS;
    // End of variables declaration//GEN-END:variables
}
