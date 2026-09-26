# Mini Hiring Pipeline 🚀

A modern Spring Boot application designed to streamline the hiring process with automated pipelines and AI-assisted candidate evaluation.

---

## 🛠️ Tech Stack

- **Backend:** Java 17, Spring Boot 4.x
- **Build Tool:** Maven
- **Database:** PostgreSQL / MySQL (configurable)
- **AI Integration:** LLM services (e.g., OpenAI, Gemini)

---

## 📋 Prerequisites

- **Java JDK 17+** installed
- **Maven** (or use included `./mvnw` / `mvnw.cmd`)
- **Database instance** (e.g., PostgreSQL / MySQL)
- **LLM API Key** (e.g., OpenAI, Google Gemini, Anthropic)

---

## ⚙️ Configuration & Secrets Setup

To prevent sensitive credentials (database passwords, LLM API keys) from being committed to Git:

1. Copy the example configuration file:
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```
2. Open `src/main/resources/application.properties` and provide your actual credentials:
   ```properties
   # Database Credentials
   spring.datasource.url=jdbc:postgresql://localhost:5432/hiring_pipeline_db
   spring.datasource.username=your_db_username
   spring.datasource.password=your_secure_password

   # LLM API Secrets
   gemini.api.key=your_gemini_api_key_here
   openai.api.key=your_openai_api_key_here
   ```

> 🔒 **Note:** `application.properties`, `.env`, and all secret files are ignored by git in `.gitignore`. Never commit your real secret keys.

---

## 🚀 Running the Application

### Using Maven Wrapper (Windows):
```cmd
.\mvnw.cmd spring-boot:run
```

### Using Maven Wrapper (Linux / macOS):
```bash
./mvnw spring-boot:run
```

The server will start at `http://localhost:8080`.

---

## 📄 License

This project is licensed under the MIT License.
