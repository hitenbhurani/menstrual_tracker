# Experiment 12: Advanced Background Tasks for Periodic and Continuous Processing

## 1. Aim
To architect and implement a robust, enterprise-grade background execution engine using the **Android WorkManager API**. The system is designed to manage multiple, concurrent health-tracking tasks with distinct schedules, hardware constraints, and high-priority execution requirements.

---

## 2. Theory & Concepts

### Background-as-a-Service (BaaS) & WorkManager
In a health-critical application like **FemCare**, certain logic must execute reliably even when the app is closed. **WorkManager** is the industry-standard solution for deferrable, guaranteed background work. It intelligently chooses the best way to run tasks based on the device's API level and battery state.

### Key Advanced Concepts:
*   **PeriodicWorkRequest:** Enables recurring logic loops (e.g., daily health reminders) that persist through app kills and device reboots.
*   **OneTimeWorkRequest:** Handles immediate, high-priority tasks (e.g., data synchronization upon login).
*   **System Constraints:** Logic-based rules (e.g., `NetworkType.CONNECTED`, `BatteryNotLow`) that prevent the app from wasting system resources.
*   **WakeLocks (Power Management):** Ensures the CPU stays active during complex menstrual cycle calculations, preventing process interruption by the OS.
*   **Foreground Services:** Promotes background tasks to high-priority "Foreground" status to prevent the system from killing the process during heavy synchronization.

---

## 3. Implementation Steps

### Step 1: Dependency Integration
The WorkManager library is integrated into the `build.gradle.kts` to access advanced scheduling APIs:
```kotlin
implementation("androidx.work:work-runtime:2.9.0")
```

### Step 2: Developing the Central Worker (`ScheduledNotificationWorker.java`)
Created a centralized `Worker` class that acts as the "Brain" of the background system. It handles:
1.  **WakeLock Acquisition:** Prevents CPU sleep.
2.  **Foreground Info:** Provides system visibility.
3.  **Mode-Based Logic:** Dynamically executes Daily, Cycle, or Weekly checks based on input data.

### Step 3: Architecting the Scheduler (`BackgroundTaskScheduler.java`)
Implemented a dedicated scheduler to manage the lifecycle of all background work:
*   **Daily Log Reminder (24h):** Scheduled at 8:00 PM for maximum user engagement.
*   **Cycle Alerts (6h):** Frequent checks to update period predictions as the day progresses.
*   **Weekly Wellness (7d):** Long-interval tasks for health education.

### Step 4: Lifecycle Registration (`FemCareApplication.java`)
The scheduler is initialized in the `onCreate()` method of the `Application` class. This ensures that the background "heartbeat" is re-established every time the app process starts.

---

## 4. Key Source Code

### A. The "Advanced" Worker Logic
```java
public Result doWork() {
    // Acquire WakeLock to prevent CPU from sleeping
    PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, "FemCare:Sync");
    
    try {
        wakeLock.acquire(10 * 60 * 1000L); // 10 min timeout
        
        // If immediate sync, run as a Foreground Service
        if (MODE_IMMEDIATE.equals(mode)) {
            setForegroundAsync(createForegroundInfo(context));
        }

        // Execute core health logic...
    } finally {
        if (wakeLock.isHeld()) wakeLock.release();
    }
}
```

### B. Periodic Scheduling with Constraints
```java
PeriodicWorkRequest cycleAlerts = new PeriodicWorkRequest.Builder(
        ScheduledNotificationWorker.class, 6, TimeUnit.HOURS)
    .setConstraints(new Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build())
    .build();

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "work_cycle_alerts", ExistingPeriodicWorkPolicy.KEEP, cycleAlerts);
```

---

## 5. Technical Defense (Q&A for Evaluators)

**Q1: What makes this "Advanced" rather than just a simple timer?**
> **Answer:** It uses **persistence** and **constraints**. Unlike a timer, this survives phone restarts and app crashes. It also respects the device state (e.g., it won't run if the battery is critically low or if there is no internet).

**Q2: How do you handle "Continuous" processing?**
> **Answer:** By utilizing `ExistingPeriodicWorkPolicy.KEEP` and self-healing loops. The WorkManager stores task states in a local SQLite database, ensuring the cycle tracking logic continues indefinitely.

**Q3: Why use WakeLocks?**
> **Answer:** Menstrual cycle predictions involve complex date math and Firestore network calls. The **WakeLock** ensures the Android system doesn't put the CPU to sleep in the middle of these calculations.

---

## 6. Conclusion
The implementation of Experiment 12 provides **FemCare** with a professional, proactive background engine. By moving beyond foreground-only execution, the app effectively becomes a **"Smart Health Assistant"** that tracks data and sends alerts autonomously, fulfilling all requirements for advanced periodic and continuous processing.