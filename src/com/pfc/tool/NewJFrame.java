package com.pfc.tool;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import com.pfc.lib.DllEntry;
import com.pfc.lib.UsbSmb;
import com.pfc.ui.*;
import com.pfc.xml.*;
import com.pfc.xml.sax.*;
import java.awt.Cursor;
import java.awt.event.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.jfree.chart.ui.UIUtils;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
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
    JFileChooser fcIfi, fcCod, fcBin, fcLog;
    int socCmd;
    int stradr;
    boolean bNewSBS;
    Timer timer;
    final int ONE_SECOND = 1000;
    private boolean scanning, logging = false;
    private PrintWriter pwLog;
    ChemTableModel chemTableModel;
    String devName;

    /**
     * Creates new form NewJFrame
     */
    public NewJFrame() {
        try {
            FileInputStream in = new FileInputStream("../ini/Preferences.xml");
            Preferences.importPreferences(in);
        } catch (IOException|InvalidPreferencesFormatException ex) {
            System.err.println(ex);
        }
        Preferences prefs = Preferences.userNodeForPackage(NewJFrame.class);
        devName = prefs.get("devName", "");
        bNewSBS = prefs.getBoolean("newSbs", false);
        if (bNewSBS) {
            usbSmb.setAddr((byte)0xAA);
        }
        socCmd = Integer.decode(prefs.get("socCmd", "0"));
        if (socCmd == 0)
            socCmd = bNewSBS ? 0x2C: 0x0D;
        stradr = bNewSBS ? 0x7600 : 0xB000;
        SBSHandler sbsHandler = new SBSHandler();
        BitsHandler bitsHandler = new BitsHandler();
        DataFlashHandler dfHandler = new DataFlashHandler();
        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            SAXParser saxParser = saxParserFactory.newSAXParser();
            saxParser.parse(new File("../ini/SBS.xml"), sbsHandler);
            saxParser.parse(new File("../ini/DataFlash.xml"), dfHandler);
            saxParser.parse(new File("../ini/Bits.xml"), bitsHandler);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            System.err.println(e);
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
        
        chemTableModel = new ChemTableModel();

        initComponents();

        // Set custom color renderer
        BitsColorRenderer colorRenderer = new BitsColorRenderer();
        jTableBits.setDefaultRenderer(String.class, colorRenderer);
        
        refreshSBS();
        
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

        //Create file choosers
        FileNameExtensionFilter filter;
        filter = new FileNameExtensionFilter("DataFlash Files (*.ifi)", "ifi");
        fcIfi = new JFileChooser();
        fcIfi.setFileFilter(filter);
        filter = new FileNameExtensionFilter("Code Binary Files (*.cod)", "cod");
        fcCod = new JFileChooser();
        fcCod.setFileFilter(filter);
        filter = new FileNameExtensionFilter("Binary Files (*.bin)", "bin");
        fcBin = new JFileChooser();
        fcBin.setFileFilter(filter);
        filter = new FileNameExtensionFilter("Log Files (*.log)", "log");
        fcLog = new JFileChooser();
        fcLog.setFileFilter(filter);

        //Create a timer.
        timer = new Timer(ONE_SECOND, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                refreshSBS();
                if (logging) {
                    Date date = new Date();
                    pwLog.print(String.format("%tF", date));
                    pwLog.print(String.format("\t%tT", date));
                    for (int i = 0; i < jTableSBS.getRowCount(); i++) {
                        if ((boolean) jTableSBS.getValueAt(i, 4)) {
                            pwLog.print("\t");
                            pwLog.print(jTableSBS.getValueAt(i, 2));
                        }
                    }
                    pwLog.println();
                }
            }
        });
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
        jCheckBoxScan = new javax.swing.JCheckBox();
        jLabel7 = new javax.swing.JLabel();
        jTextFieldInterval = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jButtonStartLog = new javax.swing.JButton();
        jButtonStopLog = new javax.swing.JButton();
        jTextFieldLogFile = new javax.swing.JTextField();
        jPanelDataFlash = new javax.swing.JPanel();
        jTabbedPaneDataFlash = new javax.swing.JTabbedPane();
        jButtonReadAll = new javax.swing.JButton();
        jButtonWriteAll = new javax.swing.JButton();
        jButtonDefault = new javax.swing.JButton();
        jButtonExport = new javax.swing.JButton();
        jButtonImport = new javax.swing.JButton();
        jProgressBarDF = new javax.swing.JProgressBar();
        jPanelCommand = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jTextFieldCommand = new javax.swing.JTextField();
        jTextFieldData = new javax.swing.JTextField();
        jTextFieldData2 = new javax.swing.JTextField();
        jButtonReadWord = new javax.swing.JButton();
        jButtonWriteWord = new javax.swing.JButton();
        jButtonWriteWord2 = new javax.swing.JButton();
        jButtonReadBlock = new javax.swing.JButton();
        jTextFieldBlock = new javax.swing.JTextField();
        jButtonWriteBlock = new javax.swing.JButton();
        jCheckBoxPEC = new javax.swing.JCheckBox();
        jTextFieldCount = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPaneMessage = new javax.swing.JTextPane();
        jPanel2 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jTextFieldMReg = new javax.swing.JTextField();
        jTextFieldByte = new javax.swing.JTextField();
        jButtonReadMReg = new javax.swing.JButton();
        jButtonWriteMReg = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jCheckBox1 = new javax.swing.JCheckBox();
        jProgressBarBL = new javax.swing.JProgressBar();
        jLabelStat = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jTextField2 = new javax.swing.JTextField();
        jTextField3 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButtonProgram = new javax.swing.JButton();
        jButtonReadFlash = new javax.swing.JButton();
        jButtonWriteFlash = new javax.swing.JButton();
        jPanelChem = new javax.swing.JPanel();
        jScrollPaneChem = new javax.swing.JScrollPane();
        jTableChem = new javax.swing.JTable();
        jButtonPlot = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        jTextFieldChemID = new javax.swing.JTextField();
        jButtonChange = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jTabbedPaneMain.setToolTipText("");
        jTabbedPaneMain.setFont(new java.awt.Font("Dialog", 0, 20)); // NOI18N
        jTabbedPaneMain.setPreferredSize(new java.awt.Dimension(720, 540));
        jTabbedPaneMain.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jTabbedPaneMainStateChanged(evt);
            }
        });

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

        jCheckBoxScan.setText("Keep Scanning");
        jCheckBoxScan.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCheckBoxScanItemStateChanged(evt);
            }
        });

        jLabel7.setText("Interval :");

        jTextFieldInterval.setText("2000");

        jLabel8.setText("ms");

        jButtonStartLog.setText("Start Log");
        jButtonStartLog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonStartLogActionPerformed(evt);
            }
        });

        jButtonStopLog.setText("Stop Log");
        jButtonStopLog.setEnabled(false);
        jButtonStopLog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonStopLogActionPerformed(evt);
            }
        });

        jTextFieldLogFile.setEditable(false);
        jTextFieldLogFile.setHorizontalAlignment(javax.swing.JTextField.TRAILING);

        javax.swing.GroupLayout jPanelSBSLayout = new javax.swing.GroupLayout(jPanelSBS);
        jPanelSBS.setLayout(jPanelSBSLayout);
        jPanelSBSLayout.setHorizontalGroup(
            jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSBSLayout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addGroup(jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelSBSLayout.createSequentialGroup()
                        .addGroup(jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanelSBSLayout.createSequentialGroup()
                                .addComponent(jLabelSOC)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jProgressBarSOC, javax.swing.GroupLayout.PREFERRED_SIZE, 410, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPaneSBS, javax.swing.GroupLayout.PREFERRED_SIZE, 464, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextFieldLogFile)
                            .addGroup(jPanelSBSLayout.createSequentialGroup()
                                .addGroup(jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jButtonRefresh)
                                    .addComponent(jCheckBoxScan)
                                    .addGroup(jPanelSBSLayout.createSequentialGroup()
                                        .addComponent(jLabel7)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextFieldInterval, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel8))
                                    .addComponent(jButtonStartLog)
                                    .addComponent(jButtonStopLog))
                                .addGap(0, 85, Short.MAX_VALUE))))
                    .addGroup(jPanelSBSLayout.createSequentialGroup()
                        .addComponent(jScrollPaneBits, javax.swing.GroupLayout.PREFERRED_SIZE, 605, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanelSBSLayout.setVerticalGroup(
            jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSBSLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPaneSBS, javax.swing.GroupLayout.PREFERRED_SIZE, 290, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanelSBSLayout.createSequentialGroup()
                        .addGap(27, 27, 27)
                        .addComponent(jButtonRefresh)
                        .addGap(14, 14, 14)
                        .addComponent(jCheckBoxScan)
                        .addGap(9, 9, 9)
                        .addGroup(jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(jTextFieldInterval, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel8))
                        .addGap(18, 18, 18)
                        .addComponent(jButtonStartLog)
                        .addGap(9, 9, 9)
                        .addComponent(jTextFieldLogFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonStopLog)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelSOC)
                    .addGroup(jPanelSBSLayout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addComponent(jProgressBarSOC, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPaneBits, javax.swing.GroupLayout.PREFERRED_SIZE, 259, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6))
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

        jProgressBarDF.setMaximum(2048);

        javax.swing.GroupLayout jPanelDataFlashLayout = new javax.swing.GroupLayout(jPanelDataFlash);
        jPanelDataFlash.setLayout(jPanelDataFlashLayout);
        jPanelDataFlashLayout.setHorizontalGroup(
            jPanelDataFlashLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelDataFlashLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanelDataFlashLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jTabbedPaneDataFlash, javax.swing.GroupLayout.PREFERRED_SIZE, 507, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jProgressBarDF, javax.swing.GroupLayout.PREFERRED_SIZE, 392, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelDataFlashLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonWriteAll)
                    .addComponent(jButtonReadAll)
                    .addComponent(jButtonDefault)
                    .addComponent(jButtonExport)
                    .addComponent(jButtonImport))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelDataFlashLayout.setVerticalGroup(
            jPanelDataFlashLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelDataFlashLayout.createSequentialGroup()
                .addGap(82, 82, 82)
                .addComponent(jButtonReadAll)
                .addGap(32, 32, 32)
                .addComponent(jButtonWriteAll)
                .addGap(32, 32, 32)
                .addComponent(jButtonDefault)
                .addGap(28, 28, 28)
                .addComponent(jButtonExport)
                .addGap(45, 45, 45)
                .addComponent(jButtonImport)
                .addContainerGap(265, Short.MAX_VALUE))
            .addGroup(jPanelDataFlashLayout.createSequentialGroup()
                .addGap(42, 42, 42)
                .addComponent(jTabbedPaneDataFlash)
                .addGap(18, 18, 18)
                .addComponent(jProgressBarDF, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(47, 47, 47))
        );

        jTabbedPaneMain.addTab("DataFlash", jPanelDataFlash);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("SBS Command"));

        jLabel1.setText("Command : (hex)");

        jLabel2.setText("Word Data : (hex)");

        jLabel3.setText("Word 2 : (hex)");

        jLabel4.setText("Hex :");

        jTextFieldCommand.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextFieldCommand.setText("0");

        jTextFieldData.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextFieldData.setText("0");

        jTextFieldData2.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextFieldData2.setText("0");
        jTextFieldData2.setToolTipText("");

        jButtonReadWord.setText("Read Word");
        jButtonReadWord.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReadWordActionPerformed(evt);
            }
        });

        jButtonWriteWord.setText("Write Word");
        jButtonWriteWord.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonWriteWordActionPerformed(evt);
            }
        });

        jButtonWriteWord2.setText("Write 2 Word");
        jButtonWriteWord2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonWriteWord2ActionPerformed(evt);
            }
        });

        jButtonReadBlock.setText("Read Block");
        jButtonReadBlock.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReadBlockActionPerformed(evt);
            }
        });

        jButtonWriteBlock.setText("Write Block");
        jButtonWriteBlock.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonWriteBlockActionPerformed(evt);
            }
        });

        jCheckBoxPEC.setText("Use PEC");
        jCheckBoxPEC.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCheckBoxPECItemStateChanged(evt);
            }
        });

        jTextFieldCount.setText("32");

        jLabel10.setText("Count :");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(184, 184, 184)
                        .addComponent(jButtonReadWord)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonReadBlock))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                        .addComponent(jTextFieldBlock)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButtonWriteBlock))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                        .addGap(0, 0, Short.MAX_VALUE)
                                        .addComponent(jLabel10)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextFieldCount, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jTextFieldCommand, javax.swing.GroupLayout.DEFAULT_SIZE, 53, Short.MAX_VALUE))
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel2)
                                            .addComponent(jLabel3))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(jTextFieldData2, javax.swing.GroupLayout.DEFAULT_SIZE, 53, Short.MAX_VALUE)
                                            .addComponent(jTextFieldData))))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jButtonWriteWord2)
                                    .addComponent(jButtonWriteWord))
                                .addGap(34, 34, 34)
                                .addComponent(jCheckBoxPEC)))))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextFieldCommand, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonReadWord)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jButtonReadBlock)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextFieldCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel10))))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(3, 3, 3)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButtonWriteWord)
                            .addComponent(jTextFieldData, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2))
                        .addGap(16, 16, 16)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(jTextFieldData2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonWriteWord2))
                        .addGap(18, 18, 18))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxPEC)
                        .addGap(40, 40, 40)))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldBlock, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonWriteBlock)
                    .addComponent(jLabel4))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTextPaneMessage.setEditable(false);
        jTextPaneMessage.setBackground(java.awt.SystemColor.controlHighlight);
        jTextPaneMessage.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        jScrollPane1.setViewportView(jTextPaneMessage);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Mode Register"));

        jLabel5.setText("Address : 10");

        jLabel6.setText("Byte Data :");

        jTextFieldMReg.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextFieldMReg.setText("00");

        jTextFieldByte.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextFieldByte.setText("00");

        jButtonReadMReg.setText("Read");
        jButtonReadMReg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReadMRegActionPerformed(evt);
            }
        });

        jButtonWriteMReg.setText("Write");
        jButtonWriteMReg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonWriteMRegActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 63, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jTextFieldByte)
                    .addComponent(jTextFieldMReg, javax.swing.GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonReadMReg)
                    .addComponent(jButtonWriteMReg))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldMReg, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonReadMReg))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldByte, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonWriteMReg))
                .addContainerGap(19, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Boot Loader"));

        jCheckBox1.setText("Boot ROM Mode");
        jCheckBox1.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCheckBox1ItemStateChanged(evt);
            }
        });

        jProgressBarBL.setStringPainted(true);

        jButton1.setText("...");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("...");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setText("...");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButtonProgram.setText("Program");
        jButtonProgram.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonProgramActionPerformed(evt);
            }
        });

        jButtonReadFlash.setText("Save to File");
        jButtonReadFlash.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReadFlashActionPerformed(evt);
            }
        });

        jButtonWriteFlash.setText("Write to DF");
        jButtonWriteFlash.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonWriteFlashActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jCheckBox1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jProgressBarBL, javax.swing.GroupLayout.PREFERRED_SIZE, 274, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelStat, javax.swing.GroupLayout.DEFAULT_SIZE, 34, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jTextField1, javax.swing.GroupLayout.DEFAULT_SIZE, 276, Short.MAX_VALUE)
                            .addComponent(jTextField2)
                            .addComponent(jTextField3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButtonWriteFlash)
                            .addComponent(jButtonProgram)
                            .addComponent(jButtonReadFlash))))
                .addGap(16, 16, 16))
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jButton2, jButton3});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabelStat, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jCheckBox1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jProgressBarBL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1)
                    .addComponent(jButtonProgram))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton3)
                    .addComponent(jButtonWriteFlash))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 23, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton2)
                    .addComponent(jButtonReadFlash))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanelCommandLayout = new javax.swing.GroupLayout(jPanelCommand);
        jPanelCommand.setLayout(jPanelCommandLayout);
        jPanelCommandLayout.setHorizontalGroup(
            jPanelCommandLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCommandLayout.createSequentialGroup()
                .addGap(33, 33, 33)
                .addGroup(jPanelCommandLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelCommandLayout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 398, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 320, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(66, Short.MAX_VALUE))
        );
        jPanelCommandLayout.setVerticalGroup(
            jPanelCommandLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCommandLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanelCommandLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelCommandLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 189, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelCommandLayout.createSequentialGroup()
                        .addGap(14, 14, 14)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(151, Short.MAX_VALUE))
        );

        jTabbedPaneMain.addTab("Command", jPanelCommand);

        jTableChem.setModel(chemTableModel);
        jTableChem.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jTableChem.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTableChemMouseClicked(evt);
            }
        });
        jScrollPaneChem.setViewportView(jTableChem);

        jButtonPlot.setText("View OCV Curve");
        jButtonPlot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPlotActionPerformed(evt);
            }
        });

        jLabel9.setText("Chemisry ID:");

        jTextFieldChemID.setEditable(false);

        jButtonChange.setText("Change Chemistry");
        jButtonChange.setEnabled(false);
        jButtonChange.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonChangeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelChemLayout = new javax.swing.GroupLayout(jPanelChem);
        jPanelChem.setLayout(jPanelChemLayout);
        jPanelChemLayout.setHorizontalGroup(
            jPanelChemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelChemLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPaneChem, javax.swing.GroupLayout.PREFERRED_SIZE, 543, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
                .addGroup(jPanelChemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelChemLayout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldChemID, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jButtonPlot, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButtonChange, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        jPanelChemLayout.setVerticalGroup(
            jPanelChemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelChemLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPaneChem, javax.swing.GroupLayout.DEFAULT_SIZE, 593, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(jPanelChemLayout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addGroup(jPanelChemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jTextFieldChemID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jButtonChange)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButtonPlot)
                .addGap(19, 19, 19))
        );

        jTabbedPaneMain.addTab("Chemistry", jPanelChem);

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
            .addComponent(jScrollPaneMain, javax.swing.GroupLayout.DEFAULT_SIZE, 659, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRefreshActionPerformed
        refreshSBS();
    }//GEN-LAST:event_jButtonRefreshActionPerformed

    private void refreshSBS() {
        usbSmb.writeWord(0, 0);
        for (int i = 0; i < jTableSBS.getRowCount(); i++) {
            if (!(boolean)jTableSBS.getValueAt(i, 5))
                continue;
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
                            int size = sbs.getSize();
                            if (size > Short.BYTES) {
                                byte[] buf = new byte[size];
                                if (usbSmb.readBlock(cmd, size, buf)) {
                                    StringBuilder result = new StringBuilder();
                                    for (byte b : buf) {
                                        result.append(String.format("%02X ", b));
                                    }
                                    str = result.toString();
                                }
                            } else {
//                            str = "0x" + Integer.toHexString(pwValue[0]);
                                str = String.format("0x%04X", pwValue[0]);
                            }
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
        jTableBits.repaint();
    }

    private void jButtonReadAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonReadAllActionPerformed
        jProgressBarDF.setValue(0);
        new Thread() {
            @Override
            public void run() {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
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
                        JOptionPane.showMessageDialog(null,
                                "Please UnSeal !!",
                                "Read DataFlash",
                                JOptionPane.INFORMATION_MESSAGE);
                        setCursor(null);
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
                    
                    jProgressBarDF.setValue(256 * (index + 1));
                }
                if (index == 8) {
                    refreshDataFlash();
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Failed !!!",
                            "Read DataFlash",
                            JOptionPane.ERROR_MESSAGE);
                }
                setCursor(Cursor.getDefaultCursor());
            }
        }.start();
    }//GEN-LAST:event_jButtonReadAllActionPerformed

    private void jButtonWriteAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonWriteAllActionPerformed
        updateDataFlash();
        jProgressBarDF.setValue(0);
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

                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
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
                    
                    jProgressBarDF.setValue(256 * (index + 1));
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
                setCursor(Cursor.getDefaultCursor());
            }
        }.start();
        
    }//GEN-LAST:event_jButtonWriteAllActionPerformed

    private void jButtonDefaultActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDefaultActionPerformed
        int len = DllEntry.dec128("../ini/Default.ifi", dfBuf);
        if (len == 2048) {
            refreshDataFlash();
        }
    }//GEN-LAST:event_jButtonDefaultActionPerformed

    private void jButtonExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonExportActionPerformed
        int returnVal = fcIfi.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fcIfi.getSelectedFile();
            String path = file.getPath();
            if (!file.getName().contains(".")) {
                path += ".ifi";
            }
            System.out.println("Saving: " + path);
