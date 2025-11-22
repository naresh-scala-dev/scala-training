# Event Management & Notification Backend

A Scala/Play Framework backend for managing events, users, teams, and tasks, with JWT-based authentication, role-based authorization, and an email notification pipeline powered by Kafka and Akka actors.

- REST API design with Play Framework
- JWT auth with roles
- Database access with Slick
- Background scheduling & Kafka-based event processing
- Email notifications via pluggable email modes (console or SMTP)

## Features

- **Authentication & Authorization**
  - Login via `/api/login` using username/password
  - JWT tokens with expiry and role embedded in the token
  - Role-based access control using a custom `AuthAction` (`EventManager`, `TeamMember`, etc.)

- **User (EventUser) Management**
  - CRUD APIs for event users
  - Uniqueness checks for username/email

- **Event Management**
  - CRUD APIs for events
  - Duplicate event prevention (same name + date)
  - Timestamps for creation and updates

- **Team Management**
  - CRUD APIs for teams
  - Assign/remove users to/from teams

- **Task Management**
  - CRUD APIs for tasks associated with events and teams
  - Prevents duplicate tasks for same event/team/description
  - Tracks status, start/end times, and special requests

- **Notification Pipeline**
  - `KafkaProducerService` sends task-related events (assignment, status change, reminders, etc.)
  - `KafkaConsumerActor` consumes messages from Kafka topic and dispatches them to actors
  - `TaskActor` and `EventActor` interpret task events and forward them to `NotificationActor`
  - `NotificationActor` calls `EmailService` to send emails
  - Background scheduler periodically:
    - Sends reminders before task start
    - Sends progress check emails for in-progress tasks
    - Sends event-day alerts to team members and event manager

- **Email Delivery**
  - Configurable email mode:
    - `console` – print emails to console (good for development)
    - `smtp` – send real emails via SMTP server (host/user/pass configurable)

## Tech Stack

- **Language:** Scala
- **Framework:** Play Framework
- **Database:** MySQL
- **Messaging:** Apache Kafka
- **Concurrency & Actors:** Akka actors (`ActorSystem`, custom actors)
- **Auth:** JWT (using `com.auth0.jwt`)
- **Email:** Jakarta Mail (`jakarta.mail`) via custom `EmailService`


## Project Structure (High-Level)

Main relevant components based on the code:

- `controllers`
  - `AuthController` – login, issues JWT
  - `EventController` – CRUD for events
  - `EventUserController` – CRUD for event users
  - `TeamController` – CRUD for teams, assign/remove users
  - `TaskController` – CRUD for tasks, publishes Kafka messages on create/update

- `security`
  - `AuthAction` – custom `ActionBuilder` that:
    - Extracts JWT from `Authorization: Bearer <token>`
    - Verifies token via `JWTUtils.verifyToken`
    - Injects `EventUserRequest` into the request with username and role
  - `JWTUtils` – create & verify JWT tokens

- `models`
  - `Event`, `EventUser`, `Team`, `Task`, etc.
  - Corresponding Slick table classes: `EventTable`, `EventUserTable`, `TeamTable`, `TaskTable`, `TeamUserTable`, etc.
  - `EventUserRequest` – wrapper request containing authenticated user data

- `dto`
  - `LoginDTO` – username/password for login
  - `EventCreateDTO`, `EventUpdateDTO`
  - `EventUserCreateDTO`, `EventUserUpdateDTO`
  - `TeamCreateDTO`, `TeamUpdateDTO`, `TeamAssignUserDTO`
  - `TaskCreateDTO`, `TaskUpdateDTO`
  - `ApiResponse` – standard wrapper for API responses

- `repositories`
  - `EventUserRepository` – user lookup by username/password
  - `TeamUserRepository` – lookup emails for users in a team
  - Other task-related repos (e.g. `taskRepo`, `userRepo`) used in schedulers

- `services`
  - `KafkaProducerService` – sends JSON events to Kafka
  - `EmailService` – sends emails (console mode or real SMTP)

- `actors`
  - `KafkaConsumerActor` – reads events from Kafka topic and parses JSON into `TaskEvent`
  - `TaskActor` – processes task-related events (`TASK_ASSIGNMENT`, `STATUS_UPDATE`, `REMINDER`, etc.)
  - `EventActor` – processes final event-day alerts (`FINAL_ALERT`)
  - `NotificationActor` – translates events into email calls (`SendTaskNotification`, `SendTaskStatusUpdate`, `SendTaskReminder`, `SendEventAlert`)

- `Main`
  - Bootstraps `ActorSystem`
  - Creates `EmailService`
  - Spawns `NotificationActor`, `TaskActor`, `EventActor`, and `KafkaConsumerActor`
  - Registers shutdown hook

- **Scheduler**
  - Periodic scheduler (using Akka scheduler) that:
    - Looks up tasks about to start → sends “REMINDER”
    - Finds tasks in progress → sends “PROGRESS_CHECK”
    - Finds tasks for today → sends “EVENT_DAY_ALERT” to team + event manager
