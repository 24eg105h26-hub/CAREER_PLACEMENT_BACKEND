package com.smartplacement.controller;

import com.smartplacement.dto.ApiResponse;
import com.smartplacement.entity.Notification;
import com.smartplacement.entity.User;
import com.smartplacement.repository.NotificationRepository;
import com.smartplacement.repository.UserRepository;
import com.smartplacement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyNotifications(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
        List<Map<String, Object>> result = list.stream().map(n -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", n.getId());
            m.put("title", n.getTitle());
            m.put("message", n.getMessage());
            m.put("type", n.getType());
            m.put("isRead", n.getIsRead());
            m.put("link", n.getLink());
            m.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : "");
            return m;
        }).toList();
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved", result));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!n.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to notification");
        }
        n.setIsRead(true);
        notificationRepository.save(n);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<Notification> list = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(currentUser.getId());
        for (Notification n : list) {
            n.setIsRead(true);
            notificationRepository.save(n);
        }
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasAnyRole('TPO', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> broadcastNotification(
            @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        String title = (String) req.getOrDefault("title", "Announcement");
        String message = (String) req.getOrDefault("message", "");
        String type = (String) req.getOrDefault("type", "ANNOUNCEMENT");
        String link = (String) req.getOrDefault("link", "");

        List<User> students = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "STUDENT".equals(u.getRole().name()))
                .toList();

        for (User u : students) {
            notificationRepository.save(Notification.builder()
                    .user(u)
                    .title(title)
                    .message(message)
                    .type(type)
                    .link(link)
                    .build());
        }

        return ResponseEntity.ok(ApiResponse.success("Broadcast sent to all students", null));
    }
}