//            Path p = Paths.get(path);
            updateDataFlash();
            try {
//                Files.write(p, dfBuf, StandardOpenOption.CREATE_NEW);
//                FileOutputStream f = new FileOutputStream(file);
                FileOutputStream f = new FileOutputStream(path);
                f.write(dfBuf);
                if (DllEntry.cod128(path)) {
                    JOptionPane.showMessageDialog(this, "SUCCESS");
                }
            } catch (IOException ex) {
                System.err.println(ex);
            }
        }
    }//GEN-LAST:event_jButtonExportActionPerformed

    private void jButtonImportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonImportActionPerformed
        int returnVal = fcIfi.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fcIfi.getSelectedFile();
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
                        System.err.println(e);
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

    private void jButtonReadWordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonReadWordActionPerformed
        int command = Integer.parseInt(jTextFieldCommand.getText(), 16);
        short pwValue[] = new short[1];
        if (usbSmb.readWord(command, pwValue)) {
            jTextPaneMessage.setText(Integer.toHexString(Short.toUnsignedInt(pwValue[0])).toUpperCase());
        } else {
            jTextPaneMessage.setText("Read Fail");
        }
    }//GEN-LAST:event_jButtonReadWordActionPerformed

    private void jButtonWriteWordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonWriteWordActionPerformed
        int command = Integer.parseInt(jTextFieldCommand.getText(), 16);
        int value = Integer.parseInt(jTextFieldData.getText(), 16);
        if (usbSmb.writeWord(command, value)) {
            jTextPaneMessage.setText("Write Success");
        } else {
            jTextPaneMessage.setText("Write Fail");
        }
    }//GEN-LAST:event_jButtonWriteWordActionPerformed

    private void jButtonWriteWord2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonWriteWord2ActionPerformed
        int command = Integer.parseInt(jTextFieldCommand.getText(), 16);
        int value = Integer.parseInt(jTextFieldData.getText(), 16),
            value2 = Integer.parseInt(jTextFieldData2.getText(), 16);
        if (usbSmb.writeWord(command, value)) {
            if (usbSmb.writeWord(command, value2)) {
                jTextPaneMessage.setText("Write Success");
                return;
            }
        }
        jTextPaneMessage.setText("Write Fail");
    }//GEN-LAST:event_jButtonWriteWord2ActionPerformed

    private void jButtonReadBlockActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonReadBlockActionPerformed
        int command = Integer.parseInt(jTextFieldCommand.getText(), 16);
        int count = Integer.parseInt(jTextFieldCount.getText());
