using System.Net.Http.Headers;

namespace BookingService.Services;

using BookingService.DTO;
using BookingService.Exceptions;

public interface IEventServiceClient
{
    Task<EventTermDetailResponse> GetEventTermAsync(Guid eventTermId);
}

public class EventServiceClient : IEventServiceClient
{
    private readonly HttpClient _httpClient;
    private readonly IHttpContextAccessor _httpContextAccessor;
    private readonly ILogger<EventServiceClient> _logger;
    private readonly string _serviceSecret;

    public EventServiceClient(HttpClient httpClient, ILogger<EventServiceClient> logger,
        IHttpContextAccessor httpclientAccessor,
        IConfiguration configuration)
    {
        _httpClient = httpClient;
        _httpContextAccessor = httpclientAccessor;
        _logger = logger;
        _serviceSecret = configuration["Service:Secret"]
            ?? "holidayplanner-internal-service-secret";
    }

    public async Task<EventTermDetailResponse> GetEventTermAsync(Guid eventTermId)
    {
        var authHeader = _httpContextAccessor
            .HttpContext?
            .Request
            .Headers["Authorization"]
            .ToString();

        try
        {
            var request = new HttpRequestMessage(
                HttpMethod.Get,
                $"/api/events/terms/{eventTermId}");

            request.Headers.Add("X-Service-Secret", _serviceSecret);

            if (!string.IsNullOrWhiteSpace(authHeader))
            {
                request.Headers.Authorization =
                    AuthenticationHeaderValue.Parse(authHeader);
            }

            var response = await _httpClient.SendAsync(request);
            response.EnsureSuccessStatusCode();

            var eventTerm = await response.Content
                    .ReadFromJsonAsync<EventTermDetailResponse>()
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
