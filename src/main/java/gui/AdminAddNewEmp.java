/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package gui;

import models.AccountAccess;
import models.Address;
import models.Employee;
import models.Benefits;
import models.HRPersonnel;
import models.User;
import models.Helpers;
import models.SystemAdministrator;
import dao.EmployeeDao;
import dao.HRPersonnelDao;
import dao.SystemAdministratorDao;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.util.*;
import javax.swing.JScrollBar;
import dao.UserDao;
import java.awt.Color;

/**
 *
 * @author Alex Resurreccion
 */
public class AdminAddNewEmp extends javax.swing.JFrame {
    private AccountAccess account;
    private User user;
    private User editableUser;
    private String action = "View";
    private UserManagement parentPanel;
    private List<Helpers.FieldWithLabel> fields = new ArrayList<>();
    private List<Helpers.FieldWithLabel> empOnlyFields = new ArrayList<>();
    private List<Helpers.FieldWithLabel> otherFields = new ArrayList<>();

    /**
     * Creates new form AdminAddNewEmp
     */
    public AdminAddNewEmp() {
        initComponents();
        defaultComponentView();
    }
    
    public AdminAddNewEmp(AccountAccess account, User user, UserManagement parentPanel) {
        this.account = account;
        this.user = user;
        this.action = "Create";
        this.parentPanel = parentPanel;
        initComponents();
        defaultComponentView();
        initializeForm();
        if(user instanceof SystemAdministrator){
            newEmpRoleValue.setEnabled(true);
            addNewEmpLabel.setText("Add New User");
            empInfo.setText("USER INFORMATION");
        }
    }
    
    public AdminAddNewEmp(AccountAccess account, User user, 
            User editableUser, String action, UserManagement parentPanel) {
        this.account = account;
        this.user = user;
        this.editableUser = editableUser;
        this.action = action;
        this.parentPanel = parentPanel;
        initComponents();
        
        loadUserDetails();
        initializeForm();
        defaultComponentView();
        if(action.equals("View")){
            Helpers.disableFields(otherFields);
            Helpers.disableFields(fields);
            Helpers.disableFields(empOnlyFields);
        }
        if(user instanceof SystemAdministrator){
            newEmpRoleValue.setEnabled(true);
        }
    }
    
    private void initializeForm(){
        List<String> validation1 = Arrays.asList("Required", "AlphabetsAndDash");
        List<String> validation2 = Arrays.asList("Required", "AlphabetsAndSpecialChars");
        List<String> validation3 = Arrays.asList("Required", "NumbersAndDash");
        List<String> validation4 = Arrays.asList("Required", "NumbersOnly");
        List<String> validation5 = Arrays.asList("Required", "WorkingAge");
        List<String> validation6 = Arrays.asList("Required");
        fields.add(new Helpers.FieldWithLabel(new Helpers.InputField(newEmpLastNameValue), addNewEmpWarnLN, validation2));
        fields.add(new Helpers.FieldWithLabel(new Helpers.InputField(newEmpFirstNameValue), addNewEmpWarnFN, validation2));
        fields.add(new Helpers.FieldWithLabel(new Helpers.InputField(newEmpBirthValue), addNewEmpWarnBirth, validation5));
        fields.add(new Helpers.FieldWithLabel(new Helpers.InputField(newEmpPhoneNumValue), addNewEmpWarnPhone, validation3));
        empOnlyFields.add(new Helpers.FieldWithLabel(new Helpers.InputField(newEmpImmediateSupValue1), addNewEmpWarnISup, validation4));
        empOnlyFields.add(new Helpers.FieldWithLabel(new Helpers.InputField(newEmpBasicSalaryValue), addNewEmpWarnBSal, validation4));
        empOnlyFields.add(new Helpers.FieldWithLabel(new Helpers.InputField(newEmpGrossSemiValue), addNewEmpWarnGSMR, validation4));
        empOnlyFields.add(new Helpers.FieldWithLabel(new Helpers.InputField(newEmpHourlyRateValue), addNewEmpWarnHRate, validation4));
        empOnlyFields.add(new Helpers.FieldWithLabel(new Helpers.InputField(newEmpRiceSubValue), addNewEmpWarnRSub, validation4));
        empOnlyFields.add(new Helpers.FieldWithLabel(new Helpers.InputField(newEmpPhoneAllowValue), addNewEmpWarnPAllow, validation4));
        empOnlyFields.add(new Helpers.FieldWithLabel(new Helpers.InputField(newEmpClothingAllowValue), addNewEmpWarnCAllow, validation4));
        empOnlyFields.add(new Helpers.FieldWithLabel(new Helpers.InputField(newEmpSssValue), addNewEmpWarnSss, validation3));
        empOnlyFields.add(new Helpers.FieldWithLabel(new Helpers.InputField(newEmpTinValue), addNewEmpWarnTin, validation3));
        empOnlyFields.add(new Helpers.FieldWithLabel(new Helpers.InputField(newEmpPhilhealthValue), addNewEmpWarnPhil, validation3));
        empOnlyFields.add(new Helpers.FieldWithLabel(new Helpers.InputField(newEmpPagIbigValue), addNewEmpWarnPag, validation3));
        
        // non verification fields but need to disable for view only
        otherFields.add(new Helpers.FieldWithLabel(new Helpers.InputField(newEmpPersonalEmailValue)));
        otherFields.add(new Helpers.FieldWithLabel(new Helpers.InputField(addressStreetValue)));
        otherFields.add(new Helpers.FieldWithLabel(new Helpers.InputField(addressBarangayValue)));
        otherFields.add(new Helpers.FieldWithLabel(new Helpers.InputField(addressCityValue)));
        otherFields.add(new Helpers.FieldWithLabel(new Helpers.InputField(addressProvinceValue)));
        otherFields.add(new Helpers.FieldWithLabel(new Helpers.InputField(addressZipcodeValue)));
        otherFields.add(new Helpers.FieldWithLabel(new Helpers.InputField(newEmpDateHiredValue)));
        otherFields.add(new Helpers.FieldWithLabel(new Helpers.InputField(newEmpStatusValue)));
        otherFields.add(new Helpers.FieldWithLabel(new Helpers.InputField(newEmpPositionValue)));
        otherFields.add(new Helpers.FieldWithLabel(new Helpers.InputField(newEmpRoleValue)));

        addNewEmpSuccessCreatedLabel.setVisible(false);
        addNewEmpErrorFill.setVisible(false);
            
        if(action.equals("Update")){
            addNewEmpSuccessCreatedLabel.setText("Successfully updated information. This window will automatically close in 3 seconds.");
        } else if(action.equals("View")){
            newEmpSubmitButton.setVisible(false);
            newEmpCancelButton.setText("Go Back");
        }
        addNewEmpWarnBirth.setVisible(false);
        
        // initialize newEmpImmediateSupValue
        
    }
    
