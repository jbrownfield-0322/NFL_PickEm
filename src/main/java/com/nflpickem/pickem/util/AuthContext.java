package com.nflpickem.pickem.util;

import com.nflpickem.pickem.model.User;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpSession;

public class AuthContext {
    
    private static final String USER_SESSION_KEY = "currentUser";
    
    public static User getCurrentUser() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpSession session = attributes.getRequest().getSession(false);
            if (session != null) {
                return (User) session.getAttribute(USER_SESSION_KEY);
            }
        }
        return null;
    }
    
    public static void setCurrentUser(User user) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpSession session = attributes.getRequest().getSession(true);
            session.setAttribute(USER_SESSION_KEY, user);
        }
    }
    
    public static void clearCurrentUser() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpSession session = attributes.getRequest().getSession(false);
            if (session != null) {
                session.removeAttribute(USER_SESSION_KEY);
            }
        }
    }
    
    public static Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }
}
