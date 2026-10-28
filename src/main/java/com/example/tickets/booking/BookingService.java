package com.example.tickets.booking;

import com.example.tickets.booking.internal.Booking;
import com.example.tickets.booking.internal.BookingRepository;
import com.example.tickets.booking.internal.BookingStatus;
import com.example.tickets.events.EventInfo;
import com.example.tickets.events.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Servicio del módulo {@code booking}: API pública síncrona.
 *
 * <p>Dependencia síncrona: {@code booking -> events} (solo {@link EventService}).
 * Dependencias asíncronas: publica {@link BookingEvents} dentro de la
 * transacción; el Event Publication Registry las entrega tras el commit a
 * {@code seats} y {@code notification}.
 *
 * <p>Devuelve {@link BookingInfo}, nunca la entidad interna.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventService eventService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Crea una reserva confirmada y publica {@code BookingConfirmedEvent}.
     */
    public BookingInfo createBooking(CreateBookingRequest request) {
        log.info("Creando reserva para {} en el evento {}", request.customerEmail(), request.eventId());

        EventInfo event = eventService.getEventById(request.eventId());

        Booking booking = Booking.builder()
                .customerEmail(request.customerEmail())
                .eventId(event.id())
                .quantity(request.quantity())
                .totalAmount(event.price().multiply(BigDecimal.valueOf(request.quantity())))
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

        return toInfo(saved);
    }

    /**
     * Cancela una reserva y publica {@code BookingCancelledEvent}.
     */
    public BookingInfo cancelBooking(Long bookingId, String reason) {
        log.info("Cancelando reserva {} por: {}", bookingId, reason);

        Booking booking = getBookingEntity(bookingId);
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

        return toInfo(saved);
    }

    @Transactional(readOnly = true)
    public BookingInfo getBookingById(Long id) {
        return toInfo(getBookingEntity(id));
    }

    @Transactional(readOnly = true)
    public List<BookingInfo> getBookingsByCustomer(String customerEmail) {
        return bookingRepository.findByCustomerEmail(customerEmail).stream().map(this::toInfo).toList();
    }

    @Transactional(readOnly = true)
    public List<BookingInfo> getAllBookings() {
        return bookingRepository.findAll().stream().map(this::toInfo).toList();
    }

    private Booking getBookingEntity(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada con id: " + id));
    }

    private BookingInfo toInfo(Booking booking) {
        return new BookingInfo(
                booking.getId(),
                booking.getCustomerEmail(),
                booking.getEventId(),
                booking.getQuantity(),
                booking.getTotalAmount(),
                booking.getStatus().name(),
                booking.getCancellationReason(),
                booking.getCreatedAt());
    }
}
