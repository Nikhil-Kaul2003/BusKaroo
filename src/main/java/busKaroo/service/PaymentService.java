package busKaroo.service;

import busKaroo.dao.PaymentRepository;
import busKaroo.dao.ReservationRepository;
import busKaroo.exception.ForbiddenException;
import busKaroo.exception.ReservationNotFoundException;
import busKaroo.model.Payment;
import busKaroo.model.Reservation;
import busKaroo.model.enums.PaymentStatus;
import busKaroo.model.enums.ReservationStatus;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final ReservationRepository reservationRepo;

    @Value("${app.reservations.pending-ttl-minutes:15}")
    private int pendingTtlMinutes = 15;

    public PaymentService(PaymentRepository paymentRepo, ReservationRepository reservationRepo) {
        this.paymentRepo = paymentRepo;
        this.reservationRepo = reservationRepo;
    }

    @Transactional
    public boolean payReservation(Long reservationId, Long userId) {
        Reservation reservation =
                reservationRepo
                        .findById(reservationId)
                        .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        if (!reservation.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only pay for your own reservations");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException(
                    "Reservation is not in PENDING state. Current status: "
                            + reservation.getStatus());
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (reservation.getCreatedAt() != null
                && reservation.getCreatedAt().isBefore(now.minusMinutes(pendingTtlMinutes))) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepo.save(reservation);
            return false;
        }

        String transactionId = UUID.randomUUID().toString();

        Payment payment = new Payment();
        payment.setReservation(reservation);
        payment.setUser(reservation.getUser());
        payment.setAmount(reservation.getTotalAmount());
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId(transactionId);
        payment.setPaidAt(now);
        payment.setGatewayResponse("{\"status\": \"success\", \"mock\": true}");

        paymentRepo.save(payment);

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepo.save(reservation);
        return true;
    }
}
