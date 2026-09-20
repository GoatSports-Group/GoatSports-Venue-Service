package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.TimeSlotRepository;
import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.domain.enums.TimeSlotStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TournamentSlotServiceImplTest {

    @Mock private TimeSlotRepository timeSlotRepository;

    private TournamentSlotServiceImpl service;

    private final UUID courtId = UUID.randomUUID();
    private final LocalDate playDate = LocalDate.now().plusDays(1);
    private final LocalTime startTime = LocalTime.of(18, 0);
    private final LocalTime endTime = LocalTime.of(20, 0);

    @BeforeEach
    void setUp() {
        service = new TournamentSlotServiceImpl(timeSlotRepository);
        when(timeSlotRepository.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    private TimeSlot slot(TimeSlotStatus status) {
        return TimeSlot.builder()
                .timeSlotId(UUID.randomUUID()).venueCourtId(courtId).date(playDate)
                .startTime(startTime).endTime(endTime).status(status).pricePerHour(100_000D)
                .build();
    }

    @Test
    @DisplayName("khung giờ trống thì giải giữ được và slot chuyển sang đã đặt")
    void availableSlotIsHeld() throws ServiceException {
        TimeSlot available = slot(TimeSlotStatus.AVAILABLE);
        when(timeSlotRepository.findByCourtAndTimeForUpdate(courtId, playDate, startTime, endTime))
                .thenReturn(Optional.of(available));

        service.hold(courtId, playDate, startTime, endTime);

        assertThat(available.getStatus()).isEqualTo(TimeSlotStatus.BOOKED);
        verify(timeSlotRepository).save(available);
    }

    @Test
    @DisplayName("giữ chỗ phải đọc qua truy vấn có khóa row, không đọc thường")
    void holdGoesThroughLockedRead() throws ServiceException {
        when(timeSlotRepository.findByCourtAndTimeForUpdate(courtId, playDate, startTime, endTime))
                .thenReturn(Optional.of(slot(TimeSlotStatus.AVAILABLE)));

        service.hold(courtId, playDate, startTime, endTime);

        verify(timeSlotRepository).findByCourtAndTimeForUpdate(courtId, playDate, startTime, endTime);
    }

    @Test
    @DisplayName("khung giờ người chơi đã đặt thì giải không giữ được")
    void bookedSlotCannotBeHeld() {
        TimeSlot booked = slot(TimeSlotStatus.BOOKED);
        when(timeSlotRepository.findByCourtAndTimeForUpdate(courtId, playDate, startTime, endTime))
                .thenReturn(Optional.of(booked));

        assertThatThrownBy(() -> service.hold(courtId, playDate, startTime, endTime))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("đã có người đặt");

        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    @DisplayName("khung giờ đang bị giữ tạm cũng không cướp được")
    void lockedSlotCannotBeHeld() {
        when(timeSlotRepository.findByCourtAndTimeForUpdate(courtId, playDate, startTime, endTime))
                .thenReturn(Optional.of(slot(TimeSlotStatus.LOCKED)));

        assertThatThrownBy(() -> service.hold(courtId, playDate, startTime, endTime))
                .isInstanceOf(ServiceException.class);

        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    @DisplayName("sân đang bảo trì báo đúng lý do thay vì báo đã có người đặt")
    void maintenanceSlotReportsItsOwnReason() {
        when(timeSlotRepository.findByCourtAndTimeForUpdate(courtId, playDate, startTime, endTime))
                .thenReturn(Optional.of(slot(TimeSlotStatus.MAINTENANCE)));

        assertThatThrownBy(() -> service.hold(courtId, playDate, startTime, endTime))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("bảo trì");
    }

    @Test
    @DisplayName("sân không có khung giờ đó thì báo rõ")
    void missingSlotIsReported() {
        when(timeSlotRepository.findByCourtAndTimeForUpdate(courtId, playDate, startTime, endTime))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.hold(courtId, playDate, startTime, endTime))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("không có khung giờ");
    }

    @Test
    @DisplayName("trả sân đưa khung giờ về trống")
    void releaseFreesTheSlot() {
        TimeSlot booked = slot(TimeSlotStatus.BOOKED);
        when(timeSlotRepository.findByCourtAndTimeForUpdate(courtId, playDate, startTime, endTime))
                .thenReturn(Optional.of(booked));

        service.release(courtId, playDate, startTime, endTime);

        assertThat(booked.getStatus()).isEqualTo(TimeSlotStatus.AVAILABLE);
    }

    @Test
    @DisplayName("trả sân không đụng vào khung giờ đang bảo trì")
    void releaseLeavesMaintenanceAlone() {
        TimeSlot maintenance = slot(TimeSlotStatus.MAINTENANCE);
        when(timeSlotRepository.findByCourtAndTimeForUpdate(courtId, playDate, startTime, endTime))
                .thenReturn(Optional.of(maintenance));

        service.release(courtId, playDate, startTime, endTime);

        assertThat(maintenance.getStatus()).isEqualTo(TimeSlotStatus.MAINTENANCE);
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    @DisplayName("trả một khung giờ không tồn tại thì im lặng bỏ qua")
    void releaseIsSafeWhenSlotMissing() {
        when(timeSlotRepository.findByCourtAndTimeForUpdate(courtId, playDate, startTime, endTime))
                .thenReturn(Optional.empty());

        service.release(courtId, playDate, startTime, endTime);

        verify(timeSlotRepository, never()).save(any());
    }
}
