package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CardRepository {

    private final List<Card> cards = new ArrayList<>();

    public void add(Card card) {
        cards.add(card);
    }

    public List<Card> findAll() {
        return cards;
    }

    public Optional<Card> findById(String id) {
        return cards.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst();
    }

    public boolean deleteById(String id) {
        return cards.removeIf(c -> c.getId().equals(id));
    }
}