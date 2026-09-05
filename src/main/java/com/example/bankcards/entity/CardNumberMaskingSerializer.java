package com.example.bankcards.entity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class CardNumberMaskingSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String number, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (number == null || number.length() < 4) {
            gen.writeString(number);
            return;
        }
        String last4 = number.substring(number.length() - 4);
        gen.writeString("**** **** **** " + last4);
    }
}
