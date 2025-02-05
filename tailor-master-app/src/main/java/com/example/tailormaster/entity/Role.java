package com.example.tailormaster.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

    @Column(length = 20)
    private String name;
}