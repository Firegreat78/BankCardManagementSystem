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
        when(cardJpaRepository.findAll(any(Pageable.class))).thenReturn(page);

        List<Card> result = cardService.list(0, 2, adminAuth());

        verify(cardJpaRepository).findAll(PageRequest.of(0, 2));
        verify(cardJpaRepository, never()).findAll();
        assertThat(result).hasSize(1);
    }

    @Test
    void list_withPageAndSize_forUser_shouldUsePaginatedRepositoryQuery() {
        User user = new User();
        user.setId("holder-1");
        user.setUsername("alice");
        when(userJpaRepository.findByUsername("alice")).thenReturn(java.util.Optional.of(user));

        Page<Card> page = new PageImpl<>(List.of(card("1")));
        when(cardJpaRepository.findByHolderId(eq("holder-1"), any(Pageable.class))).thenReturn(page);

        List<Card> result = cardService.list(0, 2, userAuth("alice"));

        verify(cardJpaRepository).findByHolderId("holder-1", PageRequest.of(0, 2));
        verify(cardJpaRepository, never()).findByHolderId("holder-1");
        assertThat(result).hasSize(1);
    }

    @Test
    void list_withoutPagination_shouldFallBackToUnpagedQuery() {
        when(cardJpaRepository.findAll()).thenReturn(List.of(card("1"), card("2")));

        List<Card> result = cardService.list(null, null, adminAuth());

        verify(cardJpaRepository).findAll();
        verify(cardJpaRepository, never()).findAll(any(Pageable.class));
        assertThat(result).hasSize(2);
    }
}
