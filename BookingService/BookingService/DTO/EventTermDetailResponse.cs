namespace BookingService.DTO;

public class EventTermDetailResponse
{
    public Guid Id { get; set; }

    public string? EventName { get; set; }

    public string? EventLocation { get; set; }

    public decimal Price { get; set; }

    public DateTime StartDateTime { get; set; }

    public DateTime EndDateTime { get; set; }

    public int MaxParticipants { get; set; }

    public string? Status { get; set; }

    public Guid OrganizationId { get; set; }
}