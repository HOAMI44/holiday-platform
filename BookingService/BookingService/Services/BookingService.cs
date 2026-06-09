using BookingService.DTO.payloads;

namespace BookingService.Services;

using BookingService.Models;
using BookingService.DTO;
using BookingService.Repositories;
using BookingService.Exceptions;
using BookingService.Kafka;

public interface IBookingService
{
    Task<BookingResponse> GetBookingAsync(Guid bookingId);
    Task<List<BookingResponse>> GetBookingsForEventTermAsync(Guid eventTermId);
    Task<long> GetBookingCountAsync(Guid eventTermId);
    Task<List<string>> GetParticipantParentEmailsAsync(Guid eventTermId);
    Task<List<string>> GetParticipantDisplayNamesAsync(Guid eventTermId);
    Task<EventTermSummaryResponse> GetEventTermSummaryAsync(Guid eventTermId);
    Task<List<BookingResponse>> GetBookingsForFamilyMemberAsync(Guid familyMemberId);
    Task<List<BookingDetailResponse>> GetBookingsForFamilyMemberEnrichedAsync(Guid familyMemberId);
    Task<BookingResponse> CreateBookingAsync(Guid familyMemberId, Guid eventTermId, string? parentEmail);
    Task<BookingResponse> CancelBookingAsync(Guid bookingId);
    Task CancelAllBookingsAsync(Guid eventTermId);
}

public class BookingServiceImpl : IBookingService
{
    private readonly IBookingRepository _bookingRepository;
    private readonly IEventServiceClient _eventServiceClient;
    private readonly IBookingEventProducer _bookingEventProducer;
    private readonly ILogger<BookingServiceImpl> _logger;

    public BookingServiceImpl(
        IBookingRepository bookingRepository,
        IEventServiceClient eventServiceClient,
        IBookingEventProducer bookingEventProducer,
        ILogger<BookingServiceImpl> logger)
    {
        _bookingRepository = bookingRepository;
        _eventServiceClient = eventServiceClient;
        _bookingEventProducer = bookingEventProducer;
        _logger = logger;
    }

    public async Task<BookingResponse> GetBookingAsync(Guid bookingId)
    {
        var booking = await _bookingRepository.GetByIdAsync(bookingId)
            ?? throw new BookingNotFoundException(bookingId);
        return BookingResponse.From(booking);
    }

    public async Task<List<BookingResponse>> GetBookingsForEventTermAsync(Guid eventTermId)
    {
        var bookings = await _bookingRepository.GetByEventTermIdAsync(eventTermId);
        return bookings.Select(BookingResponse.From).ToList();
    }

    public async Task<long> GetBookingCountAsync(Guid eventTermId)
    {
        return await _bookingRepository.CountByEventTermIdAndStatusAsync(eventTermId, BookingStatus.CONFIRMED);
    }

    public async Task<List<string>> GetParticipantParentEmailsAsync(Guid eventTermId)
    {
        var bookings = await _bookingRepository.GetByEventTermIdAndStatusAsync(eventTermId, BookingStatus.CONFIRMED);
        // In real implementation, would fetch parent emails from external service
        return bookings.Select(b => $"parent-{b.FamilyMemberId}@example.com").ToList();
    }

    public async Task<List<string>> GetParticipantDisplayNamesAsync(Guid eventTermId)
    {
        var bookings = await _bookingRepository.GetByEventTermIdAndStatusAsync(eventTermId, BookingStatus.CONFIRMED);
        return bookings.Select(b => $"Participant-{b.FamilyMemberId}").ToList();
    }

    public async Task<EventTermSummaryResponse> GetEventTermSummaryAsync(Guid eventTermId)
    {
        var bookings = await _bookingRepository.GetByEventTermIdAsync(eventTermId);
        
        return new EventTermSummaryResponse
        {
            EventTermId = eventTermId,
            ConfirmedCount = bookings.Count(b => b.Status == BookingStatus.CONFIRMED),
            WaitlistedCount = bookings.Count(b => b.Status == BookingStatus.WAITLISTED),
            CancelledCount = bookings.Count(b => b.Status == BookingStatus.CANCELLED),
            TotalCount = bookings.Count
        };
    }

