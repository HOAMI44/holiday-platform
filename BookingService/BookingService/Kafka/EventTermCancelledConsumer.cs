using BookingService.DTO.payloads;

namespace BookingService.Kafka;

using Confluent.Kafka;
using System.Text.Json;
using BookingService.Repositories;
using BookingService.Models;
using Microsoft.Extensions.Logging;
using System.Threading;

public class EventTermCancelledConsumer : BackgroundService
{
    private readonly IConsumer<string, string> _consumer;
    private readonly IServiceProvider _serviceProvider;
    private readonly ILogger<EventTermCancelledConsumer> _logger;

    public EventTermCancelledConsumer(IServiceProvider serviceProvider, ILogger<EventTermCancelledConsumer> logger)
    {
        _serviceProvider = serviceProvider;
        _logger = logger;

        var kafkaBootstrapServers = GetConfigValue("Kafka:BootstrapServers") ?? "kafka:29092";
        
        var consumerConfig = new ConsumerConfig
        {
            BootstrapServers = kafkaBootstrapServers,
            GroupId = "booking-service",
            AutoOffsetReset = AutoOffsetReset.Earliest,
            EnableAutoCommit = true
        };

        _consumer = new ConsumerBuilder<string, string>(consumerConfig).Build();
        _consumer.Subscribe(new[] { "holiday-planner.event-term.cancelled" });
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _logger.LogInformation("EventTermCancelledConsumer starting...");

        try
        {
            while (!stoppingToken.IsCancellationRequested)
            {
                var consumeResult = _consumer.Consume(stoppingToken);

                if (consumeResult?.Message == null)
                    continue;

                try
                {
                    await ProcessMessageAsync(consumeResult.Message.Value, stoppingToken);
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Error processing Kafka message");
                }
            }
        }
        catch (OperationCanceledException)
        {
            _logger.LogInformation("EventTermCancelledConsumer stopping...");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Unexpected error in EventTermCancelledConsumer");
        }
        finally
        {
            _consumer.Close();
        }
    }

    private async Task ProcessMessageAsync(string json, CancellationToken cancellationToken)
    {
        try
        {
            var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
            var envelope = JsonSerializer.Deserialize<KafkaEnvelope<EventTermCancelledPayload>>(json, options);

            if (envelope?.Payload == null)
            {
                _logger.LogWarning("Invalid payload in Kafka message");
                return;
            }

            _logger.LogInformation("Processing EventTermCancelled for EventTermId: {eventTermId}", envelope.Payload.EventTermId);

            // Use scoped services
            using var scope = _serviceProvider.CreateScope();
            var bookingRepository = scope.ServiceProvider.GetRequiredService<IBookingRepository>();

            // Get all non-cancelled bookings for this event term
            var bookings = await bookingRepository.GetByEventTermIdAsync(envelope.Payload.EventTermId);
            var activeBookings = bookings.Where(b => b.Status != BookingStatus.CANCELLED).ToList();

            // Cancel all of them
            foreach (var booking in activeBookings)
            {
                booking.Status = BookingStatus.CANCELLED;
                await bookingRepository.UpdateAsync(booking);
            }

            _logger.LogInformation("Cancelled {count} bookings for EventTermId: {eventTermId}", 
                activeBookings.Count, envelope.Payload.EventTermId);
        }
        catch (JsonException ex)
        {
            _logger.LogError(ex, "Failed to parse Kafka message as JSON");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing EventTermCancelled event");
        }
    }

    private string? GetConfigValue(string key)
    {
        using var scope = _serviceProvider.CreateScope();
        var config = scope.ServiceProvider.GetRequiredService<IConfiguration>();
        return config[key];
    }

    public override void Dispose()
    {
        _consumer?.Dispose();
        base.Dispose();
    }
}
