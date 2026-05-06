package com.fcar.modules.favorite.controller;

import com.fcar.modules.user.entity.User;
import com.fcar.modules.user.security.FcarUserDetails;
import com.fcar.modules.favorite.service.FavoriteService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Phân công: Hiệp Hiếu — API bật/tắt yêu thích (JSON). */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteApiController {

    private final FavoriteService favoriteService;

    @PostMapping("/toggle/{definitionId}")
    public ResponseEntity<?> toggleFavorite(
            @PathVariable Long definitionId, @AuthenticationPrincipal FcarUserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "login_required"));
        }
        User user = principal.getUser();
        try {
            boolean favorited = favoriteService.toggleFavorite(user, definitionId);
            return ResponseEntity.ok(Map.of("favorited", favorited));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
