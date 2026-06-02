namespace BookingService.DTO;

public class EventTermDetailResponse
{
    public Guid Id { get; set; }
    public string? Name { get; set; }
    public string? Status { get; set; }
    public int MaxParticipants { get; set; }
}
