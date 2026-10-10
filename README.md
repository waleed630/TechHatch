# TechHatch - Job Portal for Tech Talent

A full-stack job portal that connects tech professionals with companies. Recruiters post jobs and review applicants; job seekers build a profile, upload a resume, search jobs and track their applications.

---

## Features

**Accounts**
- Sign up as a job seeker or a recruiter, with the email verified by a one-time code (OTP)
- Login with email and password (JWT), limited to 5 wrong passwords per 15 minutes
- Forgot password with an OTP sent by email

**Job seekers**
- Profile with skills, education, links, profile picture and resume upload (PDF, DOC, DOCX)
- Browse and search jobs by keyword, location, job type, experience level and date posted
- Apply with a cover letter; jobs already applied to are hidden from the search
- Dashboard and "My Applications" with the current status and the recruiter's notes
- Email whenever a recruiter changes the status of an application

**Recruiters**
- Company profile with logo upload
- Post, edit, close and delete jobs (PKR, INR, USD and EUR salaries)
- See the applicants of each job with their resume, update status and add notes
- Dashboard with totals of jobs, active jobs, applications and job views

---

## Tech Stack

| Part | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Spring Security (JWT), Spring Data JPA, Thymeleaf (email templates) |
| Frontend | React 18, Vite, React Router 6, Tailwind CSS, shadcn/ui, Axios |
| Database | MySQL 8 |
| Email | SMTP (Gmail by default) |
| CI | GitHub Actions: unit tests, jar build and Docker image pushed to GHCR |

---

## Project Structure

```
TechHatch/
├── server/                          # Spring Boot API
│   ├── src/main/java/api/techhatch/com/
│   │   ├── config/                  # Security, JWT filter, mail templates
│   │   ├── controller/              # REST endpoints and error handling
│   │   ├── service/                 # Business logic
│   │   ├── repository/              # Database access
│   │   ├── model/                   # JPA entities
│   │   ├── dto/                     # Request and response objects
│   │   ├── exception/               # Custom exceptions
│   │   └── util/                    # JWT and OTP helpers
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── templates/mail-templates/
│   ├── src/test/                    # Unit tests
│   ├── Dockerfile
│   └── pom.xml
├── client/                          # React app
│   └── src/
│       ├── components/              # Shared components and UI kit
│       ├── context/                 # Login state
│       ├── pages/                   # Screens (candidate/, recruiter/, auth)
│       ├── services/                # API calls
│       └── utils/                   # Constants and formatters
└── .github/workflows/               # CI pipeline
```

---

## Running Locally

**You need:** Java 21, Node.js 20 or newer, and MySQL 8 running on your machine.

### 1. Backend

Create the file `server/.env` (it is git-ignored, never commit it):

```properties
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/techhatch?createDatabaseIfNotExist=true
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your-mysql-password

# creates and updates the tables from the entities
spring.jpa.hibernate.ddl-auto=update

# a random base64 string, at least 32 bytes long
JWT_SECRET=your-base64-secret

# the address of the frontend, allowed by CORS
CLIENT.URL=http://localhost:5173

# Gmail address and a Gmail "App password" (not the normal password), used to send OTP emails
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-gmail-app-password
```

Start the server from the `server` folder (the `.env` file is only found from there):

```
cd server
.\mvnw.cmd spring-boot:run        # Windows
./mvnw spring-boot:run            # macOS / Linux
```

The API runs on `http://localhost:8080`. The database and its tables are created on the first start.

### 2. Frontend

Create the file `client/.env`:

```
VITE_API_URL=http://localhost:8080
```

Then:

```
cd client
npm install
npm run dev
```

Open `http://localhost:5173`.

---

## Tests and Checks

```
cd server
.\mvnw.cmd test "-Dtest=!TechhatchApplicationTests"    # unit tests, no database needed
.\mvnw.cmd test                                        # all tests, needs MySQL and server/.env

cd client
npm run lint
npm run build
```

---

## API Overview

All paths start with `/api/v1`. Endpoints marked 🔒 need the header `Authorization: Bearer <token>`.

