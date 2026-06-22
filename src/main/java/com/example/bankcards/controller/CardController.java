package com.example.bankcards.controller;

import com.example.bankcards.entity.Card;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/cards")
public class CardController {
    private final List<Card> cards = new ArrayList<>();

    @GetMapping
    public List<Card> list() { return cards; }

    @PostMapping
    public Card create(@RequestBody @Valid Card card) {
        cards.add(card);
        return card;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        cards.removeIf(c -> c.getId().equals(id));
    }

    @PutMapping("/{id}")
    public Card update(@PathVariable String id, @RequestBody @Valid Card updated) {
        cards.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .ifPresent(c -> {
                    c.setNumber(updated.getNumber());
                    c.setHolder(updated.getHolder());
                    c.setBalance(updated.getBalance());
                });
        return cards.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }

    @PostMapping("/transfer")
    public void transfer(
            @RequestParam String fromId,
            @RequestParam String toId,
            @RequestParam @DecimalMin(value = "0.0", message = "Amount must be positive") BigDecimal amount) {

        Card from = cards.stream().filter(c -> c.getId().equals(fromId)).findFirst().orElseThrow();
        Card to = cards.stream().filter(c -> c.getId().equals(toId)).findFirst().orElseThrow();

        if (from.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
    }
}