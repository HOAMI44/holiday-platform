# System Operations

This document contains the unified system operations for all services in the Holiday Planner system.
It includes async/sync decisions, cross-service transaction documentation, and domain event schemas.

---

## Legend

- **SYNC** — caller blocks and waits for the response; the result affects whether the operation continues
- **ASYNC** — fire-and-forget via domain event; caller does not wait
- **[NEW]** — operation added during this review (was missing from original spec)

---

## EventManagementService (event-service · port 8081)

| Method Signature | Actor | Mode | Description | Pre-Conditions | Post-Conditions | Collaborators |
|---|---|---|---|---|---|---|
| `Event createEvent(organizationId, eventOwnerId, shortTitle, description, location, meetingPoint, price, paymentMethod, minimalAge, maximalAge, pictureUrl)` **[NEW]** | Event Owner | SYNC | Creates a new event template for an organization | Organization and event owner exist; actor has write access | Event created with no terms | — |
| `Event updateEvent(eventId, shortTitle, description, location, meetingPoint, price, paymentMethod, minimalAge, maximalAge, picture)` | Event Owner | SYNC | Edits the details of an event | Event exists and actor is the owner | Event details are updated | — |
| `List<Event> getEventsByOrganization(organizationId)` **[NEW]** | Any | SYNC | Returns all events for an organization (used on main page) | Organization exists | List of events returned | — |
| `EventTerm createEventTerm(eventId, startDateTime, endDateTime, minimalParticipants, maximalParticipants)` | Event Owner | SYNC | Creates an event term | Event exists and actor is the owner | Event term created with status DRAFT | — |
| `EventTerm changeEventTermStatus(eventTermId, newStatus)` | Event Owner | SYNC (own) + ASYNC (side-effects) | Changes the status of an event term | Event term exists and actor is the owner | Status updated. If CANCELLED: publishes `EventTermCancelled` event | Publishes `EventTermCancelled` → `BookingService::cancelAllBookings(eventTermId)` [ASYNC], `NotificationService::notifyTermCancelled(parentEmail, eventName, termDate)` [ASYNC] |
| `EventTerm updateEventTermCapacity(eventTermId, minimalParticipants, maximalParticipants)` | Event Owner | SYNC (own) + ASYNC (side-effects) | Changes the min/max participants for an event term | Event term exists and actor is the owner; max ≥ confirmed bookings | Capacity updated. If max increased: publishes `CapacityIncreased` event | `BookingService::getBookingCount(eventTermId)` [SYNC], `BookingService::promoteFromWaitingList(eventTermId, addedSlots)` [SYNC], `NotificationService::notifyBookingConfirmed(parentEmail, eventName, termDate)` [ASYNC] |
| `Remark createRemark(eventTermId, participantId, description)` | Event Owner | SYNC | Creates a remark about a participant | Participant and event term exist and actor is the owner | Remark created and visible to all event owners | — |
| `Remark[] getRemarks(eventTermId)` | Event Owner | SYNC | Retrieves remarks about all participants for an event term | Event term exists and actor is the owner | All remarks for participants of the event term are returned | — |
| `EventTerm assignCaregiverToEventTerm(eventTermId, caregiverId)` | Event Owner | SYNC | Assigns a caregiver to an event term | Event term and caregiver exist and actor is the owner | Caregiver assigned to the event term | — |
| `void sendMessageToParticipants(eventTermId, message)` | Event Owner | SYNC (own) + ASYNC (email) | Sends emails to participants of an event term | Event exists and actor is the owner; event term is ACTIVE | Email sent to all participants | `BookingService::getParticipantContactEmails(eventTermId)` [SYNC], `NotificationService::sendBulkEmail(recipients, subject, body)` [ASYNC] |
| `EventTerm verifyEventTerm(eventTermId)` **[NEW]** | System | SYNC | Verifies that an event term exists and is ACTIVE (called by BookingService before creating a booking) | — | EventTerm returned or 404 | — |
| `void autoCancelUnderfilledTerms()` **[NEW]** | Scheduler | ASYNC | Scheduled job: cancels event terms that did not reach minimum participants before start date | Runs nightly; checks terms starting within 24h that are still ACTIVE with bookings < minParticipants | Term status set to CANCELLED; publishes `EventTermCancelled` event | Same as `changeEventTermStatus` → CANCELLED |
| `void scheduleDayBeforeNotifications()` **[NEW]** | Scheduler | ASYNC | Scheduled job: triggers participant-list emails to caregivers one day before each active event term | Runs nightly; finds terms starting the next day | Publishes `ParticipantListRequested` event per term/caregiver pair | `BookingService::getParticipantContactEmails(eventTermId)` [SYNC], `NotificationService::notifyCaregiverWithParticipants(caregiverEmail, eventName, termDate, participants)` [ASYNC] |

