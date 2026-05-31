package com.vyms.controller;

import com.vyms.entity.Role;
import com.vyms.entity.User;
import com.vyms.service.SystemLogService;
import com.vyms.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;
    private final SystemLogService logService;

    // Basic input checks for employee fields.
    private static final Pattern EMP_EMAIL_PATTERN = Pattern
            .compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern EMP_NAME_PATTERN = Pattern.compile("^[A-Za-z .'-]{2,100}$");

    // Checks whether a role is one of the core system roles.
    private boolean isMainRole(Role role) {
        return role == Role.ADMIN || role == Role.MANAGER;
    }

    // Validates name, email, and password based on the operation.
    private boolean isValidEmployeeInput(String name, String email, String password, boolean passwordRequired) {
        if (name == null || !EMP_NAME_PATTERN.matcher(name.trim()).matches())
            return false;
        if (email == null || !EMP_EMAIL_PATTERN.matcher(email.trim()).matches())
            return false;
        if (passwordRequired && (password == null || password.length() < 8))
            return false;
        if (!passwordRequired && password != null && !password.isEmpty() && password.length() < 8)
            return false;
        return true;
    }

    // Creates the controller with its required services.
    @Autowired
    public AdminUserController(UserService userService, SystemLogService logService) {
        this.userService = userService;
        this.logService = logService;
    }

    // Shows the user list page with role counts.
    @GetMapping
    public String listUsers(Model model) {
        var users = userService.findAll();
        User firstAdmin = users.stream().filter(u -> u.getRole() == Role.ADMIN).findFirst().orElse(null);
        User firstManager = users.stream().filter(u -> u.getRole() == Role.MANAGER).findFirst().orElse(null);

        List<User> visibleUsers = new ArrayList<>();
        if (firstAdmin != null) {
            visibleUsers.add(firstAdmin);
        }
        if (firstManager != null && (firstAdmin == null || !firstManager.getId().equals(firstAdmin.getId()))) {
            visibleUsers.add(firstManager);
        }

        model.addAttribute("users", visibleUsers);
        model.addAttribute("roles", Role.values());
        model.addAttribute("totalUsers", visibleUsers.size());
        model.addAttribute("invalidUserCount", 0);

        model.addAttribute("countAdmin", firstAdmin != null ? 1 : 0);
        model.addAttribute("countManager", firstManager != null ? 1 : 0);
        return "admin/users";
    }

    // Creates a new user from the admin form submission.
    @PostMapping("/add")
    public String addUser(@RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("role") Role role) {
        if (!isValidEmployeeInput(username, email, password, true)) {
            return "redirect:/admin/users";
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role);

        userService.save(user);
        logService.createLog("USER_CREATED", "Admin created user: " + email, "Admin", "SUCCESS");
        return "redirect:/admin/users";
    }

    // Deletes a user if it will not remove the last core role.
    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id, RedirectAttributes ra) {
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isPresent()) {
            User target = userOpt.get();
            Role role = target.getRole();

            if (role != null && isMainRole(role)) {
                long sameRoleCount = userService.findAll().stream()
                        .filter(u -> u.getRole() == role)
                        .count();

                if (sameRoleCount <= 1) {
                    ra.addFlashAttribute("errorMsg",
                            "Cannot delete this user. At least one " + role.name() + " account must remain.");
                    logService.createLog("USER_DELETE_BLOCKED",
                            "Delete blocked for last user in role: " + role.name() + " (" + target.getEmail() + ")",
                            "Admin", "FAILED");
                    return "redirect:/admin/users";
                }
            }

            String email = target.getEmail();
            try {
                userService.deleteById(id);
                ra.addFlashAttribute("successMsg", "User deleted successfully.");
                logService.createLog("USER_DELETED", "Admin deleted user: " + email, "Admin", "SUCCESS");
            } catch (Exception ex) {
                ra.addFlashAttribute("errorMsg", "Cannot delete this user right now.");
                logService.createLog("USER_DELETE_FAILED",
                        "Delete failed for user: " + email + " | " + ex.getClass().getSimpleName(),
                        "Admin", "FAILED");
            }
            return "redirect:/admin/users";
        }
        ra.addFlashAttribute("errorMsg", "User not found.");
        return "redirect:/admin/users";
    }

    // Shows the edit form for one user.
    @GetMapping("/edit/{id}")
    public String editUserForm(@PathVariable("id") Long id, Model model) {
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isPresent()) {
            model.addAttribute("user", userOpt.get());
            model.addAttribute("roles", Role.values());
            return "admin/users-edit";
        }
        return "redirect:/admin/users";
    }

    // Updates an existing user from the admin edit form.
    @PostMapping("/edit/{id}")
    public String updateUser(@PathVariable("id") Long id,
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("role") Role role,
            @RequestParam(name = "password", required = false) String password) {
        if (!isValidEmployeeInput(username, email, password, false)) {
            return "redirect:/admin/users";
        }
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setUsername(username);
            user.setEmail(email);
            user.setRole(role);

            // Only change the password when a new one is provided.
            if (password != null && !password.trim().isEmpty()) {
                user.setPassword(password);
            }

            userService.save(user);
            logService.createLog("USER_UPDATED", "Admin updated user: " + email, "Admin", "SUCCESS");
        }
        return "redirect:/admin/users";
    }

    // Removes all users that have invalid or legacy roles.
    @PostMapping("/cleanup-invalid")
    public String cleanupInvalidUsers(RedirectAttributes ra) {
        var users = userService.findAll();
        var invalidUsers = users.stream().filter(u -> !isMainRole(u.getRole())).toList();

        if (invalidUsers.isEmpty()) {
            ra.addFlashAttribute("successMsg", "No invalid users found to remove.");
            return "redirect:/admin/users";
        }

        int removed = 0;
        for (User user : invalidUsers) {
            try {
                userService.deleteById(user.getId());
                removed++;
            } catch (Exception ex) {
                logService.createLog("USER_DELETE_FAILED",
                        "Cleanup failed for user: " + user.getEmail() + " | " + ex.getClass().getSimpleName(),
                        "Admin", "FAILED");
            }
        }

        if (removed > 0) {
            ra.addFlashAttribute("successMsg", "Removed " + removed + " invalid users.");
            logService.createLog("USER_CLEANUP", "Admin removed " + removed + " invalid users.", "Admin", "SUCCESS");
        } else {
            ra.addFlashAttribute("errorMsg", "Could not remove invalid users.");
        }
        return "redirect:/admin/users";
    }
}
