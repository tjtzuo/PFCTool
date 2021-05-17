package com.pfc.tool;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import com.pfc.lib.DllEntry;
import com.pfc.lib.ReadWriteDF;
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
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumn;
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
//    static Logger logger;
    static Preferences prefs;
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
    Timer timerSBS, timerCalib;
    final int ONE_SECOND = 1000;
    private boolean scanning, logging = false;
    private PrintWriter pwLog;
    ChemTableModel chemTableModel;
    String devName;
    final String strEncfile = "encrypt.enc";

    /**
     * Creates new form NewJFrame
     */
    public NewJFrame() {
        devName = prefs.get("devName", ""); //NOI18N
        bNewSBS = prefs.getBoolean("newSbs", false); //NOI18N
        if (bNewSBS) {
            usbSmb.setAddr((byte) 0xAA);
        }
        socCmd = Integer.decode(prefs.get("socCmd", "0")); //NOI18N
        if (socCmd == 0) {
            socCmd = bNewSBS ? 0x2C : 0x0D;
        }
//        stradr = bNewSBS ? 0x7600 : 0xB000;
        switch (devName) {
            case "1141": //NOI18N
                stradr = 0x7600;
                break;
            case "2168": //NOI18N
                stradr = 0xB000;
                break;
            case "3168": //NOI18N
                stradr = 0xEE00;
                break;
            case "1168": //NOI18N
                stradr = 0xF000;
                break;
            default:
                stradr = 0;
                break;
        }
        SBSHandler sbsHandler = new SBSHandler();
        BitsHandler bitsHandler = new BitsHandler();
        DataFlashHandler dfHandler = new DataFlashHandler();
        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            SAXParser saxParser = saxParserFactory.newSAXParser();
            saxParser.parse(new File("../ini/SBS.xml"), sbsHandler); //NOI18N
            saxParser.parse(new File("../ini/DataFlash.xml"), dfHandler); //NOI18N
            saxParser.parse(new File("../ini/Bits.xml"), bitsHandler); //NOI18N
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

        String checkBoxText = jCheckBox1.getText();
        if (bNewSBS) {
            checkBoxText += " HDQ";
        } else {
            checkBoxText += " PEC";
        }
        jCheckBox1.setText(checkBoxText);
        jCheckBox2.setText(checkBoxText);
        jCheckBox3.setText(checkBoxText);

        if (bNewSBS) {
            jCheckBoxPCM.setVisible(false);
            jPanelCalib.setVisible(false);
        } else {
            jButtonReadByte.setVisible(false);
            jButtonWriteByte.setVisible(false);
        }

        if (devName.equals("1168")) {
            jPanel2.setVisible(false);
        }

        File file = new File("../ini/" + strEncfile);
        if (file.exists()) {
            try {
                Scanner fileScanner = new Scanner(file);
                while (fileScanner.hasNextLine()) {
                    jComboBox1.addItem(fileScanner.nextLine());
                }
            } catch (Exception ex) {
                System.err.println(ex);
            }
        } else {
            jPanel9.setVisible(false);
        }

        // Set custom color renderer
        BitsColorRenderer colorRenderer = new BitsColorRenderer();
        jTableBits.setDefaultRenderer(String.class, colorRenderer);

        TableColumn column = null;
        for (int i = 0; i < jTableSBS.getColumnCount(); i++) {
            column = jTableSBS.getColumnModel().getColumn(i);
            if (i == 1) {
                column.setPreferredWidth(150); //name
            } else if (i == 2) {
                column.setPreferredWidth(75); //value
            } else {
                column.setPreferredWidth(25);
            }
        }

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
        filter = new FileNameExtensionFilter(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("DATAFLASH FILES (*.IFI)"), "ifi"); //NOI18N
        fcIfi = new JFileChooser();
        fcIfi.setFileFilter(filter);
        filter = new FileNameExtensionFilter(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("CODE BINARY FILES (*.COD)"), "cod"); //NOI18N
        fcCod = new JFileChooser();
        fcCod.setFileFilter(filter);
        filter = new FileNameExtensionFilter(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("BINARY FILES (*.BIN)"), "bin"); //NOI18N
        fcBin = new JFileChooser();
        fcBin.setFileFilter(filter);
        filter = new FileNameExtensionFilter(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("LOG FILES (*.LOG)"), "log"); //NOI18N
        fcLog = new JFileChooser();
        fcLog.setFileFilter(filter);

        //Create a timer.
        timerSBS = new Timer(ONE_SECOND, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                refreshSBS();
                if (logging) {
                    Date date = new Date();
                    pwLog.print(String.format("%tF", date)); //NOI18N
                    pwLog.print(String.format("\t%tT", date)); //NOI18N
                    for (int i = 0; i < jTableSBS.getRowCount(); i++) {
                        if ((boolean) jTableSBS.getValueAt(i, 4)) {
                            pwLog.print("\t"); //NOI18N
                            pwLog.print(jTableSBS.getValueAt(i, 2));
                        }
                    }
                    pwLog.println();
                }
            }
        });
        timerCalib = new Timer(ONE_SECOND * 2, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                byte cmdCurr = 0x0A, cmdTemp = 0x08, cmdVolt = 0x3F;
                short pwValue[] = new short[1];
                if (usbSmb.readWord(cmdCurr, pwValue)) {
                    jTextFieldCurrM.setText(Short.toString(pwValue[0]));
                }
                if (usbSmb.readWord(cmdTemp, pwValue)) {
                    jTextFieldTempM.setText(String.format("%1$5.1f", ((float) pwValue[0] - 2731.5) / 10.0));
                }
                for (int i = 0; i < 4; i++) {
                    if (usbSmb.readWord(cmdVolt - i, pwValue)) {
                        switch (i) {
                            case 0:
                                jTextFieldVCell1M.setText(Short.toString(pwValue[0]));
                                break;
                            case 1:
                                jTextFieldVCell2M.setText(Short.toString(pwValue[0]));
                                break;
                            case 2:
                                jTextFieldVCell3M.setText(Short.toString(pwValue[0]));
                                break;
                            case 3:
                                jTextFieldVCell4M.setText(Short.toString(pwValue[0]));
                                break;
                        }
                    }
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
        jCheckBox1 = new javax.swing.JCheckBox();
        jPanel9 = new javax.swing.JPanel();
        jComboBox1 = new javax.swing.JComboBox<>();
        jTextField4 = new javax.swing.JTextField();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jPanelDataFlash = new javax.swing.JPanel();
        jTabbedPaneDataFlash = new javax.swing.JTabbedPane();
        jButtonReadAll = new javax.swing.JButton();
        jButtonWriteAll = new javax.swing.JButton();
        jButtonDefault = new javax.swing.JButton();
        jButtonExport = new javax.swing.JButton();
        jButtonImport = new javax.swing.JButton();
        jProgressBarDF = new javax.swing.JProgressBar();
        jCheckBox2 = new javax.swing.JCheckBox();
        jCheckBoxPCM = new javax.swing.JCheckBox();
        jPanelCommand = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPaneMessage = new javax.swing.JTextPane();
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
        jTextFieldCount = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jButtonReadByte = new javax.swing.JButton();
        jButtonWriteByte = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jTextFieldMReg = new javax.swing.JTextField();
        jTextFieldByte = new javax.swing.JTextField();
        jButtonReadMReg = new javax.swing.JButton();
        jButtonWriteMReg = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jCheckBoxBootLoader = new javax.swing.JCheckBox();
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
        jCheckBox3 = new javax.swing.JCheckBox();
        jButtonReset = new javax.swing.JButton();
        jButtonGasGauge = new javax.swing.JButton();
        jPanelCalib = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jButtonCalibCC1 = new javax.swing.JButton();
        jLabel12 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jButtonCalibVT = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jCheckBoxTemp = new javax.swing.JCheckBox();
        jTextFieldTempM = new javax.swing.JTextField();
        jTextFieldTempA = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jCheckBoxVolt = new javax.swing.JCheckBox();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jTextFieldVCell4M = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        jTextFieldVCell4A = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jTextFieldVCell3A = new javax.swing.JTextField();
        jTextFieldVCell3M = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jTextFieldVCell2A = new javax.swing.JTextField();
        jLabel29 = new javax.swing.JLabel();
        jTextFieldVCell1A = new javax.swing.JTextField();
        jLabel27 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        jLabel30 = new javax.swing.JLabel();
        jTextFieldVCell1M = new javax.swing.JTextField();
        jLabel28 = new javax.swing.JLabel();
        jTextFieldVCell2M = new javax.swing.JTextField();
        jLabel26 = new javax.swing.JLabel();
        jLabel31 = new javax.swing.JLabel();
        jTextFieldCellN = new javax.swing.JTextField();
        jPanel8 = new javax.swing.JPanel();
        jButtonCalibCC2 = new javax.swing.JButton();
        jTextFieldCurrM = new javax.swing.JTextField();
        jTextFieldCurrA = new javax.swing.JTextField();
        jLabel32 = new javax.swing.JLabel();
        jLabel33 = new javax.swing.JLabel();
        jLabel34 = new javax.swing.JLabel();
        jLabel35 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jPanelChem = new javax.swing.JPanel();
        jScrollPaneChem = new javax.swing.JScrollPane();
        jTableChem = new javax.swing.JTable();
        jButtonPlot = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        jTextFieldChemID = new javax.swing.JTextField();
        jButtonChange = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle"); // NOI18N
        setTitle(bundle.getString("NewJFrame.title")); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jTabbedPaneMain.setToolTipText(bundle.getString("NewJFrame.jTabbedPaneMain.toolTipText")); // NOI18N
        jTabbedPaneMain.setFont(new java.awt.Font("SimSun", 1, 20)); // NOI18N
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

        jButtonRefresh.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jButtonRefresh.setText(bundle.getString("NewJFrame.jButtonRefresh.text")); // NOI18N
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

        jLabelSOC.setText(bundle.getString("NewJFrame.jLabelSOC.text")); // NOI18N

        jCheckBoxScan.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jCheckBoxScan.setText(bundle.getString("NewJFrame.jCheckBoxScan.text")); // NOI18N
        jCheckBoxScan.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCheckBoxScanItemStateChanged(evt);
            }
        });

        jLabel7.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel7.setText(bundle.getString("NewJFrame.jLabel7.text")); // NOI18N

        jTextFieldInterval.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jTextFieldInterval.setText(bundle.getString("NewJFrame.jTextFieldInterval.text")); // NOI18N

        jLabel8.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel8.setText(bundle.getString("NewJFrame.jLabel8.text")); // NOI18N

        jButtonStartLog.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jButtonStartLog.setText(bundle.getString("NewJFrame.jButtonStartLog.text")); // NOI18N
        jButtonStartLog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonStartLogActionPerformed(evt);
            }
        });

        jButtonStopLog.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jButtonStopLog.setText(bundle.getString("NewJFrame.jButtonStopLog.text")); // NOI18N
        jButtonStopLog.setEnabled(false);
        jButtonStopLog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonStopLogActionPerformed(evt);
            }
        });

        jTextFieldLogFile.setEditable(false);
        jTextFieldLogFile.setHorizontalAlignment(javax.swing.JTextField.TRAILING);

        jCheckBox1.setText(bundle.getString("NewJFrame.jCheckBox1.text")); // NOI18N
        jCheckBox1.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCheckBoxItemStateChanged(evt);
            }
        });

        jPanel9.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("NewJFrame.jPanel9.border.title"))); // NOI18N

        jTextField4.setEditable(false);
        jTextField4.setText(bundle.getString("NewJFrame.jTextField4.text")); // NOI18N

        jButton4.setText(bundle.getString("NewJFrame.jButton4.text")); // NOI18N
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jButton5.setText(bundle.getString("NewJFrame.jButton5.text")); // NOI18N
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jButton6.setText(bundle.getString("NewJFrame.jButton6.text")); // NOI18N
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton6)
                    .addGroup(jPanel9Layout.createSequentialGroup()
                        .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButton4)))
                .addContainerGap(45, Short.MAX_VALUE))
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addComponent(jComboBox1, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton6))
        );

        javax.swing.GroupLayout jPanelSBSLayout = new javax.swing.GroupLayout(jPanelSBS);
        jPanelSBS.setLayout(jPanelSBSLayout);
        jPanelSBSLayout.setHorizontalGroup(
            jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSBSLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPaneSBS, javax.swing.GroupLayout.DEFAULT_SIZE, 391, Short.MAX_VALUE)
                .addGroup(jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelSBSLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPaneBits, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelSBSLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jCheckBox1)
                        .addGap(108, 108, 108))
                    .addGroup(jPanelSBSLayout.createSequentialGroup()
                        .addGap(30, 30, 30)
                        .addGroup(jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButtonStopLog)
                            .addGroup(jPanelSBSLayout.createSequentialGroup()
                                .addGroup(jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jCheckBoxScan)
                                    .addGroup(jPanelSBSLayout.createSequentialGroup()
                                        .addComponent(jLabel7)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextFieldInterval, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel8))
                                    .addComponent(jButtonRefresh)
                                    .addComponent(jButtonStartLog))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jTextFieldLogFile, javax.swing.GroupLayout.PREFERRED_SIZE, 311, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelSBSLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelSOC)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jProgressBarSOC, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())))
        );
        jPanelSBSLayout.setVerticalGroup(
            jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSBSLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPaneSBS, javax.swing.GroupLayout.DEFAULT_SIZE, 627, Short.MAX_VALUE)
                    .addGroup(jPanelSBSLayout.createSequentialGroup()
                        .addComponent(jCheckBox1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanelSBSLayout.createSequentialGroup()
                                .addComponent(jButtonRefresh)
                                .addGap(18, 18, 18)
                                .addComponent(jCheckBoxScan)
                                .addGap(9, 9, 9)
                                .addGroup(jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel7)
                                    .addComponent(jTextFieldInterval, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel8))
                                .addGap(19, 19, 19)
                                .addComponent(jButtonStartLog))
                            .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(jTextFieldLogFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButtonStopLog)
                        .addGap(21, 21, 21)
                        .addGroup(jPanelSBSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jProgressBarSOC, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabelSOC))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPaneBits, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))))
        );

        jTabbedPaneMain.addTab(bundle.getString("NewJFrame.jPanelSBS.TabConstraints.tabTitle"), jPanelSBS); // NOI18N

        jTabbedPaneDataFlash.setTabPlacement(javax.swing.JTabbedPane.LEFT);
        jTabbedPaneDataFlash.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N

        jButtonReadAll.setText(bundle.getString("NewJFrame.jButtonReadAll.text")); // NOI18N
        jButtonReadAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReadAllActionPerformed(evt);
            }
        });

        jButtonWriteAll.setText(bundle.getString("NewJFrame.jButtonWriteAll.text")); // NOI18N
        jButtonWriteAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonWriteAllActionPerformed(evt);
            }
        });

        jButtonDefault.setText(bundle.getString("NewJFrame.jButtonDefault.text")); // NOI18N
        jButtonDefault.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDefaultActionPerformed(evt);
            }
        });

        jButtonExport.setText(bundle.getString("NewJFrame.jButtonExport.text")); // NOI18N
        jButtonExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExportActionPerformed(evt);
            }
        });

        jButtonImport.setText(bundle.getString("NewJFrame.jButtonImport.text")); // NOI18N
        jButtonImport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonImportActionPerformed(evt);
            }
        });

        jProgressBarDF.setMaximum(2048);

        jCheckBox2.setText(bundle.getString("NewJFrame.jCheckBox1.text")); // NOI18N
        jCheckBox2.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCheckBoxItemStateChanged(evt);
            }
        });

        jCheckBoxPCM.setText(bundle.getString("NewJFrame.jCheckBoxPCM.text")); // NOI18N

        javax.swing.GroupLayout jPanelDataFlashLayout = new javax.swing.GroupLayout(jPanelDataFlash);
        jPanelDataFlash.setLayout(jPanelDataFlashLayout);
        jPanelDataFlashLayout.setHorizontalGroup(
            jPanelDataFlashLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelDataFlashLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanelDataFlashLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jTabbedPaneDataFlash, javax.swing.GroupLayout.PREFERRED_SIZE, 507, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jProgressBarDF, javax.swing.GroupLayout.PREFERRED_SIZE, 392, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanelDataFlashLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelDataFlashLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanelDataFlashLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelDataFlashLayout.createSequentialGroup()
                                .addComponent(jButtonWriteAll)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jCheckBoxPCM))
                            .addComponent(jButtonReadAll)
                            .addComponent(jButtonDefault)
                            .addComponent(jButtonExport)
                            .addComponent(jButtonImport))
                        .addContainerGap(80, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelDataFlashLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jCheckBox2)
                        .addGap(41, 41, 41))))
        );
        jPanelDataFlashLayout.setVerticalGroup(
            jPanelDataFlashLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelDataFlashLayout.createSequentialGroup()
                .addGap(82, 82, 82)
                .addComponent(jButtonReadAll)
                .addGap(32, 32, 32)
                .addGroup(jPanelDataFlashLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonWriteAll)
                    .addComponent(jCheckBoxPCM))
                .addGap(32, 32, 32)
                .addComponent(jButtonDefault)
                .addGap(28, 28, 28)
                .addComponent(jButtonExport)
                .addGap(33, 33, 33)
                .addComponent(jButtonImport)
                .addContainerGap(315, Short.MAX_VALUE))
            .addGroup(jPanelDataFlashLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelDataFlashLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPaneDataFlash)
                    .addGroup(jPanelDataFlashLayout.createSequentialGroup()
                        .addComponent(jCheckBox2)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jProgressBarDF, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8))
        );

        jTabbedPaneMain.addTab(bundle.getString("NewJFrame.jPanelDataFlash.TabConstraints.tabTitle"), jPanelDataFlash); // NOI18N

        jTextPaneMessage.setEditable(false);
        jTextPaneMessage.setBackground(java.awt.SystemColor.controlHighlight);
        jTextPaneMessage.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        jScrollPane1.setViewportView(jTextPaneMessage);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, bundle.getString("NewJFrame.jPanel1.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("SimSun", 0, 12))); // NOI18N

        jLabel1.setText(bundle.getString("NewJFrame.jLabel1.text")); // NOI18N

        jLabel2.setText(bundle.getString("NewJFrame.jLabel2.text")); // NOI18N

        jLabel3.setText(bundle.getString("NewJFrame.jLabel3.text")); // NOI18N

        jLabel4.setText(bundle.getString("NewJFrame.jLabel4.text")); // NOI18N

        jTextFieldCommand.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextFieldCommand.setText(bundle.getString("NewJFrame.jTextFieldCommand.text")); // NOI18N

        jTextFieldData.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextFieldData.setText(bundle.getString("NewJFrame.jTextFieldData.text")); // NOI18N

        jTextFieldData2.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextFieldData2.setText(bundle.getString("NewJFrame.jTextFieldData2.text")); // NOI18N
        jTextFieldData2.setToolTipText(bundle.getString("NewJFrame.jTextFieldData2.toolTipText")); // NOI18N

        jButtonReadWord.setText(bundle.getString("NewJFrame.jButtonReadWord.text")); // NOI18N
        jButtonReadWord.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReadWordActionPerformed(evt);
            }
        });

        jButtonWriteWord.setText(bundle.getString("NewJFrame.jButtonWriteWord.text")); // NOI18N
        jButtonWriteWord.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonWriteWordActionPerformed(evt);
            }
        });

        jButtonWriteWord2.setText(bundle.getString("NewJFrame.jButtonWriteWord2.text")); // NOI18N
        jButtonWriteWord2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonWriteWord2ActionPerformed(evt);
            }
        });

        jButtonReadBlock.setText(bundle.getString("NewJFrame.jButtonReadBlock.text")); // NOI18N
        jButtonReadBlock.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReadBlockActionPerformed(evt);
            }
        });

        jButtonWriteBlock.setText(bundle.getString("NewJFrame.jButtonWriteBlock.text")); // NOI18N
        jButtonWriteBlock.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonWriteBlockActionPerformed(evt);
            }
        });

        jTextFieldCount.setText(bundle.getString("NewJFrame.jTextFieldCount.text")); // NOI18N

        jLabel10.setText(bundle.getString("NewJFrame.jLabel10.text")); // NOI18N

        jButtonReadByte.setText(bundle.getString("NewJFrame.jButtonReadByte.text")); // NOI18N
        jButtonReadByte.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReadByteActionPerformed(evt);
            }
        });

        jButtonWriteByte.setText(bundle.getString("NewJFrame.jButtonWriteByte.text")); // NOI18N
        jButtonWriteByte.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonWriteByteActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldBlock))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel2)
                                    .addComponent(jLabel3))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jTextFieldData2, javax.swing.GroupLayout.DEFAULT_SIZE, 53, Short.MAX_VALUE)
                                    .addComponent(jTextFieldData))
                                .addGap(21, 21, 21)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jButtonWriteWord2)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addComponent(jButtonReadWord)
                                            .addComponent(jButtonWriteWord))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jButtonReadByte)
                                            .addComponent(jButtonWriteByte)))))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addGap(18, 18, 18)
                                .addComponent(jTextFieldCommand, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButtonReadBlock, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldCount, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jButtonWriteBlock, javax.swing.GroupLayout.Alignment.TRAILING))))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(jTextFieldCommand, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonReadWord)
                            .addComponent(jButtonReadByte)))
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
                            .addComponent(jLabel2)
                            .addComponent(jButtonWriteByte))
                        .addGap(16, 16, 16)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(jTextFieldData2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonWriteWord2)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(52, 52, 52)
                        .addComponent(jButtonWriteBlock)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldBlock, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addContainerGap(13, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, bundle.getString("NewJFrame.jPanel2.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("SimSun", 0, 12))); // NOI18N

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText(bundle.getString("NewJFrame.jLabel5.text")); // NOI18N

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText(bundle.getString("NewJFrame.jLabel6.text")); // NOI18N

        jTextFieldMReg.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextFieldMReg.setText(bundle.getString("NewJFrame.jTextFieldMReg.text")); // NOI18N

        jTextFieldByte.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextFieldByte.setText(bundle.getString("NewJFrame.jTextFieldByte.text")); // NOI18N

        jButtonReadMReg.setText(bundle.getString("NewJFrame.jButtonReadMReg.text")); // NOI18N
        jButtonReadMReg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReadMRegActionPerformed(evt);
            }
        });

        jButtonWriteMReg.setText(bundle.getString("NewJFrame.jButtonWriteMReg.text")); // NOI18N
        jButtonWriteMReg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonWriteMRegActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jTextFieldByte)
                    .addComponent(jTextFieldMReg, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonReadMReg)
                    .addComponent(jButtonWriteMReg))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldMReg, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonReadMReg))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldByte, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonWriteMReg))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(null, bundle.getString("NewJFrame.jPanel3.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("SimSun", 0, 12))); // NOI18N

        jCheckBoxBootLoader.setText(bundle.getString("NewJFrame.jCheckBoxBootLoader.text")); // NOI18N
        jCheckBoxBootLoader.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCheckBoxBootLoaderItemStateChanged(evt);
            }
        });

        jProgressBarBL.setStringPainted(true);

        jButton1.setText(bundle.getString("NewJFrame.jButton1.text")); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText(bundle.getString("NewJFrame.jButton2.text")); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setText(bundle.getString("NewJFrame.jButton3.text")); // NOI18N
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButtonProgram.setText(bundle.getString("NewJFrame.jButtonProgram.text")); // NOI18N
        jButtonProgram.setEnabled(false);
        jButtonProgram.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonProgramActionPerformed(evt);
            }
        });

        jButtonReadFlash.setText(bundle.getString("NewJFrame.jButtonReadFlash.text")); // NOI18N
        jButtonReadFlash.setEnabled(false);
        jButtonReadFlash.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReadFlashActionPerformed(evt);
            }
        });

        jButtonWriteFlash.setText(bundle.getString("NewJFrame.jButtonWriteFlash.text")); // NOI18N
        jButtonWriteFlash.setEnabled(false);
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
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jCheckBoxBootLoader)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jProgressBarBL, javax.swing.GroupLayout.DEFAULT_SIZE, 324, Short.MAX_VALUE))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelStat, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jButton2, jButton3});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabelStat, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jCheckBoxBootLoader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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

        jCheckBox3.setText(bundle.getString("NewJFrame.jCheckBox1.text")); // NOI18N
        jCheckBox3.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCheckBoxItemStateChanged(evt);
            }
        });

        jButtonReset.setText(bundle.getString("NewJFrame.jButtonReset.text")); // NOI18N
        jButtonReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonResetActionPerformed(evt);
            }
        });

        jButtonGasGauge.setText(bundle.getString("NewJFrame.jButtonGasGauge.text")); // NOI18N
        jButtonGasGauge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGasGaugeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelCommandLayout = new javax.swing.GroupLayout(jPanelCommand);
        jPanelCommand.setLayout(jPanelCommandLayout);
        jPanelCommandLayout.setHorizontalGroup(
            jPanelCommandLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCommandLayout.createSequentialGroup()
                .addGroup(jPanelCommandLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelCommandLayout.createSequentialGroup()
                        .addGap(33, 33, 33)
                        .addGroup(jPanelCommandLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelCommandLayout.createSequentialGroup()
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(57, 57, 57)
                                .addGroup(jPanelCommandLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jButtonReset)
                                    .addComponent(jButtonGasGauge)))
                            .addGroup(jPanelCommandLayout.createSequentialGroup()
                                .addGap(4, 4, 4)
                                .addGroup(jPanelCommandLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addGroup(jPanelCommandLayout.createSequentialGroup()
                        .addGap(43, 43, 43)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 399, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(106, 106, 106)
                        .addComponent(jCheckBox3)))
                .addContainerGap(138, Short.MAX_VALUE))
        );
        jPanelCommandLayout.setVerticalGroup(
            jPanelCommandLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCommandLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelCommandLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckBox3, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGroup(jPanelCommandLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelCommandLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelCommandLayout.createSequentialGroup()
                        .addGap(51, 51, 51)
                        .addComponent(jButtonReset)
                        .addGap(18, 18, 18)
                        .addComponent(jButtonGasGauge)))
                .addGap(18, 18, 18)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPaneMain.addTab(bundle.getString("NewJFrame.jPanelCommand.TabConstraints.tabTitle"), jPanelCommand); // NOI18N

        jLabel11.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel11.setText(bundle.getString("NewJFrame.jLabel11.text")); // NOI18N

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(null, bundle.getString("NewJFrame.jPanel4.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("SimSun", 0, 12))); // NOI18N

        jButtonCalibCC1.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jButtonCalibCC1.setText(bundle.getString("NewJFrame.jButtonCalibCC1.text")); // NOI18N
        jButtonCalibCC1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCalibCC1ActionPerformed(evt);
            }
        });

        jLabel12.setBackground(java.awt.Color.yellow);
        jLabel12.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel12.setText(bundle.getString("NewJFrame.jLabel12.text")); // NOI18N
        jLabel12.setOpaque(true);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButtonCalibCC1)
                .addGap(18, 18, 18)
                .addComponent(jLabel12)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonCalibCC1)
                    .addComponent(jLabel12))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder(null, bundle.getString("NewJFrame.jPanel5.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("SimSun", 0, 12))); // NOI18N

        jButtonCalibVT.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jButtonCalibVT.setText(bundle.getString("NewJFrame.jButtonCalibVT.text")); // NOI18N
        jButtonCalibVT.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCalibVTActionPerformed(evt);
            }
        });

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder(null, bundle.getString("NewJFrame.jPanel6.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("SimSun", 0, 12))); // NOI18N

        jCheckBoxTemp.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jCheckBoxTemp.setText(bundle.getString("NewJFrame.jCheckBoxTemp.text")); // NOI18N

        jTextFieldTempM.setEditable(false);
        jTextFieldTempM.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jTextFieldTempM.setText(bundle.getString("NewJFrame.jTextFieldTempM.text")); // NOI18N

        jTextFieldTempA.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jTextFieldTempA.setText(bundle.getString("NewJFrame.jTextFieldTempA.text")); // NOI18N

        jLabel13.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel13.setText(bundle.getString("NewJFrame.jLabel13.text")); // NOI18N

        jLabel14.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel14.setText(bundle.getString("NewJFrame.jLabel14.text")); // NOI18N

        jLabel15.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel15.setText(bundle.getString("NewJFrame.jLabel15.text")); // NOI18N

        jLabel16.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel16.setText(bundle.getString("NewJFrame.jLabel16.text")); // NOI18N

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jCheckBoxTemp)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jTextFieldTempM, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
                        .addComponent(jLabel13))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel15)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addGap(18, 18, 18)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jTextFieldTempA, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel14))
                    .addComponent(jLabel16))
                .addGap(2, 2, 2))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addGap(0, 4, Short.MAX_VALUE)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldTempM, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldTempA, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13)
                    .addComponent(jLabel14))
                .addContainerGap())
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jCheckBoxTemp)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder(null, bundle.getString("NewJFrame.jPanel7.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("SimSun", 0, 12))); // NOI18N

        jCheckBoxVolt.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jCheckBoxVolt.setText(bundle.getString("NewJFrame.jCheckBoxVolt.text")); // NOI18N

        jLabel17.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel17.setText(bundle.getString("NewJFrame.jLabel17.text")); // NOI18N

        jLabel18.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel18.setText(bundle.getString("NewJFrame.jLabel18.text")); // NOI18N

        jTextFieldVCell4M.setEditable(false);
        jTextFieldVCell4M.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jTextFieldVCell4M.setText(bundle.getString("NewJFrame.jTextFieldVCell4M.text")); // NOI18N

        jLabel19.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel19.setText(bundle.getString("NewJFrame.jLabel19.text")); // NOI18N

        jTextFieldVCell4A.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jTextFieldVCell4A.setText(bundle.getString("NewJFrame.jTextFieldVCell4A.text")); // NOI18N

        jLabel20.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel20.setText(bundle.getString("NewJFrame.jLabel20.text")); // NOI18N

        jLabel21.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel21.setText(bundle.getString("NewJFrame.jLabel21.text")); // NOI18N

        jTextFieldVCell3A.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jTextFieldVCell3A.setText(bundle.getString("NewJFrame.jTextFieldVCell3A.text")); // NOI18N

        jTextFieldVCell3M.setEditable(false);
        jTextFieldVCell3M.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jTextFieldVCell3M.setText(bundle.getString("NewJFrame.jTextFieldVCell3M.text")); // NOI18N

        jLabel22.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel22.setText(bundle.getString("NewJFrame.jLabel22.text")); // NOI18N

        jLabel24.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel24.setText(bundle.getString("NewJFrame.jLabel24.text")); // NOI18N

        jLabel23.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel23.setText(bundle.getString("NewJFrame.jLabel23.text")); // NOI18N

        jTextFieldVCell2A.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jTextFieldVCell2A.setText(bundle.getString("NewJFrame.jTextFieldVCell2A.text")); // NOI18N

        jLabel29.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel29.setText(bundle.getString("NewJFrame.jLabel29.text")); // NOI18N

        jTextFieldVCell1A.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jTextFieldVCell1A.setText(bundle.getString("NewJFrame.jTextFieldVCell1A.text")); // NOI18N

        jLabel27.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel27.setText(bundle.getString("NewJFrame.jLabel27.text")); // NOI18N

        jLabel25.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel25.setText(bundle.getString("NewJFrame.jLabel25.text")); // NOI18N

        jLabel30.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel30.setText(bundle.getString("NewJFrame.jLabel30.text")); // NOI18N

        jTextFieldVCell1M.setEditable(false);
        jTextFieldVCell1M.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jTextFieldVCell1M.setText(bundle.getString("NewJFrame.jTextFieldVCell1M.text")); // NOI18N

        jLabel28.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel28.setText(bundle.getString("NewJFrame.jLabel28.text")); // NOI18N

        jTextFieldVCell2M.setEditable(false);
        jTextFieldVCell2M.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jTextFieldVCell2M.setText(bundle.getString("NewJFrame.jTextFieldVCell2M.text")); // NOI18N

        jLabel26.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel26.setText(bundle.getString("NewJFrame.jLabel26.text")); // NOI18N

        jLabel31.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel31.setText(bundle.getString("NewJFrame.jLabel31.text")); // NOI18N

        jTextFieldCellN.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jTextFieldCellN.setText(bundle.getString("NewJFrame.jTextFieldCellN.text")); // NOI18N

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addContainerGap(10, Short.MAX_VALUE)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel17)
                        .addGap(34, 34, 34)
                        .addComponent(jLabel18))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addGroup(jPanel7Layout.createSequentialGroup()
                                    .addComponent(jLabel21)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(jTextFieldVCell4M, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jLabel19)
                                    .addGap(29, 29, 29)
                                    .addComponent(jTextFieldVCell4A, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(jLabel20))
                                .addGroup(jPanel7Layout.createSequentialGroup()
                                    .addComponent(jCheckBoxVolt)
                                    .addGap(18, 18, 18)
                                    .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                                            .addComponent(jLabel24)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(jTextFieldVCell3M, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(jLabel22)
                                            .addGap(29, 29, 29)
                                            .addComponent(jTextFieldVCell3A, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(jLabel23))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                                            .addComponent(jLabel30)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(jTextFieldVCell2M, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(jLabel26)
                                            .addGap(29, 29, 29)
                                            .addComponent(jTextFieldVCell2A, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(jLabel28))))
                                .addGroup(jPanel7Layout.createSequentialGroup()
                                    .addComponent(jLabel29)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(jTextFieldVCell1M, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jLabel25)
                                    .addGap(29, 29, 29)
                                    .addComponent(jTextFieldVCell1A, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(jLabel27)))
                            .addGroup(jPanel7Layout.createSequentialGroup()
                                .addGap(71, 71, 71)
                                .addComponent(jLabel31)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldCellN, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(8, 8, 8)))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel18))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldVCell4M, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldVCell4A, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel19)
                    .addComponent(jLabel20)
                    .addComponent(jLabel21))
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jCheckBoxVolt))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGap(7, 7, 7)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextFieldVCell3M, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextFieldVCell3A, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel22)
                            .addComponent(jLabel23)
                            .addComponent(jLabel24))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextFieldVCell2M, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextFieldVCell2A, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel26)
                            .addComponent(jLabel28)
                            .addComponent(jLabel30))
                        .addGap(7, 7, 7)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextFieldVCell1M, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextFieldVCell1A, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel25)
                            .addComponent(jLabel27)
                            .addComponent(jLabel29))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 11, Short.MAX_VALUE)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel31)
                    .addComponent(jTextFieldCellN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonCalibVT))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButtonCalibVT)
                .addGap(18, 18, 18)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel8.setBorder(javax.swing.BorderFactory.createTitledBorder(null, bundle.getString("NewJFrame.jPanel8.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("SimSun", 0, 12))); // NOI18N

        jButtonCalibCC2.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jButtonCalibCC2.setText(bundle.getString("NewJFrame.jButtonCalibCC2.text")); // NOI18N
        jButtonCalibCC2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCalibCC2ActionPerformed(evt);
            }
        });

        jTextFieldCurrM.setEditable(false);
        jTextFieldCurrM.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jTextFieldCurrM.setText(bundle.getString("NewJFrame.jTextFieldCurrM.text")); // NOI18N

        jTextFieldCurrA.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jTextFieldCurrA.setText(bundle.getString("NewJFrame.jTextFieldCurrA.text")); // NOI18N

        jLabel32.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel32.setText(bundle.getString("NewJFrame.jLabel32.text")); // NOI18N

        jLabel33.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel33.setText(bundle.getString("NewJFrame.jLabel33.text")); // NOI18N

        jLabel34.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel34.setText(bundle.getString("NewJFrame.jLabel34.text")); // NOI18N

        jLabel35.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jLabel35.setText(bundle.getString("NewJFrame.jLabel35.text")); // NOI18N

        jScrollPane2.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane2.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        jScrollPane2.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jScrollPane2.setHorizontalScrollBar(null);

        jTextArea1.setEditable(false);
        jTextArea1.setBackground(java.awt.Color.cyan);
        jTextArea1.setColumns(20);
        jTextArea1.setFont(new java.awt.Font("SimSun", 0, 12)); // NOI18N
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setText(bundle.getString("NewJFrame.jTextArea1.text")); // NOI18N
        jTextArea1.setWrapStyleWord(true);
        jScrollPane2.setViewportView(jTextArea1);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jButtonCalibCC2, javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel8Layout.createSequentialGroup()
                                .addGap(11, 11, 11)
                                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel8Layout.createSequentialGroup()
                                        .addComponent(jTextFieldCurrM, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel32))
                                    .addComponent(jLabel34))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel8Layout.createSequentialGroup()
                                        .addComponent(jTextFieldCurrA, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel33))
                                    .addComponent(jLabel35))))
                        .addGap(0, 59, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addComponent(jButtonCalibCC2)
                .addGap(18, 18, 18)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel34, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel35))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextFieldCurrM, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel32)))
                    .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jTextFieldCurrA, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel33)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 21, Short.MAX_VALUE)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(24, 24, 24))
        );

        javax.swing.GroupLayout jPanelCalibLayout = new javax.swing.GroupLayout(jPanelCalib);
        jPanelCalib.setLayout(jPanelCalibLayout);
        jPanelCalibLayout.setHorizontalGroup(
            jPanelCalibLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCalibLayout.createSequentialGroup()
                .addGroup(jPanelCalibLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelCalibLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel11))
                    .addGroup(jPanelCalibLayout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(jPanelCalibLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanelCalibLayout.createSequentialGroup()
                                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(29, 29, 29)
                                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap(125, Short.MAX_VALUE))
        );
        jPanelCalibLayout.setVerticalGroup(
            jPanelCalibLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCalibLayout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(jLabel11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 49, Short.MAX_VALUE)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(42, 42, 42)
                .addGroup(jPanelCalibLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(31, 31, 31))
        );

        jTabbedPaneMain.addTab(bundle.getString("NewJFrame.jPanelCalib.TabConstraints.tabTitle"), jPanelCalib); // NOI18N

        jTableChem.setModel(chemTableModel);
        jTableChem.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jTableChem.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTableChemMouseClicked(evt);
            }
        });
        jScrollPaneChem.setViewportView(jTableChem);

        jButtonPlot.setText(bundle.getString("NewJFrame.jButtonPlot.text")); // NOI18N
        jButtonPlot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPlotActionPerformed(evt);
            }
        });

        jLabel9.setText(bundle.getString("NewJFrame.jLabel9.text")); // NOI18N

        jTextFieldChemID.setEditable(false);

        jButtonChange.setText(bundle.getString("NewJFrame.jButtonChange.text")); // NOI18N
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 118, Short.MAX_VALUE)
                .addGroup(jPanelChemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelChemLayout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldChemID, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jButtonPlot, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButtonChange))
                .addContainerGap())
        );
        jPanelChemLayout.setVerticalGroup(
            jPanelChemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelChemLayout.createSequentialGroup()
                .addGroup(jPanelChemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanelChemLayout.createSequentialGroup()
                        .addGap(32, 32, 32)
                        .addGroup(jPanelChemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel9)
                            .addComponent(jTextFieldChemID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(jButtonChange)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonPlot))
                    .addGroup(jPanelChemLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPaneChem, javax.swing.GroupLayout.DEFAULT_SIZE, 617, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jTabbedPaneMain.addTab(bundle.getString("NewJFrame.jPanelChem.TabConstraints.tabTitle"), jPanelChem); // NOI18N

        jScrollPaneMain.setViewportView(jTabbedPaneMain);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPaneMain, javax.swing.GroupLayout.DEFAULT_SIZE, 812, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPaneMain, javax.swing.GroupLayout.DEFAULT_SIZE, 677, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonRefreshActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonRefreshActionPerformed
        refreshSBS();
    }//GEN-LAST:event_jButtonRefreshActionPerformed

    private void refreshSBS() {
        usbSmb.writeWord(0, 0);
        for (int i = 0; i < jTableSBS.getRowCount(); i++) {
            if (!(boolean) jTableSBS.getValueAt(i, 5)) {
                continue;
            }
            String str = ""; //NOI18N
            SBS sbs = sbsList.get(i);
            String format = sbs.getFormat();
            int cmd = sbs.getCmd();
            if (format.equals("String")) { //NOI18N
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
            } else {
                short[] pwValue = new short[1];
//                if (usbSmb.ReadWord(Byte.decode("0x"+cmd), pwValue)) {
                if (usbSmb.readWord(cmd, pwValue)) {
                    switch (sbs.getFormat()) {
                        case "Hex": //NOI18N
                            int size = sbs.getSize();
                            if (size > Short.BYTES) {
                                byte[] buf = new byte[size];
                                if (usbSmb.readBlock(cmd, size, buf)) {
                                    StringBuilder result = new StringBuilder();
                                    for (byte b : buf) {
                                        result.append(String.format("%02X ", b)); //NOI18N
                                    }
                                    str = result.toString();
                                }
                            } else {
//                            str = "0x" + Integer.toHexString(pwValue[0]);
                                str = String.format("0x%04X", pwValue[0]); //NOI18N
                            }
                            break;
                        case "Date": //NOI18N
                            str = String.format("%1$d-%2$d-%3$d", //NOI18N
                                    1980 + (pwValue[0] >> 9), (pwValue[0] >> 5) & 0xF, pwValue[0] & 0x1F);
                            break;
                        case "Temp": //NOI18N
                            str = String.format("%1$5.1f", ((float) pwValue[0] - 2731.5) / 10.0); //NOI18N
                            break;
                        case "UInt": //NOI18N
                            str = Integer.toString(Short.toUnsignedInt(pwValue[0]));
                            break;
                        case "Int": //NOI18N
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
        for (BitField bf : bfList) {
            short[] pwValue = new short[1];
            if (usbSmb.readWord(bf.getCmd(), pwValue)) {
                int size = bf.getSize();
                for (int j = 0; j < size; j++) {
                    byte val = (byte) (pwValue[0] >> ((size - 1 - j) * 8));
                    String str = String.format("0x%1$02X", val); //NOI18N
                    jTableBits.setValueAt(str, i++, 2);
                }
            }
        }
        jTableBits.repaint();

        if (jPanel9.isVisible()) {
            short[] pwEncryptIndex = new short[1];
            if (usbSmb.readWord(0xF1, pwEncryptIndex)) {
                if (pwEncryptIndex[0] < -1 || pwEncryptIndex[0] >= jComboBox1.getItemCount()) {
                    pwEncryptIndex[0] = 0;
                }
                jTextField4.setText(Short.toString(pwEncryptIndex[0]));
                jComboBox1.setSelectedIndex(pwEncryptIndex[0]);
            }
        }
    }

    private void jButtonReadAllActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonReadAllActionPerformed
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
                        JOptionPane.showMessageDialog(null,
                                java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("PLEASE UNSEAL !!"),
                                java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("READ DATAFLASH"),
                                JOptionPane.INFORMATION_MESSAGE);
                        setCursor(null);
                        return;
                    }
                }

                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                int index;
                for (index = 0; index < 8; index++) {
                    if (bNewSBS) {
                        for (retry_count = 0; retry_count < retry_end; retry_count++) {
                            if (usbSmb.writeByteVerify(0x3e, index)) {
//                            if (usbSmb.writeWordVerify(0x77, index)) {
                                break;
                            }
                        }
                        if (retry_count == retry_end) {
                            break;
                        }
                    }
                    for (retry_count = 0; retry_count < retry_end; retry_count++) {
                        byte[] sector_buf = new byte[256];
                        boolean success;
                        if (bNewSBS) {
                            success = usbSmb.readDataFlashSector(bNewSBS, sector_buf);
                        } else {
                            success = ReadWriteDF.readDataFlash(0, index, sector_buf, usbSmb.isPEC());
                        }
                        if (success) {
                            System.arraycopy(sector_buf, 0, dfBuf, index * 256, sector_buf.length);
                            break;
                        }
                    }
                    if (retry_count == retry_end) {
                        break;
                    }
                    jProgressBarDF.setValue(256 * (index + 1));
                }
                if (index == 8) {
                    refreshDataFlash();
                } else {
                    JOptionPane.showMessageDialog(null,
                            java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("FAILED !!!"),
                            java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("READ DATAFLASH"),
                            JOptionPane.ERROR_MESSAGE);
                }
                setCursor(Cursor.getDefaultCursor());
            }
        }.start();
    }//GEN-LAST:event_jButtonReadAllActionPerformed

    /****************************************************************************/
    /**
    *   Calculates the CRC-8 used as part of SMBus over a block of memory.
    */
    int Crc8( int inCrc, int inData)
    {
        int data = inCrc ^ inData;
        for (int i = 0; i < 8; i++ )
        {
            if (( data & 0x80 ) != 0 )
            {
                data <<= 1;
                data ^= 0x07;
            }
            else
            {
                data <<= 1;
            }
        }
        return (data & 0xFF);
    } // Crc8
    int Crc8Block( int inCrc, byte[] data, int len )
    {
        int crc = inCrc;
        for (int i = 0; i < len; i++)
        {
            crc = Crc8( crc, data[i] );
        }
        return (crc & 0xFF);
    } // Crc8Block

    private void jButtonWriteAllActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonWriteAllActionPerformed
        updateDataFlash();

        if (jCheckBoxPCM.isSelected())
        {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("PCM Flash File (*.pcm)", "pcm"));
