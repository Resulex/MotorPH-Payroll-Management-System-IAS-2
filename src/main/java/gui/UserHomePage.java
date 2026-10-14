/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package gui;
import javax.swing.*;
import java.util.*;
import keeptoo.KGradientPanel;
import models.AccountAccess;
import models.User;
import models.Employee;
import models.HRPersonnel;
import models.SystemAdministrator;
import dao.AccountAccessDao;
import dao.UserDao;
import utils.SessionManager;
import java.awt.Window;

/**
 *
 * @author Alex Resurreccion
 */

public class UserHomePage extends javax.swing.JFrame {
    public boolean selectedFrame = true;
    private com.k33ptoo.components.KButton activeButton = null;
    private AccountAccess account;
    private User user;
    /**
     * Creates new form UserHomePage
     */
    public UserHomePage() {
        initAll();
    }
    
     public UserHomePage(AccountAccess account) {
        this.account = account;
        initAll();
    }
     
    private void initAll(){
        UserDao userDao = new UserDao();
        user = userDao.getUserInfo(account.getEmployeeNumber(), account.getRole(), account.getIsAuthenticated());
        initComponents();
        welcomeLabel.setText("Welcome, " + user.getFirstName());
        adminProfile.setText(account.getRole());
        initializeMenus();

          if (account != null && account.getIsAuthenticated()) {
            SessionManager.start(this::handleSessionTimeout);
        }
    }

    private void initializeMenus() {
        //Set default content
        javax.swing.JPanel homepage;
        if(!(user instanceof Employee)){
            homepage = new Dashboard(account, user);
        } else {
            homepage = new HomePagePanel(account, ((Employee) user));
        }
        replacePanelContent(homepage);
        
        createSidebar();
    }

    private KGradientPanel createSidebar() {
        KGradientPanel sidebar = new KGradientPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        // Add menu items for the selected user type
        List<String> menuItems = this.account.getPermissions();
        int baseYCoordinate = 220;
        int buttonYMargin = 60;
        
        boolean isEmployee = user instanceof Employee;

        for (String item : menuItems) {
            com.k33ptoo.components.KButton sideBarButton = new com.k33ptoo.components.KButton();
            sideBarButton.setText(item);
            sideBarButton.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
            sideBarButton.setHideActionText(true);
            sideBarButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
            sideBarButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
            sideBarButton.setIconTextGap(30);
            sideBarButton.setBorderPainted(false);
            sideBarButton.setkBackGroundColor(new java.awt.Color(255, 153, 0));
            sideBarButton.setkBorderRadius(30);
            sideBarButton.setkEndColor(new java.awt.Color(0, 0, 205));
            sideBarButton.setkHoverEndColor(new java.awt.Color(255, 153, 0));
            sideBarButton.setkHoverForeGround(new java.awt.Color(255, 255, 255));
            sideBarButton.setkHoverStartColor(new java.awt.Color(255, 153, 0));
            sideBarButton.setkIndicatorThickness(10);
            sideBarButton.setkPressedColor(new java.awt.Color(255, 102, 0));
            sideBarButton.setkSelectedColor(new java.awt.Color(255, 102, 0));
            sideBarButton.setkStartColor(new java.awt.Color(153, 51, 255));
            sideBarButton.setRequestFocusEnabled(false);
            sideBarButton.setRolloverEnabled(false);
            sideBarButton.setVerticalAlignment(javax.swing.SwingConstants.TOP);

            // Set default active button based on user type
            if (isEmployee && item.equals("Home")) {
                sideBarButton.setkStartColor(new java.awt.Color(255, 102, 0));  // Active color for Home (if Employee)
                sideBarButton.setkEndColor(new java.awt.Color(255, 102, 0));    // Active color for Home (if Employee)
                activeButton = sideBarButton;
            } else if (!isEmployee && item.equals("Dashboard")) {
                sideBarButton.setkStartColor(new java.awt.Color(255, 102, 0));  // Active color for Dashboard (if not Employee)
                sideBarButton.setkEndColor(new java.awt.Color(255, 102, 0));    // Active color for Dashboard (if not Employee)
                activeButton = sideBarButton;
            }

            sideBarButton.addActionListener(e -> {
                // Reset previous active button
                if (activeButton != null) {
                    activeButton.setkStartColor(new java.awt.Color(153, 51, 255)); // Restore purple
                    activeButton.setkEndColor(new java.awt.Color(0, 0, 205));     // Restore blue
                }

                // Set new active button color
                sideBarButton.setkStartColor(new java.awt.Color(255, 102, 0));
                sideBarButton.setkEndColor(new java.awt.Color(255, 102, 0));

                // Set the active button
                activeButton = sideBarButton;
                onMenuItemClick(item);
            });
            kGradientPanel1.add(sideBarButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, baseYCoordinate, -1, 45));
            baseYCoordinate += buttonYMargin;
        }

