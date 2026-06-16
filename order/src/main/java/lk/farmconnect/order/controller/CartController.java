package lk.farmconnect.order.controller;

import jakarta.validation.Valid;
import lk.farmconnect.common.response.ApiResponse;
import lk.farmconnect.order.dto.AddToCartRequest;
import lk.farmconnect.order.dto.CartResponse;
import lk.farmconnect.order.service.CartService;
import lk.farmconnect.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@AuthenticationPrincipal User buyer) {
        return ResponseEntity.ok(ApiResponse.success(cartService.getCart(buyer)));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            @AuthenticationPrincipal User buyer) {
        return ResponseEntity.ok(ApiResponse.success(cartService.addToCart(buyer, request)));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @PathVariable UUID itemId,
            @AuthenticationPrincipal User buyer) {
        cartService.removeItem(buyer, itemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart"));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(@AuthenticationPrincipal User buyer) {
        cartService.clearCart(buyer);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared"));
    }
}