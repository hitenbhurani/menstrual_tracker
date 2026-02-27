# FemCare – Menstrual Tracker & Health Management System
## Class Diagram Documentation

### 1. Introduction
FemCare is a comprehensive health-tracking application designed to help users monitor their menstrual cycles, log daily symptoms, receive smart health notifications, and export medical data for healthcare professionals.

This document explains the class structure, database design, and relationships used in the system, specifically highlighting Role-Based Access Control (RBAC), user activity logging, and the core tracking entities.

---

### 2. Login and Role-Based Access Control (RBAC) Module
To ensure secure access and distinguish between standard users and system administrators, FemCare utilizes a 4-table RBAC architecture.

**2.1 User**
Represents a registered user of the system (Firebase Auth integrated).
* **Attributes:** `userId`, `email`, `passwordHash`, `createdAt`, `isActive`
* **Methods:** `register()`, `login()`

**2.2 Role**
Defines the access levels within the app (e.g., Standard_User, Premium_User, Admin).
* **Attributes:** `roleId`, `roleName`, `description`

**2.3 UserRole**
A mapping table that assigns specific roles to users.
* **Attributes:** `userId`, `roleId`, `assignedAt`

**2.4 Permission**
Defines granular access rights (e.g., "Export_PDF", "Access_Admin_Panel").
* **Attributes:** `permissionId`, `permissionName`

---

### 3. User Activity Logging Module
To fulfill analytics and monitoring requirements, user interactions and session times are logged.

**3.1 UserActivityLog**
Tracks user actions, session durations, and engagement metrics for analytics.
* **Attributes:** `logId`, `userId`, `actionType` (e.g., "Logged Symptom", "Generated PDF"), `sessionDurationSecs`, `timestamp`, `deviceIp`
* **Methods:** `recordActivity()`, `calculateScreenTime()`

---

### 4. Core Application Entities (FemCare Specifics)
These 8 tables handle the primary business logic, tracking, and notification systems of the application.

**4.1 UserProfile**
Stores onboarding data and baseline cycle averages.
* **Attributes:** `profileId`, `userId`, `birthDate`, `typicalCycleLength`, `typicalPeriodLength`

**4.2 CycleTracker**
Manages individual menstrual cycles and AI-driven predictions.
* **Attributes:** `cycleId`, `userId`, `startDate`, `endDate`, `predictedNextDate`
* **Methods:** `calculatePrediction()`, `endCycle()`

**4.3 DailyLog**
Represents a user's daily check-in for flow and mood.
* **Attributes:** `logId`, `cycleId`, `logDate`, `flowIntensity`, `mood`, `notes`
* **Methods:** `createLog()`, `updateLog()`

**4.4 Symptom**
A catalog of trackable conditions (e.g., Cramps, Fatigue, Bloating).
* **Attributes:** `symptomId`, `name`, `category` (Uneasiness, Lifestyle, Reproductive)

**4.5 LogSymptomMapping**
A bridge table connecting a `DailyLog` to multiple `Symptoms` with severity levels.
* **Attributes:** `logId`, `symptomId`, `severityLevel`

**4.6 SmartNotification**
Manages automated alerts for upcoming cycles or daily log reminders.
* **Attributes:** `notificationId`, `userId`, `type`, `message`, `triggerDate`, `isRead`

**4.7 DoctorReport**
Handles the generation and tracking of exported PDF health reports.
* **Attributes:** `reportId`, `userId`, `generatedDate`, `pdfUrl`, `status`
* **Methods:** `generatePdf()`, `shareReport()`

**4.8 UserSettings**
Manages app-wide user preferences.
* **Attributes:** `settingsId`, `userId`, `notificationsEnabled`, `themePreference`

---

### 5. Relationships Summary
* **RBAC:** `User` (1) —— (M) `UserRole` —— (1) `Role`
* **Logging:** `User` (1) —— (M) `UserActivityLog`
* **Tracking:** `User` (1) —— (M) `CycleTracker`
* **Daily Data:** `CycleTracker` (1) —— (M) `DailyLog`
* **Symptoms:** `DailyLog` (1) —— (M) `LogSymptomMapping` —— (1) `Symptom`
* **Features:** `User` (1) —— (M) `DoctorReport`

### 6. System Design Highlights
* **Role-Based Security:** Allows for secure feature gating (e.g., premium users unlocking PDF exports).
* **Granular Activity Tracking:** `UserActivityLog` monitors session length and feature usage for UI/UX improvements.
* **Scalable Health Data:** Separating `Symptom` catalogs from `DailyLog` using a mapping table ensures users can select multiple symptoms per day efficiently.