package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardJpaRepository extends JpaRepository<Card, String> {

    boolean existsByNumber(String number);

    boolean existsByNumberAndIdNot(String number, String id);

    List<Card> findByHolderId(String holderId);
}