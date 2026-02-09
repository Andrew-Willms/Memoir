The following is the task breakdown for the week ahead to prepare for P4

Front-End:
Core Screens to Build:
Home / Daily Review Screen
Tasks:
- Load today's photos (mock data OK)
- Show photo carousel/grid
- "Write journal entry" button
- Tag button

Journal Editor Screen
Tasks:
- Display photo
- Text input for reflection
- Add tags
- Select album to share
- Requirement/NFR: journaling should be done within same screen in minimal interactions

Albums / Sharing Screen
Tasks:
- Create shared album
- Add friends (mock list OK)
- View shared memories

Search Screen
Tasks:
Filter by:
- tags
- date
- people

Platform:
Photo Access Layer (Device Integration)
Tasks:
- Access device photos (Android API)
- Filter photos by date
- Load thumbnails

Backend:
Journaling Data Model
Tasks:
- save journal entry locally
- edit journal
- retrieve entries

Local Storage / Offline Logic
Tasks:
- local DB (Room / SQLite)
- save draft entries offline
- load cached entries

Sharing / Album System (Mock Backend)
Tasks:
- album model
- invite users (fake users ok)
- add entries to album
Fake backend options:
- local JSON
- Firebase mock
- in-memory repository

Notification System (Daily Prompt)
Tasks:
- scheduled notification
- opens daily review screen

Search Engine
Tasks:
- filter by tags
- filter by date
- keyword search journal text

Architecture + Diagrams (Presentation Deliverable)
Build slides showing:
- Mobile client
- Local database
- Sharing service
- Notification service
