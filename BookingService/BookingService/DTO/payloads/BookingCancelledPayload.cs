namespace BookingService.DTO.payloads;

public class BookingCancelledPayload
{
    public Guid BookingId { get; set; }
    public Guid FamilyMemberId { get; set; }
    public Guid EventTermId { get; set; }
    public string? ReasonCode { get; set; }
}