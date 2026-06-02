namespace BookingService.Exceptions;

public class BookingNotFoundException : Exception
{
    public BookingNotFoundException(Guid bookingId) 
        : base($"Booking with ID {bookingId} not found")
    {
    }
}
