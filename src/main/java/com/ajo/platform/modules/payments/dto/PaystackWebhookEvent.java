package com.ajo.platform.modules.payments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaystackWebhookEvent {

    private String event;
    private EventData data;

    @Data
    public static class EventData {
        private String reference;
        private String status;
        private BigDecimal amount;
        private String currency;

        @JsonProperty("customer")
        private Customer customer;

        @JsonProperty("metadata")
        private Metadata metadata;
    }

    @Data
    public static class Customer {
        private String email;
    }

    @Data
    public static class Metadata {
        @JsonProperty("group_id")
        private Long groupId;

        @JsonProperty("cycle_number")
        private Integer cycleNumber;

        @JsonProperty("user_id")
        private Long userId;
    }
}