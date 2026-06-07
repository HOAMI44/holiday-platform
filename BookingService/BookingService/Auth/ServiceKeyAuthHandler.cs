using Microsoft.AspNetCore.Authentication;
using Microsoft.Extensions.Options;
using System.Security.Claims;
using System.Text.Encodings.Web;

namespace BookingService.Auth;

public class ServiceKeyAuthHandler : AuthenticationHandler<AuthenticationSchemeOptions>
{
    public const string SchemeName = "ServiceKey";
    private readonly string _serviceSecret;

    public ServiceKeyAuthHandler(
        IOptionsMonitor<AuthenticationSchemeOptions> options,
        ILoggerFactory logger,
        UrlEncoder encoder,
        IConfiguration configuration)
        : base(options, logger, encoder)
    {
        _serviceSecret = configuration["Service:Secret"] ?? "holidayplanner-local-service-secret";
    }

    protected override Task<AuthenticateResult> HandleAuthenticateAsync()
    {
        if (!Request.Headers.TryGetValue("X-Service-Secret", out var secret))
            return Task.FromResult(AuthenticateResult.NoResult());

        if (secret != _serviceSecret)
            return Task.FromResult(AuthenticateResult.Fail("Invalid service secret"));

        var claims = new[]
        {
            new Claim(ClaimTypes.Name, "service"),
            new Claim(ClaimTypes.Role, "SERVICE")
        };
        var identity = new ClaimsIdentity(claims, SchemeName);
        var ticket = new AuthenticationTicket(new ClaimsPrincipal(identity), SchemeName);
        return Task.FromResult(AuthenticateResult.Success(ticket));
    }
}
