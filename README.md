# BusKaroo

BusKaroo is a bus search and reservation web application for travel across India. Customers can search direct trips, view one-transfer alternatives, choose seats, pay through the application's payment simulation, and manage reservations. Prices are displayed in INR.

## Features

### Customer application

- Customer sign-up, sign-in, profile editing, and avatar upload.
- Search by Indian city or terminal, travel date, passenger count, and departure-time preference.
- Direct trip search with live seat availability and fare results.
- One-transfer alternatives when a suitable direct trip is unavailable. Each leg is booked separately.
- Seat selection and passenger details before checkout.
- Reservation history, cancellation, and printable ticket details.
- Simulated payment flow; no real payment gateway is connected.
- Responsive light and dark themes, and INR price formatting.

### Bus classes and fleet

The customer search identifies these bus classes:

| Class | Default configuration |
|---|---|
| Normal | Non-AC coach, 2+2 seats |
| Luxury | Air-conditioned coach, premium 2+1 seats |
| Sleeper | Air-conditioned overnight coach, 1+1 berths |

Seed data includes Indian state roadways coaches and 30 dedicated Luxury plus 30 dedicated Sleeper buses. Startup initialization adds missing categories and fleet records to an existing database and schedules category trips on active Indian routes. Fleet records and bus categories can be managed from the admin dashboard.

## Technology

| Area | Stack |
|---|---|
| Backend | Java 17, Spring Boot 3.5, Spring MVC, Spring Security, Spring Data JPA / Hibernate |
| Database | PostgreSQL |
| Authentication | Stateless JWT |
| Frontend | React 19, JavaScript, Vite, React Router, Axios |
| Build | Maven Wrapper and npm |

The backend Java package is `busKaroo`.

## Repository layout

```text
.
├── src/main/java/busKaroo/       # Spring Boot application
│   ├── config/                   # CORS and security configuration
│   ├── controller/               # REST API controllers
│   ├── dao/                      # Spring Data repositories
│   ├── dto/                      # API requests, responses, and mappers
│   ├── exception/                # API exceptions and error responses
│   ├── model/                    # JPA entities and enums
│   ├── security/                 # JWT and user authentication
│   └── service/                  # Business logic and startup data initialization
├── src/main/resources/
│   ├── application.properties
│   ├── application-existing-db.properties
│   ├── schema.sql                # PostgreSQL enum/legacy column compatibility
│   └── data.sql                  # Idempotent Indian seed data
├── src/test/java/busKaroo/       # Backend unit and integration tests
├── frontend/                     # React and JavaScript application
├── docker-compose.yml            # Backend and PostgreSQL services
├── Dockerfile
└── pom.xml
```

## Requirements

- Java 17 or newer
- Node.js and npm
- PostgreSQL 15 or compatible, or Docker with Docker Compose

## Run locally

Start PostgreSQL first. The backend reads its connection from Spring environment variables. Set values for your own database; do not commit credentials or production secrets.

### Backend with a new or seedable database

In PowerShell, set the database connection and a private JWT secret, then start Spring Boot:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/bus2holiday"
$env:SPRING_DATASOURCE_USERNAME = "YOUR_DATABASE_USER"
$env:SPRING_DATASOURCE_PASSWORD = "YOUR_DATABASE_PASSWORD"
$env:JWT_SECRET = "YOUR_PRIVATE_SECRET_AT_LEAST_32_CHARACTERS_LONG"
.\mvnw.cmd spring-boot:run
```

The backend listens on `http://localhost:8081` by default.

### Database initialization

Flyway is not used by this project. Hibernate with `ddl-auto=update` creates missing tables and applies additive entity changes. After Hibernate initializes, `schema.sql` ensures PostgreSQL enum types and a few legacy status columns exist; `data.sql` inserts the default seed records. Java startup initializers add the Normal, Luxury, and Sleeper categories and related fleet data. The default profile also generates trip schedules for seeded routes that have no trips yet.

