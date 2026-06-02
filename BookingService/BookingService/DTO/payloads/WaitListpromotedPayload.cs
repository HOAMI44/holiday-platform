namespace BookingService.DTO.payloads;

public class WaitListPromotedPayload
{
    public Guid BookingId { get; set; }
    public Guid FamilyMemberId { get; set; }
    public Guid EventTermId { get; set; }
}