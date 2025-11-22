# Corporate Equipment Allocation & Tracking System

This system solves the problem of managing **shared corporate equipment** (laptops, projectors, tablets) by:
- Letting **reception staff** allocate & return items with due dates and conditions.
- Automatically notifying **employees**, **inventory**, and **maintenance** via email.
- Keeping equipment **status and availability** consistent across services.

The repo contains two Scala projects that work together:

- `equipment-tracker/` – Play Framework REST API + DB
- `equipment-akka-tracker/` – Akka/Kafka notification microservice

They communicate over the Kafka topic **`equipment-events`**.

---

## 1. equipment-tracker (Play REST API)

**Tech stack:** Play Framework (Scala), Slick, MySQL (or any JDBC DB), JWT, Kafka client.

**Responsibility:** Implements the *business system and persistence.

**What it does (mapped to requirements):**

- **Equipment Allocation & Return**
  - Endpoints to:
    - Create, update, list equipment (status: `available`, `allocated`, `damaged`).
    - Allocate equipment to an employee with **expected return date**.
    - Return equipment, capturing final **condition** (OK or damaged).
  - Updates DB tables: `equipment`, `allocations`, `users`.

- **Automated Notifications – Producer side**
  - On **allocate / return / mark damaged**:
    - Publishes Kafka events: `allocated`, `returned`, `damaged`.
  - **Overdue Reminder**:
    - `OverdueScheduler` periodically scans for overdue allocations.
    - For each overdue record:
      - Fetches employee email.
      - Publishes an `overdue` event to Kafka.

- **Security**
  - `POST /api/login` issues **JWT**.
  - All `/api/**` routes (except login) are protected with `Authorization: Bearer <token>`.
  - Role-based access: `admin`, `reception_staff`, `inventory_staff`, `maintenance_staff`.

---

## 2. equipment-akka-tracker (Akka microservice)

**Tech stack:** Akka Actors, Kafka consumer, Jakarta Mail.

**Responsibility:** Implements the *notification and background processing layer*.

**What it does:**

- **Consumes Kafka events** from `equipment-events`:
  - `allocated`, `returned`, `damaged`, `repaired`, `overdue`.

- **Maintains in-memory state**
  - `InventoryActor`:
    - Tracks `equipmentId -> status`.
  - `MaintenanceActor`:
    - Tracks which equipment is **under repair**.

- **Sends email notifications**
  - `NotificationActor` + `EmailService`:
    - To **employees** – allocation, return, overdue reminders.
    - To **inventory team** – every allocation/return/damage/repair.
    - To **maintenance team** – whenever equipment is flagged as damaged.

This microservice is completely **decoupled** from the API: it only sees Kafka events and sends emails.

---

## End-to-end Flow (High Level)

1. Reception uses Play REST APIs (`equipment-tracker`) to allocate or return equipment.
2. `equipment-tracker`:
   - Writes changes to the database.
   - Emits one or more Kafka events (`allocated`, `returned`, `damaged`, `overdue`).
3. `equipment-akka-tracker`:
   - Consumes the events.
   - Updates in-memory equipment and maintenance state.
   - Sends the required notification emails to **employees**, **inventory**, and **maintenance**.
4. Result: Office staff always see **up-to-date equipment status**, and no overdue or damaged item is missed.

---

## Run Locally

**Prerequisites**

- Java 17
- `sbt`
- Kafka running on `localhost:9092` with topic `equipment-events`
- (Optional) SMTP test server on `localhost:1025` (e.g. MailHog)

**Commands**

- API service (Play):
  - `cd equipment-tracker`
  - `sbt run`
- Akka notification service:
  - `cd equipment-akka-tracker`
  - `sbt run`

> For deployment, both services can be containerized with Docker and run together with Kafka and SMTP in a single docker-compose stack.
