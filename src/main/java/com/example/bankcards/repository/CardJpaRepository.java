package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardJpaRepository extends JpaRepository<Card, String> {

    boolean existsByNumber(String number);

    boolean existsByNumberAndIdNot(String number, String id);
}