# Corporate Equipment Allocation & Tracking System

A single project made of two Scala services:

- **equipment-tracker** – Play Framework REST API + DB
- **equipment-akka-tracker** – Akka + Kafka notifications service

Together they manage shared corporate equipment (laptops, projectors, tablets) with real‑time status, overdue reminders, and automated emails.

---

## Problem

- Companies share devices (laptops, projectors, tablets) across teams.
- Reception/admin staff usually track allocations in Excel or on paper.
- Issues:
  - Hard to know which item is **available / allocated / damaged**.
  - **Overdue returns** are easy to miss.
  - **Damaged** items may not reach maintenance quickly.
  - Inventory and maintenance teams don’t get consistent updates.

---

## Solution Overview

- Central Play API where reception staff can:
  - Login and authenticate.
  - Create and maintain user and equipment records.
  - Allocate and return equipment with due dates and conditions.
- Background Akka microservice that:
  - Consumes Kafka events from the API.
  - Maintains lightweight in‑memory state.
  - Sends email notifications to employees, inventory, and maintenance.
- Communication between services is **event-driven** using Kafka topic `equipment-events`.

---

## Code Structure (combined project view)

### models (Play API)

- `User`, `Employee`, `Equipment`, `Allocation`
  - Core domain models.
- `UserTable`, `EquipmentTable`, `AllocationTable`
  - Slick table classes mapping models to DB tables.
- `UserRequest`
  - Wrapper for Play requests that includes authenticated user id and role.

### dto (Play API)

- `LoginDTO` – username/password for login.
- `UserCreateDTO`, `UserUpdateDTO` – create/update users.
- `EquipmentDTO` – equipment details for responses.
- `EquipmentRequestDTO`, `EquipmentUpdateDTO` – payloads for equipment create/update.
- `EmployeeDTO` – simplified employee view used in responses.
- `AllocationDTO` – allocation + equipment + employee combined.
- `AllocationRequestDTO` – allocate equipment (equipmentId, userId, expectedReturn).
- `ReturnRequestDTO` – return equipment (equipmentId, userId, condition).
- `ApiResponse` – standard response wrapper for JSON APIs.

### controllers (Play API)

- `AuthController`
  - `POST /api/login` – validates credentials via `UserRepository`.
  - Returns JWT token and expiry in `ApiResponse`.
- `UserController`
  - Admin-only CRUD for users (create, list, update, delete).
  - `getUser` – admin can see any user; non-admins can only see their own record.
- `EquipmentController`
  - `listEquipment` – list all equipment with current status.
  - `createEquipment` – admin creates new equipment records.
  - `updateEquipment` – admin/inventory update equipment name/type/status.
  - `allocateEquipment`
    - Validates equipment is not already allocated.
    - Validates equipment and employee exist.
    - Inserts new `Allocation` row and sets equipment status to `allocated`.
    - Sends an `allocated` event to Kafka.
  - `returnEquipment`
    - Finds active allocation for given equipment + user.
    - Sets `returnedAt` and `equipmentCondition`.
    - Updates equipment status to `available` or `damaged`.
    - Sends `returned` and/or `damaged` events to Kafka.

### repositories (Play API)

- `UserRepository`
  - Find user by username/password (for login).
  - Get user email by id.
- `AllocationRepository`
  - Retrieve active allocations.
  - `getOverdue(now)` – list of overdue allocations based on expectedReturn.
  - `markReminderSent(allocationId)` – marks that an overdue reminder was sent.

### services (Play API)

- `KafkaProducerService`
  - Sends JSON messages to Kafka topic `equipment-events`.
  - Overload 1: `sendEvent(eventType, equipmentId, employeeEmail, inventoryEmail)` for `allocated`, `returned`, `damaged`.
  - Overload 2: `sendEvent(eventType, jsonPayload)` for detailed `overdue` events.
