package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CardJpaRepository extends JpaRepository<Card, String> {

    boolean existsByNumberHash(String numberHash);

    boolean existsByNumberHashAndIdNot(String numberHash, String id);

    /**
     * Row-level write lock, used by transfers so that a concurrent transfer on
     * the same card cannot read a balance that another transaction is about to
     * change. Callers must lock cards in a consistent order to avoid deadlock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Card c WHERE c.id = :id")
    Optional<Card> findByIdForUpdate(@Param("id") String id);

    /**
     * Single entry point for listing cards: every filter is optional, and a
     * null holderId means "all holders" (admin). Filtering and paging both stay
     * in the database rather than being applied to an in-memory list.
     */
    @Query("""
            SELECT c FROM Card c
            WHERE (:holderId IS NULL OR c.holderId = :holderId)
              AND (:status IS NULL OR c.status = :status)
              AND (:last4 IS NULL OR c.last4 = :last4)
            """)
    Page<Card> search(
            @Param("holderId") String holderId,
            @Param("status") CardStatus status,
            @Param("last4") String last4,
            Pageable pageable
    );
}
