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
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final ReservationRepository reservationRepo;

    public PaymentService(PaymentRepository paymentRepo, ReservationRepository reservationRepo) {
        this.paymentRepo = paymentRepo;
        this.reservationRepo = reservationRepo;
    }

    @Transactional
    public void payReservation(Long reservationId, Long userId) {
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

        String transactionId = UUID.randomUUID().toString();

        Payment payment = new Payment();
        payment.setReservation(reservation);
        payment.setUser(reservation.getUser());
        payment.setAmount(reservation.getTotalAmount());
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId(transactionId);
        payment.setPaidAt(OffsetDateTime.now(ZoneOffset.UTC));
        payment.setGatewayResponse("{\"status\": \"success\", \"mock\": true}");

        paymentRepo.save(payment);

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepo.save(reservation);
    }
}