//            fc.setAcceptAllFileFilterUsed(false);
            int returnVal = fc.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                String path = file.getPath();
                if (!file.getName().contains(".")) { //NOI18N
                    path += ".pcm"; //NOI18N
                }
                if (new File(path).exists()) {
                    if (JOptionPane.showConfirmDialog(this, java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("OVERWRITE EXIST FILE !?")) != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                try {
                    PrintWriter pw = new PrintWriter(new FileWriter(path));
                    for (int i = 0; i < 8; i++) {
                        pw.printf("16 77 0%d 00", i);
                        if (usbSmb.isPEC()) {
                            pw.printf(" %02x", Crc8(Crc8(Crc8(Crc8(0, 0x16), 0x77), i), 0));
                        }
                        pw.println();
                        for (int j = 0; j < 8; j++) {
                            int crc = Crc8(Crc8(Crc8(0, 0x16), 0x78+j), 0x20);
                            pw.printf("16 %2x 20", 0x78+j);
                            for (int k = 0; k < 32; k++) {
                                byte data = dfBuf[i*256+j*32+k];
                                pw.printf(" %02x", data);
                                crc = Crc8(crc, data);
                            }
                            if (usbSmb.isPEC()) {
                                pw.printf(" %02x", crc);
                            }
                            pw.println();
                        }
                    }
                    pw.close();
                } catch (IOException ex) {
                    System.err.println(ex);
                }
            }
            return;
        }

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
                    if (bNewSBS) {
                        for (retry_count = 0; retry_count < retry_end; retry_count++) {
                            if (usbSmb.writeByteVerify(0x3e, index)) {
//                            if (usbSmb.writeWordVerify(0x77, index)) {
                                break;
                            }
                        }
                        try {
                            sleep(10);
                        } catch (InterruptedException ex) {
                        }
                        if (retry_count == retry_end) {
                            break;
                        }
                    }

                    for (retry_count = 0; retry_count < retry_end; retry_count++) {
                        byte[] sector_buf = new byte[256];
                        System.arraycopy(dfBuf, index * 256, sector_buf, 0, sector_buf.length);
                        if (bNewSBS) {
                            if (usbSmb.writeDataFlashSector(bNewSBS, sector_buf)) {
                                break;
                            }
                        } else {
                            if (ReadWriteDF.writeDataFlash(0, index, sector_buf, usbSmb.isPEC())) {
                                break;
                            }
                        }
                    }
                    try {
                        sleep(10);
                    } catch (InterruptedException ex) {
                    }
                    if (retry_count == retry_end) {
                        break;
                    }

                    if (devName.equals("1168") && index == 3) {
                        index = 7;
                    }

                    jProgressBarDF.setValue(256 * (index + 1));
                }

                if (index == 8) {
                    JOptionPane.showMessageDialog(null,
                            java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("SUCCESS"),
                            java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("WRITE DATAFLASH"),
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    usbSmb.writeWord(0, 0x43);
                    JOptionPane.showMessageDialog(null,
                            java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("FAILED !!!"),
                            java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("WRITE DATAFLASH"),
                            JOptionPane.ERROR_MESSAGE);
                }
                setCursor(Cursor.getDefaultCursor());
            }
        }.start();
    }//GEN-LAST:event_jButtonWriteAllActionPerformed

    private void jButtonDefaultActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonDefaultActionPerformed
        int len = DllEntry.dec128("../ini/Default.ifi", dfBuf); //NOI18N
        if (len == 2048) {
            refreshDataFlash();
        }
    }//GEN-LAST:event_jButtonDefaultActionPerformed

    private void jButtonExportActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonExportActionPerformed
        int returnVal = fcIfi.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fcIfi.getSelectedFile();
            String path = file.getPath();
            if (!file.getName().contains(".")) { //NOI18N
                path += ".ifi"; //NOI18N
            }
            System.out.println("Saving: " + path);
