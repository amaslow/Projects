/*
 * DesktopApplication1View.java
 */

package desktopapplication1;

import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import org.jdesktop.application.Task;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.persistence.RollbackException;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jdesktop.beansbinding.AbstractBindingListener;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.PropertyStateEvent;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.view.*;


/**
 * The application's main frame.
 */
public class DesktopApplication1View extends FrameView {
    
    public DesktopApplication1View(SingleFrameApplication app) {
        super(app);

        initComponents();

        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
	messageTimer = new Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
	messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        }); 
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String)(evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer)(evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });

        // tracking table selection
        masterTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    firePropertyChange("recordSelected", !isRecordSelected(), isRecordSelected());
                }
            });
        detailTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    firePropertyChange("detailRecordSelected", !isDetailRecordSelected(), isDetailRecordSelected());
                }
            }); 

        // tracking changes to save
        bindingGroup.addBindingListener(new AbstractBindingListener() {
            @Override
            public void targetChanged(Binding binding, PropertyStateEvent event) {
                // save action observes saveNeeded property
                setSaveNeeded(true);
            }
        });

        // have a transaction started
        entityManager.getTransaction().begin();
    }


    public boolean isSaveNeeded() {
        return saveNeeded;
    }

    private void setSaveNeeded(boolean saveNeeded) {
        if (saveNeeded != this.saveNeeded) {
            this.saveNeeded = saveNeeded;
            firePropertyChange("saveNeeded", !saveNeeded, saveNeeded);
        }
    }

    public boolean isRecordSelected() {
        return masterTable.getSelectedRow() != -1;
    }
    
    public boolean isDetailRecordSelected() {
        return detailTable.getSelectedRow() != -1;
    }

    @Action
    public void newRecord() {
        desktopapplication1.Projects p = new desktopapplication1.Projects();
        entityManager.persist(p);
        list.add(p);
        int row = list.size()-1;
        masterTable.setRowSelectionInterval(row, row);
        masterTable.scrollRectToVisible(masterTable.getCellRect(row, 0, true));
        setSaveNeeded(true);
    }

    @Action(enabledProperty = "recordSelected")
    public void deleteRecord() {
        int[] selected = masterTable.getSelectedRows();
        List<desktopapplication1.Projects> toRemove = new ArrayList<desktopapplication1.Projects>(selected.length);
        for (int idx=0; idx<selected.length; idx++) {
            desktopapplication1.Projects p = list.get(masterTable.convertRowIndexToModel(selected[idx]));
            toRemove.add(p);
            entityManager.remove(p);
        }
        list.removeAll(toRemove);
        setSaveNeeded(true);
    }
    
    @Action(enabledProperty = "recordSelected")
    public void newDetailRecord() {
        int index = masterTable.getSelectedRow();
        desktopapplication1.Projects p = list.get(masterTable.convertRowIndexToModel(index));
        Collection<desktopapplication1.Remarks> rs = p.getRemarksList();
        if (rs == null) {
            rs = new LinkedList<desktopapplication1.Remarks>();
            p.setRemarksList((List)rs);
        }
        desktopapplication1.Remarks r = new desktopapplication1.Remarks();
        entityManager.persist(r);
        r.setProject(p);
        rs.add(r);
        masterTable.clearSelection();
        masterTable.setRowSelectionInterval(index, index);
        int row = rs.size()-1;
        detailTable.setRowSelectionInterval(row, row);
        detailTable.scrollRectToVisible(detailTable.getCellRect(row, 0, true));
        setSaveNeeded(true);
    }

    @Action(enabledProperty = "detailRecordSelected")
    public void deleteDetailRecord() {
        int index = masterTable.getSelectedRow();
        desktopapplication1.Projects p = list.get(masterTable.convertRowIndexToModel(index));
        Collection<desktopapplication1.Remarks> rs = p.getRemarksList();
        int[] selected = detailTable.getSelectedRows();
        List<desktopapplication1.Remarks> toRemove = new ArrayList<desktopapplication1.Remarks>(selected.length);
        for (int idx=0; idx<selected.length; idx++) {
            selected[idx] = detailTable.convertRowIndexToModel(selected[idx]);
            int count = 0;
            Iterator<desktopapplication1.Remarks> iter = rs.iterator();
            while (count++ < selected[idx]) iter.next();
            desktopapplication1.Remarks r = iter.next();
            toRemove.add(r);
            entityManager.remove(r);
        }
        rs.removeAll(toRemove);
        masterTable.clearSelection();
        masterTable.setRowSelectionInterval(index, index);
        setSaveNeeded(true);
    }

    @Action(enabledProperty = "saveNeeded")
    public Task save() {
        return new SaveTask(getApplication());
    }

    private class SaveTask extends Task {
        SaveTask(org.jdesktop.application.Application app) {
            super(app);
        }
        @Override protected Void doInBackground() {
            try {
                entityManager.getTransaction().commit();
                entityManager.getTransaction().begin();
            } catch (RollbackException rex) {
                rex.printStackTrace();
                entityManager.getTransaction().begin();
                List<desktopapplication1.Projects> merged = new ArrayList<desktopapplication1.Projects>(list.size());
                for (desktopapplication1.Projects p : list) {
                    merged.add(entityManager.merge(p));
                }
                list.clear();
                list.addAll(merged);
            }
            return null;
        }
        @Override protected void finished() {
            setSaveNeeded(false);
        }
    }

    /**
     * An example action method showing how to create asynchronous tasks
     * (running on background) and how to show their progress. Note the
     * artificial 'Thread.sleep' calls making the task long enough to see the
     * progress visualization - remove the sleeps for real application.
     */
    @Action
    public Task refresh() {
       return new RefreshTask(getApplication());
    }

    private class RefreshTask extends Task {
        RefreshTask(org.jdesktop.application.Application app) {
            super(app);
        }
        @SuppressWarnings("unchecked")
        @Override protected Void doInBackground() {
            try {
                setProgress(0, 0, 4);
                setMessage("Rolling back the current changes...");
                setProgress(1, 0, 4);
                entityManager.getTransaction().rollback();
                Thread.sleep(1000L); // remove for real app
                setProgress(2, 0, 4);

                setMessage("Starting a new transaction...");
                entityManager.getTransaction().begin();
                Thread.sleep(500L); // remove for real app
                setProgress(3, 0, 4);

                setMessage("Fetching new data...");
                java.util.Collection data = query.getResultList();
                for (Object entity : data) {
                    entityManager.refresh(entity);
                }
                Thread.sleep(1300L); // remove for real app
                setProgress(4, 0, 4);

                Thread.sleep(150L); // remove for real app
                list.clear();
                list.addAll(data);
            } catch(InterruptedException ignore) { }
            return null;
        }
        @Override protected void finished() {
            setMessage("Done.");
            setSaveNeeded(false);
        }
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = DesktopApplication1.getApplication().getMainFrame();
            aboutBox = new DesktopApplication1AboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        DesktopApplication1.getApplication().show(aboutBox);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        mainPanel = new javax.swing.JPanel();
        masterScrollPane = new javax.swing.JScrollPane();
        masterTable = new javax.swing.JTable();
        newButton = new javax.swing.JButton();
        deleteButton = new javax.swing.JButton();
        detailScrollPane = new javax.swing.JScrollPane();
        detailTable = new javax.swing.JTable();
        saveButton = new javax.swing.JButton();
        refreshButton = new javax.swing.JButton();
        deleteDetailButton = new javax.swing.JButton();
        newDetailButton = new javax.swing.JButton();
        DateRemarkChooser = new com.toedter.calendar.JDateChooser();
        jScrollPane1 = new javax.swing.JScrollPane();
        RemarkTextArea = new javax.swing.JTextArea();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JSeparator();
        jSeparator5 = new javax.swing.JSeparator();
        jLabel2 = new javax.swing.JLabel();
        OwnerLabel = new javax.swing.JLabel();
        ProjectLabel = new javax.swing.JLabel();
        OwnerComboBox = new javax.swing.JComboBox();
        jDateChooser1 = new com.toedter.calendar.JDateChooser();
        DateStartLabel = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        TypeComboBox = new javax.swing.JComboBox();
        jDateChooser2 = new com.toedter.calendar.JDateChooser();
        jLabel4 = new javax.swing.JLabel();
        StatusComboBox = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();
        BuyerComboBox = new javax.swing.JComboBox();
        jLabel8 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        ProjectTextField = new javax.swing.JFormattedTextField();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem newRecordMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem deleteRecordMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        javax.swing.JMenuItem saveMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem refreshMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        overviewMenu = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(desktopapplication1.DesktopApplication1.class).getContext().getResourceMap(DesktopApplication1View.class);
        entityManager = java.beans.Beans.isDesignTime() ? null : javax.persistence.Persistence.createEntityManagerFactory(resourceMap.getString("entityManager.persistenceUnit")).createEntityManager(); // NOI18N
        query = java.beans.Beans.isDesignTime() ? null : entityManager.createQuery(resourceMap.getString("query.query")); // NOI18N
        list = java.beans.Beans.isDesignTime() ? java.util.Collections.emptyList() : org.jdesktop.observablecollections.ObservableCollections.observableList(query.getResultList());
        rowSorterToStringConverter1 = new desktopapplication1.RowSorterToStringConverter();

        mainPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 2));
        mainPanel.setMinimumSize(new java.awt.Dimension(700, 600));
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setPreferredSize(new java.awt.Dimension(700, 600));
        mainPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        masterScrollPane.setName("masterScrollPane"); // NOI18N

        masterTable.setName("masterTable"); // NOI18N

        org.jdesktop.swingbinding.JTableBinding jTableBinding = org.jdesktop.swingbinding.SwingBindings.createJTableBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, list, masterTable);
        org.jdesktop.swingbinding.JTableBinding.ColumnBinding columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${project}"));
        columnBinding.setColumnName("Project");
        columnBinding.setColumnClass(String.class);
        columnBinding.setEditable(false);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${buyer}"));
        columnBinding.setColumnName("Buyer");
        columnBinding.setColumnClass(String.class);
        columnBinding.setEditable(false);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${owner}"));
        columnBinding.setColumnName("Owner");
        columnBinding.setColumnClass(String.class);
        columnBinding.setEditable(false);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${type}"));
        columnBinding.setColumnName("Type");
        columnBinding.setColumnClass(String.class);
        columnBinding.setEditable(false);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${dateStart}"));
        columnBinding.setColumnName("Date Start");
        columnBinding.setColumnClass(java.util.Date.class);
        columnBinding.setEditable(false);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${dateFinish}"));
        columnBinding.setColumnName("Date Finish");
        columnBinding.setColumnClass(java.util.Date.class);
        columnBinding.setEditable(false);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${status}"));
        columnBinding.setColumnName("Status");
        columnBinding.setColumnClass(String.class);
        columnBinding.setEditable(false);
        bindingGroup.addBinding(jTableBinding);
        jTableBinding.bind();
        masterScrollPane.setViewportView(masterTable);
        masterTable.getColumnModel().getColumn(0).setHeaderValue(resourceMap.getString("masterTable.columnModel.title0")); // NOI18N
        masterTable.getColumnModel().getColumn(1).setHeaderValue(resourceMap.getString("masterTable.columnModel.title6")); // NOI18N
        masterTable.getColumnModel().getColumn(2).setHeaderValue(resourceMap.getString("masterTable.columnModel.title1")); // NOI18N
        masterTable.getColumnModel().getColumn(3).setHeaderValue(resourceMap.getString("masterTable.columnModel.title3")); // NOI18N
        masterTable.getColumnModel().getColumn(4).setHeaderValue(resourceMap.getString("masterTable.columnModel.title2")); // NOI18N
        masterTable.getColumnModel().getColumn(5).setHeaderValue(resourceMap.getString("masterTable.columnModel.title4")); // NOI18N
        masterTable.getColumnModel().getColumn(6).setHeaderValue(resourceMap.getString("masterTable.columnModel.title5")); // NOI18N

        mainPanel.add(masterScrollPane, new org.netbeans.lib.awtextra.AbsoluteConstraints(17, 195, 670, 120));

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(desktopapplication1.DesktopApplication1.class).getContext().getActionMap(DesktopApplication1View.class, this);
        newButton.setAction(actionMap.get("newRecord")); // NOI18N
        newButton.setText(resourceMap.getString("newButton.text")); // NOI18N
        newButton.setName("newButton"); // NOI18N
        mainPanel.add(newButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 330, -1, -1));

        deleteButton.setAction(actionMap.get("deleteRecord")); // NOI18N
        deleteButton.setText(resourceMap.getString("deleteButton.text")); // NOI18N
        deleteButton.setName("deleteButton"); // NOI18N
        mainPanel.add(deleteButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(590, 330, -1, -1));

        detailScrollPane.setName("detailScrollPane"); // NOI18N

        detailTable.setName("detailTable"); // NOI18N

        org.jdesktop.beansbinding.ELProperty eLProperty = org.jdesktop.beansbinding.ELProperty.create("${selectedElement.remarksList}");
        jTableBinding = org.jdesktop.swingbinding.SwingBindings.createJTableBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, masterTable, eLProperty, detailTable);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${dateRemark}"));
        columnBinding.setColumnName("Date Remark");
        columnBinding.setColumnClass(java.util.Date.class);
        columnBinding.setEditable(false);
        jTableBinding.setSourceUnreadableValue(java.util.Collections.emptyList());
        bindingGroup.addBinding(jTableBinding);
        jTableBinding.bind();
        detailScrollPane.setViewportView(detailTable);
        detailTable.getColumnModel().getColumn(0).setHeaderValue(resourceMap.getString("detailTable.columnModel.title0")); // NOI18N

        mainPanel.add(detailScrollPane, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 410, 80, 130));

        saveButton.setAction(actionMap.get("save")); // NOI18N
        saveButton.setName("saveButton"); // NOI18N
        mainPanel.add(saveButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(620, 550, 71, -1));

        refreshButton.setAction(actionMap.get("refresh")); // NOI18N
        refreshButton.setName("refreshButton"); // NOI18N
        mainPanel.add(refreshButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(540, 550, -1, -1));

        deleteDetailButton.setAction(actionMap.get("deleteDetailRecord")); // NOI18N
        deleteDetailButton.setText(resourceMap.getString("deleteDetailButton.text")); // NOI18N
        deleteDetailButton.setName("deleteDetailButton"); // NOI18N
        mainPanel.add(deleteDetailButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 550, 110, -1));

        newDetailButton.setAction(actionMap.get("newDetailRecord")); // NOI18N
        newDetailButton.setText(resourceMap.getString("newDetailButton.text")); // NOI18N
        newDetailButton.setName("newDetailButton"); // NOI18N
        mainPanel.add(newDetailButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 550, -1, -1));

        DateRemarkChooser.setName("DateRemarkChooser"); // NOI18N

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, detailTable, org.jdesktop.beansbinding.ELProperty.create("${selectedElement.dateRemark}"), DateRemarkChooser, org.jdesktop.beansbinding.BeanProperty.create("date"));
        bindingGroup.addBinding(binding);

        binding.setSourceUnreadableValue(null);
        mainPanel.add(DateRemarkChooser, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 410, -1, -1));

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        RemarkTextArea.setColumns(20);
        RemarkTextArea.setRows(5);
        RemarkTextArea.setName("RemarkTextArea"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, detailTable, org.jdesktop.beansbinding.ELProperty.create("${selectedElement.remark}"), RemarkTextArea, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        RemarkTextArea.addMouseListener(new ContextMenuMouseListener());
        binding.setSourceUnreadableValue(null);
        jScrollPane1.setViewportView(RemarkTextArea);

        mainPanel.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 450, 590, 90));

        jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
        jLabel6.setName("jLabel6"); // NOI18N
        mainPanel.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 410, -1, 20));

        jLabel7.setText(resourceMap.getString("jLabel7.text")); // NOI18N
        jLabel7.setName("jLabel7"); // NOI18N
        mainPanel.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 435, -1, -1));

        jLabel1.setFont(resourceMap.getFont("jLabel1.font")); // NOI18N
        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        mainPanel.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 380, -1, -1));

        jSeparator4.setForeground(resourceMap.getColor("jSeparator4.foreground")); // NOI18N
        jSeparator4.setName("jSeparator4"); // NOI18N
        mainPanel.add(jSeparator4, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 370, 700, 10));

        jSeparator5.setForeground(resourceMap.getColor("jSeparator5.foreground")); // NOI18N
        jSeparator5.setName("jSeparator5"); // NOI18N
        mainPanel.add(jSeparator5, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 375, 700, 10));

        jLabel2.setFont(resourceMap.getFont("jLabel2.font")); // NOI18N
        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N
        mainPanel.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 0, -1, -1));

        OwnerLabel.setText(resourceMap.getString("OwnerLabel.text")); // NOI18N
        OwnerLabel.setName("OwnerLabel"); // NOI18N
        mainPanel.add(OwnerLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(370, 110, -1, 20));

        ProjectLabel.setText(resourceMap.getString("ProjectLabel.text")); // NOI18N
        ProjectLabel.setName("ProjectLabel"); // NOI18N
        mainPanel.add(ProjectLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 110, 70, 20));

        OwnerComboBox.setMaximumRowCount(12);
        OwnerComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { " ", "Ad", "Anna", "Artur", "Dirk", "Janny", "Jasmine", "Kelly", "Linda", "Marco", "QC Team", "Ron", "Roos" }));
        OwnerComboBox.setName("OwnerComboBox"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, masterTable, org.jdesktop.beansbinding.ObjectProperty.create(), OwnerComboBox, org.jdesktop.beansbinding.BeanProperty.create("elements"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, masterTable, org.jdesktop.beansbinding.ELProperty.create("${selectedElement.owner}"), OwnerComboBox, org.jdesktop.beansbinding.BeanProperty.create("selectedItem"));
        bindingGroup.addBinding(binding);

        binding.setSourceUnreadableValue(null);
        mainPanel.add(OwnerComboBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 110, 70, -1));

        jDateChooser1.setName("jDateChooser1"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, masterTable, org.jdesktop.beansbinding.ELProperty.create("${selectedElement.dateStart}"), jDateChooser1, org.jdesktop.beansbinding.BeanProperty.create("date"));
        bindingGroup.addBinding(binding);

        binding.setSourceUnreadableValue(null);
        mainPanel.add(jDateChooser1, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 150, 110, -1));

        DateStartLabel.setText(resourceMap.getString("DateStartLabel.text")); // NOI18N
        DateStartLabel.setName("DateStartLabel"); // NOI18N
        mainPanel.add(DateStartLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 150, -1, 20));

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N
        mainPanel.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(520, 110, 30, 20));

        TypeComboBox.setMaximumRowCount(11);
        TypeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { " ", "Approval", "Authority Complaints", "Customer Complaints", "Inspections", "General", "New Item", "QC Product Modification", "Reminder", "Sample Check", "Technical Check", "Technical File", "Updating Certificates" }));
        TypeComboBox.setName("TypeComboBox"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, masterTable, org.jdesktop.beansbinding.ObjectProperty.create(), TypeComboBox, org.jdesktop.beansbinding.BeanProperty.create("elements"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, masterTable, org.jdesktop.beansbinding.ELProperty.create("${selectedElement.type}"), TypeComboBox, org.jdesktop.beansbinding.BeanProperty.create("selectedItem"));
        bindingGroup.addBinding(binding);

        binding.setSourceUnreadableValue(null);
        mainPanel.add(TypeComboBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(550, 110, 140, -1));

        jDateChooser2.setName("jDateChooser2"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, masterTable, org.jdesktop.beansbinding.ELProperty.create("${selectedElement.dateFinish}"), jDateChooser2, org.jdesktop.beansbinding.BeanProperty.create("date"));
        bindingGroup.addBinding(binding);

        binding.setSourceUnreadableValue(null);
        mainPanel.add(jDateChooser2, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 150, 110, -1));

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N
        mainPanel.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 150, -1, 20));

        StatusComboBox.setMaximumRowCount(10);
        StatusComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { " ", "Placing / Receiving", "Starting Up", "Rejected", "In Process (1 line)", "In Process (2 line)", "Standstill", "Complete", "Finalizing", "End of Project" }));
        StatusComboBox.setName("StatusComboBox"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, masterTable, org.jdesktop.beansbinding.ObjectProperty.create(), StatusComboBox, org.jdesktop.beansbinding.BeanProperty.create("elements"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, masterTable, org.jdesktop.beansbinding.ELProperty.create("${selectedElement.status}"), StatusComboBox, org.jdesktop.beansbinding.BeanProperty.create("selectedItem"));
        bindingGroup.addBinding(binding);

        binding.setSourceUnreadableValue(null);
        mainPanel.add(StatusComboBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(570, 150, 120, -1));

        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N
        mainPanel.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 150, -1, 20));

        BuyerComboBox.setMaximumRowCount(17);
        BuyerComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { " ", "Ad", "Arian", "Artur", "Christy", "Dennis", "Dirk", "Fanny", "Jasmine", "Kit", "Marco", "Marijn", "Niels", "Sigrid", "Sven", "Rob", "Ron" }));
        BuyerComboBox.setName("BuyerComboBox"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, masterTable, org.jdesktop.beansbinding.ObjectProperty.create(), BuyerComboBox, org.jdesktop.beansbinding.BeanProperty.create("elements"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, masterTable, org.jdesktop.beansbinding.ELProperty.create("${selectedElement.buyer}"), BuyerComboBox, org.jdesktop.beansbinding.BeanProperty.create("selectedItem"));
        bindingGroup.addBinding(binding);

        binding.setSourceUnreadableValue(null);
        mainPanel.add(BuyerComboBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 110, -1, -1));

        jLabel8.setText(resourceMap.getString("jLabel8.text")); // NOI18N
        jLabel8.setName("jLabel8"); // NOI18N
        mainPanel.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 110, -1, 20));

        jTextField1.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jTextField1.setName("jTextField1"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, masterTable, org.jdesktop.beansbinding.ELProperty.create("${rowSorter}"), jTextField1, org.jdesktop.beansbinding.BeanProperty.create("text"));
        binding.setConverter(rowSorterToStringConverter1);
        bindingGroup.addBinding(binding);

        mainPanel.add(jTextField1, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 50, 140, -1));

        jLabel9.setText(resourceMap.getString("jLabel9.text")); // NOI18N
        jLabel9.setName("jLabel9"); // NOI18N
        mainPanel.add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 50, -1, 20));

        try {
            ProjectTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.MaskFormatter("********************")));
        } catch (java.text.ParseException ex) {
            ex.printStackTrace();
        }
        ProjectTextField.setName("ProjectTextField"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, masterTable, org.jdesktop.beansbinding.ELProperty.create("${selectedElement.project}"), ProjectTextField, org.jdesktop.beansbinding.BeanProperty.create("value"));
        bindingGroup.addBinding(binding);

        binding.setSourceUnreadableValue(null);
        mainPanel.add(ProjectTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 110, 110, -1));

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        newRecordMenuItem.setAction(actionMap.get("newRecord")); // NOI18N
        newRecordMenuItem.setName("newRecordMenuItem"); // NOI18N
        fileMenu.add(newRecordMenuItem);

        deleteRecordMenuItem.setAction(actionMap.get("deleteRecord")); // NOI18N
        deleteRecordMenuItem.setName("deleteRecordMenuItem"); // NOI18N
        fileMenu.add(deleteRecordMenuItem);

        jSeparator1.setName("jSeparator1"); // NOI18N
        fileMenu.add(jSeparator1);

        saveMenuItem.setAction(actionMap.get("save")); // NOI18N
        saveMenuItem.setName("saveMenuItem"); // NOI18N
        fileMenu.add(saveMenuItem);

        refreshMenuItem.setAction(actionMap.get("refresh")); // NOI18N
        refreshMenuItem.setName("refreshMenuItem"); // NOI18N
        fileMenu.add(refreshMenuItem);

        jSeparator2.setName("jSeparator2"); // NOI18N
        fileMenu.add(jSeparator2);

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        overviewMenu.setText(resourceMap.getString("overviewMenu.text")); // NOI18N
        overviewMenu.setName("overviewMenu"); // NOI18N

        jMenuItem2.setAction(actionMap.get("PrintProject")); // NOI18N
        jMenuItem2.setText(resourceMap.getString("jMenuItem2.text")); // NOI18N
        jMenuItem2.setName("jMenuItem2"); // NOI18N
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        overviewMenu.add(jMenuItem2);

        jMenuItem1.setAction(actionMap.get("PrintAll")); // NOI18N
        jMenuItem1.setText(resourceMap.getString("jMenuItem1.text")); // NOI18N
        jMenuItem1.setName("jMenuItem1"); // NOI18N
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        overviewMenu.add(jMenuItem1);

        jMenuItem3.setText(resourceMap.getString("jMenuItem3.text")); // NOI18N
        jMenuItem3.setName("jMenuItem3"); // NOI18N
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        overviewMenu.add(jMenuItem3);

        menuBar.add(overviewMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 700, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 530, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        rowSorterToStringConverter1.setTable(masterTable);

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);

        bindingGroup.bind();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed

               String owner=OwnerComboBox.getSelectedItem().toString();
               String project="%";
               String from = "%";
        
        
        try 
        {
           Connection conn = DriverManager.getConnection("jdbc:mysql://192.168.35.210:3306/ELRO", "root", "IP1212IP");

            FileInputStream fis = new FileInputStream("R:\\Database\\Project\\Project overview.jrxml");
                                                        
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fis);

            Map map = new HashMap();
            map.put("Owner", owner);
            map.put("Project", project);
            map.put("From", from);
                        
            
            JasperReport jasperReport = (JasperReport) JasperCompileManager.compileReport(bufferedInputStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, map, conn);
            
            JasperViewer.viewReport(jasperPrint,false);
          
            
        } catch (Exception X) {}
       
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed

        try 
        {
//            String owner=OwnerComboBox.getSelectedItem().toString();
            String owner="%";
            String project=ProjectTextField.getText();
            project=project.replaceAll("\\s+$","");
//            String from = BuyerComboBox.getSelectedItem().toString();
            String from = "%";
            
                      
            Connection conn = DriverManager.getConnection("jdbc:mysql://192.168.35.210:3306/ELRO", "root", "IP1212IP");

            FileInputStream fis = new FileInputStream("R:\\Database\\Project\\Project overview.jrxml");
                                                        
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fis);

            Map map = new HashMap();
            map.put("Owner", owner);
            map.put("Project", project);
            map.put("From", from);
                        
        
            
            JasperReport jasperReport = (JasperReport) JasperCompileManager.compileReport(bufferedInputStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, map, conn);
            
            JasperViewer.viewReport(jasperPrint,false);
          
            
        } catch (Exception X) {}
     
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
       
                       String owner=OwnerComboBox.getSelectedItem().toString();
                       String project="%";
                       String from = BuyerComboBox.getSelectedItem().toString();
        
        try 
        {
           Connection conn = DriverManager.getConnection("jdbc:mysql://192.168.35.210:3306/ELRO", "root", "IP1212IP");

            FileInputStream fis = new FileInputStream("R:\\Database\\Project\\Project overview.jrxml");
                                                        
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fis);

            Map map = new HashMap();
            map.put("Owner", owner);
            map.put("Project", project);
            map.put("From", from);
                        
            
            JasperReport jasperReport = (JasperReport) JasperCompileManager.compileReport(bufferedInputStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, map, conn);
            
            JasperViewer.viewReport(jasperPrint,false);
          
            
        } catch (Exception X) {}
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox BuyerComboBox;
    private com.toedter.calendar.JDateChooser DateRemarkChooser;
    private javax.swing.JLabel DateStartLabel;
    private javax.swing.JComboBox OwnerComboBox;
    private javax.swing.JLabel OwnerLabel;
    private javax.swing.JLabel ProjectLabel;
    private javax.swing.JFormattedTextField ProjectTextField;
    private javax.swing.JTextArea RemarkTextArea;
    private javax.swing.JComboBox StatusComboBox;
    private javax.swing.JComboBox TypeComboBox;
    private javax.swing.JButton deleteButton;
    private javax.swing.JButton deleteDetailButton;
    private javax.swing.JScrollPane detailScrollPane;
    private javax.swing.JTable detailTable;
    private javax.persistence.EntityManager entityManager;
    private com.toedter.calendar.JDateChooser jDateChooser1;
    private com.toedter.calendar.JDateChooser jDateChooser2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JTextField jTextField1;
    private java.util.List<desktopapplication1.Projects> list;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JScrollPane masterScrollPane;
    private javax.swing.JTable masterTable;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JButton newButton;
    private javax.swing.JButton newDetailButton;
    private javax.swing.JMenu overviewMenu;
    private javax.swing.JProgressBar progressBar;
    private javax.persistence.Query query;
    private javax.swing.JButton refreshButton;
    private desktopapplication1.RowSorterToStringConverter rowSorterToStringConverter1;
    private javax.swing.JButton saveButton;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;

    private boolean saveNeeded;
}
