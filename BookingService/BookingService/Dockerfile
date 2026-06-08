# Build stage
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src

# Copy project file and restore dependencies
COPY ["BookingService/BookingService.csproj", "BookingService/"]
RUN dotnet restore "BookingService/BookingService.csproj"

# Copy all source code
COPY . .

# Build and publish
RUN dotnet publish "BookingService/BookingService.csproj" -c Release -o /app/publish

# Runtime stage
FROM mcr.microsoft.com/dotnet/aspnet:8.0
WORKDIR /app

# Copy published artifacts
COPY --from=build /app/publish .

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD dotnet exec /app/BookingService.dll || exit 1

# Run the application
ENTRYPOINT ["dotnet", "BookingService.dll"]
