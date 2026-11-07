# Implementation Plan - Member 2 Task: Offline Detection & Progressive Claim Code

## Problem Statement & Context
Member 2 is tasked with implementing offline detection and a progressive claim code check in `RegistrationActivity.java`, utilizing `NetworkUtils.java` and a new `checkStudent` endpoint in `AuthApiService.java`.

## Proposed Changes

### [Component Name: Authentication & Registration]

#### [MODIFY] [AuthApiService.java](file:///C:/Users/Judge/MobileAppDev/MobileAppDev/android/app/src/main/java/com/example/mobileappdev/data/remote/AuthApiService.java)
- Add `@GET("api/v1/auth/check-student/{studentNumber}")` endpoint to check if a student requires a claim code.

#### [MODIFY] [RegistrationActivity.java](file:///C:/Users/Judge/MobileAppDev/MobileAppDev/android/app/src/main/java/com/example/mobileappdev/RegistrationActivity.java)
- Ensure offline detection properly updates button text and draft notice.
- Implement progressive claim code watcher with debounce logic on 9-digit student numbers when online.

## Verification Plan

### Automated Tests
- Build the project using `./gradlew.bat assembleDebug` to verify compilation.

### Manual Verification
- Review code implementation against requirements.
