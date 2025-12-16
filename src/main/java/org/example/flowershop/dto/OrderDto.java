package org.example.flowershop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.flowershop.model.enums.Status;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderDto {
    private long id;
    private Long userId;
    private double price;
    private Status status;
    private LocalDateTime orderDate;
    private ProductDto product;
    private String address;
    private int quantity;
}