---

## BookingService (booking-service · port 8082)

| Method Signature | Actor | Mode | Description | Pre-Conditions | Post-Conditions | Collaborators |
|---|---|---|---|---|---|---|
| `Booking[] getBookingsForEventTerm(eventTermId)` | Event Owner | SYNC | Retrieves all bookings for a given event term | Event term exists and actor is the owner | List of confirmed bookings and waitlist entries returned | `EventManagementService::verifyEventTerm(eventTermId)` [SYNC] |
| `Booking createBooking(familyMemberId, eventTermId)` | User | SYNC (own) + ASYNC (side-effects) | Books an event term for a family member | Event term is ACTIVE; family member meets age requirements; booking window is open | Booking created with CONFIRMED or WAITLISTED status; publishes `BookingCreated` event | `EventManagementService::verifyEventTerm(eventTermId)` [SYNC], `IdentityService::getFamilyMember(familyMemberId)` [SYNC for age check]; then `PaymentService::createPayment(bookingId, organizationId, amount)` [ASYNC via event], `NotificationService::notifyBookingConfirmed(parentEmail, eventName, termDate)` [ASYNC via event] |
| `void cancelBooking(bookingId)` | Event Owner | SYNC (own) + ASYNC (side-effects) | Cancels a booking of a participant | Booking exists and actor is the owner of the associated term | Booking cancelled; next waitlisted entry promoted; publishes `BookingCancelled` | `BookingService::promoteFromWaitingList(eventTermId, 1)` [SYNC internal], `NotificationService::notifyBookingCancelledByOwner(parentEmail, eventName, termDate)` [ASYNC], `NotificationService::notifyBookingConfirmed(promotedParentEmail, eventName, termDate)` [ASYNC] |
| `void cancelBookingByUser(bookingId)` | User | SYNC (own) + ASYNC (side-effects) | Cancels a booking (up to 3 days before event) | Booking exists; cancellation deadline not passed | Booking cancelled; next waitlisted entry promoted; publishes `BookingCancelled` | `BookingService::promoteFromWaitingList(eventTermId, 1)` [SYNC internal], `NotificationService::notifyBookingConfirmed(promotedParentEmail, eventName, termDate)` [ASYNC] |
| `void cancelAllBookings(eventTermId)` **[NEW]** | System (event-driven) | ASYNC | Cancels all bookings for a cancelled event term | Triggered by `EventTermCancelled` event | All bookings for the term set to CANCELLED; publishes `BookingCancelled` per booking | `NotificationService::notifyTermCancelled(parentEmail, eventName, termDate)` [ASYNC per participant] |
| `void promoteFromWaitingList(eventTermId, slots)` **[NEW]** | System | SYNC | Promotes the next N waitlisted bookings to CONFIRMED | Waitlisted bookings exist for the term | Up to `slots` bookings promoted to CONFIRMED; publishes `WaitlistPromoted` per promoted booking | — |
| `long getBookingCount(eventTermId)` **[NEW]** | System | SYNC | Returns the count of CONFIRMED bookings for a term | Event term exists | Count returned | — |
| `boolean hasActiveBookings(memberId)` **[NEW]** | System | SYNC | Checks whether a family member has any CONFIRMED or WAITLISTED bookings | — | Returns true/false | — |
| `List<String> getParticipantContactEmails(eventTermId)` **[NEW]** | System | SYNC | Returns parent email addresses of all confirmed participants of a term | Event term exists | List of email addresses returned | — |

---

## IdentityService (identity-service · port 8083)

