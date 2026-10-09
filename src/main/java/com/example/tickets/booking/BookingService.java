package com.example.tickets.booking;

import com.example.tickets.events.Event;
import com.example.tickets.events.EventService;
import com.example.tickets.seats.SeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Servicio del módulo {@code booking}.
 *
 * <p>Dependencia síncrona: {@code booking -> events} (únicamente a través de
 * {@link EventService}, su API pública).
 *
 * <p>Dependencias asíncronas: publica {@link BookingEvents} dentro de la
 * transacción; con el Event Publication Registry de Spring Modulith, cada
 * publicación se registra en {@code event_publication} y se entrega tras el
 * commit a los listeners ({@code seats}, {@code notification}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventService eventService;
//    private final SeatService seatService; // ⚠️ VIOLACIÓN: ciclo booking ↔ seats
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Crea una reserva confirmada y publica {@code BookingConfirmedEvent}.
     */
    public Booking createBooking(CreateBookingRequest request) {
        log.info("Creando reserva para {} en el evento {}", request.customerEmail(), request.eventId());

        Event event = eventService.getEventById(request.eventId());
//        seatService.getAvailability(event.getId()); // ⚠️ trabajo de Seats hecho desde Booking

        Booking booking = Booking.builder()
                .customerEmail(request.customerEmail())
                .eventId(event.getId())
                .quantity(request.quantity())
                .totalAmount(event.getPrice().multiply(BigDecimal.valueOf(request.quantity())))
                .status(BookingStatus.CONFIRMED)
                .createdAt(Instant.now())
                .build();

        Booking saved = bookingRepository.save(booking);
        log.info("Reserva {} creada ({} entradas, total {})", saved.getId(), saved.getQuantity(), saved.getTotalAmount());

        eventPublisher.publishEvent(new BookingEvents.BookingConfirmedEvent(
                saved.getId(),
                saved.getEventId(),
                saved.getCustomerEmail(),
                saved.getQuantity(),
                saved.getTotalAmount(),
                Instant.now()
        ));
        log.info("BookingConfirmedEvent publicado para la reserva {}", saved.getId());

        return saved;
    }

    /**
     * Cancela una reserva y publica {@code BookingCancelledEvent}.
     */
    public Booking cancelBooking(Long bookingId, String reason) {
        log.info("Cancelando reserva {} por: {}", bookingId, reason);

        Booking booking = getBookingById(bookingId);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        Booking saved = bookingRepository.save(booking);

        eventPublisher.publishEvent(new BookingEvents.BookingCancelledEvent(
                saved.getId(),
                saved.getEventId(),
                saved.getCustomerEmail(),
                saved.getQuantity(),
                reason,
                Instant.now()
        ));
        log.info("BookingCancelledEvent publicado para la reserva {}", saved.getId());

        return saved;
    }

    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada con id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Booking> getBookingsByCustomer(String customerEmail) {
        return bookingRepository.findByCustomerEmail(customerEmail);
    }

    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }
}
