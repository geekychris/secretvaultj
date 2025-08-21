package com.example.vault.dto;

public class VersionInfoResponse {
    
    private String path;
    private Long totalVersions;
    private Integer earliestVersion;
    private Integer latestVersion;
    
    public VersionInfoResponse() {}
    
    public VersionInfoResponse(String path, Long totalVersions, Integer earliestVersion, Integer latestVersion) {
        this.path = path;
        this.totalVersions = totalVersions;
        this.earliestVersion = earliestVersion;
        this.latestVersion = latestVersion;
    }
    
    // Getters and Setters
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public Long getTotalVersions() { return totalVersions; }
    public void setTotalVersions(Long totalVersions) { this.totalVersions = totalVersions; }
    
    public Integer getEarliestVersion() { return earliestVersion; }
    public void setEarliestVersion(Integer earliestVersion) { this.earliestVersion = earliestVersion; }
    
    public Integer getLatestVersion() { return latestVersion; }
    public void setLatestVersion(Integer latestVersion) { this.latestVersion = latestVersion; }
}