//            Path p = Paths.get(path);
            updateDataFlash();
            try {
//                Files.write(p, dfBuf, StandardOpenOption.CREATE_NEW);
//                FileOutputStream f = new FileOutputStream(file);
                FileOutputStream f = new FileOutputStream(path);
                f.write(dfBuf);
                f.close();
                if (DllEntry.cod128(path)) {
                    JOptionPane.showMessageDialog(this, java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("SUCCESS"));
                }
            } catch (IOException ex) {
                System.err.println(ex);
            }
        }
    }//GEN-LAST:event_jButtonExportActionPerformed

    private void jButtonImportActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonImportActionPerformed
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
                int value = 0, value2 = 0,
                        addr = df.getStartAdr() - stradr;
                String str = "", type = df.getType(); //NOI18N
                switch (type) {
                    case "S1": //NOI18N
                        value = dfBuf[addr];
                        break;
                    case "U1": //NOI18N
                        value = Byte.toUnsignedInt(dfBuf[addr]);
                        break;
                    case "S2": //NOI18N
                        value = (dfBuf[addr] << 8)
                                | Byte.toUnsignedInt(dfBuf[addr + 1]);
                        break;
                    case "U2": //NOI18N
                        value = (Byte.toUnsignedInt(dfBuf[addr]) << 8)
                                | Byte.toUnsignedInt(dfBuf[addr + 1]);
                        break;
                    case "S8": //NOI18N
                    case "U8": //NOI18N
                        value2 = (dfBuf[addr + 4] << 24)
                                | (Byte.toUnsignedInt(dfBuf[addr + 5]) << 16)
                                | (Byte.toUnsignedInt(dfBuf[addr + 6]) << 8)
                                | Byte.toUnsignedInt(dfBuf[addr + 7]);
                    case "S4": //NOI18N
                    case "U4": //NOI18N
                        value = (dfBuf[addr] << 24)
                                | (Byte.toUnsignedInt(dfBuf[addr + 1]) << 16)
                                | (Byte.toUnsignedInt(dfBuf[addr + 2]) << 8)
                                | Byte.toUnsignedInt(dfBuf[addr + 3]);
                        break;
                    case "string": //NOI18N
                        byte len = dfBuf[addr];
                        if (len > 0 && len < 32) {
                            str = new String(dfBuf, addr + 1, len);
                        }
                        break;
                    case "-": //NOI18N
                        str = "-"; //NOI18N
                        break;
                    default:
                        assert (false);
                }
                if (df.getUnit().equals("hex")) { //NOI18N
                    switch (type) {
                        case "S1": //NOI18N
                        case "U1": //NOI18N
                        case "S2": //NOI18N
                        case "U2": //NOI18N
                        case "S4": //NOI18N
                        case "U4": //NOI18N
                            str = "0x" + Integer.toHexString(value); //NOI18N
                            break;
                        case "S8": //NOI18N
                        case "U8": //NOI18N
                            str = "0x" + Long.toHexString(((long) value << 32) | Integer.toUnsignedLong(value2)); //NOI18N
                            break;
                        default:
                    }
                } else if (df.getUnit().equals("date")) { //NOI18N
                    str = String.format("%1$d-%2$d-%3$d", 1980 + (value >> 9), (value >> 5) & 0xF, value & 0x1F); //NOI18N
                } else {
                    switch (type) {
                        case "S1": //NOI18N
                        case "U1": //NOI18N
                        case "S2": //NOI18N
                        case "U2": //NOI18N
                        case "S4": //NOI18N
                            str = Integer.toString(value);
                            break;
                        case "U4": //NOI18N
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
                if (type.equals("string")) { //NOI18N
                    byte[] bytes = str.getBytes();
                    dfBuf[addr] = (byte) Math.min(bytes.length, 32);
                    System.arraycopy(bytes, 0, dfBuf, addr + 1, dfBuf[addr]);
                } else if (type.equals("-")) { //NOI18N
                } else {
                    int value = 0;
                    try {
                        if (df.getUnit().equals("hex")) { //NOI18N
                            if (str.length() > 2) {
                                value = Integer.parseUnsignedInt(str.substring(2), 16);
                            }
                        } else if (df.getUnit().equals("date")) { //NOI18N
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
                        case "S1": //NOI18N
                        case "U1": //NOI18N
                            dfBuf[addr] = (byte) value;
                            break;
                        case "S2": //NOI18N
                        case "U2": //NOI18N
                            dfBuf[addr] = (byte) (value >> 8);
                            dfBuf[addr + 1] = (byte) value;
                            break;
                        case "S4": //NOI18N
                        case "U4": //NOI18N
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

    private void jButtonReadWordActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonReadWordActionPerformed
        int command = Integer.parseInt(jTextFieldCommand.getText(), 16);
        short pwValue[] = new short[1];
        if (usbSmb.readWord(command, pwValue)) {
            jTextPaneMessage.setText(Integer.toHexString(Short.toUnsignedInt(pwValue[0])).toUpperCase());
        } else {
            jTextPaneMessage.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("READ FAIL"));
        }
    }//GEN-LAST:event_jButtonReadWordActionPerformed

    private void jButtonWriteWordActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonWriteWordActionPerformed
        int command = Integer.parseInt(jTextFieldCommand.getText(), 16);
        int value = Integer.parseInt(jTextFieldData.getText(), 16);
        if (usbSmb.writeWord(command, value)) {
            jTextPaneMessage.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("WRITE SUCCESS"));
        } else {
            jTextPaneMessage.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("WRITE FAIL"));
        }
    }//GEN-LAST:event_jButtonWriteWordActionPerformed

    private void jButtonWriteWord2ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonWriteWord2ActionPerformed
        int command = Integer.parseInt(jTextFieldCommand.getText(), 16);
        int value = Integer.parseInt(jTextFieldData.getText(), 16),
                value2 = Integer.parseInt(jTextFieldData2.getText(), 16);
        if (usbSmb.isHDQ()) {
            if (usbSmb.writeByte(command, -1)) {
                if (usbSmb.writeByte(command + 1, (byte) (value >> 8))) {
                    if (usbSmb.writeByte(command, (byte) value)) {
                        if (usbSmb.writeByte(command + 1, (byte) (value2 >> 8))) {
                            if (usbSmb.writeByte(command, (byte) value2)) {
                                jTextPaneMessage.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("WRITE SUCCESS"));
                                return;
                            }
                        }
                    }
                }
            }
        } else if (usbSmb.writeWord(command, value)) {
            if (usbSmb.writeWord(command, value2)) {
                jTextPaneMessage.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("WRITE SUCCESS"));
                return;
            }
        }
        jTextPaneMessage.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("WRITE FAIL"));
    }//GEN-LAST:event_jButtonWriteWord2ActionPerformed

    private void jButtonReadBlockActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonReadBlockActionPerformed
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
                    result.append(String.format("%02X ", b)); //NOI18N
                }
                jTextPaneMessage.setText(result.toString());
                return;
            }
        }
