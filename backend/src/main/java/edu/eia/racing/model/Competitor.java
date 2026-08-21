package edu.eia.racing.model;

import edu.eia.racing.model.enums.CompetitorStatus;
import edu.eia.racing.model.enums.CompetitorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "competitors", uniqueConstraints = @UniqueConstraint(columnNames = "nickname"), indexes = {
        @Index(name = "idx_competitors_type", columnList = "type"),
        @Index(name = "idx_competitors_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Competitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompetitorType type;

    @Past
    private LocalDate birthDate;

    @Positive
    @Column(nullable = false)
    private Double weight;

    @Positive
    @Column(nullable = false)
    private Double height;

    private String originCountry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompetitorStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @Builder.Default
    @ColumnDefault("0")
    @Column(nullable = false)
    private int wins = 0;

    @Builder.Default
    @ColumnDefault("0")
    @Column(nullable = false)
    private int losses = 0;

    @Builder.Default
    @ColumnDefault("0")
    @Column(nullable = false)
    private int racesCompleted = 0;

    @PrePersist
    void onCreate() {
        registeredAt = LocalDateTime.now();
    }
}
