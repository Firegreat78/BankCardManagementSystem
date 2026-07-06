package com.example.bankcards.controller;

import com.example.bankcards.entity.Card;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/cards")
@SuppressWarnings("unused")
public class CardController {
    private final List<Card> cards = new ArrayList<>();

    @PostMapping
    public Card create(@RequestBody @Valid Card card) {
        // Check duplicate number
        boolean exists = cards.stream()
                .anyMatch(c -> c.getNumber().equals(card.getNumber()));
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Card number already exists");
        }
        card.setId(UUID.randomUUID().toString());
        cards.add(card);
        return card;
    }

    @GetMapping
    public List<Card> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        // If neither page nor size, return all
        if (page == null && size == null) {
            return cards;
        }

        // If only page provided, bad request
        if (page != null && size == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size required when page specified");
        }

        // If only size provided, default page=0
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

    @GetMapping("/{id}")
    public Card getById(@PathVariable String id) {
        return cards.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
    }

    @PutMapping("/{id}")
    public Card update(@PathVariable String id, @RequestBody @Valid Card updated) {
        Card existing = cards.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));

        // Check duplicate number if changed
        if (!existing.getNumber().equals(updated.getNumber())) {
            boolean exists = cards.stream()
                    .anyMatch(c -> c.getNumber().equals(updated.getNumber()) && !c.getId().equals(id));
            if (exists) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Card number already exists");
            }
        }

        existing.setNumber(updated.getNumber());
        existing.setHolderId(updated.getHolderId());
        existing.setBalance(updated.getBalance());
        return existing;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        boolean removed = cards.removeIf(c -> c.getId().equals(id));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found");
        }
    }

    @PostMapping("/transfer")
    public void transfer(
            @RequestParam String fromId,
            @RequestParam String toId,
            @RequestParam BigDecimal amount) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        }

        Card from = cards.stream()
                .filter(c -> c.getId().equals(fromId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "From card not found"));
        Card to = cards.stream()
                .filter(c -> c.getId().equals(toId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "To card not found"));

        if (from.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
    }

    @PatchMapping("/{id}/block")
    public Card block(@PathVariable String id) {
        Card card = cards.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));

        card.setStatus("BLOCKED");
        return card;
    }

    @PatchMapping("/{id}/activate")
    public Card activate(@PathVariable String id) {
        Card card = cards.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));

        card.setStatus("ACTIVE");
        return card;
    }

    @PatchMapping("/{id}/block-request")
    public Card requestBlock(@PathVariable String id) {
        Card card = cards.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));

        card.setStatus("BLOCK_REQUESTED");
        return card;
    }
}