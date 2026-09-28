package com.example.tailormaster.dto;

import com.example.tailormaster.entity.Customer;
import com.example.tailormaster.util.ThymeleafUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@ToString
public class CustomerDTO {
    private String bookNumber;
    private String fullName;
    private String phoneNumber;
    private String createdAt;
    private String encryptedId;

    public CustomerDTO(Customer customer, ThymeleafUtil thymeleafUtil) {
        this.bookNumber = customer.getBookNumber();
        this.fullName = customer.getFullName();
        this.phoneNumber = customer.getPhoneNumber();
        this.createdAt = customer.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.encryptedId = thymeleafUtil.encryptId(customer.getId());
    }
}
