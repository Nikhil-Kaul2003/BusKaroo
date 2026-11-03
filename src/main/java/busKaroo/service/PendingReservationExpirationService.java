package busKaroo.service;

import busKaroo.dao.ReservationRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PendingReservationExpirationService {

    private static final Logger LOG = LoggerFactory.getLogger(PendingReservationExpirationService.class);

    private final ReservationRepository reservationRepository;

    @Value("${app.reservations.pending-ttl-minutes:15}")
    private int pendingTtlMinutes;

    public PendingReservationExpirationService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Scheduled(fixedDelayString = "${app.reservations.expiration-scan-ms:30000}")
    @Transactional
    public void expireStalePendingReservations() {
        int expired = reservationRepository.expirePendingReservations(pendingTtlMinutes);
        if (expired > 0) {
            LOG.info("Expired {} unpaid reservation(s) older than {} minutes", expired, pendingTtlMinutes);
        }
    }
}
