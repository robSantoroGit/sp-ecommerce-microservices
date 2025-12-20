package com.ecommerce.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PaymentRequestDTO {

    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "CREDIT_CARD|DEBIT_CARD|PAYPAL|BANK_TRANSFER", 
             message = "Invalid payment method")
    private String paymentMethod;

    @Size(max = 100, message = "Card details must not exceed 100 characters")
    private String cardDetails;  // Simplified - in production would be tokenized

    // Constructors
    public PaymentRequestDTO() {
    }

    public PaymentRequestDTO(String paymentMethod, String cardDetails) {
        this.paymentMethod = paymentMethod;
        this.cardDetails = cardDetails;
    }

    // Getters and Setters
    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCardDetails() {
        return cardDetails;
    }

    public void setCardDetails(String cardDetails) {
        this.cardDetails = cardDetails;
    }
}