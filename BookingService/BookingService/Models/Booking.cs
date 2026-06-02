namespace BookingService.Models;

public class Booking
{
    public Guid Id { get; set; }
    public Guid FamilyMemberId { get; set; }
    public Guid EventTermId { get; set; }
    public BookingStatus Status { get; set; }
    public DateTime BookedAt { get; set; }

    public Booking()
    {
        Id = Guid.NewGuid();
        BookedAt = DateTime.UtcNow;
    }
}