| Method Signature | Actor | Mode | Description | Pre-Conditions | Post-Conditions | Collaborators |
|---|---|---|---|---|---|---|
| `User registerUser(email, password, phoneNumber, organizationId)` | User | SYNC | Registers a new user for an organization | Email not already in use; organization exists | New user account created | — |
| `User loginUser(email, password)` **[NEW]** | User | SYNC | Authenticates a user and returns a session token / JWT | User exists; password correct | JWT token returned | — |
| `User getUserById(userId)` **[NEW]** | System | SYNC | Returns user profile (called by other services for age/email resolution) | User exists | User returned | — |
| `FamilyMember addFamilyMember(userId, firstName, lastName, birthDate, zip)` | User | SYNC | Adds a family member to a user's profile | User exists | Family member added to profile | — |
| `FamilyMember updateFamilyMember(memberId, firstName, lastName, birthDate, zip)` | User | SYNC | Updates a family member's details | Family member exists and belongs to user | Family member updated | — |
| `void removeFamilyMember(memberId)` | User | SYNC | Removes a family member | No active bookings exist for this family member | Family member removed | `BookingService::hasActiveBookings(memberId)` [SYNC — must veto before deletion] |
| `List<FamilyMember> getFamilyMembers(userId)` **[NEW]** | User | SYNC | Returns all family members for a user | User exists | List of family members returned | — |
| `Caregiver createCaregiver(firstName, lastName, email, phoneNumber)` | Event Owner | SYNC | Adds a caregiver to the system | Caregiver email not already in use | New caregiver profile created | — |
| `Caregiver getCaregiverById(caregiverId)` **[NEW]** | System | SYNC | Returns a caregiver by ID | Caregiver exists | Caregiver returned | — |

---

## OrganizationService (organization-service · port 8084)

| Method Signature | Actor | Mode | Description | Pre-Conditions | Post-Conditions | Collaborators |
|---|---|---|---|---|---|---|
| `Organization createOrganization(name, bankAccount, bookingStartTime)` | Admin | SYNC | Creates a new organization | Organization name not already taken | New organization created | — |
| `Organization updateOrganization(organizationId, bankAccount, bookingStartTime)` | Org Team | SYNC | Updates organization settings | Organization exists | Organization updated | — |
| `Organization getOrganization(organizationId)` **[NEW]** | Any | SYNC | Returns organization details | Organization exists | Organization returned | — |
| `List<Organization> getAllOrganizations()` **[NEW]** | Any | SYNC | Returns all organizations (used on registration/main page) | — | List of organizations returned | — |
| `TeamMember addTeamMember(organizationId, userId, firstName, lastName, email, role)` | Org Team | SYNC | Adds a team member to the organization | Organization exists | Team member added | — |
| `void removeTeamMember(teamMemberId)` | Org Team | SYNC | Removes a team member | Team member exists | Team member removed | — |
| `List<TeamMember> getTeamMembers(organizationId)` **[NEW]** | Org Team | SYNC | Returns all team members for an organization | Organization exists | List of team members returned | — |
| `Sponsor addSponsor(organizationId, name, amount)` | Org Team | SYNC | Adds a sponsor to the organization | Organization exists | Sponsor added | — |
| `void removeSponsor(sponsorId)` **[NEW]** | Org Team | SYNC | Removes a sponsor from the organization | Sponsor exists | Sponsor removed | — |
| `List<Sponsor> getSponsors(organizationId)` **[NEW]** | Org Team | SYNC | Returns all sponsors for an organization | Organization exists | List of sponsors returned | — |

---

## PaymentService (payment-service · port 8085)

