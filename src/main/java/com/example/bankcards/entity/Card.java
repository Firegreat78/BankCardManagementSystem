package com.example.bankcards.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
public class Card {
    @Id String id;
    String number;
    String holder;

    public Card() { this.id = UUID.randomUUID().toString(); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public String getHolder() { return holder; }
    public void setHolder(String holder) { this.holder = holder; }
}