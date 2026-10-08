package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.OwnerCustomerMetricsQuery;
import iuh.fit.venueservice.application.dto.request.venue.OwnerRevenueQuery;
import iuh.fit.venueservice.application.dto.response.venue.OwnerCustomerMetricsPeriod;
import iuh.fit.venueservice.application.dto.response.venue.OwnerCustomerMetricsRecord;
import iuh.fit.venueservice.application.dto.response.venue.OwnerCustomerMetricsView;
import iuh.fit.venueservice.application.dto.response.venue.OwnerDailyRevenue;
import iuh.fit.venueservice.application.dto.response.venue.OwnerHourlyRevenue;
import iuh.fit.venueservice.application.dto.response.venue.OwnerRevenuePeriod;
import iuh.fit.venueservice.application.dto.response.venue.OwnerRevenueStatusBreakdown;
import iuh.fit.venueservice.application.dto.response.venue.OwnerRevenueView;
import iuh.fit.venueservice.application.dto.response.venue.PaymentSnapshot;
import iuh.fit.venueservice.application.dto.response.venue.RevenueBookingRecord;
import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.PaymentQueryPort;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.OwnerRevenueService;
import iuh.fit.venueservice.domain.entity.Venue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

public class OwnerRevenueServiceImpl implements OwnerRevenueService {
    private static final int PAYMENT_BATCH_SIZE = 100;
    private static final String CURRENCY = "VND";
    private static final String PERIOD_BASIS = "BOOKING_PLAY_DATE";
    private static final String SUCCEEDED = "SUCCEEDED";

    private final BookingRepository bookingRepository;
    private final VenueRepository venueRepository;
    private final PaymentQueryPort paymentQueryPort;

    public OwnerRevenueServiceImpl(
            BookingRepository bookingRepository,
            VenueRepository venueRepository,
            PaymentQueryPort paymentQueryPort
    ) {
        this.bookingRepository = bookingRepository;
        this.venueRepository = venueRepository;
        this.paymentQueryPort = paymentQueryPort;
    }

    @Override
    public List<UUID> findOwnerIds() {
        return venueRepository.findOwnerIds();
    }

    @Override
    public OwnerRevenueView getRevenue(OwnerRevenueQuery query) throws ServiceException {
        query.validate();
        ensureVenueScope(query.venueId(), query.ownerId());

        long periodDays = ChronoUnit.DAYS.between(query.fromDate(), query.toDate()) + 1;
        LocalDate previousTo = query.fromDate().minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(periodDays - 1);

        PeriodAnalysis current = analyze(
                query.fromDate(), query.toDate(), loadBookings(query, query.fromDate(), query.toDate())
        );
        PeriodAnalysis previous = analyze(
                previousFrom, previousTo, loadBookings(query, previousFrom, previousTo)
        );

        return new OwnerRevenueView(
                query.venueId(), CURRENCY, PERIOD_BASIS,
                current.summary(), previous.summary(),
                percentage(current.summary().totalRevenue(), previous.summary().totalRevenue()),
                percentage(
                        BigDecimal.valueOf(current.summary().bookingCount()),
                        BigDecimal.valueOf(previous.summary().bookingCount())
                ),
                current.statusBreakdown(), current.dailyRevenue(), current.hourlyRevenue()
        );
    }

    @Override
    public OwnerCustomerMetricsView getCustomerMetrics(OwnerCustomerMetricsQuery query) throws ServiceException {
        query.validate();
        ensureVenueScope(query.venueId(), query.ownerId());

        YearMonth currentMonth = query.month();
        YearMonth previousMonth = currentMonth.minusMonths(1);
        LocalDate currentFrom = currentMonth.atDay(1);
        LocalDate currentToExclusive = currentMonth.plusMonths(1).atDay(1);
        LocalDate previousFrom = previousMonth.atDay(1);
        OwnerCustomerMetricsRecord metrics = bookingRepository.findOwnedCustomerMetrics(
                query.ownerId(), query.venueId(), previousFrom, currentFrom, currentToExclusive
        );

        return new OwnerCustomerMetricsView(
                query.venueId(), PERIOD_BASIS,
                customerPeriod(
                        currentFrom, currentToExclusive.minusDays(1),
                        metrics.currentTotalCustomers(), metrics.currentNewCustomers(),
                        metrics.currentReturningCustomers()
                ),
                customerPeriod(
                        previousFrom, currentFrom.minusDays(1),
                        metrics.previousTotalCustomers(), metrics.previousNewCustomers(),
                        metrics.previousReturningCustomers()
                )
        );
    }

    private List<RevenueBookingRecord> loadBookings(
            OwnerRevenueQuery query, LocalDate fromDate, LocalDate toDate
    ) {
        return bookingRepository.findOwnedRevenueBookings(
                query.ownerId(), query.venueId(), fromDate, toDate
        );
    }