//        }
        jTextPaneMessage.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("READ FAIL"));
    }//GEN-LAST:event_jButtonReadBlockActionPerformed

    private void jButtonWriteBlockActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonWriteBlockActionPerformed
        int command = Integer.parseInt(jTextFieldCommand.getText(), 16);
        String hexData = jTextFieldBlock.getText();
        int count = hexData.length() / 3;
        if ((hexData.length() % 3) != 0) {
            count++;
        }
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
                jTextPaneMessage.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("WRITE SUCCESS"));
            } else {
                jTextPaneMessage.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("WRITE FAIL"));
            }
        }
    }//GEN-LAST:event_jButtonWriteBlockActionPerformed

    private void jButtonReadMRegActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonReadMRegActionPerformed
        byte mreg = (byte) Integer.parseInt(jTextFieldMReg.getText(), 16);
        byte[] bytes = {0x10, mreg};
        if (usbSmb.writeBytes(0xF5, bytes.length, bytes)) {
            if (usbSmb.readBytes(0xF5, bytes.length, bytes)) {
//                jTextFieldByte.setText(Integer.toHexString(Byte.toUnsignedInt(bytes[0])).toUpperCase());
                jTextFieldByte.setText(String.format("%02X", bytes[0])); //NOI18N
                jTextPaneMessage.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("READ SUCCESS"));
                return;
            }
        }
        jTextPaneMessage.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("READ FAIL"));
    }//GEN-LAST:event_jButtonReadMRegActionPerformed

    private void jButtonWriteMRegActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonWriteMRegActionPerformed
        byte mreg = (byte) Integer.parseInt(jTextFieldMReg.getText(), 16),
                value = (byte) Integer.parseInt(jTextFieldByte.getText(), 16);
        byte[] bytes = {mreg, value};
        if (usbSmb.writeBytes(0xF4, bytes.length, bytes)) {
            jTextPaneMessage.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("WRITE SUCCESS"));
        } else {
            jTextPaneMessage.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("WRITE FAIL"));
        }
    }//GEN-LAST:event_jButtonWriteMRegActionPerformed

    private void jCheckBoxBootLoaderItemStateChanged(ItemEvent evt) {//GEN-FIRST:event_jCheckBoxBootLoaderItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            jCheckBox3.setSelected(false);
            jProgressBarBL.setValue(0);
            if (devName.equals("1168")) { //NOI18N
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            if (usbSmb.writeWord(0, 0x0F00)) {
                                short pwValue[] = new short[1];
                                for (int i = 0; i < 10; i++) {
                                    sleep(100);
                                    if (usbSmb.readWord(0, pwValue)) {
                                        if (pwValue[0] == 0x000F) {
                                            break;
                                        }
                                    }
                                }
                                if (usbSmb.writeWord(0xFA, 0x888)) {
                                    if (usbSmb.readWord(0xFA, pwValue)) {
                                        if (pwValue[0] == 0x888) {
                                            jProgressBarBL.setValue(jProgressBarBL.getMaximum());
                                            jButtonProgram.setEnabled(true);
                                            jButtonWriteFlash.setEnabled(true);
                                            jButtonReadFlash.setEnabled(true);
                                        }
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            System.err.println(ex);
                        } finally {
                            setCursor(Cursor.getDefaultCursor());
                        }
                    }
                }.start();
            } else {
            try {
//                File file = new File("../ini/BootLoader_A1141.bin");
                File file = new File("../ini/BootLoader_A" + devName + ".bin"); //NOI18N
                int len = (int) file.length();
                byte blBuf[] = new byte[len];
                new FileInputStream(file).read(blBuf);
                jProgressBarBL.setMaximum(len * 2);
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            byte[] buf = new byte[34];
                            if (devName.equals("2168")) { //NOI18N
                                buf[0] = 0x1c;
                                buf[1] = 0x0f;
                            } else {
                                buf[0] = 0x1e;
                                buf[1] = 0x00;
                            }
                            if (!usbSmb.writeBytes(0xF4, 2, buf)) {
                                return;
                            }
                            usbSmb.setAddr((byte) 0x16);
                            sleep(100);
                            if (!usbSmb.writeByte(0xFF, 0x09)) {
                                return;
                            }
                            sleep(100);
                            if (!usbSmb.writeByte(0xF3, 0x11)) {
                                return;
                            }
                            sleep(100);
                            buf[0] = 0x10;
                            buf[1] = 0x30;
                            buf[2] = 0x00;
                            if (!usbSmb.writeBytes(0xF4, 3, buf)) {
                                return;
                            }
                            sleep(100);
                            for (int nWriteBytes, nPointer = 0; nPointer < len; nPointer += nWriteBytes) {
                                jProgressBarBL.setValue(nPointer);
                                nWriteBytes = Math.min(len - nPointer, 32);
                                System.arraycopy(blBuf, nPointer, buf, 2, nWriteBytes);
                                buf[0] = (byte) (nPointer >> 8);
                                buf[1] = (byte) nPointer;
                                if (!usbSmb.writeBytes(0xF4, nWriteBytes + 2, buf)) {
                                    return;
                                }
                            }
                            sleep(100);
                            buf[0] = 0x00;
                            buf[1] = 0x00;
                            if (!usbSmb.writeBytes(0xF4, 2, buf)) {
                                return;
                            }
                            sleep(100);
                            for (int nReadBytes, i = 0; i < len; i += nReadBytes) {
                                jProgressBarBL.setValue(len + i);
                                nReadBytes = Math.min(len - i, 32);
                                if (!usbSmb.readBytes(0xF5, nReadBytes, buf)) {
                                    return;
                                }
                                if (!Arrays.equals(Arrays.copyOfRange(blBuf, i, i + nReadBytes),
                                        Arrays.copyOfRange(buf, 0, nReadBytes))) {
                                    return;
                                }
                            }
                            sleep(100);
                            if (!usbSmb.writeByte(0xF1, 0x01)) {
                                return;
                            }
                            sleep(100);
                            if (!usbSmb.writeByte(0xFF, 0x09)) {
                                return;
                            }
                            sleep(100);
                            buf[0] = -120;
                            buf[1] = 8;
                            if (!usbSmb.writeBytes(0xFA, 2, buf)) {
                                return;
                            }
                            sleep(100);
                            if (!usbSmb.readBytes(0xFA, 2, buf)) {
                                return;
                            }
                            if ((Byte.toUnsignedInt(buf[1]) << 8 | Byte.toUnsignedInt(buf[0])) != 0x888) {
                                return;
                            }
                            buf[0] = 1;
                            buf[1] = 0;
                            usbSmb.writeBytes(0x99, 2, buf);
                            jProgressBarBL.setValue(len * 2);
                            jButtonProgram.setEnabled(true);
                            jButtonWriteFlash.setEnabled(true);
                            jButtonReadFlash.setEnabled(true);
                        } catch (InterruptedException ex) {
                        }
                    }
                }.start();
            } catch (IOException ex) {
                System.err.println(ex);
            }
            }
        } else {
            jProgressBarBL.setMaximum(100);
            new Thread() {
                @Override
                public void run() {
                    try {
                        jProgressBarBL.setValue(100);
                        if (devName.equals("1168")) { //NOI18N
                            if (!usbSmb.writeWord(0xFF, 0x888)) {
                                if (!usbSmb.writeWord(0xFF, 0x888)) {
                                    return;
                                }
                            }
                            sleep(250);
                            jProgressBarBL.setValue(90);
                            sleep(250);
                            jProgressBarBL.setValue(80);
                            sleep(250);
                            jProgressBarBL.setValue(70);
                            sleep(250);
                            jProgressBarBL.setValue(60);
                            sleep(250);
                            jProgressBarBL.setValue(50);
                            sleep(250);
                            jProgressBarBL.setValue(40);
                        } else {
                        byte[] buf = new byte[2];
                        buf[1] = 0x00;
                        if (devName.equals("2168")) { //NOI18N
                            buf[0] = 0x0F;
                            if (!usbSmb.writeBytes(0x1C, 2, buf)) {
                                return;
                            }
                        } else {
                            buf[0] = 0x00;
                            if (!usbSmb.writeBytes(0x1E, 2, buf)) {
                                return;
                            }
                        }
                        sleep(50);
                        jProgressBarBL.setValue(90);
                        sleep(50);
                        jProgressBarBL.setValue(80);
                        if (!usbSmb.writeByte(0xFF, 0x09)) {
                            return;
                        }
                        sleep(50);
                        jProgressBarBL.setValue(70);
                        sleep(50);
                        jProgressBarBL.setValue(60);
                        if (!usbSmb.writeByte(0xF0, 0x00)) {
                            return;
                        }
                        sleep(50);
                        jProgressBarBL.setValue(50);
                        sleep(50);
                        jProgressBarBL.setValue(40);
                        if (!usbSmb.writeByte(0xFF, 0x09)) {
                            return;
                        }
                        }
                        sleep(250);
                        jProgressBarBL.setValue(30);
                        sleep(250);
                        jProgressBarBL.setValue(20);
                        sleep(250);
                        jProgressBarBL.setValue(10);
                        sleep(250);
                    } catch (InterruptedException ex) {
                    }
                    jProgressBarBL.setValue(0);
                    if (bNewSBS) {
                        usbSmb.setAddr((byte) 0xAA);
                    }
                    jButtonProgram.setEnabled(false);
                    jButtonWriteFlash.setEnabled(false);
                    jButtonReadFlash.setEnabled(false);
                    jLabelStat.setText("");
                }
            }.start();
        }
    }//GEN-LAST:event_jCheckBoxBootLoaderItemStateChanged

    private void jButton1ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        int returnVal = fcCod.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fcCod.getSelectedFile();
            if (file.exists()) {
                jTextField1.setText(file.getPath());
            }
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        int returnVal = fcBin.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fcBin.getSelectedFile();
            String path = file.getPath();
            if (!file.getName().contains(".")) { //NOI18N
                path += ".bin"; //NOI18N
            }
            if (new File(path).exists()) {
                if (JOptionPane.showConfirmDialog(this, java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("OVERWRITE EXIST FILE !?")) != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            jTextField2.setText(path);
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        int returnVal = fcIfi.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fcIfi.getSelectedFile();
            if (file.exists()) {
                jTextField3.setText(file.getPath());
            }
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButtonProgramActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonProgramActionPerformed
        final int WRITEBYTE = 32, ROMSIZE = 0xE00;
        String path = jTextField1.getText();
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        int len = (int) file.length();
        if (len > 65536) {
            return;
        }
        byte[] writeBuf = new byte[len];
        if (path.toLowerCase().endsWith(".cod")) { //NOI18N
            if (DllEntry.dec64(path, writeBuf) != len) {
                return;
            }
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
        if (devName.equals("1141") || devName.equals("3168")) //NOI18N
        {
            if (!str.equals("PFC")) {
                return; //NOI18N
            }
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    jProgressBarBL.setMaximum(len);
                    jProgressBarBL.setValue(0);
                    byte[] buf = new byte[34];
                    ByteBuffer bb = ByteBuffer.wrap(buf);
                    jLabelStat.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("ERASE"));
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    if (devName.equals("2168")) { //NOI18N
                        bb.putShort(0, (short) 1);
                        if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf)) {
                            return;
                        }
                        sleep(100);
                        bb.putShort(0, (short) 0x204);
                        if (!usbSmb.writeBytes(0xFC, Short.BYTES, buf)) {
                            return;
                        }
                        sleep(1500);
                        if (!usbSmb.readBytes(0xFA, Short.BYTES, buf)) {
                            return;
                        }
                        if (bb.getShort(0) != 0) {
                            jLabelStat.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("FAIL"));
                            return;
                        }
                    } else if (devName.equals("3168")) { //NOI18N
                        if (len > stradr - ROMSIZE) {
                            bb.putShort(0, (short) 1);
                            if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf)) {
                                return;
                            }
                            sleep(100);
                            bb.putShort(0, (short) 0x102);
                            if (!usbSmb.writeBytes(0xFB, Short.BYTES, buf)) {
                                return;
                            }
                            sleep(1000);
                            if (!usbSmb.readBytes(0xFA, Short.BYTES, buf)) {
                                return;
                            }
                            if (bb.getShort(0) != -1) {
                                jLabelStat.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("FAIL"));
                                return;
                            }
                        } else {
                            bb.putShort(0, (short) 1);
                            if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf)) {
                                return;
                            }
                            sleep(100);
                            bb.putShort(0, (short) 0x204);
                            if (!usbSmb.writeBytes(0xFC, Short.BYTES, buf)) {
                                return;
                            }
