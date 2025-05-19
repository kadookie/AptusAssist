# AptusAssist

AptusAssist is a full-stack automation system designed to enhance the booking experience for shared residential amenities managed through the **Aptus Portal by Assa Abloy** — a platform commonly used in Swedish apartment buildings.

This project was built to solve a real-world problem while demonstrating practical engineering ability across backend development, scraping, automation, messaging integration, and DevOps.

**Key outcomes:**

- Replaces a cumbersome, manual booking process with a fully automated flow.
- Provides a clean, login-free web UI for smart displays.
- Monitors cancellations in real-time and notifies the user via Telegram with inline booking.

Although built for personal use, this project is structured and documented to showcase modern software engineering practices, including modular Spring-based architecture, clean code principles, Dockerized deployment, and full end-to-end automation.

---

## 💡 Motivation

The Aptus Portal offers no public API and requires multiple manual steps just to check or manage bookings. Users must log in through a browser, navigate a dated interface, and recheck frequently to catch last-minute cancellations — which are not surfaced unless manually observed.

This project was born out of a simple, recurring frustration: wanting to immediately claim newly available time slots without repeatedly logging in.

Instead of tolerating the system’s limitations, I reverse-engineered its login flow, built an automation engine around it, and created an interface tailored for smart displays and real-time alerts — turning a daily annoyance into a seamless background process.

The official "Aptus Home" mobile app, which simply wraps the same web interface in a native shell, currently holds a 1.2-star rating on the App Store — reinforcing the broader need for a more modern, usable, and automation-friendly alternative like AptusAssist.

It’s worth noting that the lack of a public API is intentional: the Aptus platform also controls access to physical doors, gates, and building infrastructure. Assa Abloy, a global leader in access control, prioritizes closed integrations for security reasons — and rightly so.

This project operates strictly within the **booking module** and does not interact with any security-related systems or endpoints.

---

## ✨ Features

- **Smart display–friendly web interface**
  Clean, minimal calendar UI for viewing and booking available time slots. Designed for use on tablets and home automation dashboards.

- **Real-time slot monitoring**
  Automatically checks for availability every 5 minutes and detects newly freed-up slots.

- **Telegram notifications with inline booking**
  Sends instant alerts when a slot becomes available — with an inline “Book Now” button for one-click booking.

- **Full backend booking integration**
  Bookings triggered from either the web UI or Telegram are executed directly through the backend — no login required on the client side.

- **Lightweight local storage**
  Caches slot data to minimize redundant notifications and reduce external requests.

- **Personal-use optimized**
  Built for reliability, speed, and zero-maintenance — designed to quietly run in the background.

---

## 🛡 Architecture

The project follows a **layered, modular architecture** with a strong separation of concerns — built for maintainability and clarity.

### **Core Structure**

- **Controller Layer**
  Handles HTTP requests (web UI) and Telegram bot callbacks.

- **Service Layer**
  Encapsulates business logic: login automation, scraping, slot diffing, notification dispatch, and booking orchestration.

- **Repository Layer**
  Manages persistence using Spring Data JPA and a lightweight file-based H2 database.

### **Scheduled Automation**

- A scheduled task runs every 5 minutes:

  - Authenticates with Aptus via reverse-engineered flow
  - Scrapes current and upcoming slot data
  - Compares with cached data to detect changes
  - Triggers Telegram notifications for newly available slots

### **External Integrations**

- **Aptus Portal (Assa Abloy)**
  Auth flow includes redirects, CSRF tokens, salted password hashing, and session cookie management.

- **Telegram Bot API**
  Sends real-time messages with inline actions. Supports direct booking from the message.

### **Web Frontend**

- React-based calendar UI for desktop and tablet use.
- Supports click-to-book interactions.
- Communicates with backend via REST — frontend stays login-free.

### **DevOps**

- The system is **fully Dockerized** for consistency and reproducibility.
- Runs as a containerized service on a local **Ubuntu machine**, designed for 24/7 uptime with minimal resource usage.
- Includes:

  - Dockerfile with production-level build setup
  - Volume-mounted persistent storage for slot data
  - `.env`-based configuration for environment-specific variables

- Can be deployed with a single command (`docker compose up -d`) and monitored via logs or health checks.

---

## 🛠 Tech Stack

This project leverages a modern, pragmatic stack designed to solve a focused problem efficiently and cleanly.

### **Backend**

- Java 21
- Spring Boot
- Spring MVC
- Spring Data JPA
- H2 Database (file-based)
- Lombok

### **Scraping & HTTP**

- Jsoup
- OkHttp

### **Scheduling & Automation**

- Spring Scheduler

### **Frontend**

- React (minimal UI)
- REST-based integration

### **Messaging Integration**

- Telegram Bot API

### **Build & DevOps**

- Maven
- Docker
- Ubuntu
- Javadoc, JUnit 5, Spring Test

---

## 📸 Demo

This system has been running in production on a local machine for over a year. While it’s not intended for general use, the outcome is real, stable, and integrated into daily life.

### 1. Original Aptus Portal Interface

![Aptus Portal](docs/aptus-portal.png)

### 2. Smart Display Calendar UI

![Calendar UI](docs/calendar-ui.png)

### 3. Telegram Notification

![Telegram Bot Message](docs/telegram-message.png)

---

## 🗂 Project Structure

```
.
├── config/
│   ├── BotConfig.java
│   └── WebConfig.java
├── controller/
│   └── BookingController.java
├── entity/
│   └── Booking.java
├── model/
│   └── LoginResponse.java
├── repository/
│   └── BookingRepository.java
├── runner/
│   └── SlotUpdateScheduler.java
├── service/
│   ├── BookingService.java
│   ├── DbService.java
│   ├── LoginService.java
│   └── NotificationService.java
├── Application.java
└── Dockerfile
```

---

## 🏷 Badges

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-success)
![Build](https://img.shields.io/badge/build-passing-brightgreen)
![Dockerized](https://img.shields.io/badge/docker-ready-blue)
![License](https://img.shields.io/github/license/kadookie/aptusassist)

---

## ✅ TODO

- [x] Reverse-engineer Aptus login flow
- [x] Build scraper, diff engine, and Telegram bot
- [x] Implement React-based calendar UI
- [x] Containerize the application with Docker
- [ ] Add dark mode to web interface
- [ ] Make polling interval configurable via UI
- [ ] Add admin UI for monitoring/logs
- [ ] Build a Home Assistant plugin

---

## ⚖ License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

**Disclaimer:** This project is not affiliated with or endorsed by Assa Abloy or the Aptus Portal team. It was developed independently for personal use and operates only on the booking module without interacting with access control or security systems.

---

## ✉ Contact

Built by **[@kadookie](https://github.com/kadookie)** as part of a full-stack engineering portfolio.
For questions or feedback, feel free to reach out via GitHub.
