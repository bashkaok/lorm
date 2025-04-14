package com.jisj.orm.testdata;

import com.jisj.orm.DAOException;
import com.jisj.orm.repository.CRUDRepository;
import com.jisj.orm.repository.OrmRepoContainer;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;


@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Entity
@Table(name = "Parent")
public class NestedEntity {
    @Column(unique = true, updatable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    @Column
    private String name;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "Parent_Child",
            joinColumns = {@JoinColumn(name = "PARENT_ID", referencedColumnName = "id")},
            inverseJoinColumns = @JoinColumn(name = "CHILD_ID", referencedColumnName = "id"))
    private List<NestedEntity> nestedChild;

    @SuppressWarnings("unchecked")
    public static void checkIntegrity(OrmRepoContainer global) {
        CRUDRepository<NestedEntity, Integer> crud = (CRUDRepository<NestedEntity, Integer>) global.getCrudRepository(NestedEntity.class);
        NestedEntity e = NestedEntity.builder()
                .name("System record")
                .build();

        try {
            crud.add(e);
        } catch (DAOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
