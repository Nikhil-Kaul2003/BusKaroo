package busKaroo.service;

import busKaroo.dao.TripRepository;
import busKaroo.exception.ResourceNotFoundException;
import busKaroo.model.Trip;
import busKaroo.model.RouteStop;
import busKaroo.model.Terminal;
import busKaroo.model.Bus;
import busKaroo.model.enums.TripStatus;
import busKaroo.dto.response.TripResponse;
import busKaroo.dto.response.ConnectionResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class TripService {

    private final TripRepository tripRepository;
    private final busKaroo.dao.ReservationPassengerRepository reservationPassengerRepository;

    @Value("${app.reservations.pending-ttl-minutes:15}")
    private int pendingReservationTtlMinutes;

    public TripService(TripRepository tripRepository,
                       busKaroo.dao.ReservationPassengerRepository reservationPassengerRepository) {
        this.tripRepository = tripRepository;
        this.reservationPassengerRepository = reservationPassengerRepository;
    }

    public List<Trip> findAll() {
        return tripRepository.findAll();
    }

    public Trip findById(Long id) {
        return tripRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + id));
    }

    public List<Trip> searchTrips(Long routeId, LocalDate date) {
        OffsetDateTime startOfDay = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        return tripRepository.findAllByRouteIdAndDepartureDatetimeBetweenAndStatus(
                routeId, startOfDay, endOfDay, TripStatus.SCHEDULED);
    }

    public List<Trip> findByDriverId(Long driverId) {
        return tripRepository.findByDriverUserId(driverId);
    }

    @Transactional
    public Trip create(Trip trip) {
        trip.setStatus(TripStatus.SCHEDULED);
        return tripRepository.save(trip);
    }

    @Transactional
    public Trip update(Long id, Trip tripDetails) {
        Trip trip = findById(id);
        trip.setRoute(tripDetails.getRoute());
        trip.setBus(tripDetails.getBus());
        trip.setDriver(tripDetails.getDriver());
        trip.setPriceKoefficient(tripDetails.getPriceKoefficient());
        trip.setReverse(tripDetails.isReverse());
        trip.setDepartureDatetime(tripDetails.getDepartureDatetime());
        trip.setArrivalDatetime(tripDetails.getArrivalDatetime());
        return tripRepository.save(trip);
    }

    @Transactional
    public void updateStatus(Long id, TripStatus status) {
        Trip trip = findById(id);
        trip.setStatus(status);
        tripRepository.save(trip);
    }

    @Transactional
    public void delete(Long id) {
        Trip trip = findById(id);
        tripRepository.delete(trip);
    }

    public int getAvailableSeats(Long tripId) {
        Trip trip = findById(tripId);
        int totalSeats = trip.getBus().getTotalSeats();
        List<busKaroo.model.RouteStop> stops = trip.getRoute().getStops();
        if (stops.size() < 2) {
            return totalSeats;
        }
        int numSegments = stops.size() - 1;
        int[] bookedCount = new int[numSegments];

        for (busKaroo.model.Reservation res : trip.getReservations()) {
            if (res.getStatus() == busKaroo.model.enums.ReservationStatus.CANCELLED
                    || res.getStatus() == busKaroo.model.enums.ReservationStatus.EXPIRED) {
                continue;
            }
            if (res.getStatus() == busKaroo.model.enums.ReservationStatus.PENDING
                    && res.getCreatedAt() != null
                    && res.getCreatedAt().isBefore(
                            OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(pendingReservationTtlMinutes))) {
                continue;
            }
            int start = res.getOriginRouteStop().getSequenceOrder();
            int end = res.getTargetRouteStop().getSequenceOrder();

            int startIdx = -1;
            int endIdx = -1;
            for (int i = 0; i < stops.size(); i++) {
                if (stops.get(i).getSequenceOrder() == start) {
                    startIdx = i;
                }
                if (stops.get(i).getSequenceOrder() == end) {
                    endIdx = i;
                }
            }

            if (startIdx != -1 && endIdx != -1) {
                int from = Math.min(startIdx, endIdx);
                int to = Math.max(startIdx, endIdx);
                for (int s = from; s < to; s++) {
                    bookedCount[s] += res.getPassengers().size();
                }
            }
        }

        int maxBooked = 0;
        for (int count : bookedCount) {
            if (count > maxBooked) {
                maxBooked = count;
            }
        }
        return totalSeats - maxBooked;
    }

    public List<TripResponse> searchTripsNew(
            String fromCity,
            Long fromTerminalId,
            String toCity,
            Long toTerminalId,
            LocalDate date,
            Integer passengers) {
        return searchTripsNew(fromCity, fromTerminalId, toCity, toTerminalId, date, passengers, null);
    }

    public List<TripResponse> searchTripsNew(
            String fromCity,
            Long fromTerminalId,
            String toCity,
            Long toTerminalId,
            LocalDate date,
            Integer passengers,
            String timeWindow) {

        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
        boolean isToday = date.equals(today);

        OffsetDateTime actualTime = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime startOfTodayMidnight = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfTodayMidnight = date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfTomorrowMidnight = date.plusDays(2).atStartOfDay().atOffset(ZoneOffset.UTC);

        Long queryFromTerminalId = (fromTerminalId != null && fromTerminalId > 0) ? fromTerminalId : null;
        String queryFromCity = (queryFromTerminalId == null && fromCity != null && !fromCity.trim().isEmpty()) ? fromCity.trim().toLowerCase() : null;

        Long queryToTerminalId = (toTerminalId != null && toTerminalId > 0) ? toTerminalId : null;
        String queryToCity = (queryToTerminalId == null && toCity != null && !toCity.trim().isEmpty()) ? toCity.trim().toLowerCase() : null;

        // Long-distance routes may leave their origin up to three days before
        // the searched travel date. Fetch those trips so later route stops are
        // still searchable, then keep only segments departing in the requested
        // date window below.
        OffsetDateTime dbStart = startOfTodayMidnight.minusDays(3);
        OffsetDateTime dbEnd = endOfTomorrowMidnight;

        List<Trip> candidateTrips = tripRepository.findCandidateTrips(
                dbStart, dbEnd, TripStatus.SCHEDULED,
                queryFromTerminalId, queryFromCity,
                queryToTerminalId, queryToCity);

        List<TripResponse> results = new ArrayList<>();

        if (isToday) {
            // 1. Process today's options starting from actual time until the end of today
            List<TripResponse> todayResults = processCandidateTrips(
                    candidateTrips,
                    queryFromTerminalId, queryFromCity,
                    queryToTerminalId, queryToCity,
                    actualTime, endOfTodayMidnight);

            results.addAll(todayResults);

            // 2. Determine if options today are 2 or fewer
            if (todayResults.size() <= 2) {
                // Output all of tomorrow's options
                List<TripResponse> tomorrowResults = processCandidateTrips(
                        candidateTrips,
                        queryFromTerminalId, queryFromCity,
                        queryToTerminalId, queryToCity,
                        endOfTodayMidnight, endOfTomorrowMidnight);
                results.addAll(tomorrowResults);
            } else {
                // Output tomorrow's options only up to actual time tomorrow
                List<TripResponse> tomorrowResults = processCandidateTrips(
                        candidateTrips,
                        queryFromTerminalId, queryFromCity,
                        queryToTerminalId, queryToCity,
                        endOfTodayMidnight, actualTime.plusDays(1));
                results.addAll(tomorrowResults);
            }
        } else {
            // 1. Process all options on the query date
            List<TripResponse> queryDateResults = processCandidateTrips(
                    candidateTrips,
                    queryFromTerminalId, queryFromCity,
                    queryToTerminalId, queryToCity,
                    startOfTodayMidnight, endOfTodayMidnight);

            results.addAll(queryDateResults);

            // 2. Determine if options on query date are 2 or fewer
            if (queryDateResults.size() <= 2) {
                // Output all options of the next day as well
                List<TripResponse> nextDayResults = processCandidateTrips(
                        candidateTrips,
                        queryFromTerminalId, queryFromCity,
                        queryToTerminalId, queryToCity,
                        endOfTodayMidnight, endOfTomorrowMidnight);
                results.addAll(nextDayResults);
            }
        }

        results.removeIf(trip -> !matchesTimeWindow(trip, timeWindow));
        results.sort(Comparator.comparing(TripResponse::departureDatetime));
        return results;
    }

    /**
     * Finds practical one-transfer journeys when the requested city pair has
     * no direct service. Returns every available two-leg combination with a
     * 30-minute to 8-hour connection at any transfer city.
     */
    public List<ConnectionResponse> searchConnections(
            String fromCity, String toCity, LocalDate date, Integer passengers, String timeWindow) {
        if (fromCity == null || toCity == null || date == null
                || fromCity.isBlank() || toCity.isBlank()
                || fromCity.equalsIgnoreCase(toCity)) {
            return List.of();
        }

        String origin = fromCity.trim().toLowerCase(Locale.ROOT);
        String destination = toCity.trim().toLowerCase(Locale.ROOT);
        int passengerCount = passengers == null || passengers < 1 ? 1 : passengers;
        OffsetDateTime requestedStart = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime nextDay = date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime firstLegStart = date.equals(LocalDate.now(ZoneId.of("Asia/Kolkata")))
                ? OffsetDateTime.now(ZoneOffset.UTC)
                : requestedStart;
        OffsetDateTime candidateStart = requestedStart.minusDays(3);
        OffsetDateTime candidateEnd = date.plusDays(8).atStartOfDay().atOffset(ZoneOffset.UTC);

        // Fetch each side once. Searching separately for every possible hub
        // made a no-direct-route request issue hundreds of database queries.
        List<Trip> firstCandidates = tripRepository.findCandidateTrips(
                candidateStart, nextDay, TripStatus.SCHEDULED,
                null, origin, null, null);
        List<Trip> secondCandidates = tripRepository.findCandidateTrips(
                candidateStart, candidateEnd, TripStatus.SCHEDULED,
                null, null, null, destination);
        Set<String> firstSideCities = citiesIn(firstCandidates);
        Set<String> transferCities = citiesIn(secondCandidates);
        transferCities.retainAll(firstSideCities);
        transferCities.removeIf(city -> city.equalsIgnoreCase(fromCity)
                || city.equalsIgnoreCase(toCity));

        List<ConnectionResponse> connections = new ArrayList<>();
        for (String transferCity : transferCities) {
            String transfer = transferCity.toLowerCase(Locale.ROOT);
            List<TripResponse> firstLegs = processCandidateTrips(
                    firstCandidates, null, origin, null, transfer, firstLegStart, nextDay);
            firstLegs.removeIf(leg -> leg.availableSeats().size() < passengerCount
                    || !matchesTimeWindow(leg, timeWindow));
            firstLegs.sort(Comparator.comparing(TripResponse::departureDatetime));
            if (firstLegs.isEmpty()) continue;

            List<TripResponse> secondLegs = processCandidateTrips(
                    secondCandidates, null, transfer, null, destination, candidateStart, candidateEnd);
            secondLegs.removeIf(leg -> leg.availableSeats().size() < passengerCount);
            secondLegs.sort(Comparator.comparing(TripResponse::departureDatetime));
            if (secondLegs.isEmpty()) continue;

            for (TripResponse first : firstLegs) {
                OffsetDateTime earliestTransferDeparture = first.arrivalDatetime().plusMinutes(30);
                OffsetDateTime latestTransferDeparture = first.arrivalDatetime().plusHours(8);
                for (TripResponse second : secondLegs) {
                    if (second.departureDatetime().isBefore(earliestTransferDeparture)) continue;
                    if (second.departureDatetime().isAfter(latestTransferDeparture)) break;
                    long layover = java.time.Duration.between(
                            first.arrivalDatetime(), second.departureDatetime()).toMinutes();
                    connections.add(new ConnectionResponse(
                            transferCity,
                            first,
                            second,
                            first.price().add(second.price()),
                            layover));
                }
            }
        }

        connections.sort(Comparator
                .comparing((ConnectionResponse option) -> option.firstLeg().departureDatetime())
                .thenComparing(ConnectionResponse::totalPrice));
        return connections;
    }

    private Set<String> citiesIn(List<Trip> candidates) {
        Set<String> cities = new LinkedHashSet<>();
        for (Trip trip : candidates) {
            for (RouteStop stop : trip.getRoute().getStops()) {
                Terminal terminal = stop.getTerminal();
                if (terminal != null && terminal.getCity() != null && !terminal.getCity().isBlank()) {
                    cities.add(terminal.getCity().trim());
                }
            }
        }
        return cities;
    }

    private boolean matchesTimeWindow(TripResponse trip, String timeWindow) {
        if (timeWindow == null || timeWindow.isBlank() || "any".equalsIgnoreCase(timeWindow)) {
            return true;
        }
        int hour = trip.departureDatetime().atZoneSameInstant(ZoneId.of("Asia/Kolkata")).getHour();
        return switch (timeWindow.toLowerCase()) {
            case "overnight" -> hour < 6;
            case "morning" -> hour >= 6 && hour < 12;
            case "afternoon" -> hour >= 12 && hour < 18;
            case "evening" -> hour >= 18;
            default -> throw new IllegalArgumentException("Unsupported departure time window: " + timeWindow);
        };
    }

    private List<TripResponse> processCandidateTrips(
            List<Trip> candidateTrips,
            Long queryFromTerminalId, String queryFromCity,
            Long queryToTerminalId, String queryToCity,
            OffsetDateTime startRange, OffsetDateTime endRange) {
        List<TripResponse> results = new ArrayList<>();
        for (Trip trip : candidateTrips) {
            List<RouteStop> stops = trip.getRoute().getStops();
            List<RouteStop> fromStops = new ArrayList<>();
            List<RouteStop> toStops = new ArrayList<>();

            for (RouteStop stop : stops) {
                Terminal terminal = stop.getTerminal();
                if (terminal == null) continue;

                boolean matchesFrom = false;
                if (queryFromTerminalId != null) {
                    matchesFrom = terminal.getId().equals(queryFromTerminalId);
                } else if (queryFromCity != null) {
                    matchesFrom = terminal.getCity().equalsIgnoreCase(queryFromCity);
                }
                if (matchesFrom) {
                    fromStops.add(stop);
                }

                boolean matchesTo = false;
                if (queryToTerminalId != null) {
                    matchesTo = terminal.getId().equals(queryToTerminalId);
                } else if (queryToCity != null) {
                    matchesTo = terminal.getCity().equalsIgnoreCase(queryToCity);
                }
                if (matchesTo) {
                    toStops.add(stop);
                }
            }

            RouteStop bestFStop = null;
            RouteStop bestTStop = null;

            for (RouteStop fStop : fromStops) {
                for (RouteStop tStop : toStops) {
                    if (fStop.getId().equals(tStop.getId())) continue;

                    boolean sequenceMatches = trip.isReverse() 
                            ? fStop.getSequenceOrder() > tStop.getSequenceOrder()
                            : fStop.getSequenceOrder() < tStop.getSequenceOrder();

                    if (sequenceMatches) {
                        if (bestFStop == null) {
                            bestFStop = fStop;
                            bestTStop = tStop;
                        } else {
                            if (trip.isReverse()) {
                                if (fStop.getSequenceOrder() > bestFStop.getSequenceOrder() ||
                                    (fStop.getSequenceOrder() == bestFStop.getSequenceOrder() && tStop.getSequenceOrder() < bestTStop.getSequenceOrder())) {
                                    bestFStop = fStop;
                                    bestTStop = tStop;
                                }
                            } else {
                                if (fStop.getSequenceOrder() < bestFStop.getSequenceOrder() ||
                                    (fStop.getSequenceOrder() == bestFStop.getSequenceOrder() && tStop.getSequenceOrder() > bestTStop.getSequenceOrder())) {
                                    bestFStop = fStop;
                                    bestTStop = tStop;
                                }
                            }
                        }
                    }
                }
            }

            if (bestFStop == null || bestTStop == null) {
                continue;
            }

            OffsetDateTime segmentDeparture;
            OffsetDateTime segmentArrival;

            if (!trip.isReverse()) {
                segmentDeparture = trip.getDepartureDatetime().plusMinutes(bestFStop.getDepartureOffsetMinutes());
                segmentArrival = trip.getDepartureDatetime().plusMinutes(bestTStop.getArrivalOffsetMinutes());
            } else {
                int maxOffset = 0;
                for (RouteStop stop : stops) {
                    if (stop.getDepartureOffsetMinutes() > maxOffset) {
                        maxOffset = stop.getDepartureOffsetMinutes();
                    }
                }
                segmentDeparture = trip.getDepartureDatetime().plusMinutes(maxOffset - bestFStop.getArrivalOffsetMinutes());
                segmentArrival = trip.getDepartureDatetime().plusMinutes(maxOffset - bestTStop.getDepartureOffsetMinutes());
            }

            if (segmentDeparture.compareTo(startRange) < 0 || segmentDeparture.compareTo(endRange) >= 0) {
                continue;
            }

            Bus bus = trip.getBus();
            int busTotalSeats = bus.getTotalSeats();

            Set<String> allSeats = new HashSet<>();
            try {
                com.fasterxml.jackson.databind.JsonNode rootNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(bus.getSeatLayout());
                com.fasterxml.jackson.databind.JsonNode categories = rootNode.path("categories");
                if (categories.isObject()) {
                    java.util.Iterator<String> fieldNames = categories.fieldNames();
                    while (fieldNames.hasNext()) {
                        String catName = fieldNames.next();
                        com.fasterxml.jackson.databind.JsonNode seatsNode = categories.path(catName);
                        if (seatsNode.isArray()) {
                            for (com.fasterxml.jackson.databind.JsonNode seat : seatsNode) {
                                allSeats.add(seat.asText());
                            }
                        }
                    }
                }
            } catch (Exception e) {}

            if (allSeats.size() < busTotalSeats) {
                boolean hasLetters = false;
                for (String s : allSeats) {
                    if (s.matches(".*[A-Za-z].*")) {
                        hasLetters = true;
                        break;
                    }
                }
                if (hasLetters) {
                    int rows = 14;
                    int seatsPerRow = 4;
                    try {
                        com.fasterxml.jackson.databind.JsonNode rootNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(bus.getSeatLayout());
                        if (rootNode.has("rows")) rows = rootNode.get("rows").asInt();
                        if (rootNode.has("seatsPerRow")) seatsPerRow = rootNode.get("seatsPerRow").asInt();
                    } catch (Exception e) {}

                    char[] letters = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'};
                    for (int r = 1; r <= rows; r++) {
                        for (int c = 0; c < seatsPerRow; c++) {
                            if (allSeats.size() >= busTotalSeats) break;
                            String seatNum = r + "" + letters[c];
                            allSeats.add(seatNum);
                        }
                    }
                } else {
                    for (int i = 1; i <= busTotalSeats; i++) {
                        allSeats.add(String.valueOf(i));
                    }
                }
            }

            int queryStart = bestFStop.getSequenceOrder();
            int queryEnd = bestTStop.getSequenceOrder();

            int minQuery = Math.min(queryStart, queryEnd);
            int maxQuery = Math.max(queryStart, queryEnd);

            List<String> occupied = reservationPassengerRepository.findOccupiedSeatsForTripAndSegment(
                    trip.getId(), minQuery, maxQuery, pendingReservationTtlMinutes);
            Set<String> occupiedSeats = new HashSet<>(occupied);

            List<String> availableList = new ArrayList<>(allSeats);
            availableList.removeAll(occupiedSeats);

            // Sort natural/alphabetic order
            availableList.sort((s1, s2) -> {
                try {
                    return Integer.compare(Integer.parseInt(s1), Integer.parseInt(s2));
                } catch (Exception e) {
                    return s1.compareTo(s2);
                }
            });

            List<String> occupiedList = new ArrayList<>(occupiedSeats);
            occupiedList.sort((s1, s2) -> {
                try {
                    return Integer.compare(Integer.parseInt(s1), Integer.parseInt(s2));
                } catch (Exception e) {
                    return s1.compareTo(s2);
                }
            });

            String driverName = trip.getDriver() != null
                    ? trip.getDriver().getUser().getFirstName() + " " + trip.getDriver().getUser().getLastName()
                    : null;

            BigDecimal segmentPrice = bestTStop.getBasePriceFromOrigin()
                    .subtract(bestFStop.getBasePriceFromOrigin())
                    .abs()
                    .multiply(trip.getPriceKoefficient());

            String busName = bus.getManufacturer() + " " + bus.getModel();

            results.add(new TripResponse(
                    trip.getId(),
                    trip.getRoute().getId(),
                    trip.getRoute().getName(),
                    bus.getRegistrationNumber(),
                    busName,
                    BusCategoryLabel.from(bus),
                    bus.getSeatLayout(),
                    bestFStop.getTerminal().getName(),
                    bestTStop.getTerminal().getName(),
                    segmentDeparture,
                    segmentArrival,
                    segmentPrice,
                    trip.getStatus(),
                    driverName,
                    availableList,
                    occupiedList,
                    trip.isReverse()
            ));
        }
        return results;
    }
}
