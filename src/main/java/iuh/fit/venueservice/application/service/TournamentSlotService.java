package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public interface TournamentSlotService {
    void hold(UUID venueCourtId, LocalDate playDate, LocalTime startTime, LocalTime endTime)
            throws ServiceException;

    void release(UUID venueCourtId, LocalDate playDate, LocalTime startTime, LocalTime endTime);
}
