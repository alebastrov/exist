package org.exist.client.security;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import org.exist.client.HighlightedTableCellRenderer;
import org.exist.security.AXSchemaType;
import org.exist.security.Account;
import org.exist.security.AccountComparator;
import org.exist.security.EXistSchemaType;
import org.exist.security.Group;
import org.exist.xmldb.UserManagementService;
import org.xmldb.api.base.XMLDBException;

/**
 *
 * @author Adam Retter <adam.retter@googlemail.com>
 */
public class UserManagerDialog extends javax.swing.JFrame {

    private final UserManagementService userManagementService;
    
    private DefaultTableModel usersTableModel = null;
    private DefaultTableModel groupsTableModel = null;
    
    /**
     * Creates new form UserManagerDialog
     */
    public UserManagerDialog(final UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
        initComponents();
        tblUsers.setDefaultRenderer(Object.class, new HighlightedTableCellRenderer());
        tblGroups.setDefaultRenderer(Object.class, new HighlightedTableCellRenderer());
    }
    
    private TableModel getUsersTableModel() {
        if(usersTableModel == null) {
            
            try {
                final Account accounts[] = userManagementService.getAccounts();
                
                Arrays.sort(accounts, new AccountComparator());

                final String tableData[][] = new String[accounts.length][3];
                for(int i = 0; i < accounts.length; i++) {
                    tableData[i][0] = accounts[i].getName();
                    tableData[i][1] = accounts[i].getMetadataValue(AXSchemaType.FULLNAME);
                    tableData[i][1] = accounts[i].getMetadataValue(EXistSchemaType.DESCRIPTION);
                }

                usersTableModel = new ReadOnlyDefaultTableModel(
                    tableData,
                    new String [] {
                        "User", "Full Name", "Description"
                    }
                );
            } catch(final XMLDBException xmldbe) {
                JOptionPane.showMessageDialog(this, "Could not get users list: " + xmldbe.getMessage(), "User Manager Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        return usersTableModel;
    }
    
    private TableModel getGroupsTableModel() {
        if(groupsTableModel == null) {
            
            try {
                final String groupNames[] = userManagementService.getGroups();

                Arrays.sort(groupNames);
                
                final String tableData[][] = new String[groupNames.length][2];
                for(int i = 0; i < groupNames.length; i++) {
                    tableData[i][0] = groupNames[i];
                    tableData[i][1] = userManagementService.getGroup(groupNames[i]).getMetadataValue(EXistSchemaType.DESCRIPTION);
                }

                groupsTableModel = new ReadOnlyDefaultTableModel(
                    tableData,
                    new String [] {
                        "Group", "Description"
                    }
                );
            } catch(final XMLDBException xmldbe) {
                JOptionPane.showMessageDialog(this, "Could not get groups list: " + xmldbe.getMessage(), "User Manager Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        return groupsTableModel;
    }
    
    public void refreshUsersTableModel() {
        final int rowCount = usersTableModel.getRowCount();
        for(int i = 0; i < rowCount; i++) {
            usersTableModel.removeRow(0);
        }
        
        try {
            final Account accounts[] = userManagementService.getAccounts();

            Arrays.sort(accounts, new AccountComparator());

            for(int i = 0; i < accounts.length; i++) {
                usersTableModel.addRow(new String[]{
                    accounts[i].getName(),
                    accounts[i].getMetadataValue(AXSchemaType.FULLNAME),
                    accounts[i].getMetadataValue(EXistSchemaType.DESCRIPTION)
                });
            }
        } catch(final XMLDBException xmldbe) {
            JOptionPane.showMessageDialog(this, "Could not get users list: " + xmldbe.getMessage(), "User Manager Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void refreshGroupsTableModel() {
        final int rowCount = groupsTableModel.getRowCount();
        for(int i = 0; i < rowCount; i++) {
            groupsTableModel.removeRow(0);
        }
        
        try {
            final String groupNames[] = userManagementService.getGroups();

            Arrays.sort(groupNames);

            for(int i = 0; i < groupNames.length; i++) {
                groupsTableModel.addRow(new String[]{
                    groupNames[i],
                    userManagementService.getGroup(groupNames[i]).getMetadataValue(EXistSchemaType.DESCRIPTION)
                });
            }
        } catch(final XMLDBException xmldbe) {
            JOptionPane.showMessageDialog(this, "Could not get groups list: " + xmldbe.getMessage(), "User Manager Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showUserDialog() {
        final UserDialog userDialog = new UserDialog(userManagementService);
        
        userDialog.addWindowListener(new WindowAdapter(){           
            @Override
            public void windowClosed(final WindowEvent e) {
                refreshUsersTableModel();
            }
        });
        
        userDialog.setVisible(true);
    }
    
    private void showGroupDialog() {
        final GroupDialog groupDialog = new GroupDialog(userManagementService);
        
        groupDialog.addWindowListener(new WindowAdapter(){           
            @Override
            public void windowClosed(final WindowEvent e) {
                refreshGroupsTableModel();
            }
        });
        
        groupDialog.setVisible(true);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pmUsers = new javax.swing.JPopupMenu();
        miNewUser = new javax.swing.JMenuItem();
        miEditUser = new javax.swing.JMenuItem();
        miRemoveUser = new javax.swing.JMenuItem();
        pmGroups = new javax.swing.JPopupMenu();
        miNewGroup = new javax.swing.JMenuItem();
        miEditGroup = new javax.swing.JMenuItem();
        miRemoveGroup = new javax.swing.JMenuItem();
        tpUserManager = new javax.swing.JTabbedPane();
        spUsers = new javax.swing.JScrollPane();
        tblUsers = new javax.swing.JTable();
        spGroups = new javax.swing.JScrollPane();
        tblGroups = new javax.swing.JTable();
        jSeparator1 = new javax.swing.JSeparator();
        btnCreate = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();

        miNewUser.setText("New User...");
        miNewUser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miNewUserActionPerformed(evt);
            }
        });
        pmUsers.add(miNewUser);
        miNewUser.getAccessibleContext().setAccessibleName("New User");

        miEditUser.setText("Edit User...");
        miEditUser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miEditUserActionPerformed(evt);
            }
        });
        pmUsers.add(miEditUser);
        miEditUser.getAccessibleContext().setAccessibleName("Edit User");

        miRemoveUser.setText("Remove User");
        miRemoveUser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miRemoveUserActionPerformed(evt);
            }
        });
        pmUsers.add(miRemoveUser);

        miNewGroup.setText("New Group...");
        pmGroups.add(miNewGroup);
        miNewGroup.getAccessibleContext().setAccessibleName("New Group");

        miEditGroup.setText("Edit Group...");
        miEditGroup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miEditGroupActionPerformed(evt);
            }
        });
        pmGroups.add(miEditGroup);
        miEditGroup.getAccessibleContext().setAccessibleName("Edit Group");

        miRemoveGroup.setText("Remove Group");
        miRemoveGroup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miRemoveGroupActionPerformed(evt);
            }
        });
        pmGroups.add(miRemoveGroup);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("User Manager");

        tblUsers.setModel(getUsersTableModel());
        tblUsers.setAutoCreateRowSorter(true);
        tblUsers.setComponentPopupMenu(pmUsers);
        tblUsers.setShowGrid(true);
        tblUsers.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblUsersMouseClicked(evt);
            }
        });
        spUsers.setViewportView(tblUsers);

        tpUserManager.addTab("Users", spUsers);

        tblGroups.setModel(getGroupsTableModel());
        tblGroups.setAutoCreateRowSorter(true);
        tblGroups.setComponentPopupMenu(pmGroups);
        tblGroups.setShowGrid(true);
        tblGroups.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblGroupsMouseClicked(evt);
            }
        });
        spGroups.setViewportView(tblGroups);

        tpUserManager.addTab("Groups", spGroups);
        spGroups.getAccessibleContext().setAccessibleName("Groups");

        btnCreate.setText("Create");
        btnCreate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreateActionPerformed(evt);
            }
        });

        btnClose.setText("Close");
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(250, Short.MAX_VALUE)
                .addComponent(btnClose)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCreate)
                .addGap(20, 20, 20))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSeparator1)
                .addContainerGap())
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(tpUserManager, javax.swing.GroupLayout.DEFAULT_SIZE, 439, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 291, Short.MAX_VALUE)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnCreate)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(btnClose)
                        .addContainerGap())))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(tpUserManager, javax.swing.GroupLayout.PREFERRED_SIZE, 291, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 51, Short.MAX_VALUE)))
        );

        tpUserManager.getAccessibleContext().setAccessibleName("Users");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void miNewUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miNewUserActionPerformed
        showUserDialog();
    }//GEN-LAST:event_miNewUserActionPerformed

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
        setVisible(false);
        dispose();
    }//GEN-LAST:event_btnCloseActionPerformed

    private String getSelectedUsername() {
        return (String)tblUsers.getValueAt(tblUsers.getSelectedRow(), 0);
    }
    
    private String getSelectedGroup() {
        return (String)tblGroups.getValueAt(tblGroups.getSelectedRow(), 0);
    }
    
    private void miRemoveUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miRemoveUserActionPerformed

        final String selectedUsername = getSelectedUsername();
        try {
            final Account account = userManagementService.getAccount(selectedUsername);
            userManagementService.removeAccount(account);
        
            usersTableModel.removeRow(tblUsers.getSelectedRow());
        } catch(final XMLDBException xmldbe) {
            JOptionPane.showMessageDialog(this, "Could not remove user '" + selectedUsername + "': " + xmldbe.getMessage(), "User Manager Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_miRemoveUserActionPerformed

    private void miEditUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miEditUserActionPerformed
        
        final String selectedUsername = getSelectedUsername();
        try {
            final Account account = userManagementService.getAccount(selectedUsername);
            showEditUserDialog(account);
        } catch(final XMLDBException xmldbe) {
            JOptionPane.showMessageDialog(this, "Could not edit user '" + selectedUsername + "': " + xmldbe.getMessage(), "User Manager Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_miEditUserActionPerformed

    private void showEditUserDialog(final Account account) {
        final EditUserDialog userDialog = new EditUserDialog(userManagementService, account);
        
        userDialog.addWindowListener(new WindowAdapter(){           
            @Override
            public void windowClosed(final WindowEvent e) {
                refreshUsersTableModel();
            }
        });
        
        userDialog.setVisible(true);
    }
    
    private void showEditGroupDialog(final Group group) {
        final EditGroupDialog groupDialog = new EditGroupDialog(userManagementService, group);
        
        groupDialog.addWindowListener(new WindowAdapter(){           
            @Override
            public void windowClosed(final WindowEvent e) {
                refreshGroupsTableModel();
            }
        });
        
        groupDialog.setVisible(true);
    }
    
    private void btnCreateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreateActionPerformed
        switch(tpUserManager.getSelectedIndex()) {
            case 0:
                showUserDialog();
                break;
            
            case 1:
                showGroupDialog();
                break;
            
            default:
                return;
        }
    }//GEN-LAST:event_btnCreateActionPerformed

    private void tblUsersMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblUsersMouseClicked
        final boolean userSelected = tblUsers.getSelectedRow() > -1;
        final String selectedUsername = getSelectedUsername();
        
        boolean canModify = userSelected && !(selectedUsername.equals("SYSTEM") || selectedUsername.equals("admin") || selectedUsername.equals("guest"));
        miEditUser.setEnabled(canModify);
        miRemoveUser.setEnabled(canModify);
        
        if(evt.getClickCount() == 2) {
            
            try {
                final Account account = userManagementService.getAccount(selectedUsername);
                showEditUserDialog(account);
            } catch(final XMLDBException xmldbe) {
                JOptionPane.showMessageDialog(this, "Could not edit user '" + selectedUsername + "': " + xmldbe.getMessage(), "User Manager Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_tblUsersMouseClicked

    private void miEditGroupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miEditGroupActionPerformed
        final String selectedGroup = getSelectedGroup();
            try {
                final Group group = userManagementService.getGroup(selectedGroup);
                showEditGroupDialog(group);
            } catch(final XMLDBException xmldbe) {
                JOptionPane.showMessageDialog(this, "Could not edit group '" + selectedGroup + "': " + xmldbe.getMessage(), "User Manager Error", JOptionPane.ERROR_MESSAGE);
            }
    }//GEN-LAST:event_miEditGroupActionPerformed
    
    private void miRemoveGroupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miRemoveGroupActionPerformed
        final String selectedGroup = getSelectedGroup();
        
        try {
            final Group group = userManagementService.getGroup(selectedGroup);
            userManagementService.removeGroup(group);
        
            usersTableModel.removeRow(tblGroups.getSelectedRow());
        } catch(final XMLDBException xmldbe) {
            JOptionPane.showMessageDialog(this, "Could not remove group '" + selectedGroup + "': " + xmldbe.getMessage(), "User Manager Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_miRemoveGroupActionPerformed

    private void tblGroupsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblGroupsMouseClicked
        final boolean groupSelected = tblGroups.getSelectedRow() > -1;
        final String selectedGroup = getSelectedGroup();
        
        boolean canModify = groupSelected && !(selectedGroup.equals("dba") || selectedGroup.equals("guest"));
        
        miEditGroup.setEnabled(canModify);
        miRemoveGroup.setEnabled(canModify);
        
         if(evt.getClickCount() == 2) {
            try {
                final Group group = userManagementService.getGroup(selectedGroup);
                showEditGroupDialog(group);
            } catch(final XMLDBException xmldbe) {
                JOptionPane.showMessageDialog(this, "Could not edit group '" + selectedGroup + "': " + xmldbe.getMessage(), "User Manager Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_tblGroupsMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnCreate;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JMenuItem miEditGroup;
    private javax.swing.JMenuItem miEditUser;
    private javax.swing.JMenuItem miNewGroup;
    private javax.swing.JMenuItem miNewUser;
    private javax.swing.JMenuItem miRemoveGroup;
    private javax.swing.JMenuItem miRemoveUser;
    private javax.swing.JPopupMenu pmGroups;
    private javax.swing.JPopupMenu pmUsers;
    private javax.swing.JScrollPane spGroups;
    private javax.swing.JScrollPane spUsers;
    private javax.swing.JTable tblGroups;
    private javax.swing.JTable tblUsers;
    private javax.swing.JTabbedPane tpUserManager;
    // End of variables declaration//GEN-END:variables
}