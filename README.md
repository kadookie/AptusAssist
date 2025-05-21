![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-success)
![React](https://img.shields.io/badge/React-18-blue)
![PWA](https://img.shields.io/badge/PWA-supported-success)
![Docker](https://img.shields.io/badge/docker-ready-blue)
![Docker Compose](https://img.shields.io/badge/Docker%20Compose-3-blue)
![Maven](https://img.shields.io/badge/Maven-3.9.6-blue)
![H2 Database](https://img.shields.io/badge/H2-Database-lightgrey)
![JUnit 5](https://img.shields.io/badge/JUnit-5-red)
![OkHttp](https://img.shields.io/badge/OkHttp-4.x-green)
![Jsoup](https://img.shields.io/badge/Jsoup-1.15.3-orange)
![Build](https://img.shields.io/badge/build-passing-brightgreen)
![License](https://img.shields.io/github/license/kadookie/aptusassist)

# AptusAssist

AptusAssist is a full-stack automation system with Progressive Web App (PWA) support, designed to streamline the booking experience for shared residential amenities managed through the Aptus Portal by Assa Abloy, a platform commonly used in Swedish apartment buildings by HSB, Stockholmshem, Svenska Bostäder, Stena Fastigheter and Riksbyggen among others.

This project solves a real-world problem while serving as a professional portfolio piece to demonstrate modern software engineering skills across backend development, scraping, automation, messaging integration, and DevOps. Actively running in production for personal use since 2022, it is structured to be both functional and showcase best practices for public use.

---

**Key Outcomes:**
- Replaces a cumbersome, manual booking process with a fully automated flow.
- Delivers a clean, login-free PWA UI with real-time cancellation monitoring and Telegram notifications.

---

## Motivation
The Aptus Portal’s lack of a public API and cumbersome manual booking process, requiring multiple browser-based steps, create a frustrating user experience. AptusAssist solves this by reverse-engineering a complex login flow (handling multiple redirects, session and auth cookies, CSRF tokens, and password salting) to automate slot monitoring and booking, delivering a modern interface and Telegram integration. The official “Aptus Home” mobile app, with a 1.2-star rating on the App Store, underscores the need for a more usable, automation-friendly alternative.

AptusAssist operates strictly within the booking module, ensuring no interaction with Assa Abloy’s security systems.

---

## Skills Demonstrated
- Full-stack development (Java, Spring Boot, React)
- Reverse-engineering and web scraping
- PWA implementation for mobile support
- Dockerized deployment and DevOps practices
- Real-time automation and messaging integration

---

## Features
- **Provides PWA-enabled calendar UI**  
  Clean, responsive interface optimized for smart displays, tablets, and mobile devices, offering a native-like experience.
  
- **Monitors slots in real-time**  
  Checks availability periodically using Spring Scheduler to detect newly freed-up slots.
  
- **Sends instant Telegram notifications**  
  Delivers alerts with inline booking via the Telegram Bot API, enabling one-tap reservations.
  
- **Executes seamless backend booking**  
  Processes bookings from the UI or Telegram without client-side login, optimized for reliability.
  
- **Caches data in lightweight storage**  
  Uses H2 database to store slot data, minimizing external requests and redundant notifications.

---

## Architecture
A layered, modular architecture designed for maintainability and scalability.

**Core Structure:**
- **Controller Layer:** Handles HTTP requests (web UI) and Telegram bot callbacks.
- **Service Layer:** Encapsulates business logic: login automation, scraping, slot diffing, notification dispatch, and booking orchestration.
- **Repository Layer:** Manages persistence using Spring Data JPA and a lightweight file-based H2 database.
- **Testing:** Includes unit and integration tests using JUnit 5 and Spring Test for robust functionality.

**Scheduled Automation:**  
A Spring Scheduler task runs periodically to authenticate with Aptus via a reverse-engineered flow, scrape slot data, compare it with cached data, and trigger Telegram notifications for new slots.

---

## External Integrations
- **Aptus Portal (Assa Abloy):** Handles a complex auth flow with multiple redirects, CSRF tokens, salted password hashing, and session cookie management.
- **Telegram Bot API:** Sends real-time messages with inline actions, supporting direct booking from messages.

---

## Web Frontend
- React-based PWA calendar UI for desktop, tablet, and mobile use, offering a responsive, native-like experience.
- Supports click-to-book interactions.
- Communicates with backend via REST, remaining login-free.

---

## DevOps
- Fully Dockerized for consistency and reproducibility.
- Runs as a containerized service on a local Ubuntu machine, designed for 24/7 uptime with minimal resource usage.
- Includes:
  - `Dockerfile` with production-level build setup
  - Volume-mounted persistent storage for slot data
  - `.env`-based configuration for environment-specific variables
  - Deployable with a single command (`docker compose up -d`) and monitored via logs or health checks.
- Optimized for local deployment but adaptable for cloud platforms like AWS or Heroku.

---

## Tech Stack
**Backend:**
- Java 21
- Spring Boot 3.2
- Spring MVC
- Spring Data JPA
- H2 Database (file-based)
- Lombok

**Scraping & HTTP:**
- Jsoup
- OkHttp

**Scheduling & Automation:**
- Spring Scheduler

**Frontend:**
- React 18 (PWA-enabled with service workers for mobile support)
- REST-based integration

**Messaging Integration:**
- Telegram Bot API

**Testing:**
- JUnit 5, Spring Test for unit and integration testing, ensuring robust functionality

**Build & DevOps:**
- Maven
- Docker
- Ubuntu (local deployment environment)
- Javadoc

---

## 📷 Screenshots
- PWA calendar UI for booking slots on smart displays and mobile devices.
- Telegram notification with inline booking button, showcasing the interactive booking flow.

---

## Project Structure
The project is organized into backend and frontend modules, with a clear separation of concerns for maintainability.
.
├── aptusassist-backend/
│ ├── src/main/java/net/kadookie/aptusassist/
│ │ ├── AptusAssistApplication.java
│ │ ├── config/
│ │ │ ├── TelegramBotConfig.java
│ │ │ └── WebConfig.java
│ │ ├── controller/
│ │ │ └── SlotController.java
│ │ ├── dto/
│ │ │ └── LoginResponse.java
│ │ ├── entity/
│ │ │ └── Slot.java
│ │ ├── repository/
│ │ │ └── SlotRepository.java
│ │ ├── scheduler/
│ │ │ └── SlotUpdateScheduler.java
│ │ ├── service/
│ │ │ ├── LoginService.java
│ │ │ ├── NotificationService.java
│ │ │ ├── SlotDbService.java
│ │ │ └── SlotService.java
│ │ └── util/
│ │ └── PasswordEncoder.java
│ ├── src/main/resources/
│ │ ├── application.properties
│ │ └── schema.sql
│ └── Dockerfile
├── aptusassist-frontend/
│ ├── src/
│ │ ├── App.jsx
│ │ ├── components/
│ │ │ └── SpaBookingCalendar.jsx
│ │ └── utils/
│ │ └── date.js
│ ├── public/
│ │ ├── favicon.svg
│ │ ├── icon-192.png
│ │ └── icon-512.png
│ ├── Dockerfile
│ ├── index.html
│ └── vite.config.ts
├── docker-compose.yml
├── LICENSE
└── README.md

---

## TODO
- [ ] Add dark mode to web interface
- [ ] Make polling interval configurable via UI
- [ ] Add admin UI for monitoring/logs
- [ ] Build a Home Assistant plugin

---

## ⚖ License
This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

## Disclaimer
This project is not affiliated with or endorsed by Assa Abloy or the Aptus Portal team. It was developed independently for personal use and operates only on the booking module without interacting with access control or security systems.

---

## ✉ Contact
Built by [@kadookie](https://github.com/kadookie) as part of a full-stack engineering portfolio.  
For questions or feedback, feel free to reach out via GitHub.