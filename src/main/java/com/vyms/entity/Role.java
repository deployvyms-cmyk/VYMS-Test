package com.vyms.entity;

/**
 * System roles used for access control and dashboard routing.
 *
 * Only two roles for production deployment:
 *   ADMIN   – user management, reports, system oversight
 *   MANAGER – all day-to-day operations (vehicles, repairs, sales, reports)
 */
public enum Role {
    ADMIN,
    MANAGER
}