    public async Task<List<BookingResponse>> GetBookingsForFamilyMemberAsync(Guid familyMemberId)
    {
        var bookings = await _bookingRepository.GetByFamilyMemberIdAsync(familyMemberId);
        return bookings.Select(BookingResponse.From).ToList();
    }

    public async Task<List<BookingDetailResponse>> GetBookingsForFamilyMemberEnrichedAsync(Guid familyMemberId)
    {
        var bookings = await _bookingRepository.GetActiveBookingsByFamilyMemberAsync(familyMemberId);
        return bookings.Select(b => BookingDetailResponse.From(b)).ToList();
    }

    public async Task<BookingResponse> CreateBookingAsync(Guid familyMemberId, Guid eventTermId, string? parentEmail)
    {
        //Caching in GetEventTerm
        var eventTerm = await _eventServiceClient.GetEventTermAsync(eventTermId);

        if (eventTerm.Status != "ACTIVE")
            throw new InvalidOperationException($"Event term is not active: {eventTermId}");

        var confirmedCount = await _bookingRepository.CountByEventTermIdAndStatusAsync(eventTermId, BookingStatus.CONFIRMED);
        var status = confirmedCount < eventTerm.MaxParticipants ? BookingStatus.CONFIRMED : BookingStatus.WAITLISTED;

        var booking = new Booking
        {
            FamilyMemberId = familyMemberId,
            EventTermId = eventTermId,
            Status = status
        };

        var saved = await _bookingRepository.AddAsync(booking);

        var payload = new BookingCreatedPayload
        {
            BookingId = saved.Id,
            FamilyMemberId = saved.FamilyMemberId,
            EventTermId = saved.EventTermId,
            Status = status.ToString(),
            ParentEmail = parentEmail,
            EventName = eventTerm.EventName,
            TermDate = eventTerm.StartDateTime.ToString(),
            OrganizationId = eventTerm.OrganizationId,
            Amount = eventTerm.Price
        };
        await _bookingEventProducer.PublishBookingCreatedAsync(payload);

        return BookingResponse.From(saved);
    }

    public async Task<BookingResponse> CancelBookingAsync(Guid bookingId)
    {
        var booking = await _bookingRepository.GetByIdAsync(bookingId)
            ?? throw new BookingNotFoundException(bookingId);

        booking.Status = BookingStatus.CANCELLED;
        await _bookingRepository.UpdateAsync(booking);

        var payload = new BookingCancelledPayload
        {
            BookingId = booking.Id,
            FamilyMemberId = booking.FamilyMemberId,
            EventTermId = booking.EventTermId,
            ReasonCode = "parent"
        };
        await _bookingEventProducer.PublishBookingCancelledAsync(payload);

        if (booking.EventTermId != Guid.Empty)
        {
            await PromoteFromWaitingListAsync(booking.EventTermId, 1);
        }

        return BookingResponse.From(booking);
    }

    public async Task CancelAllBookingsAsync(Guid eventTermId)
    {
        var bookings = await _bookingRepository.GetByEventTermIdAsync(eventTermId);
        var activeBookings = bookings.Where(b => b.Status != BookingStatus.CANCELLED).ToList();

        foreach (var booking in activeBookings)
        {
            booking.Status = BookingStatus.CANCELLED;
            await _bookingRepository.UpdateAsync(booking);
        }
    }

    private async Task PromoteFromWaitingListAsync(Guid eventTermId, int slots)
    {
        var waitlisted = await _bookingRepository.GetByEventTermIdAndStatusAsync(eventTermId, BookingStatus.WAITLISTED);
        //Take heißt nimmt die "slots" ersten Elemente der Liste
        foreach (var booking in waitlisted.Take(slots))
        {
            booking.Status = BookingStatus.CONFIRMED;
            await _bookingRepository.UpdateAsync(booking);

            var payload = new WaitListPromotedPayload
            {
                BookingId = booking.Id,
                FamilyMemberId = booking.FamilyMemberId,
                EventTermId = booking.EventTermId
            };
            await _bookingEventProducer.PublishWaitlistPromotedAsync(payload);
        }
    }
}