The `existing-db` profile sets `spring.sql.init.mode=never`, so it does not rerun `schema.sql` or `data.sql` against an existing database. Hibernate still applies entity schema updates, and the idempotent category/fleet initializer still runs. This split supports a seeded local setup and a populated database without reseeding customer data. `ddl-auto=update` is for this demo/local workflow; production schema changes should use a reviewed migration process.

### Backend with an existing database

Use this profile to prevent `data.sql` from being run against an already populated database. The startup initializer still creates missing Normal, Luxury, and Sleeper categories and the BusKaroo category fleet/schedules.

```powershell
$env:SPRING_PROFILES_ACTIVE = "existing-db"
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/YOUR_DATABASE"
$env:SPRING_DATASOURCE_USERNAME = "YOUR_DATABASE_USER"
$env:SPRING_DATASOURCE_PASSWORD = "YOUR_DATABASE_PASSWORD"
$env:JWT_SECRET = "YOUR_PRIVATE_SECRET_AT_LEAST_32_CHARACTERS_LONG"
.\mvnw.cmd spring-boot:run
```

The `existing-db` profile listens on `http://localhost:8082` and disables SQL seed-file execution. The startup initializer is idempotent; restart the backend after changes to seed or initializer logic so it can apply them.

Pending reservations hold their seats for 15 minutes by default. Set `PENDING_RESERVATION_TTL_MINUTES` to change the hold duration. A scheduled backend task marks stale pending reservations `EXPIRED`, while availability checks ignore pending reservations past the hold time even before the next scan.

### Frontend

Open a second terminal. The Vite development proxy defaults to the backend on port `8081`. For the `existing-db` profile, set the proxy target to port `8082`:

```powershell
cd frontend
npm install
$env:VITE_API_PROXY_TARGET = "http://localhost:8082"
npm run dev
```

For the default backend port, omit the `VITE_API_PROXY_TARGET` line. The frontend is available at `http://localhost:5173`.

When hosting the frontend and API separately, set `VITE_API_BASE_URL` to the reachable API base ending in `/api`, for example `https://api.example.com/api`. A browser cannot use a developer's `localhost` address to reach a remotely hosted backend.

### Docker Compose

From the repository root:

```bash
docker compose up --build
```

Compose loads the root `.env` file automatically. Copy `.env.example` to `.env`, set `JWT_SECRET` to your own Base64-encoded key (at least 32 decoded bytes), and change the local database values before the first start. `JWT_EXPIRATION` is the token lifetime in milliseconds; it defaults to `86400000` (24 hours). `PENDING_RESERVATION_TTL_MINUTES` defaults to 15. Spring Boot also imports the root `.env` for local `mvnw spring-boot:run` sessions. Inside Compose, the backend must connect to the PostgreSQL service using hostname `postgres` (as configured in `docker-compose.yml`); `localhost` is only for applications running outside the Compose network. Compose starts PostgreSQL and the backend after the database health check passes. The API is exposed on `http://localhost:8080`; PostgreSQL is exposed on port `5432`. These sample settings are for local development only. Use secure, unique credentials and a strong JWT secret for any deployment.

### First run on a new computer

No customer or administrator login accounts are included in the GitHub repository. Each person creates an account in their own database. To start a local Docker setup, copy the example environment file and create a private JWT signing key:

```powershell
Copy-Item .env.example .env
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

Paste the generated output into the `JWT_SECRET` line in `.env`. Choose a local database password and use the same value for `POSTGRES_PASSWORD` and `SPRING_DATASOURCE_PASSWORD`. Then start the backend and database:

```powershell
docker compose up --build
```

In another terminal, start the frontend with the Docker API port as its proxy target:

```powershell
cd frontend
npm install
$env:VITE_API_PROXY_TARGET = "http://localhost:8080"
npm run dev
```

Open `http://localhost:5173` and choose **Sign Up** to create a customer login with a password of your choice. For an administrator account, set a private `ADMIN_REGISTRATION_KEY` in `.env` and use that key on the administrator registration page. Do not share your local `.env` file; each developer should create their own.