| Method Signature | Actor | Mode | Description | Pre-Conditions | Post-Conditions | Collaborators |
|---|---|---|---|---|---|---|
| `Payment createPayment(bookingId, organizationId, amount)` | System (event-driven) | ASYNC | Creates a payment record for a confirmed booking | Triggered by `BookingCreated` event; booking exists | Payment created with status PENDING | — |
| `Payment markAsPaid(paymentId, note)` | Accountant | SYNC | Marks a payment as paid after bank transfer received | Payment exists and status is PENDING | Payment status set to PAID; paidAt timestamp recorded | — |
| `Payment refundPayment(paymentId, note)` | Accountant | SYNC (own) + ASYNC (notification) | Refunds a payment for a cancelled event | Payment exists and status is PAID | Payment status set to REFUNDED; publishes `PaymentRefunded` event | `NotificationService::notifyRefund(parentEmail, eventName, amount)` [ASYNC via event] |
| `BigDecimal calculateBalance(organizationId)` | Accountant | SYNC | Calculates total income for an organization (sum of PAID payments) | Organization exists | Balance returned | — |
| `List<Payment> getPaymentsByOrganization(organizationId)` **[NEW]** | Accountant | SYNC | Returns all payments for an organization | Organization exists | List of payments returned | — |
| `List<Payment> getPendingPayments(organizationId)` **[NEW]** | Accountant | SYNC | Returns all PENDING payments for an organization | Organization exists | List of pending payments returned | — |
| `Payment getPaymentByBooking(bookingId)` **[NEW]** | Accountant | SYNC | Returns the payment linked to a specific booking | Booking exists | Payment returned | — |

---

## NotificationService (notification-service · port 8086)

All operations are fire-and-forget (ASYNC). Other services publish domain events or call this service after completing their primary operation.

| Method Signature | Actor | Mode | Description | Pre-Conditions | Post-Conditions | Collaborators |
|---|---|---|---|---|---|---|
| `void sendEmail(to, subject, body)` | System | ASYNC | Sends a single email | Valid email address | Email delivered | — |
| `void sendBulkEmail(recipients, subject, body)` | System | ASYNC | Sends email to multiple recipients | Valid list of emails | Emails sent to all recipients | — |
| `void notifyBookingConfirmed(parentEmail, eventName, termDate)` | System | ASYNC | Notifies parent that their booking is confirmed (also used for waitlist promotion) | — | Confirmation email sent | — |
| `void notifyBookingCancelledByOwner(parentEmail, eventName, termDate)` **[NEW]** | System | ASYNC | Notifies parent that their booking was cancelled by the event owner | — | Cancellation email sent | — |
| `void notifyTermCancelled(parentEmail, eventName, termDate)` | System | ASYNC | Notifies parent that an event term was cancelled | — | Cancellation email sent | — |
| `void notifyRefund(parentEmail, eventName, amount)` **[NEW]** | System | ASYNC | Notifies parent that a refund has been issued | Triggered by `PaymentRefunded` event | Refund notification email sent | — |
| `void notifyCaregiverWithParticipants(caregiverEmail, eventName, termDate, participants)` | System | ASYNC | Sends participant list PDF to caregiver one day before event | Triggered by `ParticipantListRequested` event | Email with participant list PDF sent | `BookletService::generateParticipantListPdf(eventName, termDate, participantNames)` [SYNC within notification] |
| `void notifyCaregiversOfAutoCancellation(caregiverEmails, eventName, termDate)` **[NEW]** | System | ASYNC | Notifies all caregivers of an auto-cancelled event term | Triggered by scheduler | Cancellation email sent to each caregiver | — |

---

## BookletService (booklet-service · port 8087)

| Method Signature | Actor | Mode | Description | Pre-Conditions | Post-Conditions | Collaborators |
|---|---|---|---|---|---|---|
| `byte[] generateBooklet(organizationName, contactInfo, eventSummaries, sponsorNames)` | Org Team | SYNC | Generates a PDF booklet for the organization with all active events and sponsors | Organization exists with active events | PDF booklet generated and returned as byte array | — |
| `byte[] generateParticipantListPdf(eventName, termDate, participantNames)` | System | SYNC | Generates a participant list PDF for a caregiver | Event term exists with confirmed participants | PDF generated and returned as byte array | — |

---

## Cross-Service Transactions

### 1. createBooking (Choreography Saga)

**Services involved:** BookingService → EventService (SYNC) → IdentityService (SYNC) → PaymentService (ASYNC) → NotificationService (ASYNC)

