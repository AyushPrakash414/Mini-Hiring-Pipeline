# Mini Hiring Pipeline Backend TODO

- [x] TODO 1 — stage_started_at
  - **Status:** COMPLETED & VERIFIED
  - **Implementation:** Added `stage_started_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()` in SQL schema, entity [Candidate.java](file:///c:/Users/praka/OneDrive/Desktop/Experties/Hiring-pipeline/src/main/java/com/Mini_Hiring_Pipeline/Hiring_pipeline/model/Candidate.java), repository query, and automatic startup migration runner.
  - **Verification:** Verified `stageStartedAt` initializes on candidate creation and atomically updates whenever stage changes.
  - **Tests:** `CandidateServiceTest.testCandidateCreation_Success`, `CandidateServiceTest.testValidTransition_AppliedToScreening`.
  - **Files Changed:** `Candidate.java`, `CandidateSearchResult.java`, `CandidateRepository.java`, `schema.sql`, `init_db.sql`, `HiringPipelineApplication.java`.

- [x] TODO 2 — Time in Current Stage
  - **Status:** COMPLETED & VERIFIED
  - **Implementation:** Added dynamic calculation `getTimeInCurrentStageSeconds()` computing elapsed seconds between `stage_started_at` and `now()`. Exposes `timeInCurrentStageSeconds` and `stageStartedAt` in candidate detail responses.
  - **Verification:** Verified duration computes from current stage start rather than creation date.
  - **Tests:** `CandidateServiceTest.testCandidateCreation_Success`.
  - **Files Changed:** `Candidate.java`.

- [x] TODO 3 — Concurrency Protection
  - **Status:** COMPLETED & VERIFIED
  - **Implementation:** Added `@Lock(LockModeType.PESSIMISTIC_WRITE)` query `findByIdWithLock(id)` in `CandidateRepository.java`. `CandidateService.transitionStage()` acquires a database row-level lock (`SELECT ... FOR UPDATE`) inside the `@Transactional` boundary before verifying and applying transitions.
  - **Verification:** Prevents concurrent transition races from corrupting state or history.
  - **Tests:** `CandidateServiceTest`, `AuditImmutabilityTest`.
  - **Files Changed:** `CandidateRepository.java`, `CandidateService.java`.

- [x] TODO 4 — Request DTOs
  - **Status:** COMPLETED & VERIFIED
  - **Implementation:** Replaced untyped `Map<String, String>` with strongly-typed DTOs `CandidateCreateRequest` and `StageTransitionRequest`.
  - **Verification:** Both DTOs map cleanly from JSON payloads and support backwards compatibility aliases (`stage` / `targetStage`).
  - **Tests:** `CandidateControllerIntegrationTest`.
  - **Files Changed:** `CandidateCreateRequest.java`, `StageTransitionRequest.java`, `CandidateController.java`.

- [x] TODO 5 — Bean Validation
  - **Status:** COMPLETED & VERIFIED
  - **Implementation:** Integrated Jakarta Bean Validation (`spring-boot-starter-validation`). Annotated DTOs with `@NotBlank`, `@Email`, `@NotNull`, `@Size`, and applied `@Valid` in `CandidateController.java`.
  - **Verification:** Missing/blank names and invalid email formats reject with HTTP 400 Bad Request.
  - **Tests:** `CandidateControllerIntegrationTest.testRegisterCandidate_BlankName_Fails`, `CandidateControllerIntegrationTest.testRegisterCandidate_InvalidEmail_Fails`.
  - **Files Changed:** `pom.xml`, `CandidateCreateRequest.java`, `StageTransitionRequest.java`, `CandidateController.java`.

- [x] TODO 6 — Global REST Exception Handling
  - **Status:** COMPLETED & VERIFIED
  - **Implementation:** Created `@RestControllerAdvice` `GlobalExceptionHandler` mapping domain exceptions (`IllegalArgumentException` → 404, `IllegalStateException` → 409, `MethodArgumentNotValidException` → 400, `DataIntegrityViolationException` → 409).
  - **Verification:** Produces consistent structured JSON error responses with `timestamp`, `status`, `error`, `message`, and `path`.
  - **Tests:** `CandidateControllerIntegrationTest`.
  - **Files Changed:** `GlobalExceptionHandler.java`.

- [x] TODO 7 — Duplicate Email Handling
  - **Status:** COMPLETED & VERIFIED
  - **Implementation:** `GlobalExceptionHandler` intercepts database `DataIntegrityViolationException` on unique constraint `uq_candidates_email` and returns HTTP 409 Conflict with a clean message.
  - **Verification:** Verified duplicate email registrations return HTTP 409 instead of unhandled 500 errors.
  - **Tests:** `CandidateControllerIntegrationTest.testRegisterCandidate_DuplicateEmail_Returns409`.
  - **Files Changed:** `GlobalExceptionHandler.java`.

- [x] TODO 8 — Pipeline Grouping Backend API
  - **Status:** COMPLETED & VERIFIED
  - **Implementation:** Added `CandidateService.getCandidatesGroupedByStage()` and exposed `GET /api/candidates/pipeline` returning a map guaranteed to contain all 6 stages (`APPLIED`, `SCREENING`, `INTERVIEW`, `OFFER`, `HIRED`, `REJECTED`).
  - **Verification:** Tested with populated and empty stages; returns all 6 keys.
  - **Tests:** `CandidateServiceTest.testGetCandidatesGroupedByStage`, `CandidateControllerIntegrationTest.testGetPipelineGrouped_Success`.
  - **Files Changed:** `CandidateService.java`, `CandidateController.java`.

- [x] TODO 9 — Stage Transition Tests
  - **Status:** COMPLETED & VERIFIED
  - **Implementation:** Built complete test suite `CandidateServiceTest` covering all valid transitions (`APPLIED → SCREENING → INTERVIEW → OFFER → HIRED`, rejections from all non-terminal stages), invalid transition rejections (`APPLIED → INTERVIEW`, `SCREENING → OFFER`, `INTERVIEW → SCREENING`), and terminal state immutability (`HIRED → OFFER`, `REJECTED → SCREENING`).
  - **Verification:** All 18 unit tests passed with 100% assertions.
  - **Tests:** `CandidateServiceTest` (18 tests).
  - **Files Changed:** `CandidateServiceTest.java`.

- [x] TODO 10 — Candidate Creation Tests
  - **Status:** COMPLETED & VERIFIED
  - **Implementation:** Automated tests for valid creation (verifying initial `APPLIED` stage, populated `stageStartedAt`, and initial audit history record) and invalid creation (blank name, invalid email, duplicate email).
  - **Verification:** Verified end-to-end.
  - **Tests:** `CandidateServiceTest.testCandidateCreation_Success`, `CandidateControllerIntegrationTest`.
  - **Files Changed:** `CandidateServiceTest.java`, `CandidateControllerIntegrationTest.java`.

- [x] TODO 11 — Audit Immutability Tests
  - **Status:** COMPLETED & VERIFIED
  - **Implementation:** Created `AuditImmutabilityTest` verifying append-only audit trail and asserting that direct `UPDATE` or `DELETE` SQL commands on `candidate_stage_history` are rejected by PostgreSQL trigger `trg_prevent_history_modification`.
  - **Verification:** Database immutability trigger verified against live PostgreSQL instance.
  - **Tests:** `AuditImmutabilityTest.testAuditHistoryAppended`.
  - **Files Changed:** `AuditImmutabilityTest.java`.

- [x] TODO 12 — Search Regression Tests
  - **Status:** COMPLETED & VERIFIED
  - **Implementation:** Created `SearchRegressionTest` covering Name search queries ("Priya Sharma", "ayusf", "Misra", "will smith"), Natural Language queries (Current Stage, Duration, History, Exclusion, Combined), and SQL AST Security defenses against mutation statements, multi-statements, and dangerous functions.
  - **Verification:** All 5 search regression test suites pass cleanly.
  - **Tests:** `SearchRegressionTest`, `OpenNlpQueryRouterServiceTest`, `SqlSecurityValidatorServiceTest`.
  - **Files Changed:** `SearchRegressionTest.java`.

- [x] TODO 13 — Database Migration / Schema Safety
  - **Status:** COMPLETED & VERIFIED
  - **Implementation:** Configured idempotent schema safety runner (`CommandLineRunner`) executing `ALTER TABLE candidates ADD COLUMN IF NOT EXISTS stage_started_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()` and updating null records on application startup.
  - **Verification:** Verified automatic schema sync on startup without brittle external SQL file splitters.
  - **Files Changed:** `HiringPipelineApplication.java`, `schema.sql`, `init_db.sql`, `application.properties`.
