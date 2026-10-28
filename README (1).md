# CivicDesk Chennai

Smart Civic Grievance Redressal System with SLA-based Auto-Escalation.

Citizens report civic problems (potholes, garbage, waterlogging, dark streetlights...) to the
Greater Chennai Corporation. Each complaint is routed to the ward officer, gets a deadline based on
its category, and moves up the chain automatically if the deadline is missed:
**Ward Officer → Zonal Officer → Commissioner**.

Stack: Java 21, Spring Boot 4.1.1, Spring Security (JWT), Spring Data JPA / Hibernate, MySQL,
HTML + CSS + JavaScript (Leaflet map, Chart.js).

---

## 1. What you need

| Tool | Version | Check with |
|---|---|---|
| Java (JDK) | 21 or newer | `java -version` |
| MySQL server | 8 or newer, running on `localhost:3306` | `mysql -u root -p` |
| Internet | first run only | Maven downloads the libraries once |

You do **not** need Maven installed: the project includes the Maven wrapper (`mvnw`).

## 2. Run it

```bash
unzip civicdesk.zip
cd civicdesk

export DB_PASSWORD='your-mysql-root-password'   # skip if already set in ~/.zshrc
./run.sh
```

Windows: double-click `run.cmd`, or run `set DB_PASSWORD=...` then `mvnw.cmd spring-boot:run`.

The first start downloads Maven and the dependencies (a few minutes). When you see
`Started CivicdeskApplication`, open **http://localhost:8080**.

- The database `civicdesk_app` is **created automatically**. Your tutorial database `civicdesk` is not touched.
- Tables are created by Hibernate. `data.sql` loads the 15 zones, 200 wards, 6 departments and 10 categories.
- On the very first start, demo accounts and 18 demo complaints are created.

If `./run.sh` says "permission denied": `chmod +x run.sh mvnw` and try again.
MySQL user other than root: `export DB_USERNAME=youruser`.

## 3. Demo accounts

All demo accounts use the password **`Password@123`**. The login page has "Use" buttons that fill them in.

| Role | Email | Sees |
|---|---|---|
| Citizen | `ram@example.com` | own complaints |
| Citizen | `priya@example.com`, `karthikeyan@example.com` | own complaints |
| Ward Officer | `ward170@civicdesk.in` (also 171, 172, 173, 100, 101) | complaints in that ward |
| Zonal Officer | `zonal.adyar@civicdesk.in` (zone 13), `zonal.annanagar@civicdesk.in` (zone 8) | complaints in that zone |
| Commissioner (Admin) | `admin@civicdesk.in` | everything, plus users and tools |

New citizens can sign up on the home page. Officers and admins are created by an admin under **Users**.

## 4. A 3-minute demo

1. Log in as **ram@example.com**. Report a pothole in Zone 13 (Adyar), Ward 170, and drop a pin on the map.
   It is assigned to Karthik R (Ward Officer) instantly, with a 72-hour deadline.
2. Report the same thing again as **priya@example.com** with a pin within ~150 m.
   Instead of a second ticket, Priya's report is added to Ram's complaint (shown as "2 reports").
3. Log in as **ward170@civicdesk.in**. Open the complaint, press **Start work**, then **Mark resolved** with a note.
4. Back as **ram@example.com**: open it, then either **Confirm fixed and close** with a rating, or **Not fixed, reopen**.
5. Log in as **admin@civicdesk.in**. The seeded data already includes overdue complaints; within a minute of
   startup the SLA engine escalates them (red bars, "Escalated to Zonal Officer"). Use
   **SLA engine and demo → Add complaints** to create more, then **Run SLA check** to escalate them immediately.
6. **Overview** shows open/overdue counts, average resolution time, on-time %, ratings, and charts by department.

## 5. How it works

### Roles and what each can do

| Action | Citizen | Ward Officer | Zonal Officer | Admin |
|---|---|---|---|---|
| File a complaint | ✅ | | | |
| See complaints | own (+ ones they also reported) | own ward | own zone | all |
| Assign / start work / resolve | | ✅ | ✅ | ✅ |
| Close a resolved complaint (with rating) or reopen it | owner only | | | close only |
| Close without resolving (needs a reason) | | | | ✅ |
| Dashboard numbers | | own ward | own zone | city-wide |
| Create staff, deactivate accounts, run SLA engine | | | | ✅ |

A complaint you are not allowed to see returns **404**, not 403, so complaint IDs cannot be probed.

### Complaint lifecycle

```
SUBMITTED ──(auto, if the ward has an officer)──▶ ASSIGNED ──▶ IN_PROGRESS ──▶ RESOLVED ──▶ CLOSED
                                                      ▲                             │
                                                      └────────── REOPENED ◀────────┘ (citizen: "not fixed")
```

- **Deadline** = filing time + the category's SLA hours (e.g. Waterlogging 12 h, Pothole 72 h).
- **Reopened** complaints get a fresh deadline.

### SLA engine

`SlaScheduler` runs every 60 seconds (`app.sla.check-interval-ms`). Every open complaint past its deadline:

1. moves up one level: 0 Ward Officer → 1 Zonal Officer → 2 Commissioner,
2. gets a new 24-hour window (`app.sla.escalation-extension-hours`),
3. gets an `ESCALATED` entry in its history, written by "System".

At level 2 it stays on the Commissioner's overdue list until resolved.

### Duplicate detection

When a citizen files a complaint with a map pin, the server looks for an open complaint of the **same category,
same ward, within 150 m, filed in the last 48 hours**. If one exists, the new report is added to it
(`supportCount` + 1) instead of creating a ticket. The citizen can then track that complaint too.

### Security

- Passwords are hashed with BCrypt. Only the hash is stored.
- Login returns a JWT (HS256, 8 hours) signed with `app.jwt.secret`. The browser sends it as `Authorization: Bearer <token>`.
- Public sign-up always creates a CITIZEN; nobody can register as an officer or admin.
- Role rules are in `SecurityConfig`; "own ward / own zone / own complaint" rules are in `AccessService`.
- Errors never include stack traces or internal messages.

## 6. API reference

All bodies are JSON. Endpoints marked 🔒 need `Authorization: Bearer <token>`.

| Method | Path | Who | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | anyone | citizen sign-up, returns token |
| POST | `/api/auth/login` | anyone | returns token |
| GET | `/api/auth/me` | 🔒 any | current user |
| GET | `/api/zones` | anyone | 15 zones |
| GET | `/api/zones/{id}/wards` | anyone | wards in a zone |
| GET | `/api/categories` | anyone | categories with SLA hours |
| POST | `/api/complaints` | 🔒 citizen | file a complaint (201, or 200 if merged as a duplicate) |
| GET | `/api/complaints` | 🔒 any | list, scoped by role. Query: `status`, `wardId`, `categoryId`, `overdue`, `escalated`, `page`, `size`, `sort` |
| GET | `/api/complaints/{id}` | 🔒 any | one complaint (404 if not visible to you) |
| GET | `/api/complaints/{id}/history` | 🔒 any | timeline |
| PATCH | `/api/complaints/{id}/status` | 🔒 any | `{ "status": "IN_PROGRESS", "note": "...", "rating": 5, "feedback": "..." }` |
| GET | `/api/analytics` | 🔒 officers, admin | dashboard numbers |
| GET | `/api/admin/users` | 🔒 admin | accounts |
| POST | `/api/admin/users` | 🔒 admin | create ward officer / zonal officer / admin |
| PATCH | `/api/admin/users/{id}/active` | 🔒 admin | `{ "active": false }` |
| POST | `/api/admin/sla/run` | 🔒 admin | run the SLA engine now |
| POST | `/api/admin/demo/complaints?count=5` | 🔒 admin | add random demo complaints |

Sortable fields: `id`, `dueAt`, `createdAt`, `status`, `escalationLevel`, `supportCount`.

Try it with curl:

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"ward170@civicdesk.in","password":"Password@123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

curl -s "localhost:8080/api/complaints?overdue=true" -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

## 7. Project structure

```
src/main/java/com/ram/civicdesk
├── CivicdeskApplication.java   main class (pins timezone to Asia/Kolkata, enables scheduling)
├── config/       SecurityConfig (rules, JWT, BCrypt), DataSeeder
├── controller/   REST endpoints
├── dto/          request/response records
├── entity/       JPA entities: Zone, Ward, Department, Category, User, Complaint, ComplaintHistory
├── exception/    custom exceptions + GlobalExceptionHandler (one JSON error format)
├── repository/   Spring Data repositories, incl. JPQL search and native analytics queries
├── security/     JwtService, JSON 401/403 handler
└── service/      business logic: complaints, SLA engine, analytics, admin, demo data
src/main/resources
├── application.properties
├── data.sql      zones, wards, departments, categories
└── static/       index.html, citizen.html, officer.html, admin.html, css/, js/
```

## 8. Configuration

| Property | Env variable | Default |
|---|---|---|
| `spring.datasource.username` | `DB_USERNAME` | `root` |
| `spring.datasource.password` | `DB_PASSWORD` | empty |
| `app.jwt.secret` | `JWT_SECRET` | a dev secret. **Set your own (32+ characters) before deploying.** |
| `app.jwt.expiry-hours` | | 8 |
| `app.sla.check-interval-ms` | | 60000 |
| `app.sla.escalation-extension-hours` | | 24 |
| `app.demo.seed` | | true (only seeds when there are no users) |

To start over with fresh demo data: `DROP DATABASE civicdesk_app;` in MySQL, then start the app again.

## 9. Known limitations and next steps

- **Ward names are placeholders** ("Ward 170"). Zone ranges follow the GCC 15-zone layout. Replace names with the
  official list from the OpenCity GCC wards dataset, and use its ward map (KML → GeoJSON) to detect the ward from the map pin.
- SLA hours are illustrative, not official GCC figures.
- `ddl-auto=update` is fine for development. For production, switch to Flyway migrations.
- Photo upload, SMS/email alerts and a Tamil language toggle are not implemented yet.
- The map and charts load Leaflet and Chart.js from CDNs, so those two parts need internet. Everything else works offline.
- The NYC 311 replay feed discussed during planning is replaced by the offline "Add demo complaints" tool.
