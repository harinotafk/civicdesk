package com.ram.civicdesk.service;

import com.ram.civicdesk.entity.*;
import com.ram.civicdesk.exception.BadRequestException;
import com.ram.civicdesk.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Demo accounts and complaints, so the app has something to show on first start.
 * All demo accounts use the password DEMO_PASSWORD.
 */
@Service
public class DemoDataService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataService.class);
    public static final String DEMO_PASSWORD = "Password@123";

    /** Approximate centre of each GCC zone, used to place demo complaints on the map. */
    private static final Map<Integer, double[]> ZONE_CENTRES = Map.ofEntries(
            Map.entry(1, new double[]{13.160, 80.300}),
            Map.entry(2, new double[]{13.167, 80.262}),
            Map.entry(3, new double[]{13.148, 80.231}),
            Map.entry(4, new double[]{13.127, 80.288}),
            Map.entry(5, new double[]{13.110, 80.285}),
            Map.entry(6, new double[]{13.118, 80.235}),
            Map.entry(7, new double[]{13.114, 80.155}),
            Map.entry(8, new double[]{13.085, 80.210}),
            Map.entry(9, new double[]{13.040, 80.250}),
            Map.entry(10, new double[]{13.050, 80.225}),
            Map.entry(11, new double[]{13.040, 80.175}),
            Map.entry(12, new double[]{13.000, 80.205}),
            Map.entry(13, new double[]{13.005, 80.255}),
            Map.entry(14, new double[]{12.965, 80.245}),
            Map.entry(15, new double[]{12.900, 80.228})
    );

    private static final String[] SAMPLE_DESCRIPTIONS = {
            "Problem has been there for two days, nobody has come yet",
            "Near the bus stop, very difficult for pedestrians and two-wheelers",
            "Opposite the ration shop, getting worse every day",
            "Children walk this way to school, please fix quickly",
            "Reported to the councillor already, still pending",
            "On the main road junction, causing traffic problems"
    };

    private final UserRepository userRepository;
    private final WardRepository wardRepository;
    private final ZoneRepository zoneRepository;
    private final CategoryRepository categoryRepository;
    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();

    public DemoDataService(UserRepository userRepository, WardRepository wardRepository, ZoneRepository zoneRepository,
                           CategoryRepository categoryRepository, ComplaintRepository complaintRepository,
                           ComplaintHistoryRepository historyRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.wardRepository = wardRepository;
        this.zoneRepository = zoneRepository;
        this.categoryRepository = categoryRepository;
        this.complaintRepository = complaintRepository;
        this.historyRepository = historyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ------------------------------------------------------------------
    // First-start seed
    // ------------------------------------------------------------------

    @Transactional
    public void seedIfEmpty() {
        if (userRepository.count() > 0) {
            return;
        }
        if (wardRepository.count() == 0 || categoryRepository.count() == 0) {
            log.warn("Reference data missing (data.sql did not run?) - skipping demo seed");
            return;
        }

        String hash = passwordEncoder.encode(DEMO_PASSWORD);

        user("Commissioner (Admin)", "admin@civicdesk.in", "9000000001", Role.ADMIN, null, null, hash);
        user("Lakshmi Narayanan", "zonal.adyar@civicdesk.in", "9000000013", Role.ZONAL_OFFICER, null, 13, hash);
        user("Suresh Kumar", "zonal.annanagar@civicdesk.in", "9000000008", Role.ZONAL_OFFICER, null, 8, hash);

        Map<Integer, User> officers = new HashMap<>();
        officers.put(170, user("Karthik R", "ward170@civicdesk.in", "9000000170", Role.WARD_OFFICER, 170L, null, hash));
        officers.put(171, user("Divya S", "ward171@civicdesk.in", "9000000171", Role.WARD_OFFICER, 171L, null, hash));
        officers.put(172, user("Arun P", "ward172@civicdesk.in", "9000000172", Role.WARD_OFFICER, 172L, null, hash));
        officers.put(173, user("Meena K", "ward173@civicdesk.in", "9000000173", Role.WARD_OFFICER, 173L, null, hash));
        officers.put(100, user("Vignesh M", "ward100@civicdesk.in", "9000000100", Role.WARD_OFFICER, 100L, null, hash));
        officers.put(101, user("Anitha J", "ward101@civicdesk.in", "9000000101", Role.WARD_OFFICER, 101L, null, hash));

        User ram = user("Ram", "ram@example.com", "9876543210", Role.CITIZEN, null, null, hash);
        User priya = user("Priya", "priya@example.com", "9876543211", Role.CITIZEN, null, null, hash);
        User karthikeyan = user("Karthikeyan", "karthikeyan@example.com", "9876543212", Role.CITIZEN, null, null, hash);

        LocalDateTime now = LocalDateTime.now();

        // ward, category, citizen, hours ago, final status, description, landmark,
        // resolved hours ago, closed hours ago, rating, escalation level
        seed(170, 3, ram, 100, ComplaintStatus.ASSIGNED, "Deep pothole in front of the school gate", "Opp. Adyar Govt School", null, null, null, 0, officers, now);
        seed(170, 1, priya, 30, ComplaintStatus.ASSIGNED, "Garbage not collected for two days on 2nd Main Road", "2nd Main Road", null, null, null, 0, officers, now);
        seed(170, 5, ram, 10, ComplaintStatus.IN_PROGRESS, "Three streetlights out on the stretch near the park", "Near Corporation Park", null, null, null, 0, officers, now);
        seed(170, 1, karthikeyan, 40, ComplaintStatus.REOPENED, "Garbage pile back again, earlier clean-up did not last", "Behind the market", null, null, null, 0, officers, now);
        seed(170, 4, priya, 400, ComplaintStatus.IN_PROGRESS, "Road surface completely broken after cable work", "LB Road stretch", null, null, null, 2, officers, now);
        seed(171, 6, karthikeyan, 20, ComplaintStatus.ASSIGNED, "Knee-deep water at the junction after last night's rain", "Signal junction", null, null, null, 0, officers, now);
        seed(171, 2, priya, 48, ComplaintStatus.RESOLVED, "Overflowing bin attracting stray dogs", "Bus depot entrance", 30, null, null, 0, officers, now);
        seed(171, 3, ram, 150, ComplaintStatus.CLOSED, "Pothole near the temple tank", "Temple tank road", 100, 90, 5, 0, officers, now);
        seed(172, 8, ram, 2, ComplaintStatus.ASSIGNED, "Stagnant water breeding mosquitoes in an empty plot", "Plot next to the post office", null, null, null, 0, officers, now);
        seed(172, 4, karthikeyan, 200, ComplaintStatus.IN_PROGRESS, "Road caved in near the drain cover", "Canal bank road", null, null, null, 0, officers, now);
        seed(173, 9, priya, 5, ComplaintStatus.ASSIGNED, "Sewage overflowing onto the road near the market", "Fish market", null, null, null, 0, officers, now);
        seed(173, 10, ram, 72, ComplaintStatus.CLOSED, "No water supply in the street since morning", "4th Cross Street", 60, 50, 4, 0, officers, now);
        seed(174, 7, karthikeyan, 60, ComplaintStatus.SUBMITTED, "Storm drain blocked with plastic and silt", "Beach road side", null, null, null, 0, officers, now);
        seed(100, 3, priya, 80, ComplaintStatus.ASSIGNED, "Large pothole near the bus stop, two-wheelers slipping", "Anna Nagar Roundtana", null, null, null, 0, officers, now);
        seed(100, 5, ram, 1, ComplaintStatus.ASSIGNED, "Streetlight flickering the whole night", "2nd Avenue", null, null, null, 0, officers, now);
        seed(101, 6, karthikeyan, 3, ComplaintStatus.IN_PROGRESS, "Water entering houses, drain outlet choked", "Shanthi Colony", null, null, null, 0, officers, now);
        seed(101, 1, priya, 26, ComplaintStatus.RESOLVED, "Garbage dumped on the footpath", "Tower Park gate", 4, null, null, 0, officers, now);
        seed(102, 9, ram, 15, ComplaintStatus.SUBMITTED, "Manhole overflowing near the hospital", "Near the hospital", null, null, null, 0, officers, now);

        log.info("Demo data created: 12 staff and 3 citizen accounts (password: {}), 18 complaints", DEMO_PASSWORD);
    }

    private User user(String name, String email, String phone, Role role, Long wardId, Integer zoneId, String hash) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash(hash);
        u.setRole(role);
        u.setActive(true);
        u.setCreatedAt(LocalDateTime.now().minusDays(30));
        if (wardId != null) u.setWard(wardRepository.getReferenceById(wardId));
        if (zoneId != null) u.setZone(zoneRepository.getReferenceById(zoneId));
        return userRepository.save(u);
    }

    private void seed(int wardNumber, int categoryId, User citizen, int hoursAgo, ComplaintStatus finalStatus,
                      String description, String landmark, Integer resolvedHoursAgo, Integer closedHoursAgo,
                      Integer rating, int escalationLevel, Map<Integer, User> officers, LocalDateTime now) {

        Ward ward = wardRepository.findWithZoneById((long) wardNumber).orElseThrow();
        Category category = categoryRepository.findWithDepartmentById(categoryId).orElseThrow();
        User officer = officers.get(wardNumber);

        LocalDateTime createdAt = now.minusHours(hoursAgo);
        double[] centre = ZONE_CENTRES.getOrDefault(ward.getZone().getId(), new double[]{13.05, 80.25});

        Complaint c = new Complaint();
        c.setCitizen(citizen);
        c.setCategory(category);
        c.setWard(ward);
        c.setDescription(description);
        c.setLandmark(landmark);
        c.setLatitude(round5(centre[0] + jitter()));
        c.setLongitude(round5(centre[1] + jitter()));
        c.setStatus(ComplaintStatus.SUBMITTED);
        c.setEscalationLevel(0);
        c.setSupportCount(1);
        c.setCreatedAt(createdAt);
        c.setDueAt(createdAt.plusHours(category.getSlaHours()));
        complaintRepository.save(c);

        history(c, HistoryAction.CREATED, null, ComplaintStatus.SUBMITTED, "Filed under " + category.getName(), citizen, createdAt);

        ComplaintStatus current = ComplaintStatus.SUBMITTED;
        if (officer != null && finalStatus != ComplaintStatus.SUBMITTED) {
            c.setAssignedOfficer(officer);
            history(c, HistoryAction.ASSIGNED, current, ComplaintStatus.ASSIGNED,
                    "Auto-assigned to " + officer.getName() + " (Ward Officer)", null, createdAt);
            current = ComplaintStatus.ASSIGNED;
        }

        // Escalations that already happened before "now"
        LocalDateTime cursor = c.getDueAt();
        for (int level = 1; level <= escalationLevel; level++) {
            history(c, HistoryAction.ESCALATED, current, current,
                    "Missed the deadline. Escalated to " + Mappers.escalationLabel(level) + ".", null, cursor);
            cursor = cursor.plusHours(24);
        }
        if (escalationLevel > 0) {
            c.setEscalationLevel(escalationLevel);
            c.setDueAt(cursor); // the window given at the last escalation (already passed)
        }

        boolean needsWork = finalStatus == ComplaintStatus.IN_PROGRESS || finalStatus == ComplaintStatus.RESOLVED
                || finalStatus == ComplaintStatus.CLOSED || finalStatus == ComplaintStatus.REOPENED;
        if (officer != null && needsWork) {
            history(c, HistoryAction.STATUS_CHANGED, current, ComplaintStatus.IN_PROGRESS,
                    "Work started", officer, createdAt.plusHours(1));
            current = ComplaintStatus.IN_PROGRESS;
        }

        if (officer != null && (finalStatus == ComplaintStatus.RESOLVED || finalStatus == ComplaintStatus.CLOSED
                || finalStatus == ComplaintStatus.REOPENED)) {
            LocalDateTime resolvedAt = resolvedHoursAgo != null ? now.minusHours(resolvedHoursAgo) : createdAt.plusHours(8);
            history(c, HistoryAction.STATUS_CHANGED, current, ComplaintStatus.RESOLVED,
                    "Issue fixed by the field team", officer, resolvedAt);
            c.setResolvedAt(resolvedAt);
            c.setResolutionNote("Issue fixed by the field team");
            current = ComplaintStatus.RESOLVED;

            if (finalStatus == ComplaintStatus.REOPENED) {
                LocalDateTime reopenedAt = resolvedAt.plusHours(10);
                history(c, HistoryAction.STATUS_CHANGED, current, ComplaintStatus.REOPENED,
                        "Not fixed properly, the pile is back", citizen, reopenedAt);
                c.setResolvedAt(null);
                c.setDueAt(reopenedAt.plusHours(category.getSlaHours()));
                current = ComplaintStatus.REOPENED;
            }

            if (finalStatus == ComplaintStatus.CLOSED) {
                LocalDateTime closedAt = closedHoursAgo != null ? now.minusHours(closedHoursAgo) : resolvedAt.plusHours(2);
                history(c, HistoryAction.STATUS_CHANGED, current, ComplaintStatus.CLOSED, null, citizen, closedAt);
                c.setClosedAt(closedAt);
                if (rating != null) {
                    c.setRating(rating);
                    c.setFeedback("Thank you, fixed quickly");
                    history(c, HistoryAction.FEEDBACK, ComplaintStatus.CLOSED, ComplaintStatus.CLOSED,
                            "Rated " + rating + "/5: Thank you, fixed quickly", citizen, closedAt);
                }
                current = ComplaintStatus.CLOSED;
            }
        }

        c.setStatus(current);
    }

    // ------------------------------------------------------------------
    // Admin "generate demo complaints" button
    // ------------------------------------------------------------------

    @Transactional
    public int generate(int count) {
        if (count < 1 || count > 50) {
            throw new BadRequestException("count must be between 1 and 50");
        }
        List<User> citizens = userRepository.findByRoleAndActiveTrue(Role.CITIZEN);
        if (citizens.isEmpty()) {
            throw new BadRequestException("There are no active citizen accounts to file demo complaints");
        }
        List<User> wardOfficers = userRepository.findByRoleAndActiveTrue(Role.WARD_OFFICER);
        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();

        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < count; i++) {
            Ward ward;
            User officer = null;
            if (!wardOfficers.isEmpty() && random.nextInt(10) < 8) {
                officer = wardOfficers.get(random.nextInt(wardOfficers.size()));
                ward = wardRepository.findWithZoneById(officer.getWard().getId()).orElseThrow();
            } else {
                long wardId = 1 + random.nextInt((int) wardRepository.count());
                ward = wardRepository.findWithZoneById(wardId).orElseThrow();
            }
            Category category = categories.get(random.nextInt(categories.size()));
            User citizen = citizens.get(random.nextInt(citizens.size()));
            LocalDateTime createdAt = now.minusMinutes(random.nextInt(60 * 96)); // within the last 4 days
            double[] centre = ZONE_CENTRES.getOrDefault(ward.getZone().getId(), new double[]{13.05, 80.25});

            Complaint c = new Complaint();
            c.setCitizen(citizen);
            c.setCategory(category);
            c.setWard(ward);
            c.setDescription(category.getName() + ". " + SAMPLE_DESCRIPTIONS[random.nextInt(SAMPLE_DESCRIPTIONS.length)]);
            c.setLatitude(round5(centre[0] + jitter()));
            c.setLongitude(round5(centre[1] + jitter()));
            c.setStatus(ComplaintStatus.SUBMITTED);
            c.setEscalationLevel(0);
            c.setSupportCount(1);
            c.setCreatedAt(createdAt);
            c.setDueAt(createdAt.plusHours(category.getSlaHours()));
            complaintRepository.save(c);
            history(c, HistoryAction.CREATED, null, ComplaintStatus.SUBMITTED, "Filed under " + category.getName(), citizen, createdAt);

            if (officer != null) {
                c.setAssignedOfficer(officer);
                c.setStatus(ComplaintStatus.ASSIGNED);
                history(c, HistoryAction.ASSIGNED, ComplaintStatus.SUBMITTED, ComplaintStatus.ASSIGNED,
                        "Auto-assigned to " + officer.getName() + " (Ward Officer)", null, createdAt);
            }
        }
        return count;
    }

    // ------------------------------------------------------------------

    private void history(Complaint c, HistoryAction action, ComplaintStatus from, ComplaintStatus to,
                         String note, User actor, LocalDateTime at) {
        String label = actor == null ? "System" : actor.getName() + " (" + ComplaintService.roleLabel(actor.getRole()) + ")";
        historyRepository.save(new ComplaintHistory(c, action, from, to, note, actor, label, at));
    }

    private double jitter() {
        return (random.nextDouble() - 0.5) * 0.016;
    }

    private static double round5(double v) {
        return Math.round(v * 100_000.0) / 100_000.0;
    }
}
