package com.fcar.modules.favorite.controller;

import com.fcar.modules.favorite.entity.Favorite;
import com.fcar.modules.user.entity.User;
import com.fcar.modules.favorite.repository.FavoriteRepository;
import com.fcar.modules.user.security.FcarUserDetails;
import com.fcar.modules.favorite.service.FavoriteService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Phân công: Hiệp Hiếu — danh sách xe yêu thích. */
@Controller
@RequiredArgsConstructor
@RequestMapping("/favorites")
public class FavoriteController {

    private final FavoriteRepository favoriteRepository;
    private final FavoriteService favoriteService;

    @GetMapping
    public String listFavorites(@AuthenticationPrincipal FcarUserDetails principal, Model model) {
        if (principal == null) {
            return "redirect:/auth/login?redirect=/favorites";
        }
        User user = principal.getUser();
        List<Favorite> favorites = favoriteRepository.findByUserWithDetails(user);
        model.addAttribute("favorites", favorites);
        model.addAttribute("title", "Xe yêu thích");
        return "favorites/list";
    }

    @GetMapping("/toggle/{definitionId}")
    public String toggleFavorite(@PathVariable Long definitionId,
                                 @AuthenticationPrincipal FcarUserDetails principal,
                                 @RequestParam(value = "redirect", required = false) String redirect) {
        if (principal == null) {
            return "redirect:/auth/login?redirect=/cars/" + definitionId;
        }
        User user = principal.getUser();
        favoriteService.toggleFavorite(user, definitionId);
        if (redirect != null && !redirect.isBlank()) {
            return "redirect:" + redirect;
        }
        return "redirect:/cars/" + definitionId;
    }
}
