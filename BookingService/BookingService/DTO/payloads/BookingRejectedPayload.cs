namespace BookingService.DTO.payloads;

public class BookingRejectedPayload
{
    public Guid BookingId { get; set; }
    public Guid FamilyMemberId { get; set; }
    public Guid EventTermId { get; set; }
    public string? ReasonCode { get; set; }
}