//                            sleep(3000);
                            for (int j = 1; j <= 100; j++) {
                                jProgressBarBL.setValue(len * j / 100);
                                sleep(30);
                            }
                            if (!usbSmb.readBytes(0xFA, Short.BYTES, buf)) {
                                return;
                            }
                            if (bb.getShort(0) != 0) {
                                jLabelStat.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("FAIL"));
                                return;
                            }
                            /*for (int addr = 0; addr < len; addr += 128)
                            {
                            bb.putShort(0, (short)-1);
                            if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf)) return;
                            sleep(10);
                            bb.putShort(0, (short)addr);
                            if (!usbSmb.writeBytes(0xF9, Short.BYTES, buf)) return;
                            sleep(20);
                            if (!usbSmb.readBytes(0xFA, Short.BYTES, buf)) return;
                            if (bb.getShort(0) != addr) {
                            jLabelStat.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("FAIL"));
                            return;
                            }
                            jProgressBarBL.setValue(addr);
                            }*/
                        }
                    } else if (devName.equals("1141")) {
                        for (short block = 0; block < len; block += 2048) {
                            bb.putShort(0, (short) -1);
                            if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf)) {
                                return;
                            }
                            bb.putShort(0, block);
                            if (!usbSmb.writeBytes(0xFC, Short.BYTES, buf)) {
                                return;
                            }
                            sleep(100);
                            if (!usbSmb.readBytes(0xFA, Short.BYTES, buf)) {
                                return;
                            }
                            if (bb.getShort(0) != block) {
                                jLabelStat.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("FAIL"));
                                return;
                            }
                            jProgressBarBL.setValue(block);
                        }
                    } else if (devName.equals("1168")) {
                        /*bb.order(ByteOrder.BIG_ENDIAN);
                        Arrays.fill(buf, 2, WRITEBYTE + 2, (byte) 0);
                        bb.putShort(0, (short) 0xEFE0);
                        if (!usbSmb.writeBytes(0xF4, WRITEBYTE + 2, buf)) {
                        return;
                        }
                        sleep(10);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        for (int addr = 1024; addr < len; addr += 1024) {
                        bb.putShort(0, (short) -1);
                        if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf)) {
                        return;
                        }
                        bb.putShort(0, (short)addr);
                        if (!usbSmb.writeBytes(0xFC, Short.BYTES, buf)) {
                        return;
                        }
                        sleep(100);
                        if (!usbSmb.readBytes(0xFA, Short.BYTES, buf)) {
                        return;
                        }
                        if (bb.getShort(0) != 0) {
                        jLabelStat.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("FAIL"));
                        return;
                        }
                        jProgressBarBL.setValue(addr);
                        }*/
                        bb.putShort(0, (short) 1);
                        if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf)) {
                            return;
                        }