        return sidebar;
    }
    
    private void onMenuItemClick(String menuItem) {
        // Logic to display content based on the clicked menu item
        javax.swing.JPanel newPanel;
        switch (menuItem) {
            case "Home":
                newPanel = new HomePagePanel(account, user);
                break;
            case "Dashboard":
                newPanel = new Dashboard(account, user);
                break;
            case "Employee Management":
                newPanel = new UserManagement(account, user);
                break;
            case "Attendance":
                newPanel = new Attendance(account, user);
                break;
            case "Payoll & Report":
                newPanel = new Payroll(account, ((HRPersonnel) user));
                break;
            case "Payslip":
                newPanel = new EmployeePayslip(account, user);
                break;
            case "Leave Request":
                newPanel = new LeaveRequests(account, ((HRPersonnel) user));
                break;
            case "My Details":
                newPanel = new EmpMyDetails(account, ((Employee) user));
                break;
            case "Payroll Generation":
                newPanel = new Payroll(account, ((HRPersonnel) user));
                break;
            case "Summary Report":
                newPanel = new SummaryReport(account, ((HRPersonnel) user));
                break;
            case "My Payslip":
                newPanel = new EmployeePayslip(account, user);
                break;
            case "User Management":
                newPanel = new UserManagement(account, user);
                break;
            default:
                return;
        }
        replacePanelContent(newPanel);
    }
    
    private void replacePanelContent(javax.swing.JPanel newPanel) {

        // Add the new panel (newPanel) to the same location (AbsoluteConstraints)
//        kGradientPanel1.add(jPanel, new AbsoluteConstraints(250, 70, 860, 590));

        // Revalidate and repaint to ensure the changes are displayed
        kGradientPanel1.revalidate();
        kGradientPanel1.repaint();
        pagePanel.removeAll();
        pagePanel.add(newPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 0, 860, 590));
        pagePanel.revalidate();
        pagePanel.repaint();
    }


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        kGradientPanel1 = new keeptoo.KGradientPanel();
        topPanel = new javax.swing.JPanel();
        jSeparator6 = new javax.swing.JSeparator();
        adminProfile = new javax.swing.JLabel();
        logout = new javax.swing.JButton();
        motorPhLogo = new javax.swing.JLabel();
        welcomeLabel = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        jSeparator4 = new javax.swing.JSeparator();
        pagePanel = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        kGradientPanel1.setkEndColor(new java.awt.Color(0, 0, 204));
        kGradientPanel1.setkStartColor(new java.awt.Color(153, 0, 204));
        kGradientPanel1.setPreferredSize(new java.awt.Dimension(1110, 655));
        kGradientPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        topPanel.setBackground(new java.awt.Color(255, 255, 255));
        topPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jSeparator6.setForeground(new java.awt.Color(204, 204, 204));
        topPanel.add(jSeparator6, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 60, 860, 20));

        adminProfile.setBackground(new java.awt.Color(204, 204, 204));
        adminProfile.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        adminProfile.setForeground(new java.awt.Color(0, 0, 204));
        adminProfile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/UserIcon.png"))); // NOI18N
        adminProfile.setText("Admin User");
        topPanel.add(adminProfile, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 0, 240, 60));

        logout.setBackground(new java.awt.Color(255, 255, 255));
        logout.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        logout.setForeground(new java.awt.Color(0, 0, 204));
        logout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons8-logout-48.png"))); // NOI18N
        logout.setText("Log out");
        logout.setBorder(null);
        logout.setBorderPainted(false);
        logout.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        logout.setDebugGraphicsOptions(javax.swing.DebugGraphics.NONE_OPTION);
        logout.setFocusPainted(false);
        logout.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        logout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logoutActionPerformed(evt);
            }
        });
        topPanel.add(logout, new org.netbeans.lib.awtextra.AbsoluteConstraints(730, 10, 110, 40));

        kGradientPanel1.add(topPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 0, 860, 70));

        motorPhLogo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/aesthetics-removebg-preview.png"))); // NOI18N
        kGradientPanel1.add(motorPhLogo, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 220, 140));

        welcomeLabel.setBackground(new java.awt.Color(255, 255, 255));
        welcomeLabel.setFont(new java.awt.Font("Arial Black", 1, 18)); // NOI18N
        welcomeLabel.setForeground(new java.awt.Color(255, 255, 255));
        welcomeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        welcomeLabel.setText(" Welcome, Admin!");
        welcomeLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        kGradientPanel1.add(welcomeLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 140, 250, 50));

        jSeparator2.setForeground(new java.awt.Color(255, 255, 255));
        kGradientPanel1.add(jSeparator2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 140, 250, 20));

        jSeparator4.setForeground(new java.awt.Color(255, 255, 255));
        kGradientPanel1.add(jSeparator4, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 190, 250, 10));

        pagePanel.setBackground(new java.awt.Color(255, 255, 255));
        kGradientPanel1.add(pagePanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 60, 860, 600));

        getContentPane().add(kGradientPanel1, java.awt.BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void logoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logoutActionPerformed
    SessionManager.stop();

    dispose();
    LoginPage frame = new LoginPage();
    frame.setVisible(true);
    }//GEN-LAST:event_logoutActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main() {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(UserHomePage.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(UserHomePage.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(UserHomePage.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(UserHomePage.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new UserHomePage().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel adminProfile;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator6;
    private keeptoo.KGradientPanel kGradientPanel1;
    private javax.swing.JButton logout;
    private javax.swing.JLabel motorPhLogo;
    private javax.swing.JPanel pagePanel;
    private javax.swing.JPanel topPanel;
    private javax.swing.JLabel welcomeLabel;
    // End of variables declaration//GEN-END:variables

    private void handleSessionTimeout() {
    SessionManager.stop();

    if (account != null) {
        account.logoutUser(user);
        account.setPermissions(java.util.Collections.emptyList());
        account.setEmployeeNumber(0);
    }

    dispose();

    LoginPage loginPage = new LoginPage();
    loginPage.setVisible(true);

   
    javax.swing.JOptionPane.showMessageDialog(
        loginPage,
        "Your session has expired after 15 minutes of inactivity. Please log in again.",
        "Session Expired",
        javax.swing.JOptionPane.WARNING_MESSAGE
    );
    }
}