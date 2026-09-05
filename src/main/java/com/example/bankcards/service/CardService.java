package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardJpaRepository;
import com.example.bankcards.security.CardNumberHasher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.bankcards.repository.UserJpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class CardService {

    private final CardJpaRepository cardJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final CardNumberHasher cardNumberHasher;

    public CardService(
            CardJpaRepository cardJpaRepository,
            UserJpaRepository userJpaRepository,
            CardNumberHasher cardNumberHasher
    ) {
        this.cardJpaRepository = cardJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.cardNumberHasher = cardNumberHasher;
    }

    public Card create(Card card) {
        String numberHash = cardNumberHasher.hash(card.getNumber());
        boolean exists = cardJpaRepository.existsByNumberHash(numberHash);

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Card number already exists"
            );
        }

        card.setId(UUID.randomUUID().toString());
        card.setNumberHash(numberHash);
        cardJpaRepository.save(card);
        return card;
    }

    @Transactional
    public List<Card> list(
            Integer page,
            Integer size,
            Authentication authentication
    ) {
        List<Card> cards;

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            cards = cardJpaRepository.findAll();
        } else {
            String username = authentication.getName();

            User user = userJpaRepository.findByUsername(username)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Authenticated user not found"
                    ));

            cards = cardJpaRepository.findByHolderId(user.getId());
        }

        cards.forEach(this::expireIfPastDue);

        if (page == null && size == null) {
            return cards;
        }

        if (page != null && size == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "size required when page specified"
            );
        }

        if (page == null) {
            page = 0;
        }

        int start = page * size;
        if (start >= cards.size()) {
            return Collections.emptyList();
        }

        int end = Math.min(start + size, cards.size());
        return cards.subList(start, end);
    }

    @Transactional
    public Card getById(String id) {
        Card card = cardJpaRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Card not found"
                        ));
        expireIfPastDue(card);
        return card;
    }

    private void expireIfPastDue(Card card) {
        if (card.getStatus() != CardStatus.EXPIRED
                && card.getExpirationDate().isBefore(LocalDate.now())) {
            card.setStatus(CardStatus.EXPIRED);
        }
    }

    @Transactional
    public Card update(String id, Card updated) {
        Card existing = getById(id);
        String updatedNumberHash = cardNumberHasher.hash(updated.getNumber());
        if (!existing.getNumber().equals(updated.getNumber())) {
            boolean exists = cardJpaRepository.existsByNumberHashAndIdNot(
                    updatedNumberHash,
                    id
            );
            if (exists) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Card number already exists"
                );
            }
        }

        existing.setNumber(updated.getNumber());
        existing.setNumberHash(updatedNumberHash);
        existing.setHolderId(updated.getHolderId());
        existing.setBalance(updated.getBalance());

        return existing;
    }

    public void delete(String id) {
        Card card = getById(id);
        cardJpaRepository.delete(card);
    }

    @Transactional
    public void transfer(
            String fromId,
            String toId,
            BigDecimal amount,
            Authentication authentication
    ) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Amount must be positive"
            );
        }

        Card from = getById(fromId);
        Card to = getById(toId);

        String username = authentication.getName();

        User user = userJpaRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"
                ));

        if (!from.getHolderId().equals(user.getId())
                || !to.getHolderId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cards must belong to the authenticated user"
            );
        }

        if (from.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient balance"
            );
        }

        if (from.getStatus() != CardStatus.ACTIVE
                || to.getStatus() != CardStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Both cards must be active to transfer"
            );
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
    }

    @Transactional
    public Card block(String id) {
        return transition(id, CardStatus.BLOCKED);
    }

    @Transactional
    public Card activate(String id) {
        return transition(id, CardStatus.ACTIVE);
    }

    @Transactional
    public Card requestBlock(String id) {
        return transition(id, CardStatus.BLOCK_REQUESTED);
    }

    @Transactional
    public Card requestUnblock(String id) {
        return transition(id, CardStatus.UNBLOCK_REQUESTED);
    }

    private Card transition(String id, CardStatus to) {
        Card card = getById(id);
        if (!CardStateMachine.isTransitionAllowed(card.getStatus(), to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot transition card from " + card.getStatus() + " to " + to
            );
        }
        card.setStatus(to);
        return card;
    }
}