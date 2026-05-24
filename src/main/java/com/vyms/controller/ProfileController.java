 package com.vyms.controller;

import com.vyms.entity.Role;
import com.vyms.entity.User;
import com.vyms.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.regex.Pattern;

@Controller
public class ProfileController {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z .'-]{2,100}$");
    private static final Pattern EMAIL_PATTERN = Pattern
            .compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public ProfileController(UserService userService, BCryptPasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/profile")
    public String profile(Model model, HttpSession session, RedirectAttributes ra) {
        User currentUser = resolveCurrentUser(session);
        if (currentUser == null) {
            ra.addFlashAttribute("errorMsg", "Please sign in to access your profile.");
            return "redirect:/login";
        }
        model.addAttribute("userProfile", currentUser);
        model.addAttribute("dashboardPath", dashboardPath(currentUser.getRole()));
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam(name = "currentPassword", required = false) String currentPassword,
            @RequestParam(name = "newPassword", required = false) String newPassword,
            @RequestParam(name = "confirmPassword", required = false) String confirmPassword,
            HttpSession session,
            RedirectAttributes ra) {
        User currentUser = resolveCurrentUser(session);
        if (currentUser == null) {
            ra.addFlashAttribute("errorMsg", "Please sign in to access your profile.");
            return "redirect:/login";
        }

        String trimmedName = username == null ? "" : username.trim();
        String trimmedEmail = email == null ? "" : email.trim();

        if (!NAME_PATTERN.matcher(trimmedName).matches()) {
            ra.addFlashAttribute("errorMsg", "Enter a valid name (2-100 letters)." );
            return "redirect:/profile";
        }
        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            ra.addFlashAttribute("errorMsg", "Enter a valid email address.");
            return "redirect:/profile";
        }

        Optional<User> existing = userService.findByEmail(trimmedEmail);
        if (existing.isPresent() && !existing.get().getId().equals(currentUser.getId())) {
            ra.addFlashAttribute("errorMsg", "That email is already in use.");
            return "redirect:/profile";
        }

        boolean wantsPasswordChange = newPassword != null && !newPassword.trim().isEmpty();
        if (wantsPasswordChange) {
            if (currentPassword == null || currentPassword.trim().isEmpty()) {
                ra.addFlashAttribute("errorMsg", "Enter your current password to change it.");
                return "redirect:/profile";
            }
            if (!passwordEncoder.matches(currentPassword, currentUser.getPassword())) {
                ra.addFlashAttribute("errorMsg", "Current password is incorrect.");
                return "redirect:/profile";
            }
            if (newPassword.length() < 8) {
                ra.addFlashAttribute("errorMsg", "New password must be at least 8 characters.");
                return "redirect:/profile";
            }
            if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
                ra.addFlashAttribute("errorMsg", "Confirm password does not match.");
                return "redirect:/profile";
            }
            currentUser.setPassword(newPassword);
        }

        currentUser.setUsername(trimmedName);
        currentUser.setEmail(trimmedEmail);
        userService.save(currentUser);

        session.setAttribute("currentUserId", currentUser.getId());
        session.setAttribute("currentUserRole", currentUser.getRole() != null ? currentUser.getRole().name() : null);
        session.setAttribute("currentUserName", currentUser.getUsername());

        ra.addFlashAttribute("successMsg", "Profile updated successfully.");
        return "redirect:/profile";
    }

    private User resolveCurrentUser(HttpSession session) {
        Object idAttr = session.getAttribute("currentUserId");
        Long id = null;
        if (idAttr instanceof Long) {
            id = (Long) idAttr;
        } else if (idAttr instanceof Integer) {
            id = ((Integer) idAttr).longValue();
        }
        if (id == null) {
            return null;
        }
        return userService.findById(id).orElse(null);
    }

    private String dashboardPath(Role role) {
        if (role == null) {
            return "/login";
        }
        switch (role) {
            case ADMIN:
                return "/admin";
            case MANAGER:
                return "/manager";
            default:
                return "/login";
        }
    }
}