**Flow:**
1. BookingService calls `EventManagementService::verifyEventTerm(eventTermId)` — confirms term is ACTIVE [SYNC]
2. BookingService calls `IdentityService::getFamilyMember(familyMemberId)` — confirms age eligibility against event's min/max age [SYNC]
3. BookingService creates the Booking record (CONFIRMED or WAITLISTED)
4. BookingService publishes `BookingCreated` event
5. PaymentService subscribes → calls `createPayment(bookingId, organizationId, amount)` [ASYNC]
6. NotificationService subscribes → calls `notifyBookingConfirmed(parentEmail, eventName, termDate)` [ASYNC]

**What can go wrong:**
- EventTerm not found or not ACTIVE → return 404/409 immediately; no booking created
- FamilyMember age not eligible → return 400; no booking created
- Payment creation fails after booking is saved → booking exists without payment; compensate by scheduling a retry or flagging for manual resolution

**Compensation:** If payment creation fails, a background reconciliation job can detect bookings without a corresponding payment and create them retroactively. Alternatively, use an outbox pattern to guarantee at-least-once delivery of the `BookingCreated` event.

---

### 2. cancelBooking (Choreography Saga)

**Services involved:** BookingService → BookingService (internal) → NotificationService (ASYNC) → PaymentService (ASYNC, if refund applies)

**Flow:**
1. BookingService cancels the booking record
2. BookingService internally calls `promoteFromWaitingList(eventTermId, 1)` — promotes next waitlisted entry [SYNC]
3. Publishes `BookingCancelled` event
4. NotificationService subscribes → notifies cancelled participant [ASYNC]
5. NotificationService subscribes → notifies promoted participant (if any) [ASYNC]
6. PaymentService subscribes → if payment exists and is PAID, triggers refund process [ASYNC]

**What can go wrong:**
- Waitlist promotion fails → booking is cancelled but no one is promoted; safe to retry
- Notification fails → email not sent, but booking is correctly cancelled; acceptable failure

---

### 3. changeEventTermStatus to CANCELLED (Choreography Saga)

**Services involved:** EventService → BookingService (ASYNC) → PaymentService (ASYNC) → NotificationService (ASYNC)

**Flow:**
1. EventService updates term status to CANCELLED
2. Publishes `EventTermCancelled` event
3. BookingService subscribes → cancels all bookings for the term [ASYNC]; publishes `BookingCancelled` per booking
4. NotificationService subscribes to `EventTermCancelled` → notifies all caregivers [ASYNC]
5. NotificationService subscribes to `BookingCancelled` → notifies each parent [ASYNC]
6. PaymentService subscribes to `BookingCancelled` → refunds PAID payments [ASYNC]

**What can go wrong:**
- BookingService cancellation is partially done (e.g., crash halfway through) → idempotent retry: re-process `EventTermCancelled` event for any bookings still not CANCELLED
- Notification or refund fails → retry via message queue dead-letter handling

---

### 4. removeFamilyMember (Guard Check)

**Services involved:** IdentityService → BookingService (SYNC veto check)

**Flow:**
1. IdentityService calls `BookingService::hasActiveBookings(memberId)` [SYNC]
2. If true → return 409 Conflict; family member not removed
3. If false → delete family member

**What can go wrong:**
- BookingService is unavailable → fail safe: reject the deletion (do not remove)
- Race condition: booking created between the check and deletion → rare; acceptable with clear user-facing error

---

### 5. autoCancelUnderfilledTerms (Scheduled Saga)

**Services involved:** EventService (scheduler) → EventService (status update) → BookingService (ASYNC) → NotificationService (ASYNC) → PaymentService (ASYNC)

**Flow:** Same as "changeEventTermStatus to CANCELLED" above, triggered by the nightly scheduler instead of a user action.

---

## Domain Events

### BookingCreated
```json
{
  "eventType": "BookingCreated",
  "timestamp": "2026-07-01T10:00:00Z",
  "payload": {
    "bookingId": "uuid",
    "familyMemberId": "uuid",
    "eventTermId": "uuid",
    "status": "CONFIRMED",
    "parentEmail": "parent@example.com",
    "eventName": "Bicycle Tour",
    "termDate": "2026-07-15T09:00:00Z",
    "organizationId": "uuid",
    "amount": 15.00
  }
}
```
**Subscribers:** PaymentService (creates payment), NotificationService (sends confirmation email)

---