    private PeriodAnalysis analyze(
            LocalDate fromDate, LocalDate toDate, List<RevenueBookingRecord> bookings
    ) throws ServiceException {
        Map<UUID, RevenueBookingRecord> bookingRecords = new LinkedHashMap<>();
        for (RevenueBookingRecord booking : bookings) {
            bookingRecords.put(booking.bookingId(), booking);
        }

        List<PaymentSnapshot> payments = loadPayments(new ArrayList<>(bookingRecords.keySet()));
        Map<String, StatusAccumulator> breakdown = new TreeMap<>();
        Map<LocalDate, DailyAccumulator> daily = new LinkedHashMap<>();
        Map<Integer, DailyAccumulator> hourly = new LinkedHashMap<>();
        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            daily.put(date, new DailyAccumulator());
        }
        for (int hour = 0; hour < 24; hour++) {
            hourly.put(hour, new DailyAccumulator());
        }

        BigDecimal revenue = BigDecimal.ZERO;
        Set<UUID> paidBookingIds = new LinkedHashSet<>();
        Set<UUID> seenPaymentIds = new LinkedHashSet<>();
        for (PaymentSnapshot payment : payments) {
            if (payment.paymentId() == null || !seenPaymentIds.add(payment.paymentId())) {
                continue;
            }
            RevenueBookingRecord booking = bookingRecords.get(payment.referenceId());
            if (booking == null) {
                continue;
            }
            ensureValidPaymentSnapshot(payment);
            BigDecimal amount = BigDecimal.valueOf(payment.amount());
            StatusAccumulator status = breakdown.computeIfAbsent(
                    payment.status(), ignored -> new StatusAccumulator()
            );
            status.add(amount);

            if (SUCCEEDED.equals(payment.status())) {
                revenue = revenue.add(amount);
                paidBookingIds.add(payment.referenceId());
                daily.get(booking.playDate()).add(amount);
                hourly.get(booking.startTime().getHour()).add(amount);
            }
        }

        OwnerRevenuePeriod summary = new OwnerRevenuePeriod(
                fromDate, toDate, bookingRecords.size(), paidBookingIds.size(), money(revenue)
        );
        List<OwnerRevenueStatusBreakdown> statusRows = breakdown.entrySet().stream()
                .map(entry -> new OwnerRevenueStatusBreakdown(
                        entry.getKey(), entry.getValue().count, money(entry.getValue().amount)
                ))
                .toList();
        List<OwnerDailyRevenue> dailyRows = daily.entrySet().stream()
                .map(entry -> new OwnerDailyRevenue(
                        entry.getKey(), money(entry.getValue().amount), entry.getValue().count
                ))
                .toList();
        List<OwnerHourlyRevenue> hourlyRows = hourly.entrySet().stream()
                .map(entry -> new OwnerHourlyRevenue(
                        entry.getKey(), money(entry.getValue().amount), entry.getValue().count
                ))
                .toList();
        return new PeriodAnalysis(summary, statusRows, dailyRows, hourlyRows);
    }

    private List<PaymentSnapshot> loadPayments(List<UUID> bookingIds) {
        if (bookingIds.isEmpty()) {
            return List.of();
        }
        List<PaymentSnapshot> result = new ArrayList<>();
        for (int start = 0; start < bookingIds.size(); start += PAYMENT_BATCH_SIZE) {
            int end = Math.min(start + PAYMENT_BATCH_SIZE, bookingIds.size());
            result.addAll(paymentQueryPort.findByBookingIds(
                    new LinkedHashSet<>(bookingIds.subList(start, end))
            ));
        }
        return result;
    }

    private OwnerCustomerMetricsPeriod customerPeriod(
            LocalDate fromDate,
            LocalDate toDate,
            long totalCustomers,
            long newCustomers,
            long returningCustomers
    ) {
        BigDecimal returningRate = totalCustomers == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(returningCustomers)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalCustomers), 2, RoundingMode.HALF_UP);
        return new OwnerCustomerMetricsPeriod(
                fromDate, toDate, totalCustomers, newCustomers, returningCustomers, returningRate
        );
    }

    private void ensureVenueScope(UUID venueId, UUID ownerId) throws ServiceException {
        if (venueId == null) {
            return;
        }
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy cơ sở."));
        if (!ownerId.equals(venue.getOwnerId())) {
            throw new OwnershipException("Cơ sở không thuộc quyền sở hữu của bạn.");
        }
    }

    private void ensureValidPaymentSnapshot(PaymentSnapshot payment) throws ServiceException {
        if (payment.referenceId() == null || payment.status() == null
                || payment.status().isBlank() || payment.amount() == null
                || payment.amount() < 0 || payment.currency() == null) {
            throw new ServiceException("Payment service trả về dữ liệu doanh thu không hợp lệ.");
        }
        if (!CURRENCY.equalsIgnoreCase(payment.currency())) {
            throw new ServiceException("Không thể tổng hợp nhiều loại tiền tệ trong một báo cáo.");
        }
    }

    private BigDecimal percentage(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private record PeriodAnalysis(
            OwnerRevenuePeriod summary,
            List<OwnerRevenueStatusBreakdown> statusBreakdown,
            List<OwnerDailyRevenue> dailyRevenue,
            List<OwnerHourlyRevenue> hourlyRevenue
    ) {
    }

    private static final class StatusAccumulator {
        private long count;
        private BigDecimal amount = BigDecimal.ZERO;

        void add(BigDecimal value) {
            count++;
            amount = amount.add(value);
        }
    }

    private static final class DailyAccumulator {
        private long count;
        private BigDecimal amount = BigDecimal.ZERO;

        void add(BigDecimal value) {
            count++;
            amount = amount.add(value);
        }
    }
}
