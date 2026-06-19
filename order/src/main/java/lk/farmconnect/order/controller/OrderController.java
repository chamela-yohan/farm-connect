package lk.farmconnect.order.controller;

import jakarta.validation.Valid;
import lk.farmconnect.common.response.ApiResponse;
import lk.farmconnect.order.dto.*;
import lk.farmconnect.order.entity.Order;
import lk.farmconnect.order.service.OrderService;
import lk.farmconnect.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import lk.farmconnect.order.service.InvoiceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final InvoiceService invoiceService;

    // BUYER ENDPOINT: Checkout (Returns List of Split Orders)
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> checkout(
            @Valid @RequestBody OrderCreateRequest request,
            @AuthenticationPrincipal User buyer) {
        return ResponseEntity.ok(ApiResponse.success(orderService.checkout(buyer, request)));
    }

    // BUYER ENDPOINT: Buy Now (Direct Checkout)
    @PostMapping("/buy-now")
    public ResponseEntity<ApiResponse<OrderResponse>> buyNow(
            @Valid @RequestBody BuyNowRequest request,
            @AuthenticationPrincipal User buyer) {
        return ResponseEntity.ok(ApiResponse.success(orderService.buyNow(buyer, request)));
    }

    // SHARED ENDPOINT: View Order Details
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderById(id, currentUser)));
    }

    // FARMER ENDPOINT: Update Status
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody OrderStatusUpdateRequest request,
            @AuthenticationPrincipal User farmer) {
        return ResponseEntity.ok(ApiResponse.success(orderService.updateOrderStatus(id, request, farmer)));
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        // Reuse the security check from getOrderById
        OrderResponse orderData = orderService.getOrderById(id, currentUser);


        Order orderEntity = orderService.getOrderEntityById(id, currentUser);

        byte[] pdfBytes = invoiceService.generateInvoice(orderEntity);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "Invoice_" + orderEntity.getOrderNumber() + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}