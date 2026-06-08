using Microsoft.OpenApi.Models;

namespace BookingService.Configuration;

using BookingService.Auth;
using BookingService.Repositories;
using BookingService.Services;
using BookingService.Kafka;
using Confluent.Kafka;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using System.Text;

public static class ServiceCollectionExtensions
{
    public static IServiceCollection AddApplicationServices(this IServiceCollection services, IConfiguration configuration)
    {
        // Database
        var connectionString = configuration.GetConnectionString("DefaultConnection")
            ?? "Server=postgres;Port=5432;Database=booking_db;User Id=postgres;Password=postgres;";
        services.AddDbContext<BookingDbContext>(options =>
            options.UseNpgsql(connectionString));

        // Repositories
        services.AddScoped<IBookingRepository, BookingRepository>();

        // Services
        services.AddScoped<IBookingService, BookingServiceImpl>();

        // Kafka
        AddKafkaServices(services, configuration);

        // HTTP Clients
        AddHttpClients(services, configuration);

        // Redis Cache
        var redisConnection = configuration["Redis:ConnectionString"] ?? "localhost:6379";
        services.AddStackExchangeRedisCache(options => options.Configuration = redisConnection);

        // Authentication
        AddAuthentication(services, configuration);
        
        AddSwagger(services);

        return services;
    }

    private static void AddKafkaServices(IServiceCollection services, IConfiguration configuration)
    {
        var kafkaBootstrapServers = configuration["Kafka:BootstrapServers"] ?? "kafka:29092";

        var producerConfig = new ProducerConfig
        {
            BootstrapServers = kafkaBootstrapServers,
            ClientId = "booking-service"
        };
        var producer = new ProducerBuilder<string, string>(producerConfig).Build();
        services.AddSingleton(producer);

        services.AddScoped<IBookingEventProducer, BookingEventProducer>();
        services.AddHostedService<EventTermCancelledConsumer>();
        services.AddHostedService<BookingDecisionConsumer>();
    }

    private static void AddHttpClients(IServiceCollection services, IConfiguration configuration)
    {
        var eventServiceUrl = configuration["Services:EventService:Url"] ?? "http://event-service:8081";
        var identityServiceUrl = configuration["Services:IdentityService:Url"] ?? "http://identity-service:8083";

        services.AddHttpClient<IEventServiceClient, EventServiceClient>(client =>
        {
            client.BaseAddress = new Uri(eventServiceUrl);
            client.Timeout = TimeSpan.FromSeconds(30);
        });

        services.AddHttpClient<IIdentityServiceClient, IdentityServiceClient>(client =>
        {
            client.BaseAddress = new Uri(identityServiceUrl);
            client.Timeout = TimeSpan.FromSeconds(30);
        });
        
        
    }

    private static void AddAuthentication(IServiceCollection services, IConfiguration configuration)
    {
        var jwtSecret = configuration["Jwt:Secret"] ?? "holidayplanner-local-test-secret";

        services.AddAuthentication(options =>
            {
                options.DefaultAuthenticateScheme = "Combined";
                options.DefaultChallengeScheme = JwtBearerDefaults.AuthenticationScheme;
            })
            .AddPolicyScheme("Combined", "Combined", options =>
            {
                options.ForwardDefaultSelector = ctx =>
                    ctx.Request.Headers.ContainsKey("X-Service-Secret")
                        ? ServiceKeyAuthHandler.SchemeName
                        : JwtBearerDefaults.AuthenticationScheme;
            })
            .AddScheme<AuthenticationSchemeOptions, ServiceKeyAuthHandler>(ServiceKeyAuthHandler.SchemeName, null)
            .AddJwtBearer(options =>
            {
                options.RequireHttpsMetadata = false;
                options.TokenValidationParameters = new TokenValidationParameters
                {
                    ValidateIssuerSigningKey = true,
                    IssuerSigningKey = new SymmetricSecurityKey(Encoding.ASCII.GetBytes(jwtSecret)),
                    ValidateIssuer = false,
                    ValidateAudience = false,
                    ValidateLifetime = true
                };
            });

        services.AddAuthorization();
    }
    
    private static void AddSwagger(this IServiceCollection services)
    {
       services.AddSwaggerGen(options =>

        {

            options.AddSecurityDefinition("Bearer",

                new OpenApiSecurityScheme
                {
                    Name = "Authorization",
                    Type = SecuritySchemeType.Http,
                    Scheme = "bearer",
                    BearerFormat = "JWT",
                    In = ParameterLocation.Header,
                    Description = "JWT Token eingeben"
                });
            options.AddSecurityRequirement(
                new OpenApiSecurityRequirement
                {
                    {
                        new OpenApiSecurityScheme
                        {
                            Reference = new OpenApiReference
                            {
                                Type = ReferenceType.SecurityScheme,
                                Id = "Bearer"
                            }
                        },
                        Array.Empty<string>()
                    }
                });
        });
    }
}
