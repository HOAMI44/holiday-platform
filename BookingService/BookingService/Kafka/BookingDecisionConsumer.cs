using BookingService.DTO.payloads;

namespace BookingService.Kafka;

using BookingService.Models;
using BookingService.Repositories;
using Confluent.Kafka;
using System.Text.Json;

public class BookingDecisionConsumer : BackgroundService
{
    private readonly IConsumer<string, string> _consumer;
    private readonly IServiceProvider _serviceProvider;
    private readonly ILogger<BookingDecisionConsumer> _logger;

    public BookingDecisionConsumer(IServiceProvider serviceProvider, ILogger<BookingDecisionConsumer> logger)
    {
        _serviceProvider = serviceProvider;
        _logger = logger;

        var kafkaBootstrapServers = GetConfigValue("Kafka:BootstrapServers") ?? "kafka:29092";

        var consumerConfig = new ConsumerConfig
        {
            BootstrapServers = kafkaBootstrapServers,
            GroupId = "booking-service-booking-decisions",
            AutoOffsetReset = AutoOffsetReset.Earliest,
            EnableAutoCommit = true
        };

        _consumer = new ConsumerBuilder<string, string>(consumerConfig).Build();
        _consumer.Subscribe(new[]
        {
            "holiday-planner.booking.confirmed",
            "holiday-planner.booking.rejected"
        });
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        await Task.Yield();
        _logger.LogInformation("BookingDecisionConsumer starting...");

        try
        {
            while (!stoppingToken.IsCancellationRequested)
            {
                var consumeResult = _consumer.Consume(stoppingToken);

                if (consumeResult?.Message == null)
                    continue;

                try
                {
                    await ProcessMessageAsync(consumeResult.Topic, consumeResult.Message.Value);
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Error processing booking decision Kafka message");
                }
            }
        }
        catch (OperationCanceledException)
        {
            _logger.LogInformation("BookingDecisionConsumer stopping...");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Unexpected error in BookingDecisionConsumer");
        }
        finally
        {
            _consumer.Close();
        }
    }

    private async Task ProcessMessageAsync(string topic, string json)
    {
        var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };

        if (topic == "holiday-planner.booking.confirmed")
        {
            var envelope = JsonSerializer.Deserialize<KafkaEnvelope<BookingConfirmedPayload>>(json, options);
            if (envelope?.Payload == null)
            {
                _logger.LogWarning("Invalid BookingConfirmed payload");
                return;
            }

            await ConfirmBookingAsync(envelope.Payload);
            return;
        }

        if (topic == "holiday-planner.booking.rejected")
        {
            var envelope = JsonSerializer.Deserialize<KafkaEnvelope<BookingRejectedPayload>>(json, options);
            if (envelope?.Payload == null)
            {
                _logger.LogWarning("Invalid BookingRejected payload");
                return;
            }

            await RejectBookingAsync(envelope.Payload);
        }
    }

    private async Task ConfirmBookingAsync(BookingConfirmedPayload payload)
    {
        using var scope = _serviceProvider.CreateScope();
        var bookingRepository = scope.ServiceProvider.GetRequiredService<IBookingRepository>();
        var bookingEventProducer = scope.ServiceProvider.GetRequiredService<IBookingEventProducer>();

        var booking = await bookingRepository.GetByIdAsync(payload.BookingId);
        if (booking == null)
        {
            _logger.LogWarning("Booking {bookingId} not found for confirmation", payload.BookingId);
            return;
        }
    //der status aus payload ist string wir wandeln ihn in ein enum um welches in bookingStatus ENUM ist
    if (!Enum.TryParse(payload.Status, true, out BookingStatus status))
{
    _logger.LogWarning("Invalid booking status {status} for booking {bookingId}", payload.Status, payload.BookingId);
    return;
}

        booking.Status = status;
        await bookingRepository.UpdateAsync(booking);

        if (booking.Status == BookingStatus.CONFIRMED)
        {
            await bookingEventProducer.PublishBookingCreatedAsync(new BookingCreatedPayload
            {
                BookingId = booking.Id,
                FamilyMemberId = booking.FamilyMemberId,
                EventTermId = booking.EventTermId,
                Status = booking.Status.ToString(),
                ParentEmail = payload.ParentEmail,
                EventName = payload.EventName,
                TermDate = payload.TermDate,
                OrganizationId = payload.OrganizationId,
                Amount = payload.Amount
            });
        }
    }

    private async Task RejectBookingAsync(BookingRejectedPayload payload)
    {
        using var scope = _serviceProvider.CreateScope();
        var bookingRepository = scope.ServiceProvider.GetRequiredService<IBookingRepository>();

        var booking = await bookingRepository.GetByIdAsync(payload.BookingId);
        if (booking == null)
        {
            _logger.LogWarning("Booking {bookingId} not found for rejection", payload.BookingId);
            return;
        }

        booking.Status = BookingStatus.REJECTED;
        await bookingRepository.UpdateAsync(booking);
    }

    private string? GetConfigValue(string key)
    {
        using var scope = _serviceProvider.CreateScope();
        var config = scope.ServiceProvider.GetRequiredService<IConfiguration>();
        return config[key];
    }

    public override void Dispose()
    {
        _consumer.Dispose();
        base.Dispose();
    }
}