### Auth
| Method | Path | Description |
|---|---|---|
| POST | `/auth/register` | Start sign up, sends an OTP by email |
| POST | `/auth/otp/verify-registration` | Verify the OTP and create the account |
| POST | `/auth/otp/resend` | Send a new OTP |
| POST | `/auth/login` | Login, returns the JWT token |
| POST | `/auth/forgot-password` | Send a password reset OTP |
| POST | `/auth/reset-password` | Set a new password with the OTP |
| GET | `/auth/me` 🔒 | The logged in user |

### Jobs
| Method | Path | Description |
|---|---|---|
| GET | `/jobs` | Search active jobs (`keyword`, `location`, `jobType`, `experienceLevel`, `minSalary`, `maxSalary`, `fromDate`, `toDate`, `page`) |
| GET | `/jobs/{id}` | Job details |
| POST | `/jobs` 🔒 | Post a job (recruiter) |
| PUT | `/jobs/{id}` 🔒 | Edit a job (owner) |
| PATCH | `/jobs/{id}/close` 🔒 | Close a job (owner) |
| DELETE | `/jobs/{id}` 🔒 | Delete a job (owner) |
| GET | `/jobs/my-jobs` 🔒 | The recruiter's jobs |
| GET | `/jobs/my-stats` 🔒 | Totals for the recruiter dashboard |

### Applications
| Method | Path | Description |
|---|---|---|
| POST | `/applications` 🔒 | Apply to a job (job seeker) |
| GET | `/applications/my-applications` 🔒 | The job seeker's applications |
| GET | `/applications/my-applications/job/{jobId}` 🔒 | The job seeker's application for one job |
| GET | `/applications/my-stats` 🔒 | Totals for the job seeker dashboard |
| GET | `/applications/job/{jobId}` 🔒 | Applicants of a job (recruiter) |
| GET | `/applications/{id}` 🔒 | One application |
| PATCH | `/applications/{id}/status` 🔒 | Update status and notes (recruiter), emails the job seeker |

### Profiles
| Method | Path | Description |
|---|---|---|
| POST | `/profile/candidate` 🔒 | Create or update the job seeker profile |
| GET | `/profile/candidate/me` 🔒 | My job seeker profile |
| GET | `/profile/candidate/{id}` 🔒 | A job seeker profile |
| POST | `/profile/recruiter` 🔒 | Create or update the company profile |
| GET | `/profile/recruiter/me` 🔒 | My company profile |
| GET | `/profile/recruiter/{id}` 🔒 | A company profile |

### Files
| Method | Path | Description |
|---|---|---|
| POST | `/files/resume` 🔒 | Upload a resume: PDF, DOC or DOCX up to 5 MB (job seeker) |
| POST | `/files/profile-picture` 🔒 | Upload a picture: PNG, JPG or WEBP up to 2 MB (job seeker) |
| POST | `/files/company-logo` 🔒 | Upload a logo: PNG, JPG or WEBP up to 2 MB (recruiter) |
| GET | `/files/{id}` | Pictures and logos are public; a resume opens only for its owner and for recruiters |

Uploaded files are stored in the database, so they survive redeploys.

---

## Deployment

**Backend.** `server/Dockerfile` builds the jar and runs it, so any host that builds from a Dockerfile works. Set these environment variables on the host:

| Variable | Value |
|---|---|
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | The MySQL database |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` |
| `JWT_SECRET` | A random base64 string, different from the local one |
| `CLIENT_URL` | The address of the deployed frontend |
| `EMAIL_USERNAME`, `EMAIL_PASSWORD` | The SMTP account |

The server listens on the `PORT` variable when the host provides one, otherwise on 8080.

**Frontend.** Deploy the `client` folder as a Vite app and set `VITE_API_URL` to the address of the deployed backend.

**CI.** On every push to `main` that changes `server/`, GitHub Actions runs the unit tests, builds the jar and pushes a Docker image to `ghcr.io/<owner>/techhatch-app`.

---

## Security

- Passwords are hashed with BCrypt (strength 12)
- JWT tokens expire after 10 hours
- Role-based access on every protected endpoint
- OTP codes expire after 10 minutes and allow 3 attempts; OTP requests are limited to 3 per 15 minutes
- Login is blocked for 15 minutes after 5 wrong passwords
- CORS only allows the configured frontend address
- Secrets are read from environment variables and are never committed
