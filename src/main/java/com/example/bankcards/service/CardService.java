package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.repository.CardRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class CardService {

    private final CardRepository cardRepository;

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public Card create(Card card) {
        boolean exists = cardRepository.findAll().stream()
                .anyMatch(c -> c.getNumber().equals(card.getNumber()));

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Card number already exists"
            );
        }

        card.setId(UUID.randomUUID().toString());
        cardRepository.add(card);
        return card;
    }

    public List<Card> list(Integer page, Integer size) {
        List<Card> cards = cardRepository.findAll();

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

    public Card getById(String id) {
        return cardRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Card not found"
                        ));
    }

    public Card update(String id, Card updated) {
        Card existing = getById(id);

        if (!existing.getNumber().equals(updated.getNumber())) {
            boolean exists = cardRepository.findAll().stream()
                    .anyMatch(c ->
                            c.getNumber().equals(updated.getNumber())
                                    && !c.getId().equals(id));

            if (exists) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Card number already exists"
                );
            }
        }

        existing.setNumber(updated.getNumber());
        existing.setHolderId(updated.getHolderId());
        existing.setBalance(updated.getBalance());

        return existing;
    }

    public void delete(String id) {
        if (!cardRepository.deleteById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Card not found"
            );
        }
    }

    public void transfer(String fromId, String toId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Amount must be positive"
            );
        }

        Card from = getById(fromId);
        Card to = getById(toId);

        if (from.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient balance"
            );
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
    }

    public Card block(String id) {
        Card card = getById(id);
        card.setStatus("BLOCKED");
        return card;
    }

    public Card activate(String id) {
        Card card = getById(id);
        card.setStatus("ACTIVE");
        return card;
    }

    public Card requestBlock(String id) {
        Card card = getById(id);
        card.setStatus("BLOCK_REQUESTED");
        return card;
    }
}