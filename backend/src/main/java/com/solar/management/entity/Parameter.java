package com.solar.management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "parameters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Parameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String parameterKey;

    @Column(nullable = false)
    private String parameterValue;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParameterType type;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum ParameterType {
        STRING,
        NUMBER,
        BOOLEAN,
        DECIMAL
    }

    public BigDecimal getValueAsDecimal() {
        return new BigDecimal(parameterValue);
    }

    public Integer getValueAsInteger() {
        return Integer.parseInt(parameterValue);
    }

    public Boolean getValueAsBoolean() {
        return Boolean.parseBoolean(parameterValue);
    }
}