    public void loadUserDetails(){
        UserDao userDao = new UserDao();
        newEmpIDValue.setText(String.valueOf(editableUser.getEmployeeID()));
        String role = userDao.getRoleName(editableUser.getEmployeeID());
        addNewEmpLabel.setText(action + " "+ role);
        empInfo.setText(role + " INFORMATION");
        newEmpRoleValue.setSelectedItem(role);
        newEmpLastNameValue.setText(editableUser.getLastName());
        newEmpFirstNameValue.setText(editableUser.getFirstName());
        newEmpBirthValue.setDate(editableUser.getBirthday());
        newEmpDateHiredValue.setDate(editableUser.getDateHired());
        newEmpPositionValue.setSelectedItem(editableUser.getPosition());
        newEmpPhoneNumValue.setText(editableUser.getPhoneNumber());
        newEmpPersonalEmailValue.setText("@example.motorph.com.ph");
        addressStreetValue.setText(editableUser.getAddress().getStreet());
        addressBarangayValue.setText(editableUser.getAddress().getBarangay());
        addressCityValue.setText(editableUser.getAddress().getCity());
        addressProvinceValue.setText(editableUser.getAddress().getProvince());
        addressZipcodeValue.setText(editableUser.getAddress().getZipcode());
        newEmpSssValue.setText(editableUser.getSssNumber());
        newEmpTinValue.setText(editableUser.getTinNumber());
        newEmpPhilhealthValue.setText(editableUser.getPhilhealthNumber());
        newEmpPagIbigValue.setText(editableUser.getPagibigNumber());
        newEmpStatusValue.setSelectedItem(editableUser.getStatus());
        newEmpImmediateSupValue1.setText(String.valueOf(editableUser.getImmediateSupervisor()));
        newEmpBasicSalaryValue.setText(String.valueOf(editableUser.getBenefitsBasicSalary()));
        newEmpGrossSemiValue.setText(String.valueOf(editableUser.getBenefitsGrossSemiMonthlyRate()));
        newEmpHourlyRateValue.setText(String.valueOf(editableUser.getBenefitsHourlyRate()));
        newEmpRiceSubValue.setText(String.valueOf(editableUser.getBenefitsRiceSubsidy()));
        newEmpPhoneAllowValue.setText(String.valueOf(editableUser.getBenefitsPhoneAllowance()));
        newEmpClothingAllowValue.setText(String.valueOf(editableUser.getBenefitsClothingAllowance()));
    }
    
