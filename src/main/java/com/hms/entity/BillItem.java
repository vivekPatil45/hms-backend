package com.hms.entity;

import com.hms.enums.BillItemCategory;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class BillItem {
    private String itemId;
    private String description;

    @Enumerated(EnumType.STRING)
    private BillItemCategory category;

    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
