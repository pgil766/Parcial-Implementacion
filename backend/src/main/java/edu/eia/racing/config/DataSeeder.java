package edu.eia.racing.config;

import edu.eia.racing.model.Competitor;
import edu.eia.racing.model.Race;
import edu.eia.racing.model.RaceRegistration;
import edu.eia.racing.model.RaceResult;
import edu.eia.racing.model.Role;
import edu.eia.racing.model.Team;
import edu.eia.racing.model.TeamMember;
import edu.eia.racing.model.User;
import edu.eia.racing.model.enums.CompetitorStatus;
import edu.eia.racing.model.enums.CompetitorType;
import edu.eia.racing.model.enums.RaceStatus;
import edu.eia.racing.model.enums.RaceType;
import edu.eia.racing.model.enums.RegistrationStatus;
import edu.eia.racing.model.enums.ResultStatus;
import edu.eia.racing.model.enums.RoleName;
import edu.eia.racing.model.enums.TeamStatus;
import edu.eia.racing.repository.CompetitorRepository;
import edu.eia.racing.repository.RaceRegistrationRepository;
import edu.eia.racing.repository.RaceRepository;
import edu.eia.racing.repository.RaceResultRepository;
import edu.eia.racing.repository.RoleRepository;
import edu.eia.racing.repository.TeamMemberRepository;
import edu.eia.racing.repository.TeamRepository;
import edu.eia.racing.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the minimum data required by the project spec (section 7) so the
 * application has a usable dataset right after {@code docker compose up -d}.
 * Runs once: it skips everything if roles already exist.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CompetitorRepository competitorRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RaceRepository raceRepository;
    private final RaceRegistrationRepository raceRegistrationRepository;
    private final RaceResultRepository raceResultRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (roleRepository.count() > 0) {
            return;
        }

        Role admin = roleRepository.save(Role.builder().name(RoleName.ADMIN).build());
        Role organizerRole = roleRepository.save(Role.builder().name(RoleName.RACE_ORGANIZER).build());
        Role viewerRole = roleRepository.save(Role.builder().name(RoleName.VIEWER).build());

        userRepository.save(User.builder()
                .username("admin")
                .email("admin@eia.edu.co")
                .passwordHash(passwordEncoder.encode("admin123"))
                .role(admin)
                .build());
        User organizerUser = userRepository.save(User.builder()
                .username("organizer")
                .email("organizer@eia.edu.co")
                .passwordHash(passwordEncoder.encode("organizer123"))
                .role(organizerRole)
                .build());
        userRepository.save(User.builder()
                .username("viewer")
                .email("viewer@eia.edu.co")
                .passwordHash(passwordEncoder.encode("viewer123"))
                .role(viewerRole)
                .build());

        List<Competitor> dwarfs = List.of(
                newCompetitor("Null Pointer", "nullptr", CompetitorType.DWARF, 24, 42.0, 0.95, "Colombia"),
                newCompetitor("Stack Overflow", "stackoverflow", CompetitorType.DWARF, 27, 45.5, 0.98, "Colombia"),
                newCompetitor("Little Lambda", "lambda", CompetitorType.DWARF, 23, 40.0, 0.92, "Argentina"),
                newCompetitor("Captain Cache", "cache", CompetitorType.DWARF, 29, 47.0, 1.01, "Mexico"),
                newCompetitor("Tiny Docker", "docker", CompetitorType.DWARF, 25, 41.5, 0.94, "Chile"));
        dwarfs.forEach(competitorRepository::save);

        List<Competitor> camels = List.of(
                newCompetitor("Byte", "byte", CompetitorType.CAMEL, 6, 480.0, 2.1, "Colombia"),
                newCompetitor("Segfault", "segfault", CompetitorType.CAMEL, 5, 465.0, 2.05, "Colombia"));
        camels.forEach(competitorRepository::save);

        List<Competitor> mediums = List.of(
                newCompetitor("Middleware", "middleware", CompetitorType.MEDIUM, 8, 180.0, 1.4, "Peru"),
                newCompetitor("Half-Byte", "halfbyte", CompetitorType.MEDIUM, 7, 175.0, 1.35, "Ecuador"));
        mediums.forEach(competitorRepository::save);

        Team fiveExceptions = teamRepository.save(Team.builder()
                .name("The Five Exceptions")
                .description("Elite dwarf racing crew")
                .status(TeamStatus.ACTIVE)
                .coachName("Ada Lovelace")
                .maxMembers(10)
                .build());
        dwarfs.forEach(dwarf -> teamMemberRepository.save(TeamMember.builder()
                .team(fiveExceptions)
                .competitor(dwarf)
                .build()));

        Team camelCase = teamRepository.save(Team.builder()
                .name("Camel Case")
                .description("Long-distance camel specialists")
                .status(TeamStatus.ACTIVE)
                .coachName("Grace Hopper")
                .maxMembers(10)
                .build());
        camels.forEach(camel -> teamMemberRepository.save(TeamMember.builder()
                .team(camelCase)
                .competitor(camel)
                .build()));

        LocalDateTime now = LocalDateTime.now();

        raceRepository.save(Race.builder()
                .name("Oasis Grand Prix")
                .description("Season opener, open to individuals and teams")
                .scheduledAt(now.plusDays(30))
                .startLocation("Oasis Gate")
                .endLocation("Dune Ridge")
                .distanceMeters(2000.0)
                .maxParticipants(20)
                .type(RaceType.MIXED)
                .status(RaceStatus.DRAFT)
                .organizer(organizerUser)
                .registrationDeadline(now.plusDays(25))
                .build());

        Race sprintDeLasDunas = raceRepository.save(Race.builder()
                .name("Sprint de las Dunas")
                .description("Team sprint across the dunes")
                .scheduledAt(now.plusDays(14))
                .startLocation("Base Camp")
                .endLocation("Dune Ridge")
                .distanceMeters(1500.0)
                .maxParticipants(10)
                .type(RaceType.TEAM)
                .status(RaceStatus.OPEN_FOR_REGISTRATION)
                .organizer(organizerUser)
                .registrationDeadline(now.plusDays(10))
                .build());
        raceRegistrationRepository.save(RaceRegistration.builder()
                .race(sprintDeLasDunas)
                .team(camelCase)
                .status(RegistrationStatus.PENDING)
                .registeredBy(organizerUser)
                .build());

        Race copaFundacional = raceRepository.save(Race.builder()
                .name("Copa Fundacional EIA")
                .description("First official race of the league")
                .scheduledAt(now.minusDays(10))
                .startLocation("EIA Track")
                .endLocation("EIA Finish Line")
                .distanceMeters(1000.0)
                .maxParticipants(10)
                .type(RaceType.INDIVIDUAL)
                .status(RaceStatus.COMPLETED)
                .organizer(organizerUser)
                .registrationDeadline(now.minusDays(12))
                .build());

        double baseFinishSeconds = 120.0;
        for (int i = 0; i < dwarfs.size(); i++) {
            Competitor dwarf = dwarfs.get(i);
            int position = i + 1;

            RaceRegistration registration = raceRegistrationRepository.save(RaceRegistration.builder()
                    .race(copaFundacional)
                    .competitor(dwarf)
                    .status(RegistrationStatus.APPROVED)
                    .startingPosition(position)
                    .registeredBy(organizerUser)
                    .build());

            raceResultRepository.save(RaceResult.builder()
                    .race(copaFundacional)
                    .registration(registration)
                    .startingPosition(position)
                    .finalPosition(position)
                    .finishTimeSeconds(baseFinishSeconds + (position - 1) * 3.5)
                    .status(ResultStatus.FINISHED)
                    .recordedBy(organizerUser)
                    .build());

            dwarf.setRacesCompleted(dwarf.getRacesCompleted() + 1);
            if (position == 1) {
                dwarf.setWins(dwarf.getWins() + 1);
            } else {
                dwarf.setLosses(dwarf.getLosses() + 1);
            }
            competitorRepository.save(dwarf);
        }
    }

    private Competitor newCompetitor(String name, String nickname, CompetitorType type, int ageYears,
            double weight, double height, String originCountry) {
        return Competitor.builder()
                .name(name)
                .nickname(nickname)
                .type(type)
                .birthDate(LocalDate.now().minusYears(ageYears))
                .weight(weight)
                .height(height)
                .originCountry(originCountry)
                .status(CompetitorStatus.ACTIVE)
                .build();
    }
}
