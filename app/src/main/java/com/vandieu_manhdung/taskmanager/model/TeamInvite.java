package com.vandieu_manhdung.taskmanager.model;

public class TeamInvite {

    private String inviteId;
    private String workspaceId;
    private String email;
    private String invitedUserId;
    private String invitedUserCode;
    private String invitedDisplayName;
    private String workspaceName;
    private String role;
    private String status;
    private String invitedBy;
    private long createdAt;
    private long respondedAt;
    private long expiresAt;

    public String getInviteId() {
        return inviteId;
    }

    public void setInviteId(String inviteId) {
        this.inviteId = inviteId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getInvitedUserId() { return invitedUserId; }
    public void setInvitedUserId(String invitedUserId) { this.invitedUserId = invitedUserId; }
    public String getInvitedUserCode() { return invitedUserCode; }
    public void setInvitedUserCode(String invitedUserCode) { this.invitedUserCode = invitedUserCode; }
    public String getInvitedDisplayName() { return invitedDisplayName; }
    public void setInvitedDisplayName(String invitedDisplayName) { this.invitedDisplayName = invitedDisplayName; }
    public String getWorkspaceName() { return workspaceName; }
    public void setWorkspaceName(String workspaceName) { this.workspaceName = workspaceName; }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(String invitedBy) {
        this.invitedBy = invitedBy;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(long respondedAt) {
        this.respondedAt = respondedAt;
    }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
}
