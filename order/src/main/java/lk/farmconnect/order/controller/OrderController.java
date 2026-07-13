package lk.farmconnect.order.controller;

import jakarta.validation.Valid;
import lk.farmconnect.common.exception.BusinessException;
import lk.farmconnect.common.response.ApiResponse;
import lk.farmconnect.common.service.StorageService;
import lk.farmconnect.order.dto.*;
import lk.farmconnect.order.entity.Order;
import lk.farmconnect.order.service.OrderService;
import lk.farmconnect.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    // The Controller must NEVER know about the Repository or Database.
    private final OrderService orderService;
    private final StorageService storageService;

    // ==========================================
    // BUYER ENDPOINTS
    // ==========================================

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> checkout(
            @Valid @RequestBody OrderCreateRequest request,
            @AuthenticationPrincipal User buyer) {

        List<OrderResponse> createdOrders = orderService.checkout(buyer, request);

        // Use 201 CREATED for successful resource creation
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdOrders, "Orders placed successfully"));
    }

    @PostMapping("/buy-now")
    public ResponseEntity<ApiResponse<OrderResponse>> buyNow(
            @Valid @RequestBody BuyNowRequest request,
            @AuthenticationPrincipal User buyer) {

        OrderResponse createdOrder = orderService.buyNow(buyer, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdOrder, "Order placed successfully"));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal User buyer,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<OrderResponse> orders = orderService.getBuyerOrders(buyer, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    // ==========================================
    // FARMER ENDPOINTS
    // ==========================================

    @GetMapping("/received-orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getReceivedOrders(
            @AuthenticationPrincipal User farmer,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<OrderResponse> orders = orderService.getFarmerOrders(farmer, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody OrderStatusUpdateRequest request,
            @AuthenticationPrincipal User farmer) {

        OrderResponse updatedOrder = orderService.updateOrderStatus(id, request, farmer);
        return ResponseEntity.ok(ApiResponse.success(updatedOrder, "Order status updated successfully"));
    }

    // ==========================================
    // SHARED ENDPOINTS (Buyer & Farmer)
    // ==========================================

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderDetails(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        OrderResponse order = orderService.getOrderById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<Void> downloadInvoice(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        // Security Check (Ensures only buyer/farmer can access)
        Order order = orderService.getOrderEntityById(id, currentUser);

        String invoiceKey = order.getInvoiceKey();
        if (invoiceKey == null) {
            // The async event might still be processing the PDF
            throw new BusinessException("Invoice is currently being generated. Please try again in a few seconds.");
        }

        // Generate a FRESH presigned URL (Valid for the next 15-60 mins)
        String freshUrl = storageService.getPresignedUrl(invoiceKey);

        // Redirect the browser to the fresh URL
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(java.net.URI.create(freshUrl));
        return new ResponseEntity<>(headers, org.springframework.http.HttpStatus.FOUND); // 302 Redirect
    }

    @GetMapping("/{id}/invoice-url")
    public ResponseEntity<ApiResponse<String>> getInvoiceUrl(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        // 1Security Check
        Order order = orderService.getOrderEntityById(id, currentUser);

        String invoiceKey = order.getInvoiceKey(); // This MUST be "invoices/xxx.pdf"
        if (invoiceKey == null || invoiceKey.isBlank()) {
            throw new BusinessException("Invoice is currently being generated. Please try again in a few seconds.");
        }

        // Generate a FRESH presigned URL from the clean key
        String freshUrl = storageService.getPresignedUrl(invoiceKey);

        // Return it as JSON
        return ResponseEntity.ok(ApiResponse.success(freshUrl, "Invoice URL generated successfully"));
    }
}