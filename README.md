# FemCare - Personal Menstrual Cycle Tracker 🌸

**FemCare** is a comprehensive, privacy-focused Android application designed to help users track their menstrual cycles, monitor health symptoms, and gain insights into their reproductive health. Built with a modern tech stack, the app provides a seamless experience from onboarding to daily logging and historical analysis.

---

## 🚀 Key Features

### 📅 Smart Cycle Tracking
- **Interactive Calendar:** Visualizes cycle phases (Period, Fertile Window, Ovulation) with color-coded indicators.
- **Dynamic Predictions:** Automatically calculates and updates future cycle dates based on user history and averages.
- **Onboarding Engine:** Personalized setup flow for cycle length, period duration, and last period start date.

### 📝 Daily Health Logs & Insights
- **Symptom Tracking:** Log physical symptoms (cramps, fatigue, headache) and moods with a single tap.
- **Visual Trends:** Integrated **Bar Charts** (using MPAndroidChart) to visualize symptom frequency over the last 7 days.
- **Water Tracker:** Interactive counter to monitor daily hydration levels.
- **Daily Tips:** Personalized health insights based on the current cycle phase.

### 📓 Persistent Notes
- **Private Journaling:** Add detailed notes for any specific date on the calendar.
- **Recent History:** A dedicated "Recent Notes" section to quickly review past entries without navigating deep into the calendar.

### ⚙️ User Experience & UI
- **Responsive Design:** Built using `ConstraintLayout` and `CoordinatorLayout` for a consistent look across various screen sizes.
- **Gesture-Driven:** Supports horizontal and vertical swipes for navigating months and adjusting values.
- **Material Design:** Follows Material 3 principles with custom pink-themed styling.

---

## 🛠 Tech Stack

- **Language:** Java (100%)
- **Architecture:** MVVM (Model-View-ViewModel)
- **UI Framework:** XML (Material Components)
- **Authentication:** Firebase Auth (Email/Password Login)
- **Backend/Database:** 
  - **Firebase Firestore:** Real-time cloud storage for user profiles and logs.
  - **Room Database:** Local SQLite persistence for offline access.
- **Networking:** Retrofit & GSON for REST API interactions.
- **Libraries:**
  - `MPAndroidChart` for health analytics.
  - `WorkManager` for background tasks and period reminders.
  - `GestureDetector` for advanced touch interactions.

---

## 🏗 Project Architecture (Experiments Implemented)

This project serves as a comprehensive portfolio for **Mobile Application Development**, covering:
1. **Intents & Activity Lifecycle:** Seamless navigation between 10+ activities.
2. **Fragments:** Modular UI for Dashboard navigation.
3. **Local Storage:** `SharedPreferences` for flags and `Room` for offline data.
4. **Cloud Integration:** Firebase for Auth and Firestore for cloud sync.
5. **Event-Driven UI:** Custom gesture handling and button interactions.
6. **Data Visualization:** Transforming raw logs into readable bar charts.

---

## 📲 Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/hitenbhurani/menstrual_tracker.git
   ```
2. Open the project in **Android Studio (Ladybug or later)**.
3. Add your own `google-services.json` in the `app/` folder to connect to your Firebase instance.
4. Build and run on an emulator or physical device.

---

## 🤝 Contributing
This is a college project. Suggestions and feedback are welcome! Feel free to fork the repository and submit a pull request.

---

**Developed by:** [Hiten Bhurani](https://github.com/hitenbhurani)
