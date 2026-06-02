namespace BookingService.DTO;

public class EventTermSummaryResponse
{
    public Guid EventTermId { get; set; }
    public long ConfirmedCount { get; set; }
    public long WaitlistedCount { get; set; }
    public long CancelledCount { get; set; }
    public long TotalCount { get; set; }
}
