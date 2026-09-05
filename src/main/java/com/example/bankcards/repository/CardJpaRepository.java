package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardJpaRepository extends JpaRepository<Card, String> {

    boolean existsByNumberHash(String numberHash);

    boolean existsByNumberHashAndIdNot(String numberHash, String id);

    List<Card> findByHolderId(String holderId);
}