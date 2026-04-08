package com.railway.booking.controller;

import com.railway.booking.model.Payment;
import com.railway.booking.model.Ticket;
import com.railway.booking.model.User;
import com.railway.booking.service.BookingService;
import com.railway.booking.service.PaymentService;
import com.railway.booking.service.TrainService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingControllerAccessTest {

    @Test
    void pnrLookup_returnsValidationError_whenPnrFormatIsInvalid() {
        BookingController controller = new BookingController(
                new StubBookingService(null),
                new StubPaymentService(),
                new StubTrainService()
        );

        User requester = User.builder().email("requester@example.com").role(User.Role.PASSENGER).build();
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.pnrLookup(requester, "12AB", model);

        assertEquals("pnr-status", view);
        assertEquals("PNR must be exactly 10 digits.", model.get("error"));
    }

    @Test
    void pnrLookup_throwsAccessDenied_whenRequesterDoesNotOwnTicket() {
        User ticketOwner = User.builder().email("owner@example.com").role(User.Role.PASSENGER).build();
        Ticket ticket = Ticket.builder().user(ticketOwner).build();

        BookingController controller = new BookingController(
                new StubBookingService(ticket),
                new StubPaymentService(),
                new StubTrainService()
        );

        User requester = User.builder().email("requester@example.com").role(User.Role.PASSENGER).build();
        ExtendedModelMap model = new ExtendedModelMap();

        assertThrows(AccessDeniedException.class, () -> controller.pnrLookup(requester, "1234567890", model));
    }

    private static class StubBookingService extends BookingService {

        private final Ticket ticket;

        StubBookingService(Ticket ticket) {
            super(null, null, null, null, null, null);
            this.ticket = ticket;
        }

        @Override
        public Ticket getTicketByPnr(String pnr) {
            return ticket;
        }
    }

    private static class StubPaymentService extends PaymentService {
        StubPaymentService() {
            super(null, null);
        }

        @Override
        public Payment getPaymentByTicket(Ticket ticket) {
            return null;
        }
    }

    private static class StubTrainService extends TrainService {
        StubTrainService() {
            super(null, null, null);
        }
    }
}
