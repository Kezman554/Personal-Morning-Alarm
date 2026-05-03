Wake up alarm prd · MDCopyPersonal Morning Alarm - Product Requirements Document
Version: 1.0
Last Updated: April 2026
Overview
Problem Statement
Traditional alarms are too easy to dismiss, leading to snoozing and skipped morning goals. Users need a system that physically gets them out of bed, builds momentum through a guided wake-up sequence, and creates accountability through streak tracking — without disturbing household members.
Goals

Reliably wake the user and get them physically moving through their space
Create progressive accountability through multi-checkpoint dismissal
Track streaks and success patterns to reinforce consistency
Keep Stage 1 quiet enough to avoid disturbing a partner
Build an extensible system that supports future integrations

Target Users
Personal use. A single user who sets optional early morning goals (exercise, project work) but struggles with snoozing and needs structured accountability to follow through.
Features
Stage 1: Gentle Wake

Soft alarm at scheduled time, moderate volume
Shake phone vigorously for 15 seconds to dismiss
Visual progress bar and haptic feedback during shake
Prevents back-button exit and shows over lock screen

Stage 2: Guided Wake-Up Sequence

Configurable NFC checkpoint system (user sets number of tags)
Must tap NFC tags placed around the home to fully dismiss
Tap order randomised each morning to prevent autopilot
Content screens displayed between taps (toggleable per screen):

Motivational quote (bundled pool)
Stretching prompt (5 or 10 min timer)
Additional content slots for future use


Countdown timer runs during sequence
If timer expires before all taps complete, nuclear alarm activates

Nuclear Alarm (Stage 2 Failure)

Maximum volume, strong vibration, optional flashlight strobe
Requires ALL of: extended shake (30s), math problem, NFC tap, typed confirmation phrase
Logged as failed wake-up, streak resets

Content Toggle System

Settings screen to enable/disable each content type
Configured the evening before alarm is due
V1: bundled quotes and stretching timer
Future: external data via API integration

Data & Streaks

Daily success/failure logging with timestamps
Current and longest streak tracking
Weekly success rate
Statistics display on home screen and dedicated stats screen

Settings

Alarm time, enable/disable, Stage 2 timer duration (5-15 min)
NFC tag registration and management (configurable count)
Sound selection for Stage 1 and nuclear alarm
Content screen toggles
Dark/light theme

Scope
In Scope

Two-stage alarm with NFC checkpoints
Bundled static content (quotes, stretching prompts)
Local data storage and streak tracking
Configurable NFC tag count and content toggles
Battery optimisation prompts

Out of Scope

Cloud sync or multi-device support
Social features or leaderboards
Sleep tracking
iOS version
Widget or Wear OS support

Future Considerations

External API integration for personalised content (daily tasks, rolling to-do list, curated quotes/images)
Additional challenge types (photo verification, step counter, voice recording)
Challenge pool system with random selection
Gamification (XP, achievements, levels)
Smart difficulty adjustment based on patterns

Technical
Stack

Kotlin: Primary language
MVVM: Architecture pattern
Room: Local SQLite database
CameraX / ML Kit: QR scanning (nuclear alarm only)
Android NFC API: Checkpoint tag reading
AlarmManager: Alarm scheduling
Foreground Service: Alarm and countdown reliability
Material Design: UI components

Integrations

None for V1 (fully self-contained)
Future: External REST API for personalised content

Constraints

Min API level 26 (Android 8.0), primary test device API 30
Must work offline (no network dependency at alarm time)
Battery usage < 5% overnight
App size < 20MB
Must survive device restart (reschedule alarms on boot)
Public repository — no personal data, tokens, or private network details in code

Project Structure
PersonalMorningAlarm/
├── docs/
│   ├── wake-up-alarm_PRD.md
│   └── progress.txt
├── CLAUDE.md
├── app/
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/personalmorningalarm/
│           │       ├── data/          # Room entities, DAOs, repository
│           │       ├── ui/            # Activities, fragments, viewmodels
│           │       ├── service/       # Alarm, countdown, foreground services
│           │       ├── receiver/      # Boot, alarm broadcast receivers
│           │       ├── challenge/     # Shake, NFC, math, typing challenges
│           │       └── util/          # Helpers, constants
│           └── res/                   # Layouts, strings, themes
├── build.gradle
└── .gitignore
Success Criteria

 Alarm triggers reliably at scheduled time, including after device restart
 Stage 1 shake detection works accurately with clear progress feedback
 NFC tag registration and reading works consistently
 Tap order randomises each morning
 Content screens display between taps with correct toggle respect
 Nuclear alarm activates on Stage 2 timeout and requires all challenges
 Streak tracking persists correctly across sessions
 App does not wake household members during Stage 1
 Runs reliably on Samsung Galaxy A32 5G
 Repository contains no personal data or credentials