- `OverdueScheduler`
  - Akka-based scheduler inside the Play app.
  - Reads configuration (`overdue.scheduler.initialDelay`, `overdue.scheduler.interval`).
  - On each run:
    - Calls `AllocationRepository.getOverdue(now)`.
    - For each overdue allocation:
      - Fetches user email via `UserRepository`.
      - Builds JSON payload (allocationId, userId, userEmail, equipmentId, expectedReturn, notifiedAt).
      - Publishes an `overdue` event via `KafkaProducerService`.
      - Marks reminder as sent in DB.

### security (Play API)

- `Roles`
  - Constants for `admin`, `reception`, `inventory`, `maintenance` roles.
- `JWTUtils`
  - Creates JWT tokens with username and role.
  - Validates tokens and extracts claims.
- `AuthAction`
  - Custom Play action that:
    - Reads `Authorization: Bearer <token>` header.
    - Validates JWT.
    - Wraps request as `UserRequest` carrying user id and role.
  - Used to protect all `/api/**` endpoints except login.

### filters & modules (Play API)

- `AuthFilter`
  - Global filter enforcing authentication/authorization on protected routes.
- `Filters`
  - Registers CORS and auth filters with Play.
- `Module`
  - Guice wiring for repositories, services, filters, and `OverdueScheduler`.

---

### actors (Akka service)

- `KafkaConsumerActor`
  - Subscribes to Kafka topic `equipment-events`.
  - Parses each record into JSON and routes based on `eventType`:
    - `allocated` – updates `InventoryActor`, triggers allocation emails.
    - `returned` – updates `InventoryActor`, triggers return emails.
    - `damaged` – updates `MaintenanceActor`, sends maintenance + inventory alerts.
    - `repaired` – updates `InventoryActor`, sends inventory update.
    - `overdue` – reads extra fields (`allocationId`, `userEmail`, `expectedReturn`, etc.) and triggers overdue reminder.
- `InventoryActor`
  - Keeps a simple in-memory map `equipmentId -> status`.
  - Receives messages like `Allocated`, `Returned`, `Repaired`.
- `MaintenanceActor`
  - Tracks which equipment is damaged / under repair.
  - Receives `Damaged` and `Repaired` events.
- `NotificationActor`
  - Translates events into email commands using `EmailService`:
    - `SendAllocationNotification(employeeEmail, inventoryEmail, equipmentId)`.
    - `SendReturnNotification(employeeEmail, inventoryEmail, equipmentId, condition)`.
    - `SendMaintenanceAlert(maintenanceEmail, equipmentId)`.
    - `SendInventoryUpdate(inventoryEmail, equipmentId, eventType)`.
    - `SendOverdueReminder(employeeEmail, equipmentId, expectedReturn, condition)`.

### service (Akka)

- `EmailService`
  - Uses Jakarta Mail over SMTP (e.g. MailHog on `localhost:1025`).
  - `sendEmail(fromOpt, to, subject, body)`
    - Builds `MimeMessage`, sets from/to, subject, and body.
    - Sends email and logs to console.

### Main (Akka)

- `Main.scala`
  - Creates `ActorSystem("EquipmentSystem")`.
  - Spawns:
    - `inventoryActor`
    - `maintenanceActor`
    - `notificationActor`
    - `kafkaConsumerActor` wired with the above actors.
  - Starts polling Kafka as soon as the service starts.

---

## End-to-End Flow (summary)

1. Reception logs in via Play API and gets a JWT token.
2. Reception allocates equipment to an employee using the API.
3. API writes to MySQL and sends an `allocated` event to Kafka.
4. Akka service consumes the event, updates its in-memory state, and sends emails.
5. When equipment is returned or marked damaged, similar events are emitted and processed.
6. The OverdueScheduler regularly checks for late returns and emits `overdue` events, which trigger reminder emails.

This way, **Play + DB** handle the core business data, while **Akka + Kafka** handle background processing and notifications as one cohesive project.
