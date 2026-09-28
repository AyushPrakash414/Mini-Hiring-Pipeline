# FRONTEND TODO — React Hiring Pipeline

## Backend API Contracts (Reference)
| Endpoint | Method | Request | Response |
|---|---|---|---|
| `/api/candidates` | GET | — | `List<Candidate>` |
| `/api/candidates/{id}` | GET | — | `Candidate` (includes `stageStartedAt`, `timeInCurrentStageSeconds`) |
| `/api/candidates/{id}/history` | GET | — | `List<CandidateStageHistory>` |
| `/api/candidates/pipeline` | GET | — | `Map<CandidateStage, List<Candidate>>` |
| `/api/candidates/search?query=&limit=` | GET | query param | `List<CandidateSearchResult>` (fuzzy name) |
| `/api/candidates/route-query?query=` | GET | query param | `QueryRouteResponse` (intent: NAME_SEARCH or NATURAL_LANGUAGE) |
| `/api/candidates/nl-query` | POST | `{query: string}` | `LlmSecurityResponse` (NL query w/ results) |
| `/api/candidates` | POST | `{name, email, phone}` | `Candidate` (201) |
| `/api/candidates/{id}/transition` | POST | `{targetStage, reason}` | `Candidate` |
| `/api/candidates/seed` | POST | — | `{message: string}` |

## Error Response Format
```json
{ "timestamp": "...", "status": 400/404/409/500, "error": "...", "message": "...", "path": "..." }
```

## TODOs

- [x] 1. Set up React project with Vite in `frontend/` directory
  - **Verified:** `npm run build` succeeds, dev server runs on port 3000, Vite proxy configured for `/api` → `localhost:8080`

- [x] 2. Create main layout (Header, Search bar, Pipeline area)
  - **Verified:** Header shows "Hiring Pipeline" with orange dot, subtitle, "Seed Demo Data" + "+ Add Candidate" buttons. Search bar with placeholder visible. White/orange clean design.

- [x] 3. Implement Pipeline view — stages with candidate cards
  - **Verified:** All 6 stages render (APPLIED:11, SCREENING:9, INTERVIEW:9, OFFER:9, HIRED:8, REJECTED:4). Each card shows name, email, time in stage, stage badge. Cards are clickable.

- [x] 4. Implement Add Candidate modal with validation
  - **Verified:** Modal opens with Name/Email/Phone fields. Submitted "Test React User" → modal closed, toast appeared, APPLIED count updated 11→12, candidate card visible in APPLIED column.

- [x] 5. Implement Candidate Detail modal with history timeline
  - **Verified:** Detail modal shows name, email, phone, current stage badge, stage since date, time in current stage (e.g. "1h 38m"), action buttons ("Move to Screening" + "Reject"), and history timeline with chronological events.

- [x] 6. Implement Stage Transition actions (advance + reject)
  - **Verified:** Moved "Ayush Prakash Tiwari" APPLIED→SCREENING with reason "Testing from React". Pipeline updated (APPLIED:12→11, SCREENING:9→10). Detail modal refreshed showing SCREENING stage, updated timeline with 2 events including the new transition.

- [x] 7. Implement unified Search (fuzzy name + NL query routing)
  - **Verified:** Fuzzy search "ayufh" returned 2 results (Ayush Kumar, Ayush Prakash Tiwari) with stage badges. NL queries routed via `/route-query` → `/nl-query`. Error/denied states handled with user-friendly messages.

- [x] 8. Implement loading/error/empty states for all views
  - **Verified:** Pipeline shows spinner while loading, error state with retry button when backend is down, empty stage placeholder, search loading spinner, search error banner, form validation errors, transition conflict errors.

- [x] 9. Responsive layout + accessibility
  - **Verified:** Pipeline scrolls horizontally on smaller screens. Modals respect viewport. All interactive elements have focus-visible outlines. Buttons/inputs have proper labels and aria attributes. Keyboard navigation (Enter/Escape) works.

- [x] 10. Wire Spring Boot to serve React build, remove old static HTML
  - **Verified:** Old `src/main/resources/static/` content replaced with React build output. Spring Boot serves React app at root `/`.

- [x] 11. Final end-to-end verification
  - **Verified:** All 6 test flows passed (pipeline load, seed, add candidate, fuzzy search, candidate detail, stage transition). Production build succeeds (13.72 KB CSS, 239.54 KB JS gzipped to 3.38 KB + 73.34 KB).

## Backend API Gaps
- **None.** All required data (`stageStartedAt`, `timeInCurrentStageSeconds`) is exposed by the backend via the `Candidate` entity's `@Transient` getter.
