package edu.eia.racing.model;

import edu.eia.racing.model.enums.ResultStatus;
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
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "race_results", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"race_id", "final_position"}),
        @UniqueConstraint(columnNames = {"registration_id"})
}, indexes = @Index(name = "idx_race_results_recorded_by", columnList = "recorded_by"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RaceResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    private Race race;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", nullable = false)
    private RaceRegistration registration;

    private Integer startingPosition;

    @Column(name = "final_position")
    private Integer finalPosition;

    @Positive
    private Double finishTimeSeconds;

    @Builder.Default
    @ColumnDefault("0")
    @Column(nullable = false)
    private Double penaltyTimeSeconds = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultStatus status;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by", nullable = false)
    private User recordedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    void onCreate() {
        recordedAt = LocalDateTime.now();
    }
}
