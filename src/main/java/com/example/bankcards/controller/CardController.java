package com.example.bankcards.controller;

import com.example.bankcards.dto.CardCreateRequest;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CardUpdateRequest;
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
    public CardResponse create(@RequestBody @Valid CardCreateRequest request) {
        Card card = new Card();
        card.setNumber(request.getNumber());
        card.setHolderId(request.getHolderId());
        card.setBalance(request.getBalance());
        card.setExpirationDate(request.getExpirationDate());
        return CardResponse.from(cardService.create(card));
    }

    @GetMapping
    public List<CardResponse> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        return cardService.list(page, size, authentication)
                .stream()
                .map(CardResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public CardResponse getById(@PathVariable String id, Authentication authentication) {
        return CardResponse.from(cardService.getById(id, authentication));
    }

    @PutMapping("/{id}")
    public CardResponse update(
            @PathVariable String id,
            @RequestBody @Valid CardUpdateRequest request,
            Authentication authentication) {
        Card updated = new Card();
        updated.setNumber(request.getNumber());
        updated.setHolderId(request.getHolderId());
        updated.setBalance(request.getBalance());
        updated.setExpirationDate(request.getExpirationDate());
        return CardResponse.from(cardService.update(id, updated, authentication));
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
    public CardResponse block(@PathVariable String id, Authentication authentication) {
        return CardResponse.from(cardService.block(id, authentication));
    }

    @PatchMapping("/{id}/activate")
    public CardResponse activate(@PathVariable String id, Authentication authentication) {
        return CardResponse.from(cardService.activate(id, authentication));
    }

    @PatchMapping("/{id}/block-request")
    public CardResponse requestBlock(@PathVariable String id, Authentication authentication) {
        return CardResponse.from(cardService.requestBlock(id, authentication));
    }

    @PatchMapping("/{id}/unblock-request")
    public CardResponse requestUnblock(@PathVariable String id, Authentication authentication) {
        return CardResponse.from(cardService.requestUnblock(id, authentication));
    }
}
