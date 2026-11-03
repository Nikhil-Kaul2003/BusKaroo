package busKaroo.dao;

import busKaroo.model.Reservation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findAllByUserId(Long userId);

    @EntityGraph(attributePaths = {
            "trip.route",
            "trip.bus",
            "trip.driver.user",
            "originRouteStop.terminal",
            "targetRouteStop.terminal",
            "passengers"
    })
    List<Reservation> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query(value = "UPDATE reservation SET status = 'EXPIRED', updated_at = NOW() " +
            "WHERE status = 'PENDING' " +
            "AND created_at < NOW() - (:ttlMinutes * INTERVAL '1 minute')", nativeQuery = true)
    int expirePendingReservations(@Param("ttlMinutes") int ttlMinutes);
}
