namespace BookingService.Exceptions;

public class EventServiceException : Exception
{
    public EventServiceException(string message) : base(message)
    {
    }

    public EventServiceException(string message, Exception innerException) 
        : base(message, innerException)
    {
    }
}
