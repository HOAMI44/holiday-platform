namespace BookingService.Configuration;

using BookingService.Models;
using Microsoft.EntityFrameworkCore;

public class BookingDbContext : DbContext
{
    public BookingDbContext(DbContextOptions<BookingDbContext> options) : base(options)
    {
    }

    public DbSet<Booking> Bookings { get; set; } = null!;

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        modelBuilder.Entity<Booking>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.Property(e => e.Id).HasDefaultValueSql("gen_random_uuid()");
            entity.Property(e => e.BookedAt).HasDefaultValueSql("NOW()");
            entity.Property(e => e.Status).HasConversion<string>();
            
            entity.HasIndex(e => e.EventTermId);
            entity.HasIndex(e => e.FamilyMemberId);
            entity.HasIndex(e => new { e.EventTermId, e.Status });
        });
    }
}
