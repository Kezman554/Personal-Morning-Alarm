Claude · MDCopyProject: Personal Morning Alarm
Two-stage Android alarm app with NFC checkpoints, toggleable content screens, and streak tracking.
Built with Kotlin, MVVM, Room, Android NFC API, CameraX, ML Kit.
Structure

app/src/main/java/com/personalmorningalarm/data/ - Room entities, DAOs, repository
app/src/main/java/com/personalmorningalarm/ui/ - Activities, fragments, viewmodels
app/src/main/java/com/personalmorningalarm/service/ - Alarm, countdown, foreground services
app/src/main/java/com/personalmorningalarm/receiver/ - Boot and alarm broadcast receivers
app/src/main/java/com/personalmorningalarm/challenge/ - Shake, NFC, math, typing challenges
app/src/main/java/com/personalmorningalarm/util/ - Helpers, constants
app/src/main/res/ - Layouts, strings, drawables, themes
docs/ - PRD and progress log

Commands

./gradlew assembleDebug - Build debug APK
./gradlew installDebug - Build and install on connected device
./gradlew test - Run unit tests
./gradlew connectedAndroidTest - Run instrumented tests

Git

Do not push to GitHub without explicit permission
Commit after completing each session
Update docs/progress.txt briefly if significant work was done

Conventions

Min API level 26, target API 34
Primary test device: Samsung Galaxy A32 5G (API 30)
MVVM architecture with Repository pattern throughout
Public repo — never commit credentials, tokens, personal data, or private network details
Foreground services for alarm and countdown reliability
Extensible challenge system — new challenges implement a common interface

Reference

Requirements: docs/wake-up-alarm_PRD.md
Progress log: docs/progress.txt
Task prompts: Kanban app