//        byte pValue[] = new byte[1];
//        if (usbSmb.readByte(command, pValue)) {
//            int count = Math.min(34, pValue[0]);
            if (count > 0) {
                byte[] buf = new byte[count];
//                if (usbSmb.readBlock(command, count, buf)) {
                if (usbSmb.readBytes(command, count, buf)) {
                    StringBuilder result = new StringBuilder();
                    for (byte b : buf) {
                        result.append(String.format("%02X ", b));
                    }
                    jTextPaneMessage.setText(result.toString());
                    return;
                }
            }
//        }
        jTextPaneMessage.setText("Read Fail");
    }//GEN-LAST:event_jButtonReadBlockActionPerformed

    private void jCheckBoxPECItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCheckBoxPECItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            usbSmb.setPEC(true);
        } else {
            usbSmb.setPEC(false);
        }
    }//GEN-LAST:event_jCheckBoxPECItemStateChanged

    private void jButtonWriteBlockActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonWriteBlockActionPerformed
        int command = Integer.parseInt(jTextFieldCommand.getText(), 16);
        String hexData = jTextFieldBlock.getText();
        int count = hexData.length() / 3;
        if ((hexData.length() % 3) != 0)
            count++;
        if (count > 0) {
            byte[] buf = new byte[count];
            for (int i = 0; i < count; i++) {
                try {
                    buf[i] = (byte) Integer.parseInt(hexData.substring(i * 3, i * 3 + 2), 16);
                } catch (NumberFormatException e) {
                    System.err.println(e);
                }
            }
            if (usbSmb.writeBlock(command, count, buf)) {
                jTextPaneMessage.setText("Write Success");
            } else {
                jTextPaneMessage.setText("Write Fail");
            }
        }
    }//GEN-LAST:event_jButtonWriteBlockActionPerformed

    private void jButtonReadMRegActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonReadMRegActionPerformed
        byte mreg = (byte)Integer.parseInt(jTextFieldMReg.getText(), 16);
        byte[] bytes = { 0x10, mreg };
        if (usbSmb.writeBytes(0xF5, bytes.length, bytes)) {
            if (usbSmb.readBytes(0xF5, bytes.length, bytes)) {
//                jTextFieldByte.setText(Integer.toHexString(Byte.toUnsignedInt(bytes[0])).toUpperCase());
                jTextFieldByte.setText(String.format("%02X", bytes[0]));
                jTextPaneMessage.setText("Read Success");
                return;
            }
        }
        jTextPaneMessage.setText("Read Fail");
    }//GEN-LAST:event_jButtonReadMRegActionPerformed

    private void jButtonWriteMRegActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonWriteMRegActionPerformed
        byte mreg = (byte)Integer.parseInt(jTextFieldMReg.getText(), 16),
             value = (byte)Integer.parseInt(jTextFieldByte.getText(), 16);
        byte[] bytes = { mreg, value };
        if (usbSmb.writeBytes(0xF4, bytes.length, bytes)) {
            jTextPaneMessage.setText("Write Success");
        } else {
            jTextPaneMessage.setText("Write Fail");
        }
    }//GEN-LAST:event_jButtonWriteMRegActionPerformed

    private void jCheckBox1ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCheckBox1ItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            jProgressBarBL.setValue(0);
            try {
//                File file = new File("../ini/BootLoader_A1141.bin");
                File file = new File("../ini/BootLoader_A"+devName+".bin");
                int len = (int) file.length();
                byte blBuf[] = new byte[len];
                new FileInputStream(file).read(blBuf);
                jProgressBarBL.setMaximum(len * 2);
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            byte[] buf = new byte[34];
                            if (devName.equals("2168")) {
                                buf[0] = 0x1c;  buf[1] = 0x0f;
                            } else {
                                buf[0] = 0x1e;  buf[1] = 0x00;
                            }
                            if (!usbSmb.writeBytes(0xF4, 2, buf)) return;
                            usbSmb.setAddr((byte)0x16);
                            sleep(100);
                            if (!usbSmb.writeByte(0xFF, 0x09))  return;
                            sleep(100);
                            if (!usbSmb.writeByte(0xF3, 0x11))  return;
                            sleep(100);
                            buf[0] = 0x10;  buf[1] = 0x30;  buf[2] = 0x00;
                            if (!usbSmb.writeBytes(0xF4, 3, buf)) return;
                            sleep(100);
                            for (int nWriteBytes, nPointer = 0; nPointer < len; nPointer += nWriteBytes) {
                                jProgressBarBL.setValue(nPointer);
                                nWriteBytes = Math.min(len - nPointer, 32);
                                System.arraycopy(blBuf, nPointer, buf, 2, nWriteBytes);
                                buf[0] = (byte)(nPointer >> 8);
                                buf[1] = (byte)nPointer;
                                if (!usbSmb.writeBytes(0xF4, nWriteBytes + 2, buf))
                                    return;
                            }
                            sleep(100);
                            buf[0] = 0x00; buf[1] = 0x00;
                            if (!usbSmb.writeBytes(0xF4, 2, buf)) return;
                            sleep(100);
                            for (int nReadBytes, i = 0; i < len; i += nReadBytes)
                            {
                                jProgressBarBL.setValue(len + i);
                                nReadBytes = Math.min(len - i, 32);
                                if (!usbSmb.readBytes(0xF5, nReadBytes, buf))
                                    return;
                                if (!Arrays.equals(Arrays.copyOfRange(blBuf, i, i + nReadBytes),
                                                   Arrays.copyOfRange(buf, 0, nReadBytes)))
                                    return;
                            }
                            sleep(100);
                            if (!usbSmb.writeByte(0xF1, 0x01))  return;
                            sleep(100);
                            if (!usbSmb.writeByte(0xFF, 0x09))  return;
                            sleep(100);
                            buf[0] = -120; buf[1] = 8;
                            if (!usbSmb.writeBytes(0xFA, 2, buf)) return;
                            sleep(100);
                            if (!usbSmb.readBytes(0xFA, 2, buf))
                                return;
                            if ((Byte.toUnsignedInt(buf[1])<<8|Byte.toUnsignedInt(buf[0])) != 0x888)
                                return;
                            buf[0] = 1; buf[1] = 0;
                            usbSmb.writeBytes(0x99, 2, buf);
                            jProgressBarBL.setValue(len * 2);
                        } catch (InterruptedException ex) {
                        }
                    }
                }.start();
            } catch (IOException ex) {
                System.err.println(ex);
            }
            
        } else {
            jProgressBarBL.setMaximum(100);
            new Thread() {
                @Override
                public void run() {
                    try {
                        jProgressBarBL.setValue(100);
                        byte[] buf = new byte[2];
                        buf[1] = 0x00;
                        if (devName.equals("2168")) {
                            buf[0] = 0x0F;
                            if (!usbSmb.writeBytes(0x1C, 2, buf)) return;
                        } else {
                            buf[0] = 0x00;
                            if (!usbSmb.writeBytes(0x1E, 2, buf)) return;
                        }
                        sleep(50);
                        jProgressBarBL.setValue(90);
                        sleep(50);
                        jProgressBarBL.setValue(80);
                        if (!usbSmb.writeByte(0xFF, 0x09))  return;
                        sleep(50);
                        jProgressBarBL.setValue(70);
                        sleep(50);
                        jProgressBarBL.setValue(60);
                        if (!usbSmb.writeByte(0xF0, 0x00))  return;
                        sleep(50);
                        jProgressBarBL.setValue(50);
                        sleep(50);
                        jProgressBarBL.setValue(40);
                        if (!usbSmb.writeByte(0xFF, 0x09))  return;
                        sleep(250);
                        jProgressBarBL.setValue(30);
                        sleep(250);
                        jProgressBarBL.setValue(20);
                        sleep(250);
                        jProgressBarBL.setValue(10);
                        sleep(250);
                        jProgressBarBL.setValue(0);
                        if (bNewSBS) {
                            usbSmb.setAddr((byte)0xAA);
                        }
                    } catch (InterruptedException ex) {
                    }
                }
            }.start();
            
        }
    }//GEN-LAST:event_jCheckBox1ItemStateChanged

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        int returnVal = fcCod.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fcCod.getSelectedFile();
            if (file.exists()) {
                jTextField1.setText(file.getPath());
            }
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        int returnVal = fcBin.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fcBin.getSelectedFile();
            String path = file.getPath();
            if (!file.getName().contains(".")) {
                path += ".bin";
            }
            if (file.exists()) {
                if (JOptionPane.showConfirmDialog(this, "OverWrite Exist File !?") != JOptionPane.YES_OPTION)
                    return;
            }
            jTextField2.setText(path);
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        int returnVal = fcIfi.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fcIfi.getSelectedFile();
            if (file.exists()) {
                jTextField3.setText(file.getPath());
            }
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButtonProgramActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonProgramActionPerformed
        String path = jTextField1.getText();
        File file = new File(path);
        if (!file.exists()) return;
        int len = (int)file.length();
        if (len > 65536) return;
        byte[] writeBuf = new byte[len];
        if (path.toLowerCase().endsWith(".cod")) {
            if (DllEntry.dec64(path, writeBuf) != len)
                return;
        } else {
            try {
                FileInputStream f = new FileInputStream(path);
                f.read(writeBuf);
            } catch (IOException ex) {
                System.err.println(ex);
            }
        }
        String str = new String(writeBuf, 0, 3);
        System.out.println(str);
        if (devName.equals("1141") || devName.equals("3168"))
            if (!str.equals("PFC")) return;
        new Thread() {
            @Override
            public void run() {
                try {
                    jProgressBarBL.setMaximum(len);
                    jProgressBarBL.setValue(0);
                    byte[] buf = new byte[34];
                    ByteBuffer bb = ByteBuffer.wrap(buf);
                    jLabelStat.setText("Erase");
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    if (devName.equals("2168")) {
                        bb.putShort(0, (short)1);
                        if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf)) return;
                        sleep(100);
                        bb.putShort(0, (short)0x204);
                        if (!usbSmb.writeBytes(0xFC, Short.BYTES, buf)) return;
                        sleep(1500);
                        if (!usbSmb.readBytes(0xFA, Short.BYTES, buf)) return;
                        if (bb.getShort(0) != 0) {
                            jLabelStat.setText("Fail");
                            return;
                        }
                    } else if (devName.equals("3168")) {
                        bb.putShort(0, (short)1);
                        if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf)) return;
                        sleep(100);
                        bb.putShort(0, (short)0x102);
                        if (!usbSmb.writeBytes(0xFB, Short.BYTES, buf)) return;
                        sleep(1000);
                        if (!usbSmb.readBytes(0xFA, Short.BYTES, buf)) return;
                        if (bb.getShort(0) != -1) {
                            jLabelStat.setText("Fail");
                            return;
                        }
                    } else {
                        for (short block = 0; block < len; block += 2048)
                        {
                            bb.putShort(0, (short)-1);
                            if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf)) return;
                            bb.putShort(0, block);
                            if (!usbSmb.writeBytes(0xFC, Short.BYTES, buf)) return;
                            sleep(100);
                            if (!usbSmb.readBytes(0xFA, Short.BYTES, buf)) return;
                            if (bb.getShort(0) != block) {
                                jLabelStat.setText("Fail");
                                return;
                            }
                            jProgressBarBL.setValue(block);
                        }
                    }
                    jLabelStat.setText("Writing");
                    bb.order(ByteOrder.BIG_ENDIAN);
                    final int WRITEBYTE = 32, ROMSIZE = 0xE00;
                    if (devName.equals("2168")) {
                        for (int nWriteBytes, nPointer = 0; nPointer < len; nPointer += nWriteBytes) {
                            nWriteBytes = Math.min(len - nPointer, WRITEBYTE);
                            System.arraycopy(writeBuf, nPointer, buf, 2, nWriteBytes);
                            Arrays.fill(buf, nWriteBytes + 2, WRITEBYTE + 2, (byte)-1);
                            bb.putShort(0, (short)nPointer);
                            if (!usbSmb.writeBytes(0xF4, WRITEBYTE + 2, buf)) return;
                            sleep(10);
                            jProgressBarBL.setValue(nPointer);
                        }
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        bb.putShort(0, (short) 0);
                        if (!usbSmb.writeBytes(0xF4, Short.BYTES, buf)) return;
                    } else {
                        for (int nWriteBytes, nPointer = WRITEBYTE; nPointer < len; nPointer += nWriteBytes) {
                            nWriteBytes = Math.min(len - nPointer, WRITEBYTE);
                            System.arraycopy(writeBuf, nPointer, buf, 2, nWriteBytes);
                            Arrays.fill(buf, nWriteBytes + 2, WRITEBYTE + 2, (byte)-1);
                            bb.putShort(0, (short) (ROMSIZE + nPointer));
                            if (!usbSmb.writeBytes(0xF4, WRITEBYTE + 2, buf)) return;
//                            sleep(10);
                            sleep(15);
                            jProgressBarBL.setValue(nPointer);
                        }
                        System.arraycopy(writeBuf, 0, buf, 2, WRITEBYTE);
                        bb.putShort(0, (short) ROMSIZE);
                        if (!usbSmb.writeBytes(0xF4, WRITEBYTE + 2, buf)) return;
                        sleep(10);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        bb.putShort(0, (short) ROMSIZE);
                        if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf)) return;
                        if (!usbSmb.readBytes(0xFA, Short.BYTES, buf)) return;
                        if (bb.getShort(0) != ROMSIZE) return;
                    }
                    jLabelStat.setText("Verify");
                    for (int nReadBytes, i = 0; i < len; i += nReadBytes) {
                        jProgressBarBL.setValue(i);
                        nReadBytes = Math.min(len - i, WRITEBYTE);
                        if (!usbSmb.readBytes(0xF5, WRITEBYTE, buf)) return;
                        if (!Arrays.equals(Arrays.copyOfRange(writeBuf, i, i + nReadBytes),
                                           Arrays.copyOfRange(buf, 0, nReadBytes))) {
                            jLabelStat.setText("Verify Fail");
                            return;
                        }
                    }
                    jProgressBarBL.setValue(len);
                    jLabelStat.setText("Success");
                } catch (InterruptedException ex) {
                }
            }
        }.start();
    }//GEN-LAST:event_jButtonProgramActionPerformed

    private void jButtonReadFlashActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonReadFlashActionPerformed
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread() {
            @Override
            public void run() {
                int len = 0x8000;
                short start = 0;
                if (devName.equals("1141")) {
                    len = 0x7000;
                    start = 0xE00;
                } else if (devName.equals("2168")) {
                    len = 0xB000;
                } else if (devName.equals("3168")) {
                    len = 0xF000;
                    start = 0xE00;
                }
                String path = jTextField2.getText();
                try {
                    FileOutputStream f = new FileOutputStream(path);
                    byte[] buf = new byte[32];
                    ByteBuffer bb = ByteBuffer.wrap(buf);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    boolean success = false;
                    for (int k = 0; k < 3; k++) {
                        bb.putShort(0, start);
                        if (usbSmb.writeBytes(0xFA, Short.BYTES, buf)) {
                            if (usbSmb.readBytes(0xFA, Short.BYTES, buf)) {
                                if (bb.getShort(0) == start) {
                                    success = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (success) {
                        jProgressBarBL.setMaximum(len);
                        for (int nReadBytes = 32, i = 0; i < len; i += nReadBytes)
                        {
                            jProgressBarBL.setValue(i);
                            if (devName.equals("2168"))
                                if (i == 0xA400) {
                                    bb.putShort(0, (short) 0xB000);
                                    if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf))
                                        break;
                                }
                            if (!usbSmb.readBytes(0xF5, nReadBytes, buf))
                                break;
                            f.write(buf);
                            System.out.println(String.format("%04X : %02X", i, buf[0]));
                        }
                        jProgressBarBL.setValue(len);
                    }
                } catch (IOException ex) {
                    System.err.println(ex);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.start();
    }//GEN-LAST:event_jButtonReadFlashActionPerformed

    private void jButtonWriteFlashActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonWriteFlashActionPerformed
        String path = jTextField3.getText();
        File file = new File(path);
        if (!file.exists()) return;
        int len = (int)file.length();
        if (len > 2048) return;
        if (DllEntry.dec128(path, dfBuf) != len) return;
        System.out.println(String.format("%02X%02X", dfBuf[0], dfBuf[1]));
        new Thread() {
            @Override
            public void run() {
                try {
                    jProgressBarBL.setValue(0);
                    int nReadBytes = 32;
                    byte[] buf = new byte[34];
                    ByteBuffer bb = ByteBuffer.wrap(buf);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    short start = 0, offset = 0x400;
                    if (devName.equals("1141")) {
                        start = 0x7600;
                    } else if (devName.equals("2168")) {
                        start = (short) 0xB000;
                    }
                    bb.putShort(0, (short) (start + offset));
                    if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf)) return;
                    if (!usbSmb.readBytes(0xFA, Short.BYTES, buf)) return;
                    if (bb.getShort(0) != start + offset) return;
//                    for (int i = 0; i < 96; i += nReadBytes) {
                    for (int i = 0; i < 256; i += nReadBytes) {
                        if (!usbSmb.readBytes(0xF5, nReadBytes, buf)) return;
                        System.arraycopy(buf, 0, dfBuf, offset + i, nReadBytes);
                    }
                    jLabelStat.setText("Erase");
                    jProgressBarBL.setMaximum(100);
                    if (devName.equals("1141")) {
                        short block = 0x6800;
                        bb.putShort(0, (short)0);
                        if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf)) return;
                        bb.putShort(0, block);
                        if (!usbSmb.writeBytes(0xFC, Short.BYTES, buf)) return;
                        sleep(100);
                        if (!usbSmb.readBytes(0xFA, Short.BYTES, buf)) return;
                        if (bb.getShort(0) != block) {
                            jLabelStat.setText("Fail");
                            return;
                        }
                    } else if (devName.equals("2168")) {
                        for (int i = 0; i < 3; i++)	// sector 0~2
                        {
                            bb.putShort(0, (short) (0x308 + 0x40 * i));
                            if (!usbSmb.writeBytes(0xFD, Short.BYTES, buf)) return;
                            for (int j = 0; j < 11; j++)
                            {
                                jProgressBarBL.setValue(i*33+j*3+3);
                                sleep(110);
                            }
                        }
                    }
                    jLabelStat.setText("Writing");
                    jProgressBarBL.setMaximum(len);
                    bb.order(ByteOrder.BIG_ENDIAN);
                    for (int nWriteBytes, nPointer = 0; nPointer < len; nPointer += nWriteBytes) {
                        jProgressBarBL.setValue(nPointer);
                        nWriteBytes = Math.min(len - nPointer, 32);
                        System.arraycopy(dfBuf, nPointer, buf, 2, nWriteBytes);
                        bb.putShort(0, (short) (start + nPointer));
                        if (!usbSmb.writeBytes(0xF4, nWriteBytes + 2, buf)) return;
                        sleep(15);
                    }
                    jLabelStat.setText("Verify");
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    bb.putShort(0, start);
                    if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf)) return;
                    if (!usbSmb.readBytes(0xFA, Short.BYTES, buf)) return;
                    if (bb.getShort(0) != start) return;
                    for (int i = 0; i < len; i += nReadBytes) {
                        nReadBytes = Math.min(len - i, 32);
                        if (!usbSmb.readBytes(0xF5, nReadBytes, buf)) return;
                        if (!Arrays.equals(Arrays.copyOfRange(dfBuf, i, i + nReadBytes),
                                           Arrays.copyOfRange(buf, 0, nReadBytes))) {
                            jLabelStat.setText("Verify Fail");
                            return;
                        }
                    }
                    jProgressBarBL.setValue(len);
                    jLabelStat.setText("Success");
                } catch (InterruptedException ex) {
                }
            }
        }.start();
    }//GEN-LAST:event_jButtonWriteFlashActionPerformed

    private void jCheckBoxScanItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCheckBoxScanItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            scanning = true;
            timer.setDelay(Integer.parseInt(jTextFieldInterval.getText()));
            timer.start();
        } else {
            timer.stop();
            scanning = false;
        }
    }//GEN-LAST:event_jCheckBoxScanItemStateChanged

    private void jButtonStartLogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonStartLogActionPerformed
        if (fcLog.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        try {
            File file = fcLog.getSelectedFile();
            String path = file.getPath();
            if (!file.getName().contains(".")) {
                path += ".log";
            }
//            System.out.println("Logging: " + path);
            jTextFieldLogFile.setText(path);
            pwLog = new PrintWriter(new FileWriter(path), true);
            pwLog.print("Date");
            pwLog.print("\tTime");
            for (int i = 0; i < jTableSBS.getRowCount(); i++) {
                if ((boolean) jTableSBS.getValueAt(i, 4)) {
                    pwLog.print("\t");
                    pwLog.print(jTableSBS.getValueAt(i, 1));
                    pwLog.print(String.format(" (%s)", jTableSBS.getValueAt(i, 0)));
                }
            }
            pwLog.println();
            jButtonStartLog.setEnabled(false);
            jButtonStopLog.setEnabled(true);
            if (!scanning) {
                jCheckBoxScan.doClick();
            }
            logging = true;
        } catch (IOException ex) {
            pwLog.close();
            pwLog = null;
        }
    }//GEN-LAST:event_jButtonStartLogActionPerformed

    private void jButtonStopLogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonStopLogActionPerformed
        pwLog.close();
        pwLog = null;
        jButtonStartLog.setEnabled(true);
        jButtonStopLog.setEnabled(false);
        logging = false;
    }//GEN-LAST:event_jButtonStopLogActionPerformed

    private void jTabbedPaneMainStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTabbedPaneMainStateChanged
