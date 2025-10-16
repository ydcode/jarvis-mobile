# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Jarvis** is an Android application built with Kotlin and Jetpack Compose. The app applies GitHub's workflow paradigm (Repo/Branch/PR/Issue/Label/Milestone) to schedule management, featuring a multi-scale timeline view that supports zooming across different time scales (from minutes to centuries).

**Key Technologies:**
- Language: Kotlin
- UI Framework: Jetpack Compose with Material 3
- Navigation: Navigation Compose
- Min SDK: 29, Target SDK: 36
- Java Version: 11

## Design Philosophy: GitHub Paradigm for Schedule Management

Jarvis adopts GitHub's collaborative development workflow as the foundation for task and schedule management. This paradigm provides a familiar, powerful structure for organizing life and work.

### Core Concepts

#### 1. **Repo (Repository)**
- Represents different areas of life: work projects, personal growth, family affairs, etc.
- Branch is for content version control within the Repo, separate from Calendar

#### 2. **Discussion**
- Ideas, brainstorming, thoughts that need exploration
- Not necessarily immediately actionable
- Can evolve into Issues

#### 3. **Issue**
- Actionable tasks/items that need resolution
- Can be associated with one Schedule (0 or 1)
- Internal features:
  - Discussion area (like GitHub Issue comments)
  - Can be decomposed into N Steps (subtasks)
  - Each Step can also be associated with a Schedule

#### 4. **Schedule (Scheduling System)** - Core Concept
An independent object that can be associated with any entity (Issue, Step, Milestone, etc.)

**Schedule Types:**
- **Start-only**: Only start time is set
- **Deadline-only**: Only deadline is set
- **Time Range**: Both start time and end time
- **Recurring**:
  - Daily
  - Weekly
  - Weekly on specific days (e.g., Mon, Wed, Fri)
  - Monthly on date (e.g., 15th of each month)
  - Monthly on Nth weekday (e.g., 2nd Monday of each month)
  - Custom cycles

**Schedule Mechanism:**
```
Any Entity + Schedule → Calendar Events
```

**Conflict Resolution:**
- When Issue and its Steps' Schedules conflict:
  - System prompts based on conflict type
  - User chooses resolution
  - System auto-updates relevant Schedules to ensure consistency

#### 5. **Calendar Events**
- Concrete time-point events generated from Schedules

**Event Solidification Mechanism:**
- **Auto-solidify when time arrives**: Recurring tasks auto-solidify when scheduled time is reached
- **Solidify on manual edit**: When user modifies an unsolidified Event, it immediately solidifies to save the change
- **Purpose of solidification**:
  - Enables statistical analysis
  - Allows editing individual event details without affecting Schedule rules
  - Preserves user customizations

**Overdue Event Handling:**
- Incomplete overdue events are handled manually by users
- System doesn't auto-delete or mark, maintaining flexibility

**Display Strategy:**
- Display determined by time span and importance level
- Short events show fully in fine-grained timelines (minute/hour)
- Long events show as time blocks in coarse-grained timelines (week/month/year)
- Importance level affects visual weight and priority

**Importance Level:**
- Can inherit from associated entity (e.g., Issue priority)
- Can also be set individually at Event level
- Supports flexible priority management

#### 6. **Label**
- Event classification: meeting, personal, work, urgent, etc.
- Priority: P0/P1/P2 or High/Medium/Low
- Status: in progress, completed, cancelled, etc.

#### 7. **Milestone**
- Important deadlines
- Project phase goals
- Long-term goal checkpoints
- Can be associated with Schedule

#### 8. **PR (Pull Request) & Branch**
- (Placeholder for future enhancement)
- Branch: Repo content version management
- PR: Review mechanism for plan changes

### Data Structure

```
Repo
├── Discussions
├── Issues
│   ├── Discussion area (Comments)
│   ├── Steps (subtasks)
│   │   └── Schedule (0..1)
│   └── Schedule (0..1)
├── Labels
├── Milestones
│   └── Schedule (0..1)
├── Branches (content versioning, not Calendar-related)
└── PRs (placeholder)

Independent Layer:
Schedule (independent object, can associate with any entity)
├── Type Configuration
│   ├── Start-only
│   ├── Deadline-only
│   ├── Time Range
│   └── Recurring
│       ├── Daily
│       ├── Weekly
│       ├── Weekly on specific days
│       ├── Monthly on date
│       └── Monthly on Nth weekday
└── Generates → Calendar Events

Calendar (Timeline View)
└── Events
    ├── Future events (dynamically generated from Schedule)
    └── Historical events (solidified and stored)
        ├── Auto-solidified when time arrives
        └── Solidified when user modifies
```

### Workflow

