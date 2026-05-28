## 1. What was tested?

Main use case of booking-service: **`createBooking`**

A user books an EventTerm for a FamilyMember. The system decides if the booking is **CONFIRMED** (spot available) or **WAITLISTED** (fully booked).

| Level | Class | Purpose |
|---|---|---|
| Unit Test | `BookingServiceUnitTest` | Business logic only, all dependencies mocked |
| Integration Test | `BookingServiceIntegrationTest` | Service + real H2 database + mocked IPC |
| Component Test | `BookingControllerComponentTest` | Full HTTP stack as black box |
| Contract Test (Provider) | `BookingProviderContractTest` | API shape that booking-service exposes |
| Contract Test (Consumer) | `EventServiceConsumerContractTest` | How booking-service consumes event-service |

---

## 2. How were contracts tested?

booking-service calls `GET /api/events/terms/{id}` on event-service. If event-service renames `maxParticipants` to `capacity`, booking-service silently breaks — no compile error.

**Provider-side** (`BookingProviderContractTest`): verifies that booking-service always returns the expected fields (`id`, `status`, `familyMemberId`, `bookedAt`). If a field is renamed or removed, the test fails.

**Consumer-side** (`EventServiceConsumerContractTest`): WireMock simulates event-service with the agreed response shape. Verifies that booking-service correctly parses the response. If event-service changes the field name, the parser returns 0 instead of 20 — the test fails.

**Limitation:** WireMock always returns the stubbed response, so if event-service actually changes its API, our tests still pass. This is a parsing test, not a true contract test. A real solution would be **Pact** or **Spring Cloud Contract**, where event-service runs our consumer contract as a provider test.

---

## 3. Findings

**Security fix:** The original `createBooking` accepted `maxParticipants` as a query parameter from the client — any user could set the capacity themselves. Fixed by removing the parameter; booking-service now fetches capacity directly from event-service.

**`@PrePersist` not triggered in unit tests:** `Booking.bookedAt` is set via `@PrePersist`. In unit tests with a mocked repository, no real JPA persist happens → `bookedAt` stays null. Only testable in integration tests with a real DB.

**Partial response works:** If event-service only returns the required fields (no `startDate`, no `eventId`), booking-service does not crash. Jackson ignores missing fields by default.

**Missing `@Transactional` on `cancelBooking`:** `cancelBooking` does 2 DB writes (cancel + promote). If the second write fails, the booking is cancelled but nobody gets promoted → inconsistent state. Open: add `@Transactional`.

---

## 4. Questions

**Q1 — Contract testing without a framework:**
Our consumer contract tests with WireMock only verify that *we* parse the response correctly. If event-service changes its API, our tests still pass (WireMock returns the old response). Is this still a contract test, or just a parsing test?

**Q2 — Choreography vs. Orchestration:**
`cancelBooking` should also notify a payment service and a notification service. Is choreography (events/message queue) better here than direct calls? When is the overhead of a message queue worth it?

**Q3 — H2 vs. real PostgreSQL:**
H2 and PostgreSQL behave differently in some cases (UUID generation, specific SQL functions). How reliable are our integration tests with H2 when production runs on PostgreSQL? When do you need Testcontainers?

**Q4 — Timeout configuration:**
We set 500ms timeout manually on the RestClient in the consumer contract test. How should this be configured in a production system — per service or globally?

**Q5 — IPC Security:**
There is currently no authentication between services (booking-service calls event-service without a token). How would you implement service-to-service authentication in Spring Boot — JWT with shared secret, mTLS, or OAuth2 Client Credentials?
