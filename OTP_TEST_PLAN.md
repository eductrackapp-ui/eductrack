# OTP Authentication System Test Plan

## Overview
This document outlines the comprehensive testing strategy for the email-based OTP authentication system implemented in the EduTrack Android application.

## Test Environment Setup
- **EmailJS Service**: service_yvl11d5
- **EmailJS Template**: template_zlp263e
- **Firebase Project**: EduTrack
- **Test Users**: Admin and Regular users in Firestore

## Test Scenarios

### 1. OTP Login Flow - Regular User
**Objective**: Verify that regular users can successfully authenticate using OTP

**Prerequisites**:
- User exists in Firestore with role "parent" or "teacher"
- User has valid email address
- EmailJS service is configured

**Test Steps**:
1. Launch the app and navigate to LoginActivity
2. Enter valid email address
3. Click "Se connecter avec OTP" button
4. Verify UI switches to OTP mode (password field hidden, info text shown)
5. Click "Envoyer le code OTP" button
6. Verify loading state is shown
7. Check email for OTP code
8. Navigate to VerificationCodeActivity
9. Enter the 6-digit OTP code
10. Click "Vérifier le code" button
11. Verify successful login and redirect to appropriate home activity

**Expected Results**:
- OTP email received within 30 seconds
- OTP code is 6 digits
- Successful authentication redirects to correct home activity based on user role
- User session is established in Firebase Auth

### 2. OTP Login Flow - Admin User
**Objective**: Verify that admin users can successfully authenticate using OTP

**Prerequisites**:
- User exists in Firestore with role "admin"
- User has valid email address
- EmailJS service is configured

**Test Steps**:
1. Launch the app and navigate to LoginActivity
2. Enter admin email address
3. Click "Se connecter avec OTP" button
4. Click "Envoyer le code OTP" button
5. Check email for admin-specific OTP template
6. Navigate to VerificationCodeActivity
7. Enter the 6-digit OTP code
8. Click "Vérifier le code" button
9. Verify successful login and redirect to AdminHomeActivity

**Expected Results**:
- Admin-specific OTP email template is used
- Successful authentication redirects to AdminHomeActivity
- Admin privileges are maintained

### 3. OTP Validation and Security
**Objective**: Verify OTP security measures and validation

**Test Cases**:

#### 3.1 Invalid OTP Code
- Enter incorrect 6-digit code
- Verify error message is displayed
- Verify OTP fields are cleared
- Verify user can retry

#### 3.2 Expired OTP Code
- Wait for OTP to expire (5 minutes)
- Enter expired OTP code
- Verify appropriate error message
- Verify user can request new OTP

#### 3.3 OTP Resend Functionality
- Request OTP
- Wait for 60-second cooldown
- Click "Renvoyer le code" button
- Verify new OTP is sent
- Verify countdown timer resets

### 4. UI/UX Validation
**Objective**: Verify user interface and experience

**Test Cases**:

#### 4.1 Mode Switching
- Verify smooth transition between password and OTP modes
- Verify button text changes appropriately
- Verify UI elements show/hide correctly

#### 4.2 Input Validation
- Test email format validation
- Test empty field validation
- Test OTP field auto-focus behavior
- Test backspace handling in OTP fields

#### 4.3 Loading States
- Verify loading indicators during OTP sending
- Verify loading indicators during OTP verification
- Verify button states during loading

### 5. Error Handling
**Objective**: Verify robust error handling

**Test Cases**:

#### 5.1 Network Errors
- Test with no internet connection
- Test with poor network conditions
- Verify appropriate error messages

#### 5.2 EmailJS Service Errors
- Test with invalid EmailJS configuration
- Test with service downtime
- Verify graceful error handling

#### 5.3 Firebase Errors
- Test with invalid user data
- Test with Firestore connection issues
- Verify appropriate error messages

### 6. Integration Testing
**Objective**: Verify integration with existing systems

**Test Cases**:

#### 6.1 Firebase Auth Integration
- Verify user session creation
- Verify user data synchronization
- Verify role-based redirections

#### 6.2 Terms of Use Integration
- Test with users who haven't accepted terms
- Verify redirect to TermsOfUseActivity
- Verify proper flow after terms acceptance

### 7. Performance Testing
**Objective**: Verify system performance

**Test Cases**:

#### 7.1 OTP Delivery Time
- Measure time from OTP request to email delivery
- Target: < 30 seconds

#### 7.2 Authentication Speed
- Measure time from OTP verification to home screen
- Target: < 5 seconds

#### 7.3 UI Responsiveness
- Verify smooth animations and transitions
- Verify no UI freezing during operations

## Test Data Requirements

### Test Users in Firestore
```json
{
  "testAdmin": {
    "email": "admin@test.com",
    "username": "Test Admin",
    "role": "admin",
    "acceptedTerms": true
  },
  "testTeacher": {
    "email": "teacher@test.com",
    "username": "Test Teacher",
    "role": "teacher",
    "acceptedTerms": true
  },
  "testParent": {
    "email": "parent@test.com",
    "username": "Test Parent",
    "role": "parent",
    "acceptedTerms": false
  }
}
```

## Success Criteria
- All test scenarios pass without critical issues
- OTP delivery time < 30 seconds
- Authentication completion time < 5 seconds
- Error handling is graceful and informative
- UI/UX is smooth and intuitive
- Security measures are properly implemented

## Risk Assessment
- **High Risk**: EmailJS service availability
- **Medium Risk**: Network connectivity issues
- **Low Risk**: UI/UX minor issues

## Test Execution Schedule
1. **Phase 1**: Basic functionality testing (OTP flow)
2. **Phase 2**: Security and validation testing
3. **Phase 3**: Integration and performance testing
4. **Phase 4**: User acceptance testing

## Bug Reporting
- Use GitHub issues for bug tracking
- Include screenshots and logs
- Specify device and Android version
- Provide steps to reproduce

## Sign-off Criteria
- All critical and high-priority bugs resolved
- Performance targets met
- Security requirements satisfied
- User acceptance criteria met