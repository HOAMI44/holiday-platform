namespace BookingService.Repositories;

using BookingService.Models;
using BookingService.Configuration;
using Microsoft.EntityFrameworkCore;

public interface IBookingRepository
{
    Task<Booking?> GetByIdAsync(Guid id);
    Task<List<Booking>> GetByEventTermIdAsync(Guid eventTermId);
    Task<List<Booking>> GetByEventTermIdAndStatusAsync(Guid eventTermId, BookingStatus status);
    Task<List<Booking>> GetByFamilyMemberIdAsync(Guid familyMemberId);
    Task<long> CountByEventTermIdAndStatusAsync(Guid eventTermId, BookingStatus status);
    Task<List<Booking>> GetActiveBookingsByFamilyMemberAsync(Guid familyMemberId);
    Task<Booking> AddAsync(Booking booking);
    Task<Booking> UpdateAsync(Booking booking);
    Task<List<Booking>> GetAllAsync();
    Task SaveChangesAsync();
}

public class BookingRepository : IBookingRepository
{
    private readonly BookingDbContext _context;

    public BookingRepository(BookingDbContext context)
    {
        _context = context;
    }

    public async Task<Booking?> GetByIdAsync(Guid id)
    {
        return await _context.Bookings.FindAsync(id);
    }

    public async Task<List<Booking>> GetByEventTermIdAsync(Guid eventTermId)
    {
        return await _context.Bookings
            .Where(b => b.EventTermId == eventTermId)
            .ToListAsync();
    }

    public async Task<List<Booking>> GetByEventTermIdAndStatusAsync(Guid eventTermId, BookingStatus status)
    {
        return await _context.Bookings
            .Where(b => b.EventTermId == eventTermId && b.Status == status)
            .ToListAsync();
    }

    public async Task<List<Booking>> GetByFamilyMemberIdAsync(Guid familyMemberId)
    {
        return await _context.Bookings
            .Where(b => b.FamilyMemberId == familyMemberId)
            .ToListAsync();
    }

    public async Task<long> CountByEventTermIdAndStatusAsync(Guid eventTermId, BookingStatus status)
    {
        return await _context.Bookings
            .Where(b => b.EventTermId == eventTermId && b.Status == status)
            .CountAsync();
    }

    public async Task<List<Booking>> GetActiveBookingsByFamilyMemberAsync(Guid familyMemberId)
    {
        return await _context.Bookings
            .Where(b => b.FamilyMemberId == familyMemberId && b.Status != BookingStatus.CANCELLED)
            .OrderByDescending(b => b.BookedAt)
            .ToListAsync();
    }

    public async Task<Booking> AddAsync(Booking booking)
    {
        var entity = await _context.Bookings.AddAsync(booking);
        await _context.SaveChangesAsync();
        return entity.Entity;
    }

    public async Task<Booking> UpdateAsync(Booking booking)
    {
        _context.Bookings.Update(booking);
        await _context.SaveChangesAsync();
        return booking;
    }

    public async Task<List<Booking>> GetAllAsync()
    {
        return await _context.Bookings.ToListAsync();
    }

    public async Task SaveChangesAsync()
    {
        await _context.SaveChangesAsync();
    }
}
