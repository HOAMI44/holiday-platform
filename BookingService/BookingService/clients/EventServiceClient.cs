using System.Net.Http.Headers;
using System.Text.Json;
using Microsoft.Extensions.Caching.Distributed;

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
    private readonly IDistributedCache _cache;
    private readonly string _serviceSecret;

    private static readonly JsonSerializerOptions JsonOptions = new() { PropertyNameCaseInsensitive = true };

    public EventServiceClient(HttpClient httpClient, ILogger<EventServiceClient> logger,
        IHttpContextAccessor httpclientAccessor,
        IDistributedCache cache,
        IConfiguration configuration)
    {
        _httpClient = httpClient;
        _httpContextAccessor = httpclientAccessor;
        _logger = logger;
        _cache = cache;
        _serviceSecret = configuration["Service:Secret"]
            ?? "holidayplanner-local-service-secret";
    }

    public async Task<EventTermDetailResponse> GetEventTermAsync(Guid eventTermId)
    {
        var cacheKey = $"event-term:{eventTermId}";

        var cached = await _cache.GetStringAsync(cacheKey);
        if (cached != null)
            return JsonSerializer.Deserialize<EventTermDetailResponse>(cached, JsonOptions)!;

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
                request.Headers.Authorization = AuthenticationHeaderValue.Parse(authHeader);

            var response = await _httpClient.SendAsync(request);
            response.EnsureSuccessStatusCode();

            var eventTerm = await response.Content
                    .ReadFromJsonAsync<EventTermDetailResponse>()
                            ?? throw new EventServiceException($"Event term {eventTermId} not found");

            await _cache.SetStringAsync(cacheKey,
                JsonSerializer.Serialize(eventTerm, JsonOptions),
                new DistributedCacheEntryOptions
                {
                    AbsoluteExpirationRelativeToNow = TimeSpan.FromMinutes(5)
                });

            return eventTerm;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error fetching event term {eventTermId}", eventTermId);
            throw new EventServiceException($"Failed to fetch event term: {ex.Message}", ex);
        }
    }
}
