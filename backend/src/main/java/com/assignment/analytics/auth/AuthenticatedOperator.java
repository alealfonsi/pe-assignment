package com.assignment.analytics.auth;

import java.util.UUID;

/** Principal stored in the SecurityContext for an authenticated operator. */
public record AuthenticatedOperator(UUID operatorId, String username, String displayName, String role) {
}
