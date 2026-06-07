namespace BookingService.DTO.payloads;

public class BookingConfirmedPayload
{
    public Guid BookingId { get; set; }
    public Guid FamilyMemberId { get; set; }
    public Guid EventTermId { get; set; }
    public string? Status { get; set; }
    public string? ParentEmail { get; set; }
    public string? EventName { get; set; }
    public string? TermDate { get; set; }
    public Guid? OrganizationId { get; set; }
    public decimal? Amount { get; set; }
}