```
1. Idea → Execution
   Discussion → Issue → Associate Schedule → Generate Calendar Events

2. Task Decomposition
   Issue → Decompose into Steps → Steps associate Schedules → Generate Events

3. Conflict Handling
   Issue vs Steps Schedule conflict → System prompts → User chooses → Auto-update for consistency

4. Event Solidification
   - Recurring task time arrives → Event solidifies
   - User modifies unsolidified Event → Immediately solidify
   - Solidified events can be independently modified → Used for analytics

5. Overdue Handling
   Incomplete overdue events → User handles manually

6. Multi-scale Display
   Events based on time span and importance → Intelligent display across time scales

7. Importance Management
   Event importance ← Can inherit from entity OR set individually
```

### Design Principles

1. **Flexible Schedule System**
   - Schedule as independent object, can associate with any entity
   - Supports multiple time patterns for different scenarios

2. **Intelligent Conflict Resolution**
   - System detects and prompts conflicts
   - User makes final decisions
   - Auto-maintains data consistency

3. **Event Solidification Strategy**
   - Time-driven auto-solidification
   - User-edit-triggered solidification
   - Preserves historical data for analysis

4. **User-driven Flexibility**
   - Users manually handle overdue events
   - Importance can be inherited or customized
   - Maintains system controllability

5. **Extensibility Reserved**
   - New entity types to be discussed in future
   - Branch/PR mechanisms placeholder for enhancement
   - Maintains architectural extensibility

## Build Commands

```bash
# Build the project
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Install debug build to device
./gradlew installDebug

# Clean build
./gradlew clean

# Check for dependency updates
./gradlew dependencyUpdates
```

## Architecture

### Package Structure

```
com.jarvis/
├── data/
│   └── model/           # Data models (CalendarEvent, TodoItem, TimeScale)
├── navigation/          # Navigation setup and screen routes
│   ├── BottomNavItem.kt
│   ├── NavGraph.kt
│   └── Screen.kt
├── ui/
│   ├── components/      # Reusable UI components
│   ├── screens/         # Screen-level composables
│   └── theme/          # App theming
└── MainActivity.kt      # Entry point
```

### Multi-Scale Timeline System

The app implements a sophisticated multi-scale timeline view that allows zooming from minute-level detail to century-level overview. This is the core architectural feature:

**Key Components:**

1. **TimeScaleLevel (ui/components/TimelineState.kt:11-142)** - Enum defining 12 time scale levels:
   - MINUTE → FIVE_MINUTES → TEN_MINUTES → HALF_HOUR → HOUR → DAY → WEEK → MONTH → QUARTER → YEAR → DECADE → CENTURY
   - Each level knows its ratio to the next level and can convert positions between scales

2. **TimeScaleConverter (ui/components/TimeScaleConverter.kt)** - Bidirectional conversion utilities:
   - `scaleUnitsToCalendar()`: Converts scale units to absolute Calendar time
   - `calendarToScaleUnits()`: Converts absolute Calendar time to scale units
   - Handles different coordinate systems (e.g., DAY scale: i=0 is today, YEAR scale: i=10 is current year)

3. **CanvasTimelineView (ui/components/CanvasTimelineView.kt)** - Main timeline rendering:
   - Implements pinch-to-zoom gesture for scale transitions
   - Smooth scale interpolation during zoom animations
   - Coordinate system transformations for different time scales

4. **TimelineDrawingUtils (ui/components/TimelineDrawingUtils.kt)** - Drawing utilities:
   - Renders time labels, grid lines, and event cards
   - Scale-aware formatting (minutes for fine scales, years for coarse scales)

**Coordinate System Notes:**
- Most scales use i=0 as the reference point (today's 00:00, current week start, etc.)
- YEAR, DECADE, CENTURY scales use i=10 as current time to allow negative indexing
- Position conversions handle fractional units to represent sub-scale precision

### Navigation Structure

The app uses a bottom navigation bar with 4 main screens defined in `navigation/Screen.kt`:
1. **Todo** - Timeline view with todo items (default start destination)
2. **Contacts** - Contacts management
3. **Discover** - Discovery features
4. **Me** - User profile/settings

Navigation is managed in `ui/MainScreen.kt` with a `scrollToNowTrigger` mechanism that allows double-clicking the Todo tab to scroll the timeline to the current time.

## Development Notes

### Testing

- Unit tests: Located in `app/src/test/java/com/jarvis/`
- Instrumented tests: Located in `app/src/androidTest/java/com/jarvis/`
- Test runner: `androidx.test.runner.AndroidJUnitRunner`

### Dependency Management

Dependencies are managed using Gradle Version Catalogs (`gradle/libs.versions.toml`). To add a new dependency:
1. Add version to `[versions]` section
2. Add library to `[libraries]` section
3. Reference in `app/build.gradle` as `implementation libs.library.name`

### UI Development

- All UI is built with Jetpack Compose
- Material 3 design system
- Edge-to-edge display with transparent status bar (light icons)
- Custom canvas-based timeline components for performance

### Time Handling

When working with time-related features:
- Always use the `TimeScaleConverter` utilities for Calendar conversions
- Be aware of different coordinate systems for different scales
- The timeline supports fractional positions for sub-scale precision
- Current time indicators use the device's Calendar with timezone handling
