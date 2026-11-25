# CV Diver — Resume library

A private, single-workspace resume library built with Java, Spring Boot, Spring AI, and MongoDB. Keep up to **50 PDFs and 20 screenshots**, extract text for searching and reading, skip duplicate uploads, review originals, track a review stage and verdict, and compare up to four candidates side by side against your own criteria.

## Run locally on Windows

You need **Java 21 or newer** and a running MongoDB server. The project includes Maven Wrapper scripts, so you do not need to install Maven. The wrapper downloads Maven the first time it runs; that first run needs an internet connection.

1. Start MongoDB. For a local server, the default connection is `mongodb://localhost:27017/CV-DIVER`.
2. Open PowerShell in the project folder (the folder containing `mvnw.cmd`).
3. Check Java with `java -version`. It must report version 21 or newer.
4. Create `.env` from the sample if you have not already created it:

   ```powershell
   if (-not (Test-Path .env)) { Copy-Item .env.example .env }
   notepad .env
   ```

   Keep the MongoDB URI shown above for local MongoDB. To use Gemini, set `AI_ENABLED=true` and put your key after `GEMINI_API_KEY=`. Save and close Notepad.

5. Start the app from that same PowerShell window:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

6. Wait for `Started ResumeVaultApplication` in the output, then open [http://localhost:8080](http://localhost:8080). Keep the PowerShell window open while using the app; press Ctrl+C there to stop it.

Use `.\mvnw.cmd`, **not** `mvn`, when Maven is not installed. If PowerShell says it cannot find `mvnw.cmd`, first change to the project folder with `cd "C:\Users\Ishant Mohan\Documents\ChatGPT\CV DIver"`.

## Troubleshooting

- **`mvn` is not recognized:** run `.\mvnw.cmd spring-boot:run` from the project folder. The wrapper is included in this repository.
- **`java` is not recognized or is too old:** install a Java 21 or newer JDK, open a new PowerShell window, and check `java -version` again.
- **MongoDB connection refused:** start the MongoDB service, or correct `MONGODB_URI` in `.env`. The database name in the local example is `CV-DIVER`; do not add a trailing backslash.
- **Port 8080 is already in use:** stop the other app using it, or change `PORT=8081` in `.env`; then open `http://localhost:8081`.
- **Gemini comparison or screenshot reading is unavailable:** set both `AI_ENABLED=true` and `GEMINI_API_KEY=...` in `.env`, then restart the app. The key alone does not enable AI.

Uploads are limited to 500 MB per batch. Original files are saved under `./data/uploads` by default.

## Environment variables

Copy `.env.example` to `.env` and set only the values you need. The app imports `.env` on startup. Environment variables set by your shell override values in the file.

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8080` | HTTP port |
| `MONGODB_URI` | `mongodb://localhost:27017/CV-DIVER` | MongoDB connection string, including database name |
| `UPLOAD_DIR` | `./data/uploads` | Directory for original PDFs and images |
| `MAX_DOCUMENTS` | `70` | Overall document limit (the application also enforces 50 PDFs and 20 screenshots) |
| `AI_ENABLED` | `false` | Set `true` to enable Gemini comparisons and screenshot vision extraction |
| `GEMINI_API_KEY` | empty | Gemini Developer API key used by Spring AI |
| `GEMINI_MODEL` | `gemini-2.5-flash` | Gemini chat and vision model name |

### Optional OCR and AI

PDF text extraction works without an OCR engine. With `AI_ENABLED=true` and `GEMINI_API_KEY` configured, screenshots are sent to Gemini for vision-based transcription. Otherwise, the app can run the `tesseract` command for local OCR if Tesseract is installed, English language data is present, and `tesseract` is on your system `PATH`. No Tesseract folder variable is required. Image resumes can still be opened when OCR is unavailable; the extracted-text pane will explain that no text was found.

AI is opt-in. Set `AI_ENABLED=true` and `GEMINI_API_KEY`. In the comparison window, enter criteria or use the default project comparison prompt to compare project problem, contribution, architecture, technologies, scope, outcomes, and differences. Selected resume text or screenshot content is sent to Gemini for the requested operation. The response cites resume evidence and does not make a hiring decision; confirm details in the original files.

MongoDB stores resume metadata and extracted text. Use a persistent, backed-up volume for `UPLOAD_DIR` when deploying.

## Accessibility and privacy

The interface includes keyboard-accessible controls, visible focus styles, a skip link, semantic table headings, screen-reader labels and live status messages, responsive layouts, reduced-motion support, and J/K or arrow-key resume navigation. Press 1, 2, or 3 in the resume viewer to record a Priority, Shortlist, or Reject verdict. It stores documents on the configured server. If you enable AI, selected extracted text, comparison criteria, or screenshot content are sent to Gemini for processing. This starter is designed for a trusted local deployment and does not include user sign-in or role management; add authentication before sharing a deployment with multiple users or exposing it to the internet. The app does not connect to or scrape LinkedIn. Upload files you have permission to use.

## API overview

- `GET /api/resumes?q=...` — list and search
- `POST /api/resumes` — upload multiple PDF/image files (`files` multipart field)
- `GET /api/resumes/{id}` and `/file` — read metadata/text and original file
- `PATCH /api/resumes/{id}` — update `stage`, `verdict`, `notes`, or comma-separated `tags`
- `DELETE /api/resumes/{id}` — remove a resume and its stored file
- `POST /api/resumes/compare` — load 2–4 selected resumes
- `POST /api/ai/compare` — optional AI summary when configured
