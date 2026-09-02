package com.example.bankcards.controller;

import com.example.bankcards.entity.Card;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/cards")
@SuppressWarnings("unused")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    public Card create(@RequestBody @Valid Card card) {
        return cardService.create(card);
    }

    @GetMapping
    public List<Card> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        return cardService.list(page, size, authentication);
    }

    @GetMapping("/{id}")
    public Card getById(@PathVariable String id) {
        return cardService.getById(id);
    }

    @PutMapping("/{id}")
    public Card update(
            @PathVariable String id,
            @RequestBody @Valid Card updated) {
        return cardService.update(id, updated);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        cardService.delete(id);
    }

    @PostMapping("/transfer")
    public void transfer(
            @RequestParam String fromId,
            @RequestParam String toId,
            @RequestParam BigDecimal amount,
            Authentication authentication) {
        cardService.transfer(fromId, toId, amount, authentication);
    }

    @PatchMapping("/{id}/block")
    public Card block(@PathVariable String id) {
        return cardService.block(id);
    }

    @PatchMapping("/{id}/activate")
    public Card activate(@PathVariable String id) {
        return cardService.activate(id);
    }

    @PatchMapping("/{id}/block-request")
    public Card requestBlock(@PathVariable String id) {
        return cardService.requestBlock(id);
    }
}