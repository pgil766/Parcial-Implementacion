package edu.eia.racing.model;

import edu.eia.racing.model.enums.RegistrationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "race_registrations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"race_id", "competitor_id"}),
        @UniqueConstraint(columnNames = {"race_id", "team_id"}),
        @UniqueConstraint(columnNames = {"race_id", "starting_position"})
}, indexes = {
        @Index(name = "idx_race_registrations_competitor_id", columnList = "competitor_id"),
        @Index(name = "idx_race_registrations_team_id", columnList = "team_id"),
        @Index(name = "idx_race_registrations_registered_by", columnList = "registered_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RaceRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    private Race race;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competitor_id")
    private Competitor competitor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus status;

    @Column(name = "starting_position")
    private Integer startingPosition;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by", nullable = false)
    private User registeredBy;

    @PrePersist
    void onCreate() {
        registeredAt = LocalDateTime.now();
    }
}
