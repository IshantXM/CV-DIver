# Papertrail — Resume library

A private, single-workspace resume library built with Java, Spring Boot, and Spring AI. Keep up to **50 PDFs and 20 screenshots**, extract text for searching and reading, review originals, track a review stage, and compare up to four candidates side by side.

## Run locally

Requirements: Java 21 or newer and Maven 3.6.3 or newer. From this folder:

```powershell
mvn spring-boot:run
```

Then open [http://localhost:8080](http://localhost:8080). By default, the library uses a local H2 database at `./data/resumevault` and saves uploaded files under `./data/uploads`.

## Environment variables

Copy `.env.example` to `.env` and set only the values you need. Spring Boot does not load `.env` automatically; export these variables in your terminal, or use a tool such as `dotenv` to load the file.

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8080` | HTTP port |
| `DATABASE_URL` | `jdbc:h2:file:./data/resumevault;AUTO_SERVER=TRUE` | JDBC URL; PostgreSQL is supported with a `jdbc:postgresql:` URL |
| `DATABASE_USERNAME` | `sa` | Database username |
| `DATABASE_PASSWORD` | empty | Database password |
| `UPLOAD_DIR` | `./data/uploads` | Directory for original PDFs and images |
| `MAX_DOCUMENTS` | `70` | Overall document limit (the application also enforces 50 PDFs and 20 screenshots) |
| `TESSERACT_PATH` | empty | Path to the Tesseract `tessdata` directory, for example `C:\Program Files\Tesseract-OCR\tessdata` |
| `AI_ENABLED` | `false` | Set `true` to enable the optional Spring AI comparison summary |
| `OPENAI_API_KEY` | empty | API key used by Spring AI when AI is enabled |
| `OPENAI_MODEL` | `gpt-4o-mini` | OpenAI chat model name |

### Optional OCR and AI

PDF text extraction works without an OCR engine. To extract text from screenshot images, install Tesseract with English language data and set `TESSERACT_PATH` to the folder containing `eng.traineddata`. Image resumes can still be opened when OCR is unavailable; the extracted-text pane will explain that no text was found.

The AI comparison is opt-in. Set `AI_ENABLED=true` and `OPENAI_API_KEY`. AI output is intended as a grounded reading aid, asks for evidence from the resumes, and does not make a hiring decision. Confirm details in the original files.

## PostgreSQL

Add a PostgreSQL JDBC URL, for example `DATABASE_URL=jdbc:postgresql://localhost:5432/resumevault`, plus `DATABASE_USERNAME` and `DATABASE_PASSWORD`. The PostgreSQL driver is included. Use a persistent, backed-up volume for both the database and `UPLOAD_DIR` when deploying.

## Accessibility and privacy

The interface includes keyboard-accessible controls, visible focus styles, a skip link, semantic table headings, screen-reader labels and live status messages, responsive layouts, and reduced-motion support. It stores documents on the configured server. This starter is designed for a trusted local deployment and does not include user sign-in or role management; add authentication before sharing a deployment with multiple users or exposing it to the internet. The app does not connect to or scrape LinkedIn. Upload files you already have permission to use.

## API overview

- `GET /api/resumes?q=...` — list and search
- `POST /api/resumes` — upload multiple PDF/image files (`files` multipart field)
- `GET /api/resumes/{id}` and `/file` — read metadata/text and original file
- `PATCH /api/resumes/{id}` — update `stage` or comma-separated `tags`
- `DELETE /api/resumes/{id}` — remove a resume and its stored file
- `POST /api/resumes/compare` — load 2–4 selected resumes
- `POST /api/ai/compare` — optional AI summary when configured
