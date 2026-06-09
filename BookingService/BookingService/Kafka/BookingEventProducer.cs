using BookingService.DTO.payloads;

namespace BookingService.Kafka;

using Confluent.Kafka;
using System.Text.Json;
using Microsoft.Extensions.Logging;

public class KafkaEnvelope<T>
{
    public string? EventType { get; set; }
    public string? Version { get; set; }
    public string? Timestamp { get; set; }
    public string? Source { get; set; }
    public T? Payload { get; set; }

    public KafkaEnvelope(string eventType, string version, string timestamp, string source, T payload)
    {
        EventType = eventType;
        Version = version;
        Timestamp = timestamp;
        Source = source;
        Payload = payload;
    }
}



public interface IBookingEventProducer
{
    Task PublishBookingCreatedAsync(BookingCreatedPayload payload);
    Task PublishBookingCancelledAsync(BookingCancelledPayload payload);
    Task PublishWaitlistPromotedAsync(WaitListPromotedPayload payload);
}

public class BookingEventProducer : IBookingEventProducer
{
    private readonly IProducer<string, string> _producer;
    private readonly ILogger<BookingEventProducer> _logger;
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        // Use camelCase for JSON properties for java compatibility
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase
    };

    public BookingEventProducer(IProducer<string, string> producer, ILogger<BookingEventProducer> logger)
    {
        _producer = producer;
        _logger = logger;
    }

    public async Task PublishBookingCreatedAsync(BookingCreatedPayload payload)
    {
        try
        {
            var envelope = new KafkaEnvelope<BookingCreatedPayload>(
                "BookingCreated",
                "1",
                DateTime.UtcNow.ToString("O"),
                "booking-service",
                payload
            );

            var json = JsonSerializer.Serialize(envelope, JsonOptions);
            var message = new Message<string, string>
            {
                Key = payload.BookingId.ToString(),
                Value = json
            };

            await _producer.ProduceAsync("holiday-planner.booking.created", message);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to publish BookingCreated event");
        }
    }

    public async Task PublishBookingCancelledAsync(BookingCancelledPayload payload)
    {
        try
        {
            var envelope = new KafkaEnvelope<BookingCancelledPayload>(
                "BookingCancelled",
                "1",
                DateTime.UtcNow.ToString("O"),
                "booking-service",
                payload
            );

            var json = JsonSerializer.Serialize(envelope, JsonOptions);
            var message = new Message<string, string>
            {
                Key = payload.BookingId.ToString(),
                Value = json
            };

            await _producer.ProduceAsync("holiday-planner.booking.cancelled", message);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to publish BookingCancelled event");
        }
    }

    public async Task PublishWaitlistPromotedAsync(WaitListPromotedPayload payload)
    {
        try
        {
            var envelope = new KafkaEnvelope<WaitListPromotedPayload>(
                "WaitlistPromoted",
                "1",
                DateTime.UtcNow.ToString("O"),
                "booking-service",
                payload
            );

            var json = JsonSerializer.Serialize(envelope, JsonOptions);
            var message = new Message<string, string>
            {
                Key = payload.BookingId.ToString(),
                Value = json
            };

            await _producer.ProduceAsync("holiday-planner.booking.waitlist-promoted", message);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to publish WaitlistPromoted event");
        }
    }
}
