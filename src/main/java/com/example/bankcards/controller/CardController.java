package com.example.bankcards.controller;

import com.example.bankcards.dto.CardCreateRequest;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CardUpdateRequest;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/cards")
@Tag(name = "Cards", description = "Card administration, cardholder self-service and transfers")
@Validated
@SuppressWarnings("unused")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @Operation(summary = "Create a card", description = "Administrators only. The number is encrypted at rest and returned masked.")
    @PostMapping
    public CardResponse create(@RequestBody @Valid CardCreateRequest request) {
        Card card = new Card();
        card.setNumber(request.getNumber());
        card.setHolderId(request.getHolderId());
        card.setBalance(request.getBalance());
        card.setExpirationDate(request.getExpirationDate());
        return CardResponse.from(cardService.create(card));
    }

    @Operation(summary = "List cards", description = "Administrators see every card, other users only their own. Supports paging and filtering by status and last four digits.")
    @GetMapping
    public List<CardResponse> list(
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false)
            @Pattern(regexp = "^[0-9]{4}$", message = "last4 must be exactly 4 digits")
            String last4,
            Authentication authentication) {
        return cardService.list(page, size, status, last4, authentication)
                .stream()
                .map(CardResponse::from)
                .toList();
    }

    @Operation(summary = "Get one card", description = "Allowed for the card holder and for administrators.")
    @GetMapping("/{id}")
    public CardResponse getById(@PathVariable String id, Authentication authentication) {
        return CardResponse.from(cardService.getById(id, authentication));
    }

    @Operation(summary = "Update a card", description = "Administrators only. Status is not client-controlled and changes only through the status endpoints.")
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

    @Operation(summary = "Delete a card", description = "Administrators only.")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        cardService.delete(id);
    }

    @Operation(summary = "Transfer between own cards", description = "Both cards must belong to the caller and be active. Amounts allow at most two decimals.")
    @PostMapping("/transfer")
    public void transfer(
            @RequestBody @Valid TransferRequest request,
            Authentication authentication) {
        cardService.transfer(
                request.getFromId(),
                request.getToId(),
                request.getAmount(),
                authentication
        );
    }

    @Operation(summary = "Block a card", description = "Administrators only.")
    @PatchMapping("/{id}/block")
    public CardResponse block(@PathVariable String id, Authentication authentication) {
        return CardResponse.from(cardService.block(id, authentication));
    }

    @Operation(summary = "Activate a card", description = "Administrators only. Expired cards cannot be activated.")
    @PatchMapping("/{id}/activate")
    public CardResponse activate(@PathVariable String id, Authentication authentication) {
        return CardResponse.from(cardService.activate(id, authentication));
    }

    @Operation(summary = "Request a block", description = "Card holder asks an administrator to block the card.")
    @PatchMapping("/{id}/block-request")
    public CardResponse requestBlock(@PathVariable String id, Authentication authentication) {
        return CardResponse.from(cardService.requestBlock(id, authentication));
    }

    @Operation(summary = "Request an unblock", description = "Card holder asks an administrator to unblock the card.")
    @PatchMapping("/{id}/unblock-request")
    public CardResponse requestUnblock(@PathVariable String id, Authentication authentication) {
        return CardResponse.from(cardService.requestUnblock(id, authentication));
    }
}
