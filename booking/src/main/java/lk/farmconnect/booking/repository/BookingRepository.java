package lk.farmconnect.booking.repository;

import lk.farmconnect.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    //  CAPACITY CHECK
    // Sums the quantity of all active/pending bookings that overlap with the requested dates.
    @Query("SELECT COALESCE(SUM(b.quantity), 0) FROM Booking b WHERE b.product.id = :productId " +
            "AND b.status IN ('PENDING', 'ACCEPTED', 'ACTIVE') " +
            "AND b.startDate <= :endDate AND b.endDate >= :startDate")
    Integer getTotalBookedQuantityForPeriod(
            @Param("productId") UUID productId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}