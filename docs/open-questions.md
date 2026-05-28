# Offene Fragen

1.  **Who decides max participants?** Right now the client (frontend) sends how many max participants an event has when booking. That's wrong and dangerous - the server should look this up itself from `EventService`.

2.  **When do we create a payment?** If someone is on the waiting list, should we already create a payment record? Probably not — we should only create one when they actually get a confirmed spot.

3.  **How does BookingService know the price?** When a booking is created, we need to know the event price to create a payment. But `BookingService` doesn't have this info - it needs to ask `EventService` somehow.

4.  **How do services talk to each other securely?** We haven't defined how services authenticate themselves when calling each other. Do they use a shared password? Tokens? Certificates?

5.  **What if the PDF is too big?** When a caregiver needs the participant list, `NotificationService` asks `BookletService` to generate a PDF and waits. If there are many participants this could be too slow and time out.

6.  **Remarks only show for one event term** The spec says a remark about a child should show up everywhere that child is booked. But right now you can only see remarks for one specific event term, not across all of them.

7.  **Who cancelled the booking?** When a booking is cancelled we want to know if it was the parent, the event owner, or the system (auto-cancel). But we don't store this information anywhere yet.