### BookingCancelled
```json
{
  "eventType": "BookingCancelled",
  "timestamp": "2026-07-01T10:00:00Z",
  "payload": {
    "bookingId": "uuid",
    "familyMemberId": "uuid",
    "eventTermId": "uuid",
    "parentEmail": "parent@example.com",
    "eventName": "Bicycle Tour",
    "termDate": "2026-07-15T09:00:00Z",
    "cancelledBy": "USER | EVENT_OWNER | SYSTEM"
  }
}
```
**Subscribers:** NotificationService (notifies parent), PaymentService (triggers refund if applicable)

---

### EventTermCancelled
```json
{
  "eventType": "EventTermCancelled",
  "timestamp": "2026-07-01T10:00:00Z",
  "payload": {
    "eventTermId": "uuid",
    "eventName": "Bicycle Tour",
    "termDate": "2026-07-15T09:00:00Z",
    "organizationId": "uuid",
    "caregiverIds": ["uuid", "uuid"],
    "cancelledBy": "EVENT_OWNER | SYSTEM"
  }
}
```
**Subscribers:** BookingService (cancels all bookings), NotificationService (notifies caregivers)

---

### WaitlistPromoted
```json
{
  "eventType": "WaitlistPromoted",
  "timestamp": "2026-07-01T10:00:00Z",
  "payload": {
    "bookingId": "uuid",
    "familyMemberId": "uuid",
    "eventTermId": "uuid",
    "parentEmail": "parent@example.com",
    "eventName": "Bicycle Tour",
    "termDate": "2026-07-15T09:00:00Z"
  }
}
```
**Subscribers:** NotificationService (sends confirmation to promoted parent)

---

### ParticipantListRequested
```json
{
  "eventType": "ParticipantListRequested",
  "timestamp": "2026-07-14T02:00:00Z",
  "payload": {
    "eventTermId": "uuid",
    "caregiverEmail": "caregiver@example.com",
    "eventName": "Bicycle Tour",
    "termDate": "2026-07-15T09:00:00Z",
    "participantNames": ["Anna Müller", "Max Muster"]
  }
}
```
**Subscribers:** NotificationService (sends PDF participant list to caregiver)

---

### PaymentRefunded
```json
{
  "eventType": "PaymentRefunded",
  "timestamp": "2026-07-01T10:00:00Z",
  "payload": {
    "paymentId": "uuid",
    "bookingId": "uuid",
    "organizationId": "uuid",
    "parentEmail": "parent@example.com",
    "eventName": "Bicycle Tour",
    "amount": 15.00
  }
}
```
**Subscribers:** NotificationService (notifies parent about refund)

---

## Open Questions & Insights

1. **Age verification in createBooking**: The BookingService currently accepts `maxParticipants` as a request parameter — this is wrong. It should fetch event details from EventService to verify the age of the FamilyMember and determine capacity. The `maxParticipants` field should be removed from `CreateBookingRequest`; BookingService should call EventService to get these values.

2. **Payment amount**: When `BookingCreated` is published, the amount must be included so PaymentService can create the record without calling back to EventService. This means EventService (or BookingService after calling EventService) must include the event price in the event payload.

3. **Booking window**: `Organization.bookingStartTime` defines when booking opens. This check should happen in BookingService (or a shared utility), calling OrganizationService to get the start time. Currently not implemented.

4. **JWT / Authentication**: The identity-service has Spring Security configured. `loginUser` should return a JWT. Other services should validate the JWT locally (shared secret or public key). This is not yet defined for inter-service calls.

5. **Caregiver notification scope**: `notifyCaregiverWithParticipants` is triggered the day before the event. The participant list PDF is generated by BookletService. NotificationService must call BookletService synchronously within the notification flow, or receive the PDF bytes in the event payload.

6. **Auto-cancellation**: The scheduler in EventService must know which terms are below `minParticipants` on the day before start. This requires a query across EventService data only — no cross-service calls needed for the check itself.

7. **WAITLISTED bookings and payment**: Should WAITLISTED bookings trigger a payment? Currently the `BookingCreated` event implies payment creation. A decision is needed: only create payment when CONFIRMED, or hold a provisional payment for WAITLISTED. Recommended: only create payment on CONFIRMED status.
