package com.jisj.orm.testdata;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode
@Data
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "EmbeddedTable")
public class EmbeddedEntity {
    @Column(unique = true, updatable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    @Column
    private String firstField;
}