## Administrator access

Customer registration is public. Administrator registration is available from the admin sign-in flow and requires an invitation key. Set `ADMIN_REGISTRATION_KEY` in the backend environment before starting the server. There is no public or hardcoded registration key in the project; the person operating the backend supplies it.

The current admin page supports create, read, update, and delete operations for buses and bus categories. The backend also exposes admin-protected operations for routes, trips, drivers, and users; those API operations are not all represented as sections in the current admin page.

## API overview

All API paths are prefixed with `/api`. JSON endpoints use the JWT bearer token after login. Public access is limited to authentication, customer trip search, connection search, trip detail/seat-count reads, route reads, and terminal reads; other endpoints require authentication, with admin-only operations additionally role-checked.

| Method | Endpoint | Access / purpose |
|---|---|---|
| `POST` | `/auth/register` | Public customer registration |
| `POST` | `/auth/register-admin` | Admin registration; requires configured invitation key |
| `POST` | `/auth/login` | Sign in and receive authentication data |
| `GET` | `/terminals` | List terminals and cities |
| `GET` | `/routes`, `/routes/popular`, `/routes/{id}` | Read routes and stops |
| `GET` | `/trips/search` | Search direct trips by city/terminal, date, passengers, and time window |
| `GET` | `/connections/search` | Find journeys with one transfer |
| `GET` | `/trips/{id}`, `/trips/{id}/available-seats` | Read trip details and availability |
| `GET`, `POST`, `PUT`, `DELETE` | `/reservations` and `/reservations/{id}` | Customer reservation operations; payment uses `POST /reservations/{id}/pay` |
| `GET`, `PUT` | `/users/me`, `/users/{id}` | Read/update a profile; admin can manage user records |
| `GET`, `POST`, `PUT`, `PATCH`, `DELETE` | `/buses` and `/buses/{id}` | Admin fleet CRUD and status updates |
| `GET`, `POST`, `PUT`, `DELETE` | `/bus-categories` and `/bus-categories/{id}` | Read categories; admin category CRUD |
| `POST`, `PUT`, `DELETE` | `/routes` and `/routes/{id}` | Admin route management; route stops have nested endpoints |
| `GET`, `POST`, `PUT`, `DELETE` | `/trips` and `/trips/{id}` | Admin trip management |
| `GET`, `POST`, `PUT`, `PATCH` | `/drivers` and `/drivers/{id}` | Admin driver management and availability; drivers can read their assigned trips and update their own availability |

The driver dashboard route currently displays a placeholder in the frontend, although the driver API is present.

Frontend protected routes only control what pages the browser displays. API authorization is enforced separately by Spring Security authentication and role checks on the controllers; admin API operations require `ROLE_ADMIN` even if a customer directly calls the endpoint.

## Build and tests

Build the backend without running tests:

```powershell
.\mvnw.cmd -DskipTests package
```

Run the backend tests with Docker available for Testcontainers-based integration tests:

```powershell
.\mvnw.cmd test
```

Build the frontend:

```powershell
cd frontend
npm run build
```

## Notes

- The customer currency is INR; fares and fees are treated as Indian rupees.
- Seat occupancy is calculated from reservations for the selected trip segment. Multi-seat reservations acquire per-seat advisory locks in sorted seat order to reduce deadlock risk, and availability is rechecked within the booking transaction.
- Search may return a direct bus or one-transfer alternatives. Transfer legs are booked as separate reservations.
- Payment is simulated and does not charge a card or UPI account. Cancellation changes reservation status; it does not issue a real refund or contact a payment provider. If a confirmed reservation is cancelled, its historical simulated payment record remains `COMPLETED` because no refund transaction is created.
- Unpaid pending reservations expire after the configured hold time and release their seats.
- The seed fleet and route schedules are demo data. Confirm real operator schedules, fares, and availability before using the app as a live booking service.
