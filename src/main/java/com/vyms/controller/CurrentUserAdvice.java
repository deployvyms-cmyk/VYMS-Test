package com.vyms.controller;

import com.vyms.entity.User;
import com.vyms.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

@ControllerAdvice
public class CurrentUserAdvice {

    private final UserService userService;

    @Autowired
    public CurrentUserAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("currentUser")
    public User currentUser(HttpSession session) {
        Object idAttr = session.getAttribute("currentUserId");
        if (idAttr instanceof Long) {
            Optional<User> userOpt = userService.findById((Long) idAttr);
            return userOpt.orElse(null);
        }
        if (idAttr instanceof Integer) {
            Optional<User> userOpt = userService.findById(((Integer) idAttr).longValue());
            return userOpt.orElse(null);
        }
        return null;
    }
}