//                        sleep(10);
                        bb.putShort(0, (short) 0x102);
                        if (!usbSmb.writeBytes(0xFB, Short.BYTES, buf)) {
                            return;
                        }
                        for (int i = 0; i < 3; i++) {
                            sleep(1000);
                            if (usbSmb.readBytes(0xFA, Short.BYTES, buf)) {
                                if (bb.getShort(0) == -1) {
                                    break;
                                }
                            }
                        }
                        if (bb.getShort(0) != -1) {
                            jLabelStat.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("FAIL"));
                            return;
                        }
                    }
                    jProgressBarBL.setValue(0);
                    jLabelStat.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("WRITING"));
                    bb.order(ByteOrder.BIG_ENDIAN);
                    if (devName.equals("1168")) { //NOI18N
                        for (int nWriteBytes, nPointer = 1024; nPointer < len; nPointer += nWriteBytes) {
                            nWriteBytes = Math.min(len - nPointer, WRITEBYTE);
                            System.arraycopy(writeBuf, nPointer, buf, 2, nWriteBytes);
                            Arrays.fill(buf, nWriteBytes + 2, WRITEBYTE + 2, (byte) -1);
                            bb.putShort(0, (short) nPointer);
                            if (!usbSmb.writeBytes(0xF4, WRITEBYTE + 2, buf)) {
                                return;
                            }
                            sleep(2);
                            jProgressBarBL.setValue(nPointer - 1024);
                        }
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        bb.putShort(0, (short) 1);
                        if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf)) {
                            return;
                        }
                        bb.putShort(0, (short)0);
                        if (!usbSmb.writeBytes(0xFC, Short.BYTES, buf)) {
                            return;
                        }
                        sleep(50);
                        if (!usbSmb.readBytes(0xFA, Short.BYTES, buf)) {
                            return;
                        }
                        if (bb.getShort(0) != 0) {
                            return;
                        }
                        bb.order(ByteOrder.BIG_ENDIAN);
                        for (int nPointer = 1024 - WRITEBYTE; nPointer >= 0; nPointer -= WRITEBYTE) {
                            System.arraycopy(writeBuf, nPointer, buf, 2, WRITEBYTE);
                            bb.putShort(0, (short) nPointer);
                            usbSmb.writeBytes(0xF4, WRITEBYTE + 2, buf);
                            sleep(2);
                            jProgressBarBL.setValue(len - nPointer);
                        }
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        bb.putShort(0, (short) 0);
                        if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf)) {
                            return;
                        }
                    } else if (devName.equals("2168")) { //NOI18N
                        for (int nWriteBytes, nPointer = 0; nPointer < len; nPointer += nWriteBytes) {
                            nWriteBytes = Math.min(len - nPointer, WRITEBYTE);
                            System.arraycopy(writeBuf, nPointer, buf, 2, nWriteBytes);
                            Arrays.fill(buf, nWriteBytes + 2, WRITEBYTE + 2, (byte) -1);
                            bb.putShort(0, (short) nPointer);
                            if (!usbSmb.writeBytes(0xF4, WRITEBYTE + 2, buf)) {
                                return;
                            }
                            sleep(10);
                            jProgressBarBL.setValue(nPointer);
                        }
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        bb.putShort(0, (short) 0);
                        if (!usbSmb.writeBytes(0xF4, Short.BYTES, buf)) {
                            return;
                        }
                    } else {
                        for (int nWriteBytes, nPointer = WRITEBYTE; nPointer < len; nPointer += nWriteBytes) {
                            nWriteBytes = Math.min(len - nPointer, WRITEBYTE);
                            System.arraycopy(writeBuf, nPointer, buf, 2, nWriteBytes);
                            Arrays.fill(buf, nWriteBytes + 2, WRITEBYTE + 2, (byte) -1);
                            bb.putShort(0, (short) (ROMSIZE + nPointer));
                            if (!usbSmb.writeBytes(0xF4, WRITEBYTE + 2, buf)) {
                                return;
                            }
//                            sleep(10);
                            sleep(15);
                            jProgressBarBL.setValue(nPointer);
                        }
                        System.arraycopy(writeBuf, 0, buf, 2, WRITEBYTE);
                        bb.putShort(0, (short) ROMSIZE);
                        if (!usbSmb.writeBytes(0xF4, WRITEBYTE + 2, buf)) {
                            return;
                        }
                        sleep(10);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        bb.putShort(0, (short) ROMSIZE);
                        if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf)) {
                            return;
                        }
                        if (!usbSmb.readBytes(0xFA, Short.BYTES, buf)) {
                            return;
                        }
                        if (bb.getShort(0) != ROMSIZE) {
                            return;
                        }
                    }
                    jLabelStat.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("VERIFY"));
                    int addr = 0;
//                    if (devName.equals("1168")) { //NOI18N
//                        addr = 1024;
//                    }
                    for (int nReadBytes; addr < len; addr += nReadBytes) {
                        jProgressBarBL.setValue(addr);
                        nReadBytes = Math.min(len - addr, WRITEBYTE);
                        if (!usbSmb.readBytes(0xF5, WRITEBYTE, buf)) {
                            return;
                        }
                        if (!Arrays.equals(
                                Arrays.copyOfRange(writeBuf, addr, addr + nReadBytes),
                                Arrays.copyOfRange(buf, 0, nReadBytes))) {
                            jLabelStat.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("VERIFY FAIL"));
                            return;
                        }
                    }
                    jProgressBarBL.setValue(len);
                    jLabelStat.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("SUCCESS"));
                } catch (InterruptedException ex) {
                }
            }
        }.start();
    }//GEN-LAST:event_jButtonProgramActionPerformed

    private void jButtonReadFlashActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonReadFlashActionPerformed
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread() {
            @Override
            public void run() {
                int len = 0x8000;
                short start = 0;
                if (devName.equals("1141")) { //NOI18N
                    len = 0x7000;
                    start = 0xE00;
                } else if (devName.equals("2168")) { //NOI18N
                    len = 0xB000;
                } else if (devName.equals("3168")) { //NOI18N
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
                        for (int nReadBytes = 32, i = 0; i < len; i += nReadBytes) {
                            jProgressBarBL.setValue(i);
                            if (devName.equals("2168")) //NOI18N
                            {
                                if (i == 0xA400) {
                                    bb.putShort(0, (short) 0xB000);
                                    if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf)) {
                                        break;
                                    }
                                }
                            }
                            if (!usbSmb.readBytes(0xF5, nReadBytes, buf)) {
                                break;
                            }
                            f.write(buf);
//                            System.out.println(String.format("%04X : %02X", i, buf[0])); //NOI18N
                        }
                        jProgressBarBL.setValue(len);
                    }
                    f.close();
//                } catch (IOException ex) {
                } catch (Exception ex) {
                    System.err.println(ex);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.start();
    }//GEN-LAST:event_jButtonReadFlashActionPerformed

    private void jButtonWriteFlashActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonWriteFlashActionPerformed
        String path = jTextField3.getText();
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    int len = (int) file.length();
                    if (len > 2048) {
                        return;
                    }
                    if (path.toLowerCase().endsWith(".ifi")) { //NOI18N
                        if (DllEntry.dec128(path, dfBuf) != len) {
                            return;
                        }
                    } else {
                        FileInputStream f = new FileInputStream(path);
                        f.read(dfBuf);
                    }
//        System.out.println(String.format("%02X%02X", dfBuf[0], dfBuf[1])); //NOI18N
                    if (devName.equals("1168")) { //NOI18N
                        len = 1024;
                    }
                    jProgressBarBL.setValue(0);
                    int nReadBytes = 32;
                    byte[] buf = new byte[34];
                    ByteBuffer bb = ByteBuffer.wrap(buf);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    short offset = 0x400, start = (short) stradr;
                    if (!devName.equals("1168")) { //NOI18N
                        bb.putShort(0, (short) (start + offset));
                        if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf)) {
                            return;
                        }
                        if (!usbSmb.readBytes(0xFA, Short.BYTES, buf)) {
                            return;
                        }
                        if (bb.getShort(0) != start + offset) {
                            return;
                        }