//        JTabbedPane jTabbedPane = (JTabbedPane)evt.getSource();
        if (jCheckBox1.isSelected()) {
            jTabbedPaneMain.setSelectedIndex(jTabbedPaneMain.indexOfTab("Command"));
        } else {
            if (scanning) {
                jCheckBoxScan.doClick();
            }
            int index = jTabbedPaneMain.getSelectedIndex();
//            if (index == 3) {
            if (index == jTabbedPaneMain.indexOfTab("Chemistry")) {
                if (usbSmb.writeWord(0, 8)) {
                    short pwValue[] = new short[1];
                    if (usbSmb.readWord(0, pwValue)) {
                        String chemID = String.format("%04X", pwValue[0]);
                        jTextFieldChemID.setText(chemID);
                        for (int i = 0; i < jTableChem.getRowCount(); i++) {
                            if (chemID.equals(jTableChem.getValueAt(i, 0))) {
                                jTableChem.setRowSelectionInterval(i, i);
                                jButtonPlot.setEnabled(true);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }//GEN-LAST:event_jTabbedPaneMainStateChanged

    /**
     * Creates a dataset, consisting of two series of monthly data.
     *
     * @return The dataset.
     */
    private static XYDataset createChemDataset(String chemID) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        
        String path = "../Chemistry/" + chemID + ".chm";
        File file = new File(path);
        if (file.exists()) {
            if (file.length() == 256) {
                byte[] buf = new byte[256];
                if (DllEntry.dec128(path, buf) == 256) {
//                    System.out.println(String.format("%02X%02X", buf[208], buf[209]));
                    final int[] dod = { 100, 95, 90, 85, 80, 75, 70, 65, 60, 55, 50, 45, 40, 35, 30, 25, 20,
                                        19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 };

                    XYSeries s1 = new XYSeries(chemID);
                    
                    // set OCV table value
                    for (int i = 36; i >= 0; i--)
                    {
                        int ocv = (Byte.toUnsignedInt(buf[i * 2]) << 8) | Byte.toUnsignedInt(buf[i * 2 + 1]);
                        s1.add(dod[i], ocv);
                    }

                    dataset.addSeries(s1);
                }
            }
        }

        return dataset;
    }
    
    private void plotChemChart(int sel) {
        String id = (String)jTableChem.getValueAt(sel, 0);
        ChemistryChart chart = new ChemistryChart("Chemistry ID: " + id);
        chart.createChartPanel(createChemDataset(id));
        chart.pack();
        UIUtils.centerFrameOnScreen(chart);
        chart.setVisible(true);
    }

    private void jButtonPlotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPlotActionPerformed
        int sel = jTableChem.getSelectedRow();
        if (sel < 0) {
//            JOptionPane.showMessageDialog(this, "Please Select a Chemistry ID");
        }
        else {
            plotChemChart(sel);
        }
    }//GEN-LAST:event_jButtonPlotActionPerformed

    private void jTableChemMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTableChemMouseClicked
        jButtonChange.setEnabled(true);
        if (evt.getClickCount() == 2) {
            System.out.println("double clicked");
            plotChemChart(jTableChem.getSelectedRow());
        }
    }//GEN-LAST:event_jTableChemMouseClicked

    private void jButtonChangeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonChangeActionPerformed
        String chemID = (String)jTableChem.getValueAt(jTableChem.getSelectedRow(), 0);
        String path = "../Chemistry/" + chemID + ".chm";
        File file = new File(path);
        if (file.exists()) {
            if (file.length() == 256) {
                byte[] buf = new byte[256];
                if (DllEntry.dec128(path, buf) == 256) {
                    System.arraycopy(buf, 0, dfBuf, 0x600, 256);
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    int index = 6;
                    if (bNewSBS) {
                        usbSmb.writeByte(0x61, 0);
                        usbSmb.writeByteVerify(0x3e, index);
                    } else {
                        usbSmb.writeWordVerify(0x77, index);
                    }
                    if (usbSmb.writeDataFlashSector(bNewSBS, buf)) {
                        jTextFieldChemID.setText(chemID);
                    }
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }
    }//GEN-LAST:event_jButtonChangeActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if (jCheckBox1.isSelected()) {
            JOptionPane.showMessageDialog(this,
                "Please Exit (UnCheck) Boot Loader !!",
                "Window Closing",
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            dispose();
        }
    }//GEN-LAST:event_formWindowClosing

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        System.out.println("Window Closed");
        System.exit(0);
    }//GEN-LAST:event_formWindowClosed
    
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
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButtonChange;
    private javax.swing.JButton jButtonDefault;
    private javax.swing.JButton jButtonExport;
    private javax.swing.JButton jButtonImport;
    private javax.swing.JButton jButtonPlot;
    private javax.swing.JButton jButtonProgram;
    private javax.swing.JButton jButtonReadAll;
    private javax.swing.JButton jButtonReadBlock;
    private javax.swing.JButton jButtonReadFlash;
    private javax.swing.JButton jButtonReadMReg;
    private javax.swing.JButton jButtonReadWord;
    private javax.swing.JButton jButtonRefresh;
    private javax.swing.JButton jButtonStartLog;
    private javax.swing.JButton jButtonStopLog;
    private javax.swing.JButton jButtonWriteAll;
    private javax.swing.JButton jButtonWriteBlock;
    private javax.swing.JButton jButtonWriteFlash;
    private javax.swing.JButton jButtonWriteMReg;
    private javax.swing.JButton jButtonWriteWord;
    private javax.swing.JButton jButtonWriteWord2;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBoxPEC;
    private javax.swing.JCheckBox jCheckBoxScan;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelSOC;
    private javax.swing.JLabel jLabelStat;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanelChem;
    private javax.swing.JPanel jPanelCommand;
    private javax.swing.JPanel jPanelDataFlash;
    private javax.swing.JPanel jPanelSBS;
    private javax.swing.JProgressBar jProgressBarBL;
    private javax.swing.JProgressBar jProgressBarDF;
    private javax.swing.JProgressBar jProgressBarSOC;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPaneBits;
    private javax.swing.JScrollPane jScrollPaneChem;
    private javax.swing.JScrollPane jScrollPaneMain;
    private javax.swing.JScrollPane jScrollPaneSBS;
    private javax.swing.JTabbedPane jTabbedPaneDataFlash;
    private javax.swing.JTabbedPane jTabbedPaneMain;
    private javax.swing.JTable jTableBits;
    private javax.swing.JTable jTableChem;
    private javax.swing.JTable jTableSBS;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextFieldBlock;
    private javax.swing.JTextField jTextFieldByte;
    private javax.swing.JTextField jTextFieldChemID;
    private javax.swing.JTextField jTextFieldCommand;
    private javax.swing.JTextField jTextFieldCount;
    private javax.swing.JTextField jTextFieldData;
    private javax.swing.JTextField jTextFieldData2;
    private javax.swing.JTextField jTextFieldInterval;
    private javax.swing.JTextField jTextFieldLogFile;
    private javax.swing.JTextField jTextFieldMReg;
    private javax.swing.JTextPane jTextPaneMessage;
    // End of variables declaration//GEN-END:variables
}
