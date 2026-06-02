namespace BookingService.Services;

using BookingService.DTO;
using BookingService.Exceptions;
using System.Text.Json;

public interface IEventServiceClient
{
    Task<EventTermDetailResponse> GetEventTermAsync(Guid eventTermId);
}

public class EventServiceClient : IEventServiceClient
{
    private readonly HttpClient _httpClient;
    private readonly ILogger<EventServiceClient> _logger;

    public EventServiceClient(HttpClient httpClient, ILogger<EventServiceClient> logger)
    {
        _httpClient = httpClient;
        _logger = logger;
    }

    public async Task<EventTermDetailResponse> GetEventTermAsync(Guid eventTermId)
    {
        try
        {
            var response = await _httpClient.GetAsync($"/api/event-terms/{eventTermId}");
            response.EnsureSuccessStatusCode();
            
            var content = await response.Content.ReadAsStringAsync();
            var eventTerm = JsonSerializer.Deserialize<EventTermDetailResponse>(content)
                ?? throw new EventServiceException($"Event term {eventTermId} not found");
            
            return eventTerm;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error fetching event term {eventTermId}", eventTermId);
            throw new EventServiceException($"Failed to fetch event term: {ex.Message}", ex);
        }
    }
}