//                    for (int i = 0; i < 96; i += nReadBytes) {
                        for (int i = 0; i < 256; i += nReadBytes) {
                            if (!usbSmb.readBytes(0xF5, nReadBytes, buf)) {
                                return;
                            }
                            System.arraycopy(buf, 0, dfBuf, offset + i, nReadBytes);
                        }
                    }
                    jLabelStat.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("ERASE"));
                    jProgressBarBL.setMaximum(100);
                    switch (devName) {
                        case "1141": //NOI18N
//                            short block = 0x6800;
                            short block = (short) (stradr - offset);
                            bb.putShort(0, (short) 0);
                            if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf)) {
                                return;
                            }
                            bb.putShort(0, block);
                            if (!usbSmb.writeBytes(0xFC, Short.BYTES, buf)) {
                                return;
                            }
                            sleep(100);
                            if (!usbSmb.readBytes(0xFA, Short.BYTES, buf)) {
                                return;
                            }
                            if (bb.getShort(0) != block) {
                                jLabelStat.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("FAIL"));
                                return;
                            }
                            break;
                        case "2168": //NOI18N
                            for (int i = 0; i < 3; i++) // sector 0~2
                            {
                                bb.putShort(0, (short) (0x308 + 0x40 * i));
                                if (!usbSmb.writeBytes(0xFD, Short.BYTES, buf)) {
                                    return;
                                }
                                for (int j = 0; j < 11; j++) {
                                    jProgressBarBL.setValue(i * 33 + j * 3 + 3);
                                    sleep(110);
                                }
                            }
                            break;
                        case "3168": //NOI18N
                            for (int i = 0; i < 8; i++) {
                                bb.putShort(0, (short) (0x308 + 0x10 * i));
                                if (!usbSmb.writeBytes(0xFD, Short.BYTES, buf)) {
                                    return;
                                }
                                for (int j = 0; j < 11; j++) {
                                    jProgressBarBL.setValue(i * 33 + j * 3 + 3);
                                    sleep(10);
                                }
                            }
                            break;
                        case "1168": //NOI18N
                            bb.putShort(0, (short) 1);
                            if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf)) {
                                return;
                            }
                            bb.putShort(0, (short) stradr);
                            if (!usbSmb.writeBytes(0xFC, Short.BYTES, buf)) {
                                return;
                            }
                            sleep(100);
                            if (!usbSmb.readBytes(0xFA, Short.BYTES, buf)) {
                                return;
                            }
                            if (bb.getShort(0) != 0) {
                                jLabelStat.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("FAIL"));
                                return;
                            }
                            break;
                        default:
                            return;
                    }
                    jLabelStat.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("WRITING"));
                    jProgressBarBL.setMaximum(len);
                    bb.order(ByteOrder.BIG_ENDIAN);
                    for (int nWriteBytes, nPointer = 0; nPointer < len; nPointer += nWriteBytes) {
                        jProgressBarBL.setValue(nPointer);
                        nWriteBytes = Math.min(len - nPointer, 32);
                        System.arraycopy(dfBuf, nPointer, buf, 2, nWriteBytes);
                        bb.putShort(0, (short) (start + nPointer));
//                        if (devName.equals("1168")) { //NOI18N
//                            if (!usbSmb.writeBytes(0xF6, nWriteBytes + 2, buf)) {
//                                return;
//                            }
//                        } else {
                            if (!usbSmb.writeBytes(0xF4, nWriteBytes + 2, buf)) {
                                return;
                            }
//                        }
                        sleep(15);
                    }
                    jLabelStat.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("VERIFY"));
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    bb.putShort(0, start);
                    if (!usbSmb.writeBytes(0xFA, Short.BYTES, buf)) {
                        return;
                    }
                    if (!usbSmb.readBytes(0xFA, Short.BYTES, buf)) {
                        return;
                    }
                    if (bb.getShort(0) != start) {
                        return;
                    }
                    for (int i = 0; i < len; i += nReadBytes) {
                        nReadBytes = Math.min(len - i, 32);
//                        if (devName.equals("1168")) { //NOI18N
//                            if (!usbSmb.readBytes(0xF7, nReadBytes, buf)) {
//                                return;
//                            }
//                        } else {
                            if (!usbSmb.readBytes(0xF5, nReadBytes, buf)) {
                                return;
                            }
//                        }
                        if (!Arrays.equals(Arrays.copyOfRange(dfBuf, i, i + nReadBytes),
                                Arrays.copyOfRange(buf, 0, nReadBytes))) {
                            jLabelStat.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("VERIFY FAIL"));
                            return;
                        }
                    }
                    jProgressBarBL.setValue(len);
                    jLabelStat.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("SUCCESS"));
                } catch (Exception ex) {
                    System.err.println(ex);
                }
            }
        }.start();
    }//GEN-LAST:event_jButtonWriteFlashActionPerformed

    private void jCheckBoxScanItemStateChanged(ItemEvent evt) {//GEN-FIRST:event_jCheckBoxScanItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            scanning = true;
            timerSBS.setDelay(Integer.parseInt(jTextFieldInterval.getText()));
            timerSBS.start();
        } else {
            timerSBS.stop();
            scanning = false;
        }
    }//GEN-LAST:event_jCheckBoxScanItemStateChanged

    private void jButtonStartLogActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonStartLogActionPerformed
        if (fcLog.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            File file = fcLog.getSelectedFile();
            String path = file.getPath();
            if (!file.getName().contains(".")) { //NOI18N
                path += ".log"; //NOI18N
            }
//            System.out.println("Logging: " + path);
            jTextFieldLogFile.setText(path);
            pwLog = new PrintWriter(new FileWriter(path), true);
            pwLog.print(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("DATE"));
            pwLog.print(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("\\TTIME"));
            for (int i = 0; i < jTableSBS.getRowCount(); i++) {
                if ((boolean) jTableSBS.getValueAt(i, 4)) {
                    pwLog.print("\t"); //NOI18N
                    pwLog.print(jTableSBS.getValueAt(i, 1));
                    pwLog.print(String.format(" (%s)", jTableSBS.getValueAt(i, 0))); //NOI18N
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

    private void jButtonStopLogActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonStopLogActionPerformed
        pwLog.close();
        pwLog = null;
        jButtonStartLog.setEnabled(true);
        jButtonStopLog.setEnabled(false);
        logging = false;
    }//GEN-LAST:event_jButtonStopLogActionPerformed

    private void jTabbedPaneMainStateChanged(ChangeEvent evt) {//GEN-FIRST:event_jTabbedPaneMainStateChanged
//        JTabbedPane jTabbedPane = (JTabbedPane)evt.getSource();
        if (jCheckBoxBootLoader.isSelected()) {
//            jTabbedPaneMain.setSelectedIndex(jTabbedPaneMain.indexOfTab("Command"));
            jTabbedPaneMain.setSelectedIndex(2);
        } else {
            if (scanning) {
                jCheckBoxScan.doClick();
            }
            int index = jTabbedPaneMain.getSelectedIndex();
            if (index == 3) {
                usbSmb.setHDQ(false);
                usbSmb.setPEC(false);
                timerCalib.start();
            } else {
                if (timerCalib != null) {
                    timerCalib.stop();
                }
                switch (index) {
                    case 0:
                        if (bNewSBS) {
                            jCheckBox1.setSelected(usbSmb.isHDQ());
                        } else {
                            jCheckBox1.setSelected(usbSmb.isPEC());
                        }
                        break;
                    case 1:
                        if (bNewSBS) {
                            jCheckBox2.setSelected(usbSmb.isHDQ());
                        } else {
                            jCheckBox2.setSelected(usbSmb.isPEC());
                        }
                        break;
                    case 2:
                        if (bNewSBS) {
                            jCheckBox3.setSelected(usbSmb.isHDQ());
                        } else {
                            jCheckBox3.setSelected(usbSmb.isPEC());
                        }
                        break;
                    case 4:
//                if (index == jTabbedPaneMain.indexOfTab("Chemistry")) {
                        if (usbSmb.writeWord(0, 8)) {
                            short pwValue[] = new short[1];
                            if (usbSmb.readWord(0, pwValue)) {
                                String chemID = String.format("%04X", pwValue[0]); //NOI18N
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
                        break;
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

        String path = "../Chemistry/" + chemID + ".chm"; //NOI18N
        File file = new File(path);
        if (file.exists()) {
            if (file.length() == 256) {
                byte[] buf = new byte[256];
                if (DllEntry.dec128(path, buf) == 256) {
//                    System.out.println(String.format("%02X%02X", buf[208], buf[209]));
                    final int[] dod = {100, 95, 90, 85, 80, 75, 70, 65, 60, 55, 50, 45, 40, 35, 30, 25, 20,
                        19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0};

                    XYSeries s1 = new XYSeries(chemID);

                    // set OCV table value
                    for (int i = 36; i >= 0; i--) {
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
        String id = (String) jTableChem.getValueAt(sel, 0);
        ChemistryChart chart = new ChemistryChart(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("CHEMISTRY ID: {0}"), new Object[]{id}));
//        ChemistryChart chart = new ChemistryChart(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("CHEMISTRY ID: {0}"));
        chart.createChartPanel(createChemDataset(id));
        chart.pack();
        UIUtils.centerFrameOnScreen(chart);
        chart.setVisible(true);
    }

    private void jButtonPlotActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonPlotActionPerformed
        int sel = jTableChem.getSelectedRow();
        if (sel < 0) {
//            JOptionPane.showMessageDialog(this, "Please Select a Chemistry ID");
        } else {
            plotChemChart(sel);
        }
    }//GEN-LAST:event_jButtonPlotActionPerformed

    private void jTableChemMouseClicked(MouseEvent evt) {//GEN-FIRST:event_jTableChemMouseClicked
        jButtonChange.setEnabled(true);
        if (evt.getClickCount() == 2) {
            System.out.println("double clicked");
            plotChemChart(jTableChem.getSelectedRow());
        }
    }//GEN-LAST:event_jTableChemMouseClicked

    private void jButtonChangeActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonChangeActionPerformed
        String chemID = (String) jTableChem.getValueAt(jTableChem.getSelectedRow(), 0);
        String path = "../Chemistry/" + chemID + ".chm"; //NOI18N
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

    private void formWindowClosing(WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if (jCheckBoxBootLoader.isSelected()) {
            JOptionPane.showMessageDialog(this,
                    java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("PLEASE EXIT (UNCHECK) BOOT LOADER !!"),
                    java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("WINDOW CLOSING"),
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            dispose();
        }
    }//GEN-LAST:event_formWindowClosing

    private void formWindowClosed(WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        System.out.println("Window Closed");
        System.exit(0);
    }//GEN-LAST:event_formWindowClosed

    private void jButtonReadByteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonReadByteActionPerformed
        int command = Integer.parseInt(jTextFieldCommand.getText(), 16);
        byte pValue[] = new byte[1];
        if (usbSmb.readByte(command, pValue)) {
            jTextPaneMessage.setText(Integer.toHexString(Byte.toUnsignedInt(pValue[0])).toUpperCase());
        } else {
            jTextPaneMessage.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("READ FAIL"));
        }
    }//GEN-LAST:event_jButtonReadByteActionPerformed

    private void jButtonWriteByteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonWriteByteActionPerformed
        int command = Integer.parseInt(jTextFieldCommand.getText(), 16);
        int value = Integer.parseInt(jTextFieldData.getText(), 16);
        if (usbSmb.writeByte(command, value)) {
            jTextPaneMessage.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("WRITE SUCCESS"));
        } else {
            jTextPaneMessage.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("WRITE FAIL"));
        }
    }//GEN-LAST:event_jButtonWriteByteActionPerformed

    private void jCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCheckBoxItemStateChanged
        if (bNewSBS) {
            usbSmb.setHDQ(evt.getStateChange() == ItemEvent.SELECTED);
        } else {
            usbSmb.setPEC(evt.getStateChange() == ItemEvent.SELECTED);
        }
    }//GEN-LAST:event_jCheckBoxItemStateChanged

    /* Calibration Start Command Flag */
    final int CAL_CC_OFFSET         = 0x0001;// Bit 0 Coulomb Counter Offset 
    final int CAL_VCELL_OFFSET      = 0x0002;// Bit 1 VCell Offset 
    final int CAL_TEMPINT_OFFSET    = 0x0008;// Bit 3 Temperature, Internal 
    final int CAL_TEMPEXT1_OFFSET   = 0x0010;// Bit 4 Temperature, External 1 
    final int CAL_TEMPEXT2_OFFSET   = 0x0020;// Bit 5 Temperature, External 2 
    final int CAL_CURRENT           = 0x0040;// Bit 6 Current 
    final int CAL_ADC_CONT          = 0x4000;// Bit 14 Run ADC Task Continuously 
    final int CAL_CC_CONT           = 0x8000;// Bit 15 Run CC Task Continuously 

    /* Calibration Command */
    final byte CAL_START_CMD    = 0x51;
    final byte CAL_POLL_CMD     = 0x52;
    final byte CAL_VC1_CMD      = 0x66;
    final byte CAL_VC2_CMD      = 0x67;
    final byte CAL_VC3_CMD      = 0x68;
    final byte CAL_VC4_CMD      = 0x69;
    final byte CAL_DF_CMD       = 0x72;
    final byte CAL_EXIT_CMD     = 0x73;

    private void jButtonCalibCC1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCalibCC1ActionPerformed
        new Thread() {
            @Override
            public void run() {
                timerCalib.stop();
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    short[] pwValue = new short[1];
                    for (int i = 0; i < 5; i++) {
                        if (usbSmb.writeWord(0, 0x40)) {
                            if (usbSmb.readWord(0x50, pwValue)) {
                                if (pwValue[0] == 0x4C7A) {//Lz
                                    break;
                                }
                            }
                        }
                    }
                    usbSmb.writeWord(CAL_START_CMD, CAL_CC_OFFSET | CAL_ADC_CONT | CAL_CC_CONT);
                    for (int i = 0; i < 5; i++) {
                        if (usbSmb.readWord(CAL_POLL_CMD, pwValue)) {
                            if ((pwValue[0] & 0xFF) == 0) {
                                if (usbSmb.readWord(0x53, pwValue)) {
                                    if (pwValue[0] == 0) {
                                        break;
                                    }
                                }
                            }
                        }
                        sleep(300);
                    }
                    usbSmb.sendByte(CAL_DF_CMD);
                    for (int i = 0; i < 5; i++) {
                        sleep(500);
                        if (usbSmb.writeWord(0x54, 0)) {
                            if (usbSmb.readWord(0x50, pwValue)) {
                                if (pwValue[0] == 0x4C7A) {//Lz
                                    break;
                                }
                            }
                        }
                    }
                    for (int i = 0; i < 3; i++) {
                        if (usbSmb.sendByte(CAL_EXIT_CMD)) {
                            if (usbSmb.readWord(0x50, pwValue)) {
                                if (pwValue[0] == 0) {
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    System.err.println(ex);
                }
                setCursor(Cursor.getDefaultCursor());
                timerCalib.start();
            }
        }.start();
    }//GEN-LAST:event_jButtonCalibCC1ActionPerformed

    private void jButtonCalibVTActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCalibVTActionPerformed
        new Thread() {
            @Override
            public void run() {
                timerCalib.stop();
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    short[] pwValue = new short[1];
                    for (int i = 0; i < 5; i++) {
                        if (usbSmb.writeWord(0, 0x40)) {
                            if (usbSmb.readWord(0x50, pwValue)) {
                                if (pwValue[0] == 0x4C7A) {//Lz
                                    break;
                                }
                            }
                        }
                    }
                    int wCmdFlags = 0;
                    byte[] buf = new byte[7];
                    if (jCheckBoxTemp.isSelected()) {
                        wCmdFlags |= CAL_TEMPEXT1_OFFSET | CAL_TEMPEXT2_OFFSET | CAL_TEMPINT_OFFSET;
			/* WRITE THE ACTUAL TEMPERATURE */
                        int nCalTemp = (int) (Float.parseFloat(jTextFieldTempA.getText()) * 10 + 0.5);
                        buf[4] = (byte) nCalTemp;
                        buf[5] = (byte) (nCalTemp >> 8);
                    }
                    buf[6] = Byte.parseByte(jTextFieldCellN.getText());
                    usbSmb.writeBlock(0x58, buf.length, buf);
                    if (jCheckBoxVolt.isSelected()) {
                        wCmdFlags |= CAL_VCELL_OFFSET;
                        usbSmb.writeWord(CAL_VC1_CMD, Integer.parseInt(jTextFieldVCell1A.getText()));
                        usbSmb.writeWord(CAL_VC2_CMD, Integer.parseInt(jTextFieldVCell2A.getText()));
                        usbSmb.writeWord(CAL_VC3_CMD, Integer.parseInt(jTextFieldVCell3A.getText()));
                        usbSmb.writeWord(CAL_VC4_CMD, Integer.parseInt(jTextFieldVCell4A.getText()));
                    }
                    usbSmb.writeWord(CAL_START_CMD, wCmdFlags | CAL_ADC_CONT | CAL_CC_CONT);
                    for (int i = 0; i < 3; i++) {
                        sleep(1000);
                        if (usbSmb.readWord(CAL_POLL_CMD, pwValue)) {
                            if ((pwValue[0] & 0xFF) == 0) {
                                if (usbSmb.readWord(0x53, pwValue)) {
                                    if (pwValue[0] == 0) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    usbSmb.sendByte(CAL_DF_CMD);
                    for (int i = 0; i < 3; i++) {
                        sleep(1000);
                        if (usbSmb.writeWord(0x54, 0)) {
                            if (usbSmb.readWord(0x50, pwValue)) {
                                if (pwValue[0] == 0x4C7A) {//Lz
                                    break;
                                }
                            }
                        }
                    }
                    for (int i = 0; i < 3; i++) {
                        if (usbSmb.sendByte(CAL_EXIT_CMD)) {
                            if (usbSmb.readWord(0x50, pwValue)) {
                                if (pwValue[0] == 0) {
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    System.err.println(ex);
                }
                setCursor(Cursor.getDefaultCursor());
                timerCalib.start();
            }
        }.start();
    }//GEN-LAST:event_jButtonCalibVTActionPerformed

    private void jButtonCalibCC2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCalibCC2ActionPerformed
        new Thread() {
            @Override
            public void run() {
                timerCalib.stop();
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    short[] pwValue = new short[1];
                    for (int i = 0; i < 5; i++) {
                        if (usbSmb.writeWord(0, 0x40)) {
                            if (usbSmb.readWord(0x50, pwValue)) {
                                if (pwValue[0] == 0x4C7A) {//Lz
                                    break;
                                }
                            }
                        }
                    }
                    byte[] buf = new byte[7];
                    int nCurr = Integer.parseInt(jTextFieldCurrA.getText());
                    buf[0] = (byte) nCurr;
                    buf[1] = (byte) (nCurr >> 8);
                    usbSmb.writeBlock(0x58, buf.length, buf);
                    usbSmb.writeWord(CAL_START_CMD, CAL_CURRENT | CAL_ADC_CONT | CAL_CC_CONT);
                    for (int i = 0; i < 5; i++) {
                        if (usbSmb.readWord(CAL_POLL_CMD, pwValue)) {
                            if ((pwValue[0] & 0xFF) == 0) {
                                if (usbSmb.readWord(0x53, pwValue)) {
                                    if (pwValue[0] == 0) {
                                        break;
                                    }
                                }
                            }
                        }
                        sleep(300);
                    }
                    usbSmb.sendByte(CAL_DF_CMD);
                    for (int i = 0; i < 5; i++) {
                        sleep(500);
                        if (usbSmb.writeWord(0x54, 0)) {
                            if (usbSmb.readWord(0x50, pwValue)) {
                                if (pwValue[0] == 0x4C7A) {//Lz
                                    break;
                                }
                            }
                        }
                    }
                    for (int i = 0; i < 3; i++) {
                        if (usbSmb.sendByte(CAL_EXIT_CMD)) {
                            if (usbSmb.readWord(0x50, pwValue)) {
                                if (pwValue[0] == 0) {
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    System.err.println(ex);
                }
                setCursor(Cursor.getDefaultCursor());
                timerCalib.start();
            }
        }.start();
    }//GEN-LAST:event_jButtonCalibCC2ActionPerformed

    private void jButtonResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonResetActionPerformed
        if (usbSmb.writeWord(0, 0x41)) {
            jTextPaneMessage.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("WRITE SUCCESS"));
            JOptionPane.showMessageDialog(this, "IC Reset", "Command", JOptionPane.INFORMATION_MESSAGE);
        } else {
            jTextPaneMessage.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("WRITE FAIL"));
        }
    }//GEN-LAST:event_jButtonResetActionPerformed

    private void jButtonGasGaugeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGasGaugeActionPerformed
        if (usbSmb.writeWord(0, 0x21)) {
            jTextPaneMessage.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("WRITE SUCCESS"));
            JOptionPane.showMessageDialog(this, "Enable Gas Gauge", "Command", JOptionPane.INFORMATION_MESSAGE);
        } else {
            jTextPaneMessage.setText(java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("WRITE FAIL"));
        }
    }//GEN-LAST:event_jButtonGasGaugeActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        short[] pwEncryptIndex = new short[1];
        if (usbSmb.readWord(0xF1, pwEncryptIndex)) {
            if (pwEncryptIndex[0] < -1 || pwEncryptIndex[0] >= jComboBox1.getItemCount())
                pwEncryptIndex[0] = 0;
            jTextField4.setText(Short.toString(pwEncryptIndex[0]));
            jComboBox1.setSelectedIndex(pwEncryptIndex[0]);
        } else {
            JOptionPane.showMessageDialog(this, java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("READ FAIL"), "Encrypt", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        int nEncrypSel = jComboBox1.getSelectedIndex();
        if (!usbSmb.writeWord(0xF1, nEncrypSel)) {
            JOptionPane.showMessageDialog(this, java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("WRITE FAIL"), "Encrypt", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        int nEncrypSel = 0;
        jComboBox1.setSelectedIndex(nEncrypSel);
        if (!usbSmb.writeWord(0xF1, nEncrypSel)) {
            JOptionPane.showMessageDialog(this, java.util.ResourceBundle.getBundle("com/pfc/tool/Bundle").getString("WRITE FAIL"), "Encrypt", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_jButton6ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /*
        logger = Logger.getLogger(NewJFrame.class.getName());
        try {
            logger.addHandler(new FileHandler("error.xml"));
        } catch (IOException|SecurityException ex) {
            System.err.println(ex);
        }
         */
        try {
            FileInputStream in = new FileInputStream("../ini/Preferences.xml"); //NOI18N
            Preferences.importPreferences(in);
        } catch (IOException | InvalidPreferencesFormatException ex) {
            System.err.println(ex);
        }
        prefs = Preferences.userNodeForPackage(NewJFrame.class);
        if (prefs.getBoolean("debugLog", false)) { //NOI18N
            try {
                PrintStream out = new PrintStream(new FileOutputStream("debug.log")); //NOI18N
                System.setOut(out);
                System.setErr(out);
            } catch (IOException ex) {
                System.err.println(ex);
            }
        }

        usbSmb = new UsbSmb();
        System.out.println("USB SMBus Version: " + Integer.toHexString(usbSmb.getVersion()));

        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//                if ("Nimbus".equals(info.getName())) {
//                if ("Windows Classic".equals(info.getName())) {
                if ("Windows".equals(info.getName())) { //NOI18N
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
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButtonCalibCC1;
    private javax.swing.JButton jButtonCalibCC2;
    private javax.swing.JButton jButtonCalibVT;
    private javax.swing.JButton jButtonChange;
    private javax.swing.JButton jButtonDefault;
    private javax.swing.JButton jButtonExport;
    private javax.swing.JButton jButtonGasGauge;
    private javax.swing.JButton jButtonImport;
    private javax.swing.JButton jButtonPlot;
    private javax.swing.JButton jButtonProgram;
    private javax.swing.JButton jButtonReadAll;
    private javax.swing.JButton jButtonReadBlock;
    private javax.swing.JButton jButtonReadByte;
    private javax.swing.JButton jButtonReadFlash;
    private javax.swing.JButton jButtonReadMReg;
    private javax.swing.JButton jButtonReadWord;
    private javax.swing.JButton jButtonRefresh;
    private javax.swing.JButton jButtonReset;
    private javax.swing.JButton jButtonStartLog;
    private javax.swing.JButton jButtonStopLog;
    private javax.swing.JButton jButtonWriteAll;
    private javax.swing.JButton jButtonWriteBlock;
    private javax.swing.JButton jButtonWriteByte;
    private javax.swing.JButton jButtonWriteFlash;
    private javax.swing.JButton jButtonWriteMReg;
    private javax.swing.JButton jButtonWriteWord;
    private javax.swing.JButton jButtonWriteWord2;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JCheckBox jCheckBox3;
    private javax.swing.JCheckBox jCheckBoxBootLoader;
    private javax.swing.JCheckBox jCheckBoxPCM;
    private javax.swing.JCheckBox jCheckBoxScan;
    private javax.swing.JCheckBox jCheckBoxTemp;
    private javax.swing.JCheckBox jCheckBoxVolt;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
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
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JPanel jPanelCalib;
    private javax.swing.JPanel jPanelChem;
    private javax.swing.JPanel jPanelCommand;
    private javax.swing.JPanel jPanelDataFlash;
    private javax.swing.JPanel jPanelSBS;
    private javax.swing.JProgressBar jProgressBarBL;
    private javax.swing.JProgressBar jProgressBarDF;
    private javax.swing.JProgressBar jProgressBarSOC;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPaneBits;
    private javax.swing.JScrollPane jScrollPaneChem;
    private javax.swing.JScrollPane jScrollPaneMain;
    private javax.swing.JScrollPane jScrollPaneSBS;
    private javax.swing.JTabbedPane jTabbedPaneDataFlash;
    private javax.swing.JTabbedPane jTabbedPaneMain;
    private javax.swing.JTable jTableBits;
    private javax.swing.JTable jTableChem;
    private javax.swing.JTable jTableSBS;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextFieldBlock;
    private javax.swing.JTextField jTextFieldByte;
    private javax.swing.JTextField jTextFieldCellN;
    private javax.swing.JTextField jTextFieldChemID;
    private javax.swing.JTextField jTextFieldCommand;
    private javax.swing.JTextField jTextFieldCount;
    private javax.swing.JTextField jTextFieldCurrA;
    private javax.swing.JTextField jTextFieldCurrM;
    private javax.swing.JTextField jTextFieldData;
    private javax.swing.JTextField jTextFieldData2;
    private javax.swing.JTextField jTextFieldInterval;
    private javax.swing.JTextField jTextFieldLogFile;
    private javax.swing.JTextField jTextFieldMReg;
    private javax.swing.JTextField jTextFieldTempA;
    private javax.swing.JTextField jTextFieldTempM;
    private javax.swing.JTextField jTextFieldVCell1A;
    private javax.swing.JTextField jTextFieldVCell1M;
    private javax.swing.JTextField jTextFieldVCell2A;
    private javax.swing.JTextField jTextFieldVCell2M;
    private javax.swing.JTextField jTextFieldVCell3A;
    private javax.swing.JTextField jTextFieldVCell3M;
    private javax.swing.JTextField jTextFieldVCell4A;
    private javax.swing.JTextField jTextFieldVCell4M;
    private javax.swing.JTextPane jTextPaneMessage;
    // End of variables declaration//GEN-END:variables
}
