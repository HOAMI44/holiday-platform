namespace BookingService.DTO.payloads;

public class BookingRequestedPayload
{
    public Guid BookingId { get; set; }
    public Guid FamilyMemberId { get; set; }
    public Guid EventTermId { get; set; }
    public string? Status { get; set; }
    public string? ParentEmail { get; set; }
}
