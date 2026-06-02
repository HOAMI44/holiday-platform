namespace BookingService.Controllers;

using BookingService.Services;
using BookingService.DTO;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;

[ApiController]
[Route("api/bookings")]
public class BookingController : ControllerBase
{
    private readonly IBookingService _bookingService;
    private readonly ILogger<BookingController> _logger;

    public BookingController(IBookingService bookingService, ILogger<BookingController> logger)
    {
        _bookingService = bookingService;
        _logger = logger;
    }

    [HttpGet("health")]
    public ActionResult<string> Health()
    {
        return Ok("BookingService is running!");
    }

    [HttpGet("event-term/{eventTermId}")]
    public async Task<ActionResult<List<BookingResponse>>> GetBookingsForEventTerm(Guid eventTermId)
    {
        try
        {
            var bookings = await _bookingService.GetBookingsForEventTermAsync(eventTermId);
            return Ok(bookings);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error fetching bookings for event term {eventTermId}", eventTermId);
            return BadRequest(new { error = ex.Message });
        }
    }

    [HttpGet("event-term/{eventTermId}/count")]
    public async Task<ActionResult<long>> GetBookingCount(Guid eventTermId)
    {
        try
        {
            var count = await _bookingService.GetBookingCountAsync(eventTermId);
            return Ok(count);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error counting bookings for event term {eventTermId}", eventTermId);
            return BadRequest(new { error = ex.Message });
        }
    }

    [HttpGet("event-term/{eventTermId}/emails")]
    public async Task<ActionResult<List<string>>> GetParticipantParentEmails(Guid eventTermId)
    {
        try
        {
            var emails = await _bookingService.GetParticipantParentEmailsAsync(eventTermId);
            return Ok(emails);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error fetching participant emails for event term {eventTermId}", eventTermId);
            return BadRequest(new { error = ex.Message });
        }
    }

    [HttpGet("event-term/{eventTermId}/participant-names")]
    public async Task<ActionResult<List<string>>> GetParticipantDisplayNames(Guid eventTermId)
    {
        try
        {
            var names = await _bookingService.GetParticipantDisplayNamesAsync(eventTermId);
            return Ok(names);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error fetching participant names for event term {eventTermId}", eventTermId);
            return BadRequest(new { error = ex.Message });
        }
    }

    [HttpGet("event-term/{eventTermId}/summary")]
    public async Task<ActionResult<EventTermSummaryResponse>> GetEventTermSummary(Guid eventTermId)
    {
        try
        {
            var summary = await _bookingService.GetEventTermSummaryAsync(eventTermId);
            return Ok(summary);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error fetching event term summary for {eventTermId}", eventTermId);
            return BadRequest(new { error = ex.Message });
        }
    }

    [HttpGet("family-member/{familyMemberId}")]
    public async Task<ActionResult<List<BookingResponse>>> GetBookingsForFamilyMember(Guid familyMemberId)
    {
        try
        {
            var bookings = await _bookingService.GetBookingsForFamilyMemberAsync(familyMemberId);
            return Ok(bookings);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error fetching bookings for family member {familyMemberId}", familyMemberId);
            return BadRequest(new { error = ex.Message });
        }
    }

    [HttpGet("family-member/{familyMemberId}/details")]
    public async Task<ActionResult<List<BookingDetailResponse>>> GetBookingsForFamilyMemberEnriched(Guid familyMemberId)
    {
        try
        {
            var bookings = await _bookingService.GetBookingsForFamilyMemberEnrichedAsync(familyMemberId);
            return Ok(bookings);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error fetching enriched bookings for family member {familyMemberId}", familyMemberId);
            return BadRequest(new { error = ex.Message });
        }
    }

    [HttpPost]
    [Authorize(Roles = "USER,EVENT_OWNER,ORGANIZATION_TEAM_MEMBER")]
    public async Task<ActionResult<BookingResponse>> CreateBooking(
        [FromQuery] Guid familyMemberId,
        [FromQuery] Guid eventTermId)
    {
        try
        {
            var booking = await _bookingService.CreateBookingAsync(familyMemberId, eventTermId);
            return Created(new Uri($"/api/bookings/{booking.Id}", UriKind.Relative), booking);
        }
        catch (InvalidOperationException ex)
        {
            _logger.LogWarning(ex, "Invalid booking creation");
            return BadRequest(new { error = ex.Message });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error creating booking");
            return BadRequest(new { error = ex.Message });
        }
    }

    [HttpGet("{bookingId}")]
    public async Task<ActionResult<BookingResponse>> GetBooking(Guid bookingId)
    {
        try
        {
            var booking = await _bookingService.GetBookingAsync(bookingId);
            return Ok(booking);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error fetching booking {bookingId}", bookingId);
            return NotFound(new { error = ex.Message });
        }
    }

    [HttpDelete("{bookingId}")]
    [Authorize(Roles = "USER,EVENT_OWNER,ORGANIZATION_TEAM_MEMBER,ADMIN")]
    public async Task<ActionResult<BookingResponse>> CancelBooking(Guid bookingId)
    {
        try
        {
            var booking = await _bookingService.CancelBookingAsync(bookingId);
            return Ok(booking);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error cancelling booking {bookingId}", bookingId);
            return NotFound(new { error = ex.Message });
        }
    }
}
