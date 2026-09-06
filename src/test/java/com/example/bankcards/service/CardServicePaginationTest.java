package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardJpaRepository;
import com.example.bankcards.repository.UserJpaRepository;
import com.example.bankcards.security.CardNumberHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * Verifies list() requests paginated data from the repository via Pageable
 * instead of loading all cards and slicing the result in memory.
 */
class CardServicePaginationTest {

    private CardJpaRepository cardJpaRepository;
    private UserJpaRepository userJpaRepository;
    private CardService cardService;

    @BeforeEach
    void setUp() {
        cardJpaRepository = mock(CardJpaRepository.class);
        userJpaRepository = mock(UserJpaRepository.class);
        CardNumberHasher cardNumberHasher = mock(CardNumberHasher.class);
        cardService = new CardService(cardJpaRepository, userJpaRepository, cardNumberHasher);
    }

    private Card card(String id) {
        Card card = new Card();
        card.setId(id);
        card.setNumber("1234567890123456");
        card.setHolderId("holder-1");
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(BigDecimal.TEN);
        card.setExpirationDate(LocalDate.now().plusYears(1));
        return card;
    }

    private Authentication adminAuth() {
        Authentication auth = mock(Authentication.class);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        doReturn(authorities).when(auth).getAuthorities();
        return auth;
    }

    private Authentication userAuth(String username) {
        Authentication auth = mock(Authentication.class);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        doReturn(authorities).when(auth).getAuthorities();
        when(auth.getName()).thenReturn(username);
        return auth;
    }

    @Test
    void list_withPageAndSize_forAdmin_shouldUsePaginatedRepositoryQuery() {
        Page<Card> page = new PageImpl<>(List.of(card("1")));
        when(cardJpaRepository.search(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        List<Card> result = cardService.list(0, 2, null, null, adminAuth());

        // Null holderId = every holder, and the page request reaches the database.
        verify(cardJpaRepository).search(null, null, null, PageRequest.of(0, 2));
        assertThat(result).hasSize(1);
    }

    @Test
    void list_withPageAndSize_forUser_shouldRestrictToOwnCardsInQuery() {
        User user = new User();
        user.setId("holder-1");
        user.setUsername("alice");
        when(userJpaRepository.findByUsername("alice")).thenReturn(java.util.Optional.of(user));

        Page<Card> page = new PageImpl<>(List.of(card("1")));
        when(cardJpaRepository.search(eq("holder-1"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        List<Card> result = cardService.list(0, 2, null, null, userAuth("alice"));

        verify(cardJpaRepository).search("holder-1", null, null, PageRequest.of(0, 2));
        assertThat(result).hasSize(1);
    }

    @Test
    void list_withoutPagination_shouldUseUnpagedQuery() {
        when(cardJpaRepository.search(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(card("1"), card("2"))));

        List<Card> result = cardService.list(null, null, null, null, adminAuth());

        verify(cardJpaRepository).search(null, null, null, Pageable.unpaged());
        assertThat(result).hasSize(2);
    }

    @Test
    void list_withFilters_shouldPushStatusAndLast4IntoQuery() {
        when(cardJpaRepository.search(isNull(), eq(CardStatus.BLOCKED), eq("0001"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(card("1"))));

        List<Card> result = cardService.list(0, 10, CardStatus.BLOCKED, "0001", adminAuth());

        // Filtering must happen in the database, not by filtering a fetched list.
        verify(cardJpaRepository).search(null, CardStatus.BLOCKED, "0001", PageRequest.of(0, 10));
        assertThat(result).hasSize(1);
    }
}
