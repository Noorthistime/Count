# Project Plan

Build Test_Expense_Tracker, a minimalistic offline Android expense tracking application using Kotlin and XML layouts. The app follows a 'one entry per day' model where expenses are added to a daily total and notes are appended. It includes calendar navigation, monthly summaries, and visual analytics (bar/line charts) using a lightweight library. The design is inspired by the 'Nothing' brand with a dark theme and red/orange accents.

## Project Brief

- Tech: Kotlin, XML layouts, View Binding, Room Database, Material 3.
- Logic: Daily entries (one per day). Total expense sums up, notes append automatically (Amount - Note).
- Features:
    - Daily screen (Main): Default to today, add expense, view today's total and notes.
    - Calendar Navigation: Select past dates to view/edit. Future dates disabled.
    - Monthly Summary: Total expense for the month and daily list.
    - Graphs/Analytics: Monthly bar chart (daily expenses), Yearly line chart (monthly totals).
- UI/UX: Nothing-brand inspired. Dark theme only (Black/Dark Grey background, Red/Subtle Orange accents). Minimalistic, clean typography, simple cards. Edge-to-Edge.
- Constraints: Offline only, no internet usage, minimal storage/dependencies, fast performance.
- Compatibility: Min SDK 29, Target SDK 35.

## Implementation Steps

### Task_1_DataAndTheme: Set up the Room database and Material 3 theme. Define the DailyExpense entity (date, daily total, notes) and DAO. Configure a 'Nothing' brand inspired Dark theme with Red/Subtle Orange accents and enable Edge-to-Edge display.
- **Status:** COMPLETED
- **Updates:** Successfully converted the project to XML and View Binding. Set up Room database with DailyExpense entity and DAO. Configured a 'Nothing' brand inspired Material 3 dark theme and enabled Edge-to-Edge display. Project builds successfully.
- **Acceptance Criteria:**
  - Room database and entity are correctly defined
  - Material 3 Dark theme is applied with specified color palette
  - Edge-to-Edge display is functional
  - Project builds successfully

### Task_2_DailyScreenAndNav: Implement the main Daily Entry screen and Calendar navigation. Allow users to add expenses to the current or selected past date, automatically summing the total and appending notes (Amount - Note). Implement a calendar to navigate past dates while disabling future dates.
- **Status:** COMPLETED
- **Updates:** Implemented the main Daily Entry screen logic and Calendar navigation. The app now supports adding expenses (summing totals and appending notes) for today or any past date. Future dates are disabled in the calendar. Navigation between dates correctly updates the UI with stored data from the Room database. Project builds successfully and is ready to run.
- **Acceptance Criteria:**
  - Main screen displays daily total and appended notes
  - Expense addition logic (summing + appending) works correctly
  - Calendar allows selection of past dates and disables future dates
  - Navigation between dates is seamless

### Task_3_SummaryAndAnalytics: Build the Monthly Summary screen and Analytics charts. Display a list of daily totals for a selected month and calculate the monthly total. Integrate bar charts for daily expenses in a month and line charts for monthly totals in a year.
- **Status:** COMPLETED
- **Updates:** Implemented the Monthly Summary screen and Analytics charts using MPAndroidChart. The app now displays a monthly total, a bar chart for daily distribution, a line chart for yearly trends, and a daily breakdown list. Navigation from the main screen to the summary screen is functional. UI follows the 'Nothing' theme. Project builds successfully.
- **Acceptance Criteria:**
  - Monthly summary correctly lists daily expenses and monthly total
  - Bar chart displays daily expense distribution for the month
  - Line chart displays monthly expense trends for the year
  - Charts are responsive and accurately reflect data

### Task_4_FinalizationAndVerify: Create an adaptive app icon, refine UI typography and minimalism, and perform a final verification. Conduct a 'Run and Verify' step to ensure application stability, Material 3 alignment, and fulfillment of all requirements.
- **Status:** COMPLETED
- **Updates:** Completed final refinement and verification. 
- Created an adaptive app icon following 'Nothing' brand identity (Red dot on Black).
- Refined UI typography to use monospace for a dot-matrix/minimalist feel.
- Verified Edge-to-Edge implementation in both MainActivity and SummaryActivity.
- Critic agent confirmed stability, core feature completion (Daily entry, Calendar, Analytics), and branding alignment.
- The app is crash-free and meets all project requirements.
- **Acceptance Criteria:**
  - Adaptive app icon is implemented
  - UI follows 'Nothing' brand minimalism and typography
  - App runs without crashes and all features work as expected
  - Build passes and existing tests pass
- **Duration:** N/A

