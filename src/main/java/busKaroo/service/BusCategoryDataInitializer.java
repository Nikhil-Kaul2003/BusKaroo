package busKaroo.service;

import busKaroo.dao.BusCategoryRepository;
import busKaroo.dao.BusRepository;
import busKaroo.dao.RouteRepository;
import busKaroo.model.Bus;
import busKaroo.model.BusCategory;
import busKaroo.model.Route;
import busKaroo.model.enums.BusStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.ZoneId;

/** Adds the standard bus categories without reseeding or modifying other data. */
@Component
public class BusCategoryDataInitializer implements ApplicationRunner {

    private static final Map<String, String> DEFAULT_CATEGORIES = Map.of(
            "Normal", "Non-AC standard intercity bus with a 2+2 seating plan.",
            "Luxury", "Premium air-conditioned coach with a wider 2+1 seating plan.",
            "Sleeper", "Overnight bus with a 1+1 sleeper berth plan.");

    private final BusCategoryRepository categoryRepository;
    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final JdbcTemplate jdbcTemplate;

    public BusCategoryDataInitializer(BusCategoryRepository categoryRepository, BusRepository busRepository, RouteRepository routeRepository, JdbcTemplate jdbcTemplate) {
        this.categoryRepository = categoryRepository;
        this.busRepository = busRepository;
        this.routeRepository = routeRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        DEFAULT_CATEGORIES.forEach((name, description) -> {
            if (!categoryRepository.existsByNameIgnoreCase(name)) {
                BusCategory category = new BusCategory();
                category.setName(name);
                category.setDescription(description);
                categoryRepository.save(category);
            }
        });

        // The existing-db profile does not rerun data.sql. Normalize only the
        // numbered demo fleet so its current trips also expose all three plans.
        Map<String, BusCategory> standardCategories = new HashMap<>();
        categoryRepository.findAll().forEach(category -> {
            if (DEFAULT_CATEGORIES.containsKey(category.getName())) standardCategories.put(category.getName(), category);
        });
        ArrayList<Bus> changedBuses = new ArrayList<>();
        for (Bus bus : busRepository.findAll()) {
            String registration = bus.getRegistrationNumber();
            if (registration == null || !registration.matches(".*-AB-(10[0-9]{2}|1100)$")) continue;
            String currentName = bus.getCategory() == null ? "" : bus.getCategory().getName();
            if (DEFAULT_CATEGORIES.containsKey(currentName)) continue;

            int fleetNumber = Integer.parseInt(registration.substring(registration.length() - 4));
            String name = fleetNumber % 10 == 0 ? "Sleeper" : fleetNumber % 10 <= 5 ? "Normal" : "Luxury";
            bus.setCategory(standardCategories.get(name));
            int seatsPerRow = name.equals("Luxury") ? 3 : name.equals("Sleeper") ? 2 : 4;
            String plan = name.equals("Luxury") ? "2+1 premium" : name.equals("Sleeper") ? "1+1 sleeper berths" : "2+2 non-AC";
            int premiumSeats = name.equals("Luxury") ? Math.min(bus.getTotalSeats(), seatsPerRow) : 0;
            String categoryKey = name.equals("Luxury") ? "PREMIUM" : name.equals("Sleeper") ? "BERTH" : "STANDARD";
            StringBuilder numbers = new StringBuilder();
            for (int seat = 1; seat <= (premiumSeats > 0 ? premiumSeats : bus.getTotalSeats()); seat++) {
                if (seat > 1) numbers.append(',');
                numbers.append('"').append(seat).append('"');
            }
            bus.setSeatLayout("{\"rows\":" + (int) Math.ceil(bus.getTotalSeats() / (double) seatsPerRow)
                    + ",\"seatsPerRow\":" + seatsPerRow + ",\"layout\":\"" + plan
                    + "\",\"totalSeats\":" + bus.getTotalSeats() + ",\"airConditioned\":" + !name.equals("Normal")
                    + ",\"categories\":{\"" + categoryKey + "\":[" + numbers + "]}}");
            if (name.equals("Normal")) {
                bus.setModel(bus.getModel().replaceAll("(?i)( AC Intercity Coach| Luxury Seater)$", " Non-AC Roadways Seater"));
            } else if (name.equals("Sleeper") && !bus.getModel().toLowerCase().contains("sleeper")) {
                bus.setModel("Sleeper " + bus.getModel());
            }
            changedBuses.add(bus);
        }
        if (!changedBuses.isEmpty()) busRepository.saveAll(changedBuses);

        // Add an idempotent nationwide fleet on existing-db installations too.
        // Registration prefixes cover all state route networks; buses are linked
        // to every active route so the fleet is available to route scheduling.
        String[] states = {"AP", "AR", "AS", "BR", "CG", "GA", "GJ", "HR", "HP", "JH", "KA", "KL", "MP", "MH", "MN", "ML", "MZ", "NL", "OD", "PB", "RJ", "SK", "TN", "TS", "TR", "UP", "UK", "WB"};
        String[] manufacturers = {"Volvo", "Scania", "BharatBenz", "Tata", "Ashok Leyland", "Eicher"};
        Map<String, BusCategory> categoryByName = new HashMap<>();
        categoryRepository.findAll().forEach(category -> categoryByName.put(category.getName(), category));
        Map<String, Bus> existingByRegistration = new HashMap<>();
        busRepository.findAll().forEach(bus -> existingByRegistration.put(bus.getRegistrationNumber(), bus));
        var activeRoutes = routeRepository.findAll().stream().filter(Route::isActive).toList();
        ArrayList<Bus> fleetToAdd = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            for (String type : new String[]{"Luxury", "Sleeper"}) {
                int serial = (type.equals("Luxury") ? 3000 : 4000) + i;
                String registration = states[(i - 1) % states.length] + "-01-BK-" + serial;
                if (existingByRegistration.containsKey(registration)) continue;
                boolean luxury = type.equals("Luxury");
                int seatsPerRow = luxury ? 3 : 2;
                int totalSeats = 36;
                String layout = luxury ? "2+1 premium" : "1+1 sleeper berths";
                String seatCategories = luxury ? "\"PREMIUM\":[\"1\",\"2\",\"3\"]" : "\"BERTH\":[]";
                Bus bus = new Bus();
                bus.setRegistrationNumber(registration);
                bus.setManufacturer(manufacturers[(i - 1) % manufacturers.length]);
                bus.setModel(luxury ? "Luxury Premium 2+1 Intercity Coach" : "AC Sleeper Berth Coach 1+1");
                bus.setYear((short) (2023 + i % 4));
                bus.setTotalSeats(totalSeats);
                bus.setStatus(BusStatus.ACTIVE);
                bus.setCategory(categoryByName.get(type));
                bus.setSeatLayout("{\"rows\":" + (totalSeats / seatsPerRow) + ",\"seatsPerRow\":" + seatsPerRow
                        + ",\"layout\":\"" + layout + "\",\"totalSeats\":" + totalSeats
                        + ",\"airConditioned\":true,\"categories\":{" + seatCategories + "}}");
                bus.getRoutes().addAll(activeRoutes);
                fleetToAdd.add(bus);
            }
        }
        if (!fleetToAdd.isEmpty()) busRepository.saveAll(fleetToAdd);

