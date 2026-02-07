package com.hms.controller;

import com.hms.dto.response.ApiResponse;
import com.hms.entity.Bill;
import com.hms.entity.BillItem;
import com.hms.enums.PaymentMethod;
import com.hms.service.BillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    @PostMapping("/generate/{reservationId}")
    public ResponseEntity<ApiResponse<Bill>> generateBill(@PathVariable String reservationId) {
        Bill bill = billService.generateBill(reservationId);
        return ResponseEntity.ok(ApiResponse.success("Bill generated successfully", bill));
    }

    @GetMapping("/{billId}")
    public ResponseEntity<ApiResponse<Bill>> getBill(@PathVariable String billId) {
        Bill bill = billService.getBillById(billId);
        return ResponseEntity.ok(ApiResponse.success("Bill details retrieved", bill));
    }

    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<ApiResponse<Bill>> getBillByReservation(@PathVariable String reservationId) {
        Bill bill = billService.getBillByReservationId(reservationId);
        return ResponseEntity.ok(ApiResponse.success("Bill details retrieved", bill));
    }

    @PostMapping("/{billId}/pay")
    public ResponseEntity<ApiResponse<Bill>> processPayment(
            @PathVariable String billId,
            @RequestBody Map<String, Object> paymentRequest) {

        BigDecimal amount = new BigDecimal(paymentRequest.get("amount").toString());
        PaymentMethod paymentMethod = PaymentMethod.valueOf(paymentRequest.get("paymentMethod").toString());

        Bill updatedBill = billService.updatePayment(billId, amount, paymentMethod);
        return ResponseEntity.ok(ApiResponse.success("Payment processed successfully", updatedBill));
    }

    @PostMapping("/{billId}/items")
    public ResponseEntity<ApiResponse<Bill>> addItemToBill(
            @PathVariable String billId,
            @RequestBody BillItem item) {

        Bill updatedBill = billService.addItemToBill(billId, item);
        return ResponseEntity.ok(ApiResponse.success("Item added to bill successfully", updatedBill));
    }
}
