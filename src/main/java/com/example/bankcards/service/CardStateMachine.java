package com.example.bankcards.service;

import com.example.bankcards.entity.CardStatus;

import java.util.Map;
import java.util.Set;

public final class CardStateMachine {

    private static final Map<CardStatus, Set<CardStatus>>
            ALLOWED_TRANSITIONS = Map.of(
            CardStatus.ACTIVE,
            Set.of(
                    CardStatus.BLOCK_REQUESTED,
                    CardStatus.BLOCKED
            ),

            CardStatus.BLOCK_REQUESTED,
            Set.of(
                    CardStatus.ACTIVE,
                    CardStatus.BLOCKED
            ),

            CardStatus.BLOCKED,
            Set.of(CardStatus.UNBLOCK_REQUESTED),

            CardStatus.UNBLOCK_REQUESTED,
            Set.of(
                    CardStatus.ACTIVE,
                    CardStatus.BLOCKED
            ),

            CardStatus.EXPIRED,
            Set.of()
    );

    public static boolean isTransitionAllowed(
            CardStatus from,
            CardStatus to
    ) {
        return ALLOWED_TRANSITIONS
                .getOrDefault(from, Set.of())
                .contains(to);
    }

    private CardStateMachine() {
    }
}