        // Fleet entries alone are not bookable. Existing databases already have
        // trips, so add category-specific schedules across each active Indian
        // route as well. Exact bus/date/direction checks make startup repeatable.
        jdbcTemplate.update(CATEGORY_TRIPS_SQL, java.sql.Date.valueOf(LocalDate.now(ZoneId.of("Asia/Kolkata"))));
    }

    private static final String CATEGORY_TRIPS_SQL = """
            WITH RECURSIVE days(day_offset) AS (
                SELECT 0 UNION ALL SELECT day_offset + 1 FROM days WHERE day_offset < 179
            ), buses AS (
                SELECT b.id, c.name AS category_name,
                       ROW_NUMBER() OVER (PARTITION BY c.name ORDER BY b.registration_number) AS fleet_number
                FROM bus b JOIN bus_category c ON c.id = b.category_id
                WHERE c.name IN ('Luxury', 'Sleeper') AND b.status = 'ACTIVE'
                  AND b.registration_number LIKE '%-BK-%'
            ), fleet_sizes AS (
                SELECT category_name, COUNT(*) AS fleet_size FROM buses GROUP BY category_name
            ), drivers AS (
                SELECT user_id, ROW_NUMBER() OVER (ORDER BY user_id) - 1 AS driver_number FROM driver
            ), driver_count AS (
                SELECT COUNT(*) AS total FROM drivers
            ), plans(category_name, is_reverse, departure_time) AS (
                VALUES ('Luxury', false, TIME '06:15'), ('Luxury', true, TIME '10:15'),
                       ('Sleeper', false, TIME '20:15'), ('Sleeper', true, TIME '22:15')
            ), candidates AS (
                SELECT r.id AS route_id, bus.id AS bus_id, driver.user_id AS driver_id,
                       p.is_reverse,
                       (?::date + d.day_offset + p.departure_time) AT TIME ZONE 'UTC' AS departure_datetime,
                       GREATEST(COALESCE(MAX(rs.departure_offset_minutes), 180), 60) AS duration_minutes
                FROM route r
                JOIN route_stop rs ON rs.route_id = r.id
                JOIN terminal terminal ON terminal.id = rs.terminal_id AND terminal.country = 'India'
                CROSS JOIN days d
                CROSS JOIN plans p
                JOIN fleet_sizes fs ON fs.category_name = p.category_name AND fs.fleet_size > 0
                JOIN buses bus ON bus.category_name = p.category_name
                    AND bus.fleet_number = ((r.id + d.day_offset + CASE WHEN p.is_reverse THEN 13 ELSE 0 END) % fs.fleet_size) + 1
                CROSS JOIN driver_count dc
                JOIN drivers driver ON driver.driver_number =
                    ((r.id + d.day_offset + CASE WHEN p.is_reverse THEN 1 ELSE 0 END) % dc.total)
                WHERE r.is_active = true
                GROUP BY r.id, bus.id, driver.user_id, p.is_reverse, d.day_offset, p.departure_time
            )
            INSERT INTO trip (id, route_id, bus_id, driver_id, price_koefficient, reverse,
                              departure_datetime, arrival_datetime, status, created_at, updated_at)
            SELECT nextval(pg_get_serial_sequence('trip', 'id')), c.route_id, c.bus_id, c.driver_id,
                   1.00, c.is_reverse, c.departure_datetime,
                   c.departure_datetime + c.duration_minutes * INTERVAL '1 minute',
                   'SCHEDULED', NOW(), NOW()
            FROM candidates c
            WHERE NOT EXISTS (
                SELECT 1 FROM trip existing
                WHERE existing.route_id = c.route_id AND existing.bus_id = c.bus_id
                  AND existing.reverse = c.is_reverse
                  AND existing.departure_datetime = c.departure_datetime
            )
            """;
}
