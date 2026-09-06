package com.example.bankcards.service;

import com.example.bankcards.entity.CardStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CardStateMachine is a pure function over the status enum, so these run
 * without a Spring context.
 */
class CardStateMachineTest {

    @Test
    void validTransitions_shouldBeAllowed() {
        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.ACTIVE,
                CardStatus.BLOCK_REQUESTED
        )).isTrue();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.ACTIVE,
                CardStatus.BLOCKED
        )).isTrue();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.BLOCK_REQUESTED,
                CardStatus.ACTIVE
        )).isTrue();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.BLOCK_REQUESTED,
                CardStatus.BLOCKED
        )).isTrue();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.BLOCKED,
                CardStatus.UNBLOCK_REQUESTED
        )).isTrue();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.UNBLOCK_REQUESTED,
                CardStatus.ACTIVE
        )).isTrue();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.UNBLOCK_REQUESTED,
                CardStatus.BLOCKED
        )).isTrue();
    }

    @Test
    void invalidTransitions_shouldNotBeAllowed() {
        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.ACTIVE,
                CardStatus.ACTIVE
        )).isFalse();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.ACTIVE,
                CardStatus.UNBLOCK_REQUESTED
        )).isFalse();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.BLOCK_REQUESTED,
                CardStatus.BLOCK_REQUESTED
        )).isFalse();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.BLOCK_REQUESTED,
                CardStatus.UNBLOCK_REQUESTED
        )).isFalse();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.BLOCKED,
                CardStatus.BLOCK_REQUESTED
        )).isFalse();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.BLOCKED,
                CardStatus.BLOCKED
        )).isFalse();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.BLOCKED,
                CardStatus.ACTIVE
        )).isFalse();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.UNBLOCK_REQUESTED,
                CardStatus.UNBLOCK_REQUESTED
        )).isFalse();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.UNBLOCK_REQUESTED,
                CardStatus.BLOCK_REQUESTED
        )).isFalse();

        assertThat(CardStateMachine.isTransitionAllowed(
                CardStatus.EXPIRED,
                CardStatus.EXPIRED
        )).isFalse();
    }
}
