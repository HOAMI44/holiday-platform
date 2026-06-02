namespace BookingService.DTO;

using BookingService.Models;

public class BookingDetailResponse
{
    public Guid Id { get; set; }
    public Guid FamilyMemberId { get; set; }
    public Guid EventTermId { get; set; }
    public BookingStatus Status { get; set; }
    public DateTime BookedAt { get; set; }
    public string? EventTermName { get; set; }
    public string? EventName { get; set; }
    public DateTime? EventTermStart { get; set; }
    public DateTime? EventTermEnd { get; set; }

    public static BookingDetailResponse From(Booking booking, string? eventTermName = null, string? eventName = null)
    {
        return new BookingDetailResponse
        {
            Id = booking.Id,
            FamilyMemberId = booking.FamilyMemberId,
            EventTermId = booking.EventTermId,
            Status = booking.Status,
            BookedAt = booking.BookedAt,
            EventTermName = eventTermName,
            EventName = eventName
        };
    }
}
