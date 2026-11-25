# CV Diver — Resume library

A private, single-workspace resume library built with Java, Spring Boot, Spring AI, and MongoDB. Keep up to **50 PDFs and 20 screenshots**, extract text for searching and reading, skip duplicate uploads, review originals, track a review stage and verdict, and compare up to four candidates side by side against your own criteria.

## Run locally

Requirements: Java 21 or newer, and MongoDB running locally or remotely. The Maven Wrapper downloads Maven on its first run. Uploads are limited to 500 MB per batch. From this folder:

```powershell
.\mvnw.cmd spring-boot:run
```

Then open [http://localhost:8080](http://localhost:8080). By default, the library connects to MongoDB at `mongodb://localhost:27017/CV-DIVER` and saves uploaded files under `./data/uploads`. Set `MONGODB_URI` to your MongoDB connection string.

## Environment variables

Copy `.env.example` to `.env` and set only the values you need. The app imports `.env` on startup. Environment variables set by your shell override values in the file.

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8080` | HTTP port |
| `MONGODB_URI` | `mongodb://localhost:27017/CV-DIVER` | MongoDB connection string, including database name |
| `UPLOAD_DIR` | `./data/uploads` | Directory for original PDFs and images |
| `MAX_DOCUMENTS` | `70` | Overall document limit (the application also enforces 50 PDFs and 20 screenshots) |
| `TESSERACT_PATH` | empty | Path to the Tesseract `tessdata` directory, for example `C:\Program Files\Tesseract-OCR\tessdata` |
| `AI_ENABLED` | `false` | Set `true` to enable Gemini comparisons and screenshot vision extraction |
| `GEMINI_API_KEY` | empty | Gemini Developer API key used by Spring AI |
| `GEMINI_MODEL` | `gemini-2.5-flash` | Gemini chat and vision model name |

### Optional OCR and AI

PDF text extraction works without an OCR engine. With `AI_ENABLED=true` and `GEMINI_API_KEY` configured, screenshots are sent to Gemini for vision-based transcription. If Gemini is disabled, install Tesseract with English language data and set `TESSERACT_PATH` to the folder containing `eng.traineddata`. Image resumes can still be opened when OCR is unavailable; the extracted-text pane will explain that no text was found.

AI is opt-in. Set `AI_ENABLED=true` and `GEMINI_API_KEY`. When you request an AI comparison or use vision extraction, selected resume text or screenshot content is sent to Gemini. AI output is intended as a grounded reading aid, asks for evidence from the resumes, and does not make a hiring decision. Confirm details in the original files.

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
