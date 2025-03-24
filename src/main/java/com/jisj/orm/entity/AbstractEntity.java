package com.jisj.orm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.SuperBuilder;


@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode
@Deprecated
abstract public class AbstractEntity {
    @Column(unique = true, updatable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
}
