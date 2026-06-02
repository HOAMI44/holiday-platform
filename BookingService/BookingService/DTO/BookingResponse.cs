namespace BookingService.DTO;

using BookingService.Models;

public class BookingResponse
{
    public Guid Id { get; set; }
    public Guid FamilyMemberId { get; set; }
    public Guid EventTermId { get; set; }
    public BookingStatus Status { get; set; }
    public DateTime BookedAt { get; set; }

    public static BookingResponse From(Booking booking)
    {
        return new BookingResponse
        {
            Id = booking.Id,
            FamilyMemberId = booking.FamilyMemberId,
            EventTermId = booking.EventTermId,
            Status = booking.Status,
            BookedAt = booking.BookedAt
        };
    }
}