    public void defaultComponentView(){
        addNewEmpWarnLN.setVisible(false);
        addNewEmpWarnFN.setVisible(false);
        addNewEmpWarnBirth.setVisible(false);
        addNewEmpWarnISup.setVisible(false);
        addNewEmpWarnPhone.setVisible(false);
        addNewEmpWarnBSal.setVisible(false);
        addNewEmpWarnGSMR.setVisible(false);
        addNewEmpWarnHRate.setVisible(false);
        addNewEmpWarnRSub.setVisible(false);
        addNewEmpWarnPAllow.setVisible(false);
        addNewEmpWarnCAllow.setVisible(false);
        addNewEmpWarnSss.setVisible(false);
        addNewEmpWarnTin.setVisible(false);
        addNewEmpWarnPhil.setVisible(false);
        addNewEmpWarnPag.setVisible(false);
        addNewEmpErrorFill.setVisible(false);
        addNewEmpSuccessCreatedLabel.setVisible(false);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        AddNewEmpGradientPanel = new keeptoo.KGradientPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        addNewEmpForm = new javax.swing.JPanel();
        newEmpImmediateSup = new javax.swing.JLabel();
        newEmpLastNameValue = new javax.swing.JTextField();
        newEmpBirth = new javax.swing.JLabel();
        newEmpPayInfo = new javax.swing.JLabel();
        newEmpFirstName = new javax.swing.JLabel();
        newEmpFirstNameValue = new javax.swing.JTextField();
        newEmpPersonalEmail = new javax.swing.JLabel();
        newEmpPersonalEmailValue = new javax.swing.JTextField();
        newEmpID = new javax.swing.JLabel();
        newEmpDateHired = new javax.swing.JLabel();
        newEmpAddInfo = new javax.swing.JLabel();
        newEmpLastName = new javax.swing.JLabel();
        newEmpIDValue = new javax.swing.JTextField();
        newEmpContact = new javax.swing.JLabel();
        newEmpStatus = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        newEmpPhoneNum = new javax.swing.JLabel();
        newEmpHourlyRateValue = new javax.swing.JTextField();
        addNewEmpAllowLabel = new javax.swing.JLabel();
        addressZipcode = new javax.swing.JLabel();
        newEmpHourlyRate = new javax.swing.JLabel();
        newEmpClothingAllow = new javax.swing.JLabel();
        newEmpClothingAllowValue = new javax.swing.JTextField();
        newEmpGrossSemi = new javax.swing.JLabel();
        newEmpGrossSemiValue = new javax.swing.JTextField();
        newEmpBenefits = new javax.swing.JLabel();
        newEmpPhilhealth = new javax.swing.JLabel();
        newEmpRiceSub = new javax.swing.JLabel();
        newEmpPhoneAllow = new javax.swing.JLabel();
        newEmpPhilhealthValue = new javax.swing.JTextField();
        newEmpRiceSubValue = new javax.swing.JTextField();
        newEmpPhoneAllowValue = new javax.swing.JTextField();
        newEmpSalary = new javax.swing.JLabel();
        newEmpBasicSalaryValue = new javax.swing.JTextField();
        newEmpBasicSalary = new javax.swing.JLabel();
        newEmpPagIbig = new javax.swing.JLabel();
        newEmpPagIbigValue = new javax.swing.JTextField();
        newEmpSss = new javax.swing.JLabel();
        newEmpTin = new javax.swing.JLabel();
        newEmpSssValue = new javax.swing.JTextField();
        newEmpTinValue = new javax.swing.JTextField();
        newEmpSubmitButton = new javax.swing.JButton();
        newEmpPosition = new javax.swing.JLabel();
        newEmpRoleValue = new javax.swing.JComboBox<>();
        newEmpBirthValue = new com.toedter.calendar.JDateChooser();
        newEmpCancelButton = new javax.swing.JButton();
        addNewEmpSuccessCreatedLabel = new javax.swing.JLabel();
        newEmpUserRole = new javax.swing.JLabel();
        addNewEmpWarnPag = new javax.swing.JLabel();
        addNewEmpWarnLN = new javax.swing.JLabel();
        addNewEmpWarnFN = new javax.swing.JLabel();
        addNewEmpWarnBirth = new javax.swing.JLabel();
        addNewEmpWarnPhone = new javax.swing.JLabel();
        addNewEmpWarnBSal = new javax.swing.JLabel();
        addNewEmpWarnGSMR = new javax.swing.JLabel();
        addNewEmpWarnHRate = new javax.swing.JLabel();
        addNewEmpWarnRSub = new javax.swing.JLabel();
        addNewEmpWarnPAllow = new javax.swing.JLabel();
        addNewEmpWarnCAllow = new javax.swing.JLabel();
        addNewEmpWarnSss = new javax.swing.JLabel();
        addNewEmpWarnTin = new javax.swing.JLabel();
        addNewEmpWarnPhil = new javax.swing.JLabel();
        addNewEmpErrorFill = new javax.swing.JLabel();
        kGradientPanel2 = new keeptoo.KGradientPanel();
        empInfo = new javax.swing.JLabel();
        jSeparator7 = new javax.swing.JSeparator();
        jSeparator3 = new javax.swing.JSeparator();
        jSeparator4 = new javax.swing.JSeparator();
        jSeparator8 = new javax.swing.JSeparator();
        newEmpPhoneNumValue = new javax.swing.JTextField();
        jSeparator5 = new javax.swing.JSeparator();
        newEmpStatusValue = new javax.swing.JComboBox<>();
        newEmpPositionValue = new javax.swing.JComboBox<>();
        newEmpImmediateSupValue1 = new javax.swing.JTextField();
        addNewEmpWarnISup = new javax.swing.JLabel();
        newEmpDateHiredValue = new com.toedter.calendar.JDateChooser();
        addressStreet = new javax.swing.JLabel();
        addressBarangay = new javax.swing.JLabel();
        addressCity = new javax.swing.JLabel();
        addressProvince = new javax.swing.JLabel();
        addressProvinceValue = new javax.swing.JTextField();
        addressStreetValue = new javax.swing.JTextField();
        addressZipcodeValue = new javax.swing.JTextField();
        addressCityValue = new javax.swing.JTextField();
        addressBarangayValue = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        exitButton = new javax.swing.JLabel();
        addNewEmpLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        AddNewEmpGradientPanel.setkEndColor(new java.awt.Color(0, 0, 204));
        AddNewEmpGradientPanel.setkStartColor(new java.awt.Color(153, 0, 204));
        AddNewEmpGradientPanel.setMinimumSize(new java.awt.Dimension(820, 590));
        AddNewEmpGradientPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jScrollPane1.setBackground(new java.awt.Color(255, 255, 255));
        jScrollPane1.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                jScrollPane1MouseWheelMoved(evt);
            }
        });

        addNewEmpForm.setBackground(new java.awt.Color(255, 255, 255));
        addNewEmpForm.setForeground(new java.awt.Color(0, 0, 0));
        addNewEmpForm.setPreferredSize(new java.awt.Dimension(800, 1360));
        addNewEmpForm.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        newEmpImmediateSup.setBackground(new java.awt.Color(255, 255, 255));
        newEmpImmediateSup.setForeground(new java.awt.Color(0, 0, 0));
        newEmpImmediateSup.setText(" Immediate Supervisor ID*");
        newEmpImmediateSup.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(newEmpImmediateSup, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 280, 190, 30));

        newEmpLastNameValue.setBackground(new java.awt.Color(255, 255, 255));
        newEmpLastNameValue.setForeground(new java.awt.Color(0, 0, 0));
        newEmpLastNameValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        newEmpLastNameValue.setVerifyInputWhenFocusTarget(false);
        newEmpLastNameValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEmpLastNameValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(newEmpLastNameValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 130, 190, 30));

        newEmpBirth.setBackground(new java.awt.Color(255, 255, 255));
        newEmpBirth.setForeground(new java.awt.Color(0, 0, 0));
        newEmpBirth.setText(" Date of Birth*");
        newEmpBirth.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(newEmpBirth, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 180, 190, 30));

        newEmpPayInfo.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        newEmpPayInfo.setForeground(new java.awt.Color(0, 0, 204));
        newEmpPayInfo.setText("PAYROLL INFORMATION");
        addNewEmpForm.add(newEmpPayInfo, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 700, -1, 30));

        newEmpFirstName.setBackground(new java.awt.Color(255, 255, 255));
        newEmpFirstName.setForeground(new java.awt.Color(0, 0, 0));
        newEmpFirstName.setText(" First Name *");
        newEmpFirstName.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(newEmpFirstName, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 130, 190, 30));

        newEmpFirstNameValue.setBackground(new java.awt.Color(255, 255, 255));
        newEmpFirstNameValue.setForeground(new java.awt.Color(0, 0, 0));
        newEmpFirstNameValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        newEmpFirstNameValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEmpFirstNameValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(newEmpFirstNameValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(600, 130, 190, 30));

        newEmpPersonalEmail.setBackground(new java.awt.Color(255, 255, 255));
        newEmpPersonalEmail.setForeground(new java.awt.Color(0, 0, 0));
        newEmpPersonalEmail.setText(" Personal Email");
        newEmpPersonalEmail.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(newEmpPersonalEmail, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 400, 190, 30));

        newEmpPersonalEmailValue.setBackground(new java.awt.Color(255, 255, 255));
        newEmpPersonalEmailValue.setForeground(new java.awt.Color(0, 0, 0));
        newEmpPersonalEmailValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        newEmpPersonalEmailValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEmpPersonalEmailValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(newEmpPersonalEmailValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(600, 400, 190, 30));

        newEmpID.setBackground(new java.awt.Color(255, 255, 255));
        newEmpID.setForeground(new java.awt.Color(0, 0, 0));
        newEmpID.setText(" Employee no.");
        newEmpID.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(newEmpID, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 80, 190, 30));

        newEmpDateHired.setBackground(new java.awt.Color(255, 255, 255));
        newEmpDateHired.setForeground(new java.awt.Color(0, 0, 0));
        newEmpDateHired.setText(" Date hired");
        newEmpDateHired.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(newEmpDateHired, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 230, 190, 30));

        newEmpAddInfo.setBackground(new java.awt.Color(255, 255, 255));
        newEmpAddInfo.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        newEmpAddInfo.setForeground(new java.awt.Color(0, 0, 204));
        newEmpAddInfo.setText("ADDRESS INFORMATION");
        addNewEmpForm.add(newEmpAddInfo, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 490, -1, 30));

        newEmpLastName.setBackground(new java.awt.Color(255, 255, 255));
        newEmpLastName.setForeground(new java.awt.Color(0, 0, 0));
        newEmpLastName.setText(" Last Name *");
        newEmpLastName.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(newEmpLastName, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 130, 190, 30));

        newEmpIDValue.setEditable(false);
        newEmpIDValue.setBackground(new java.awt.Color(227, 227, 227));
        newEmpIDValue.setForeground(new java.awt.Color(0, 0, 0));
        newEmpIDValue.setToolTipText("This field is automatically system generated");
        newEmpIDValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        newEmpIDValue.setDisabledTextColor(new java.awt.Color(102, 102, 102));
        newEmpIDValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEmpIDValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(newEmpIDValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 80, 190, 30));

        newEmpContact.setBackground(new java.awt.Color(255, 255, 255));
        newEmpContact.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        newEmpContact.setForeground(new java.awt.Color(0, 0, 204));
        newEmpContact.setText("CONTACT INFORMATION");
        addNewEmpForm.add(newEmpContact, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 350, -1, 30));

        newEmpStatus.setBackground(new java.awt.Color(255, 255, 255));
        newEmpStatus.setForeground(new java.awt.Color(0, 0, 0));
        newEmpStatus.setText(" Status*");
        newEmpStatus.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(newEmpStatus, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 180, 190, 30));

        jSeparator2.setForeground(new java.awt.Color(204, 204, 204));
        addNewEmpForm.add(jSeparator2, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 1020, 730, 20));

        newEmpPhoneNum.setBackground(new java.awt.Color(255, 255, 255));
        newEmpPhoneNum.setForeground(new java.awt.Color(0, 0, 0));
        newEmpPhoneNum.setText(" Phone Number*");
        newEmpPhoneNum.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(newEmpPhoneNum, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 400, 190, 30));

        newEmpHourlyRateValue.setBackground(new java.awt.Color(255, 255, 255));
        newEmpHourlyRateValue.setForeground(new java.awt.Color(0, 0, 0));
        newEmpHourlyRateValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        newEmpHourlyRateValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEmpHourlyRateValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(newEmpHourlyRateValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 810, 200, 30));

        addNewEmpAllowLabel.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        addNewEmpAllowLabel.setForeground(new java.awt.Color(0, 0, 204));
        addNewEmpAllowLabel.setText("Allowances");
        addNewEmpForm.add(addNewEmpAllowLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 880, 100, 30));

        addressZipcode.setBackground(new java.awt.Color(255, 255, 255));
        addressZipcode.setForeground(new java.awt.Color(0, 0, 0));
        addressZipcode.setText("  Zipcode");
        addressZipcode.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(addressZipcode, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 570, 120, 30));

        newEmpHourlyRate.setForeground(new java.awt.Color(0, 0, 0));
        newEmpHourlyRate.setText(" Hourly Rate*");
        newEmpHourlyRate.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(newEmpHourlyRate, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 780, 200, 30));

        newEmpClothingAllow.setBackground(new java.awt.Color(255, 255, 255));
        newEmpClothingAllow.setForeground(new java.awt.Color(0, 0, 0));
        newEmpClothingAllow.setText(" Clothing Allowance *");
        newEmpClothingAllow.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(newEmpClothingAllow, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 920, 200, 30));

        newEmpClothingAllowValue.setBackground(new java.awt.Color(255, 255, 255));
        newEmpClothingAllowValue.setForeground(new java.awt.Color(0, 0, 0));
        newEmpClothingAllowValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        newEmpClothingAllowValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEmpClothingAllowValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(newEmpClothingAllowValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 950, 200, 30));

        newEmpGrossSemi.setForeground(new java.awt.Color(0, 0, 0));
        newEmpGrossSemi.setText(" Gross Semi-monthly Rate*");
        newEmpGrossSemi.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(newEmpGrossSemi, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 780, 200, 30));

        newEmpGrossSemiValue.setBackground(new java.awt.Color(255, 255, 255));
        newEmpGrossSemiValue.setForeground(new java.awt.Color(0, 0, 0));
        newEmpGrossSemiValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        newEmpGrossSemiValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEmpGrossSemiValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(newEmpGrossSemiValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 810, 200, 30));

        newEmpBenefits.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        newEmpBenefits.setForeground(new java.awt.Color(0, 0, 204));
        newEmpBenefits.setText("Government ID");
        addNewEmpForm.add(newEmpBenefits, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 1030, 90, 30));

        newEmpPhilhealth.setBackground(new java.awt.Color(255, 255, 255));
        newEmpPhilhealth.setForeground(new java.awt.Color(0, 0, 0));
        newEmpPhilhealth.setText(" Philhealth number*");
        newEmpPhilhealth.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(newEmpPhilhealth, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 1130, 190, 30));

        newEmpRiceSub.setBackground(new java.awt.Color(255, 255, 255));
        newEmpRiceSub.setForeground(new java.awt.Color(0, 0, 0));
        newEmpRiceSub.setText(" Rice Subsidy*");
        newEmpRiceSub.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(newEmpRiceSub, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 920, 200, 30));

        newEmpPhoneAllow.setBackground(new java.awt.Color(255, 255, 255));
        newEmpPhoneAllow.setForeground(new java.awt.Color(0, 0, 0));
        newEmpPhoneAllow.setText("Phone Allowance*");
        newEmpPhoneAllow.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(newEmpPhoneAllow, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 920, 200, 30));

        newEmpPhilhealthValue.setBackground(new java.awt.Color(255, 255, 255));
        newEmpPhilhealthValue.setForeground(new java.awt.Color(0, 0, 0));
        newEmpPhilhealthValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        newEmpPhilhealthValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEmpPhilhealthValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(newEmpPhilhealthValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 1130, 190, 30));

        newEmpRiceSubValue.setBackground(new java.awt.Color(255, 255, 255));
        newEmpRiceSubValue.setForeground(new java.awt.Color(0, 0, 0));
        newEmpRiceSubValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        newEmpRiceSubValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEmpRiceSubValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(newEmpRiceSubValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 950, 200, 30));

        newEmpPhoneAllowValue.setBackground(new java.awt.Color(255, 255, 255));
        newEmpPhoneAllowValue.setForeground(new java.awt.Color(0, 0, 0));
        newEmpPhoneAllowValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        newEmpPhoneAllowValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEmpPhoneAllowValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(newEmpPhoneAllowValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 950, 200, 30));

        newEmpSalary.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        newEmpSalary.setForeground(new java.awt.Color(0, 0, 204));
        newEmpSalary.setText("Salary Rate");
        addNewEmpForm.add(newEmpSalary, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 740, 120, 30));

        newEmpBasicSalaryValue.setBackground(new java.awt.Color(255, 255, 255));
        newEmpBasicSalaryValue.setForeground(new java.awt.Color(0, 0, 0));
        newEmpBasicSalaryValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        newEmpBasicSalaryValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEmpBasicSalaryValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(newEmpBasicSalaryValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 810, 200, 30));

        newEmpBasicSalary.setForeground(new java.awt.Color(0, 0, 0));
        newEmpBasicSalary.setText(" Basic Salary*");
        newEmpBasicSalary.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(newEmpBasicSalary, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 780, 200, 30));

        newEmpPagIbig.setBackground(new java.awt.Color(255, 255, 255));
        newEmpPagIbig.setForeground(new java.awt.Color(0, 0, 0));
        newEmpPagIbig.setText(" Pag-ibig number*");
        newEmpPagIbig.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(newEmpPagIbig, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 1130, 190, 30));

        newEmpPagIbigValue.setBackground(new java.awt.Color(255, 255, 255));
        newEmpPagIbigValue.setForeground(new java.awt.Color(0, 0, 0));
        newEmpPagIbigValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        newEmpPagIbigValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEmpPagIbigValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(newEmpPagIbigValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(600, 1130, 190, 30));

        newEmpSss.setBackground(new java.awt.Color(255, 255, 255));
        newEmpSss.setForeground(new java.awt.Color(0, 0, 0));
        newEmpSss.setText(" SSS number*");
        newEmpSss.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(newEmpSss, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 1070, 190, 30));

        newEmpTin.setBackground(new java.awt.Color(255, 255, 255));
        newEmpTin.setForeground(new java.awt.Color(0, 0, 0));
        newEmpTin.setText(" TIN number*");
        newEmpTin.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(newEmpTin, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 1070, 190, 30));

        newEmpSssValue.setBackground(new java.awt.Color(255, 255, 255));
        newEmpSssValue.setForeground(new java.awt.Color(0, 0, 0));
        newEmpSssValue.setToolTipText("Must be unique");
        newEmpSssValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        newEmpSssValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEmpSssValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(newEmpSssValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 1070, 190, 30));

        newEmpTinValue.setBackground(new java.awt.Color(255, 255, 255));
        newEmpTinValue.setForeground(new java.awt.Color(0, 0, 0));
        newEmpTinValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        newEmpTinValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEmpTinValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(newEmpTinValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(600, 1070, 190, 30));

        newEmpSubmitButton.setBackground(new java.awt.Color(255, 255, 255));
        newEmpSubmitButton.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        newEmpSubmitButton.setForeground(new java.awt.Color(0, 0, 255));
        newEmpSubmitButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/send.png"))); // NOI18N
        newEmpSubmitButton.setText("Submit");
        newEmpSubmitButton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        newEmpSubmitButton.setFocusPainted(false);
        newEmpSubmitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEmpSubmitButtonActionPerformed(evt);
            }
        });
        addNewEmpForm.add(newEmpSubmitButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 1230, 160, 50));

        newEmpPosition.setBackground(new java.awt.Color(255, 255, 255));
        newEmpPosition.setForeground(new java.awt.Color(0, 0, 0));
        newEmpPosition.setText(" Position*");
        newEmpPosition.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(newEmpPosition, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 230, 190, 30));

        newEmpRoleValue.setBackground(new java.awt.Color(227, 227, 227));
        newEmpRoleValue.setForeground(new java.awt.Color(0, 0, 0));
        newEmpRoleValue.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Employee", "HR Personnel", "System Admin" }));
        newEmpRoleValue.setLightWeightPopupEnabled(false);
        newEmpRoleValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEmpRoleValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(newEmpRoleValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(600, 80, 190, 30));

        newEmpBirthValue.setBackground(new java.awt.Color(255, 255, 255));
        newEmpBirthValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        newEmpBirthValue.setForeground(new java.awt.Color(0, 0, 0));
        newEmpBirthValue.setDateFormatString("MM/dd/yyyy");
        addNewEmpForm.add(newEmpBirthValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 180, 190, 30));

        newEmpCancelButton.setBackground(new java.awt.Color(255, 255, 255));
        newEmpCancelButton.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        newEmpCancelButton.setForeground(new java.awt.Color(204, 0, 0));
        newEmpCancelButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x-button.png"))); // NOI18N
        newEmpCancelButton.setText("Cancel");
        newEmpCancelButton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        newEmpCancelButton.setFocusPainted(false);
        newEmpCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEmpCancelButtonActionPerformed(evt);
            }
        });
        addNewEmpForm.add(newEmpCancelButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 1230, 160, 50));

        addNewEmpSuccessCreatedLabel.setFont(new java.awt.Font("Arial", 3, 12)); // NOI18N
        addNewEmpSuccessCreatedLabel.setForeground(new java.awt.Color(0, 0, 255));
        addNewEmpSuccessCreatedLabel.setText("Successfully created a new USER. This window will automatically close in 3 seconds.");
        addNewEmpForm.add(addNewEmpSuccessCreatedLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 1330, -1, 20));

        newEmpUserRole.setBackground(new java.awt.Color(255, 255, 255));
        newEmpUserRole.setForeground(new java.awt.Color(0, 0, 0));
        newEmpUserRole.setText(" User Role");
        newEmpUserRole.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(newEmpUserRole, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 80, 190, 30));

        addNewEmpWarnPag.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        addNewEmpWarnPag.setForeground(new java.awt.Color(204, 0, 51));
        addNewEmpWarnPag.setText("This is required.");
        addNewEmpForm.add(addNewEmpWarnPag, new org.netbeans.lib.awtextra.AbsoluteConstraints(600, 1160, -1, 20));

        addNewEmpWarnLN.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        addNewEmpWarnLN.setForeground(new java.awt.Color(204, 0, 51));
        addNewEmpWarnLN.setText("This is required.");
        addNewEmpForm.add(addNewEmpWarnLN, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 160, -1, 20));

        addNewEmpWarnFN.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        addNewEmpWarnFN.setForeground(new java.awt.Color(204, 0, 51));
        addNewEmpWarnFN.setText("This is required.");
        addNewEmpForm.add(addNewEmpWarnFN, new org.netbeans.lib.awtextra.AbsoluteConstraints(660, 160, -1, 20));

        addNewEmpWarnBirth.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        addNewEmpWarnBirth.setForeground(new java.awt.Color(204, 0, 51));
        addNewEmpWarnBirth.setText("This is required.");
        addNewEmpForm.add(addNewEmpWarnBirth, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 210, -1, 20));

        addNewEmpWarnPhone.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        addNewEmpWarnPhone.setForeground(new java.awt.Color(204, 0, 51));
        addNewEmpWarnPhone.setText("This is required.");
        addNewEmpForm.add(addNewEmpWarnPhone, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 430, -1, 20));

        addNewEmpWarnBSal.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        addNewEmpWarnBSal.setForeground(new java.awt.Color(204, 0, 51));
        addNewEmpWarnBSal.setText("This is required.");
        addNewEmpForm.add(addNewEmpWarnBSal, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 840, -1, 20));

        addNewEmpWarnGSMR.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        addNewEmpWarnGSMR.setForeground(new java.awt.Color(204, 0, 51));
        addNewEmpWarnGSMR.setText("This is required.");
        addNewEmpForm.add(addNewEmpWarnGSMR, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 840, -1, 20));

        addNewEmpWarnHRate.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        addNewEmpWarnHRate.setForeground(new java.awt.Color(204, 0, 51));
        addNewEmpWarnHRate.setText("This is required.");
        addNewEmpForm.add(addNewEmpWarnHRate, new org.netbeans.lib.awtextra.AbsoluteConstraints(540, 840, -1, 20));

        addNewEmpWarnRSub.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        addNewEmpWarnRSub.setForeground(new java.awt.Color(204, 0, 51));
        addNewEmpWarnRSub.setText("This is required.");
        addNewEmpForm.add(addNewEmpWarnRSub, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 980, -1, 20));

        addNewEmpWarnPAllow.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        addNewEmpWarnPAllow.setForeground(new java.awt.Color(204, 0, 51));
        addNewEmpWarnPAllow.setText("This is required.");
        addNewEmpForm.add(addNewEmpWarnPAllow, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 980, -1, 20));

        addNewEmpWarnCAllow.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        addNewEmpWarnCAllow.setForeground(new java.awt.Color(204, 0, 51));
        addNewEmpWarnCAllow.setText("This is required.");
        addNewEmpForm.add(addNewEmpWarnCAllow, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 980, -1, 20));

        addNewEmpWarnSss.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        addNewEmpWarnSss.setForeground(new java.awt.Color(204, 0, 51));
        addNewEmpWarnSss.setText("This is required.");
        addNewEmpForm.add(addNewEmpWarnSss, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 1100, -1, 20));

        addNewEmpWarnTin.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        addNewEmpWarnTin.setForeground(new java.awt.Color(204, 0, 51));
        addNewEmpWarnTin.setText("This is required.");
        addNewEmpForm.add(addNewEmpWarnTin, new org.netbeans.lib.awtextra.AbsoluteConstraints(600, 1100, -1, 20));

        addNewEmpWarnPhil.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        addNewEmpWarnPhil.setForeground(new java.awt.Color(204, 0, 51));
        addNewEmpWarnPhil.setText("This is required.");
        addNewEmpForm.add(addNewEmpWarnPhil, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 1160, -1, 20));

        addNewEmpErrorFill.setFont(new java.awt.Font("Arial", 3, 12)); // NOI18N
        addNewEmpErrorFill.setForeground(new java.awt.Color(204, 0, 0));
        addNewEmpErrorFill.setText("Error: Please fill the required (*) fields. ");
        addNewEmpForm.add(addNewEmpErrorFill, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 1300, -1, 30));

        kGradientPanel2.setkEndColor(new java.awt.Color(0, 0, 204));
        kGradientPanel2.setkStartColor(new java.awt.Color(153, 0, 204));
        addNewEmpForm.add(kGradientPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 1370, 810, 20));

        empInfo.setBackground(new java.awt.Color(255, 255, 255));
        empInfo.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        empInfo.setForeground(new java.awt.Color(0, 0, 204));
        empInfo.setText("EMPLOYEE INFORMATION");
        addNewEmpForm.add(empInfo, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 20, 270, 30));

        jSeparator7.setForeground(new java.awt.Color(204, 204, 204));
        addNewEmpForm.add(jSeparator7, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 340, 810, 20));

        jSeparator3.setForeground(new java.awt.Color(204, 204, 204));
        addNewEmpForm.add(jSeparator3, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 470, 810, 20));

        jSeparator4.setForeground(new java.awt.Color(204, 204, 204));
        addNewEmpForm.add(jSeparator4, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 1200, 810, 20));

        jSeparator8.setForeground(new java.awt.Color(204, 204, 204));
        addNewEmpForm.add(jSeparator8, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 870, 730, 20));

        newEmpPhoneNumValue.setBackground(new java.awt.Color(255, 255, 255));
        newEmpPhoneNumValue.setForeground(new java.awt.Color(0, 0, 0));
        newEmpPhoneNumValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        newEmpPhoneNumValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEmpPhoneNumValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(newEmpPhoneNumValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 400, 190, 30));

        jSeparator5.setForeground(new java.awt.Color(204, 204, 204));
        addNewEmpForm.add(jSeparator5, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 680, 810, 20));

        newEmpStatusValue.setBackground(new java.awt.Color(227, 227, 227));
        newEmpStatusValue.setEditable(true);
        newEmpStatusValue.setForeground(new java.awt.Color(0, 0, 0));
        newEmpStatusValue.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Regular", "Probationary" }));
        newEmpStatusValue.setLightWeightPopupEnabled(false);
        newEmpStatusValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEmpStatusValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(newEmpStatusValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(600, 180, 190, 30));

        newEmpPositionValue.setBackground(new java.awt.Color(227, 227, 227));
        newEmpPositionValue.setEditable(true);
        newEmpPositionValue.setForeground(new java.awt.Color(0, 0, 0));
        newEmpPositionValue.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Chief Executive Officer", "Chief Operating Officer", "Chief Finance Officer", "Chief Marketing Officer", "IT Operations and Systems", "HR Manager", "HR Team Leader", "HR Rank and File", "Accounting Head", "Payroll Manager", "Payroll Team Leader", "Payroll Rank and File", "Account Manager", "Account Team Leader", "Account Rank and File", "Sales & Marketing", "Supply Chain and Logistics", "Customer Service and Relations" }));
        newEmpPositionValue.setLightWeightPopupEnabled(false);
        newEmpPositionValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEmpPositionValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(newEmpPositionValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(600, 230, 190, 30));

        newEmpImmediateSupValue1.setBackground(new java.awt.Color(255, 255, 255));
        newEmpImmediateSupValue1.setForeground(new java.awt.Color(0, 0, 0));
        newEmpImmediateSupValue1.setToolTipText("");
        newEmpImmediateSupValue1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        newEmpImmediateSupValue1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEmpImmediateSupValue1ActionPerformed(evt);
            }
        });
        addNewEmpForm.add(newEmpImmediateSupValue1, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 280, 190, 30));

        addNewEmpWarnISup.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        addNewEmpWarnISup.setForeground(new java.awt.Color(204, 0, 51));
        addNewEmpWarnISup.setText("This is required.");
        addNewEmpForm.add(addNewEmpWarnISup, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 310, 80, 20));

        newEmpDateHiredValue.setBackground(new java.awt.Color(255, 255, 255));
        newEmpDateHiredValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        newEmpDateHiredValue.setForeground(new java.awt.Color(0, 0, 0));
        newEmpDateHiredValue.setDateFormatString("MM/dd/yyyy");
        addNewEmpForm.add(newEmpDateHiredValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 230, 190, 30));

        addressStreet.setBackground(new java.awt.Color(255, 255, 255));
        addressStreet.setForeground(new java.awt.Color(0, 0, 0));
        addressStreet.setText("  Street");
        addressStreet.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(addressStreet, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 530, 120, 30));

        addressBarangay.setBackground(new java.awt.Color(255, 255, 255));
        addressBarangay.setForeground(new java.awt.Color(0, 0, 0));
        addressBarangay.setText("  Barangay");
        addressBarangay.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(addressBarangay, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 570, 120, 30));

        addressCity.setBackground(new java.awt.Color(255, 255, 255));
        addressCity.setForeground(new java.awt.Color(0, 0, 0));
        addressCity.setText("  City");
        addressCity.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(addressCity, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 610, 120, 30));

        addressProvince.setBackground(new java.awt.Color(255, 255, 255));
        addressProvince.setForeground(new java.awt.Color(0, 0, 0));
        addressProvince.setText("  Province");
        addressProvince.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addNewEmpForm.add(addressProvince, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 530, 120, 30));

        addressProvinceValue.setBackground(new java.awt.Color(255, 255, 255));
        addressProvinceValue.setForeground(new java.awt.Color(0, 0, 0));
        addressProvinceValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addressProvinceValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addressProvinceValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(addressProvinceValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 530, 260, 30));

        addressStreetValue.setBackground(new java.awt.Color(255, 255, 255));
        addressStreetValue.setForeground(new java.awt.Color(0, 0, 0));
        addressStreetValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addressStreetValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addressStreetValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(addressStreetValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 530, 260, 30));

        addressZipcodeValue.setBackground(new java.awt.Color(255, 255, 255));
        addressZipcodeValue.setForeground(new java.awt.Color(0, 0, 0));
        addressZipcodeValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addressZipcodeValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addressZipcodeValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(addressZipcodeValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 570, 260, 30));

        addressCityValue.setBackground(new java.awt.Color(255, 255, 255));
        addressCityValue.setForeground(new java.awt.Color(0, 0, 0));
        addressCityValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addressCityValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addressCityValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(addressCityValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 610, 260, 30));

        addressBarangayValue.setBackground(new java.awt.Color(255, 255, 255));
        addressBarangayValue.setForeground(new java.awt.Color(0, 0, 0));
        addressBarangayValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        addressBarangayValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addressBarangayValueActionPerformed(evt);
            }
        });
        addNewEmpForm.add(addressBarangayValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 570, 260, 30));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 2, 10)); // NOI18N
        jLabel1.setText("This should be the Employee ID of the Supervisor.");
        addNewEmpForm.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 280, 230, 30));

        jScrollPane1.setViewportView(addNewEmpForm);

        AddNewEmpGradientPanel.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 62, 820, 500));

        exitButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons8-close-window-48.png"))); // NOI18N
        exitButton.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        exitButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                exitButtonMouseClicked(evt);
            }
        });
        AddNewEmpGradientPanel.add(exitButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(780, 10, -1, 30));

        addNewEmpLabel.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        addNewEmpLabel.setForeground(new java.awt.Color(255, 255, 255));
        addNewEmpLabel.setText("Add New Employee");
        AddNewEmpGradientPanel.add(addNewEmpLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 30, -1, -1));

        getContentPane().add(AddNewEmpGradientPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, 590));

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void exitButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exitButtonMouseClicked
        this.dispose();
    }//GEN-LAST:event_exitButtonMouseClicked

    private void newEmpCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newEmpCancelButtonActionPerformed
        dispose();
    }//GEN-LAST:event_newEmpCancelButtonActionPerformed

    private void newEmpRoleValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newEmpRoleValueActionPerformed

    }//GEN-LAST:event_newEmpRoleValueActionPerformed

    
    private void newEmpSubmitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newEmpSubmitButtonActionPerformed
        HRPersonnelDao hrPersonnelDao = new HRPersonnelDao();
        SystemAdministratorDao systemAdminDao = new SystemAdministratorDao();
        addNewEmpErrorFill.setVisible(false);
        
        String role = (String) newEmpRoleValue.getSelectedItem();
        System.out.println(role);
        if(role.equals("System Admin")){
            editableUser = new SystemAdministrator();
        } else if(role.equals("HR Personnel")){
            editableUser = new HRPersonnel();
        } else {
            editableUser = new Employee();
        }
        boolean isClear = Helpers.validateForm(fields);
        if(editableUser instanceof Employee ){
            isClear = Helpers.validateForm(empOnlyFields) && isClear;
        }
        boolean result = false;
        
        addNewEmpErrorFill.setText("Error: Please fill the required (*) fields. ");
        addNewEmpErrorFill.setVisible(!isClear);
        if(isClear){
            Address address = new Address();
            address.setStreet(addressStreetValue.getText());
            address.setBarangay(addressBarangayValue.getText());
            address.setCity(addressCityValue.getText());
            address.setProvince(addressProvinceValue.getText());
            address.setZipcode(addressZipcodeValue.getText());
            
            editableUser.setLastName(newEmpLastNameValue.getText());
            editableUser.setFirstName(newEmpFirstNameValue.getText());
            editableUser.setBirthday(newEmpBirthValue.getDate());
            editableUser.setPhoneNumber(newEmpPhoneNumValue.getText());
            editableUser.setAddress(address);
            editableUser.setSssNumber(newEmpSssValue.getText());
            editableUser.setTinNumber(newEmpTinValue.getText());
            editableUser.setPhilhealthNumber(newEmpPhilhealthValue.getText());
            editableUser.setPagibigNumber(newEmpPagIbigValue.getText());
            editableUser.setPosition((String) newEmpPositionValue.getSelectedItem());
            
            editableUser.setStatus((String) newEmpStatusValue.getSelectedItem());
            editableUser.setImmediateSupervisor(Integer.parseInt(newEmpImmediateSupValue1.getText()));
            Benefits benefit = new Benefits();
            benefit.setBasicSalary(Double.parseDouble(newEmpBasicSalaryValue.getText()));
            benefit.setGrossSemiMonthlyRate(Double.parseDouble(newEmpGrossSemiValue.getText()));
            benefit.setHourlyRate(Double.parseDouble(newEmpHourlyRateValue.getText()));
            benefit.setRiceSubsidy(Double.parseDouble(newEmpRiceSubValue.getText()));
            benefit.setPhoneAllowance(Double.parseDouble(newEmpPhoneAllowValue.getText()));
            benefit.setClothingAllowance(Double.parseDouble(newEmpClothingAllowValue.getText()));

            editableUser.setBenefits(benefit);
            
            if(action.equals("Create")){
                if(user instanceof HRPersonnel){
                    result = hrPersonnelDao.addNewEmployee((Employee) editableUser);
                } else {
                    result = systemAdminDao.addNewEmployee(editableUser);
                }
                addNewEmpSuccessCreatedLabel.setText(
                    "Successfully created a new user. "
                    + "This window will automatically close in 3 seconds.");
            } else {
                editableUser.setEmployeeID(Integer.parseInt(newEmpIDValue.getText()));
                if(user instanceof HRPersonnel){
                    result = hrPersonnelDao.updateEmployee((Employee) editableUser);
                } else {
                    Integer actingAdminEmployeeId = user == null ? null : Integer.valueOf(user.getEmployeeID());
                    result = systemAdminDao.updateEmployee(editableUser, actingAdminEmployeeId);
                }
                addNewEmpSuccessCreatedLabel.setText(
                    "Successfully updated user. "
                    + "This window will automatically close in 3 seconds.");
            }
            if(result){
                addNewEmpSuccessCreatedLabel.setVisible(true);
                newEmpSubmitButton.setEnabled(false);
    //            adminHomePage.refreshEmployeeListTable();
                Timer timer = new Timer();

                // Schedule a task to run dispose after 3 seconds.
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        dispose();
                        parentPanel.loadEmployeeList();
                    }
                }, 3000);
            } else {
                addNewEmpErrorFill.setText("Error: Something went wrong. ");
                addNewEmpErrorFill.setVisible(true);
            }
        }
    }//GEN-LAST:event_newEmpSubmitButtonActionPerformed

    private void newEmpTinValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newEmpTinValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_newEmpTinValueActionPerformed

    private void newEmpSssValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newEmpSssValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_newEmpSssValueActionPerformed

    private void newEmpPagIbigValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newEmpPagIbigValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_newEmpPagIbigValueActionPerformed

    private void newEmpBasicSalaryValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newEmpBasicSalaryValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_newEmpBasicSalaryValueActionPerformed

    private void newEmpPhoneAllowValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newEmpPhoneAllowValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_newEmpPhoneAllowValueActionPerformed

    private void newEmpRiceSubValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newEmpRiceSubValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_newEmpRiceSubValueActionPerformed

    private void newEmpPhilhealthValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newEmpPhilhealthValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_newEmpPhilhealthValueActionPerformed

    private void newEmpGrossSemiValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newEmpGrossSemiValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_newEmpGrossSemiValueActionPerformed

    private void newEmpClothingAllowValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newEmpClothingAllowValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_newEmpClothingAllowValueActionPerformed

    private void newEmpHourlyRateValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newEmpHourlyRateValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_newEmpHourlyRateValueActionPerformed

    private void newEmpIDValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newEmpIDValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_newEmpIDValueActionPerformed

    private void newEmpPersonalEmailValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newEmpPersonalEmailValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_newEmpPersonalEmailValueActionPerformed

    private void newEmpFirstNameValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newEmpFirstNameValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_newEmpFirstNameValueActionPerformed

    private void newEmpLastNameValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newEmpLastNameValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_newEmpLastNameValueActionPerformed

    private void newEmpPhoneNumValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newEmpPhoneNumValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_newEmpPhoneNumValueActionPerformed

    private void newEmpStatusValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newEmpStatusValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_newEmpStatusValueActionPerformed

    private void newEmpPositionValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newEmpPositionValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_newEmpPositionValueActionPerformed

    private void newEmpImmediateSupValue1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newEmpImmediateSupValue1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_newEmpImmediateSupValue1ActionPerformed

    private void jScrollPane1MouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_jScrollPane1MouseWheelMoved
         int rotation = evt.getWheelRotation();
                JScrollBar verticalScrollBar = jScrollPane1.getVerticalScrollBar();
                
                // Control the scroll speed here (higher value = faster scroll)
                int scrollAmount = rotation * 38;  // Adjust this multiplier for smoothness
                
                verticalScrollBar.setValue(verticalScrollBar.getValue() + scrollAmount);
    }//GEN-LAST:event_jScrollPane1MouseWheelMoved

    private void addressProvinceValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addressProvinceValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_addressProvinceValueActionPerformed

    private void addressStreetValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addressStreetValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_addressStreetValueActionPerformed

    private void addressZipcodeValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addressZipcodeValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_addressZipcodeValueActionPerformed

    private void addressCityValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addressCityValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_addressCityValueActionPerformed

    private void addressBarangayValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addressBarangayValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_addressBarangayValueActionPerformed

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
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(AdminAddNewEmp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(AdminAddNewEmp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(AdminAddNewEmp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AdminAddNewEmp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new AdminAddNewEmp().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private keeptoo.KGradientPanel AddNewEmpGradientPanel;
    private javax.swing.JLabel addNewEmpAllowLabel;
    private javax.swing.JLabel addNewEmpErrorFill;
    private javax.swing.JPanel addNewEmpForm;
    private javax.swing.JLabel addNewEmpLabel;
    private javax.swing.JLabel addNewEmpSuccessCreatedLabel;
    private javax.swing.JLabel addNewEmpWarnBSal;
    private javax.swing.JLabel addNewEmpWarnBirth;
    private javax.swing.JLabel addNewEmpWarnCAllow;
    private javax.swing.JLabel addNewEmpWarnFN;
    private javax.swing.JLabel addNewEmpWarnGSMR;
    private javax.swing.JLabel addNewEmpWarnHRate;
    private javax.swing.JLabel addNewEmpWarnISup;
    private javax.swing.JLabel addNewEmpWarnLN;
    private javax.swing.JLabel addNewEmpWarnPAllow;
    private javax.swing.JLabel addNewEmpWarnPag;
    private javax.swing.JLabel addNewEmpWarnPhil;
    private javax.swing.JLabel addNewEmpWarnPhone;
    private javax.swing.JLabel addNewEmpWarnRSub;
    private javax.swing.JLabel addNewEmpWarnSss;
    private javax.swing.JLabel addNewEmpWarnTin;
    private javax.swing.JLabel addressBarangay;
    private javax.swing.JTextField addressBarangayValue;
    private javax.swing.JLabel addressCity;
    private javax.swing.JTextField addressCityValue;
    private javax.swing.JLabel addressProvince;
    private javax.swing.JTextField addressProvinceValue;
    private javax.swing.JLabel addressStreet;
    private javax.swing.JTextField addressStreetValue;
    private javax.swing.JLabel addressZipcode;
    private javax.swing.JTextField addressZipcodeValue;
    private javax.swing.JLabel empInfo;
    private javax.swing.JLabel exitButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private keeptoo.KGradientPanel kGradientPanel2;
    private javax.swing.JLabel newEmpAddInfo;
    private javax.swing.JLabel newEmpBasicSalary;
    private javax.swing.JTextField newEmpBasicSalaryValue;
    private javax.swing.JLabel newEmpBenefits;
    private javax.swing.JLabel newEmpBirth;
    private com.toedter.calendar.JDateChooser newEmpBirthValue;
    private javax.swing.JButton newEmpCancelButton;
    private javax.swing.JLabel newEmpClothingAllow;
    private javax.swing.JTextField newEmpClothingAllowValue;
    private javax.swing.JLabel newEmpContact;
    private javax.swing.JLabel newEmpDateHired;
    private com.toedter.calendar.JDateChooser newEmpDateHiredValue;
    private javax.swing.JLabel newEmpFirstName;
    private javax.swing.JTextField newEmpFirstNameValue;
    private javax.swing.JLabel newEmpGrossSemi;
    private javax.swing.JTextField newEmpGrossSemiValue;
    private javax.swing.JLabel newEmpHourlyRate;
    private javax.swing.JTextField newEmpHourlyRateValue;
    private javax.swing.JLabel newEmpID;
    private javax.swing.JTextField newEmpIDValue;
    private javax.swing.JLabel newEmpImmediateSup;
    private javax.swing.JTextField newEmpImmediateSupValue1;
    private javax.swing.JLabel newEmpLastName;
    private javax.swing.JTextField newEmpLastNameValue;
    private javax.swing.JLabel newEmpPagIbig;
    private javax.swing.JTextField newEmpPagIbigValue;
    private javax.swing.JLabel newEmpPayInfo;
    private javax.swing.JLabel newEmpPersonalEmail;
    private javax.swing.JTextField newEmpPersonalEmailValue;
    private javax.swing.JLabel newEmpPhilhealth;
    private javax.swing.JTextField newEmpPhilhealthValue;
    private javax.swing.JLabel newEmpPhoneAllow;
    private javax.swing.JTextField newEmpPhoneAllowValue;
    private javax.swing.JLabel newEmpPhoneNum;
    private javax.swing.JTextField newEmpPhoneNumValue;
    private javax.swing.JLabel newEmpPosition;
    private javax.swing.JComboBox<String> newEmpPositionValue;
    private javax.swing.JLabel newEmpRiceSub;
    private javax.swing.JTextField newEmpRiceSubValue;
    private javax.swing.JComboBox<String> newEmpRoleValue;
    private javax.swing.JLabel newEmpSalary;
    private javax.swing.JLabel newEmpSss;
    private javax.swing.JTextField newEmpSssValue;
    private javax.swing.JLabel newEmpStatus;
    private javax.swing.JComboBox<String> newEmpStatusValue;
    private javax.swing.JButton newEmpSubmitButton;
    private javax.swing.JLabel newEmpTin;
    private javax.swing.JTextField newEmpTinValue;
    private javax.swing.JLabel newEmpUserRole;
    // End of variables declaration//GEN-END:variables
}
