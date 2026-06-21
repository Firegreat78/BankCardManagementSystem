package com.example.bankcards.controller;

import com.example.bankcards.entity.Card;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/cards")
public class CardController {
    private final List<Card> cards = new ArrayList<>();

    @GetMapping
    public List<Card> list() { return cards; }

    @PostMapping
    public Card create(@RequestBody Card card) {
        cards.add(card);
        return card;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        cards.removeIf(c -> c.getId().equals(id));
    }

    @PutMapping("/{id}")
    public Card update(@PathVariable String id, @RequestBody Card updated) {
        cards.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .ifPresent(c -> {
                    c.setNumber(updated.getNumber());
                    c.setHolder(updated.getHolder());
                });
        return cards.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }
}