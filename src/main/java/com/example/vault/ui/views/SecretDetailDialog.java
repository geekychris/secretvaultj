package com.example.vault.ui.views;

import com.example.vault.service.SecretService;
import com.example.vault.ui.dto.SecretUI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Dialog for viewing secret details and versions
 */
public class SecretDetailDialog extends Dialog {

    private final SecretService secretService;
    private final SecretUI secret;
    private final VerticalLayout content;
    private final Tabs tabs;
    
    // Components for different tabs
    private VerticalLayout detailsTab;
    private VerticalLayout versionsTab;
    
    public SecretDetailDialog(SecretService secretService, SecretUI secret) {
        this.secretService = secretService;
        this.secret = secret;
        this.content = new VerticalLayout();
        this.tabs = new Tabs();
        
        configureDialog();
        createTabs();
        add(tabs, content);
        showDetailsTab();
    }

    private void configureDialog() {
        setModal(true);
        setDraggable(true);
        setResizable(true);
        setWidth("800px");
        setHeight("600px");
        setHeaderTitle("Secret: " + secret.getFullPath());
        
        Button closeButton = new Button("Close", e -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        getFooter().add(closeButton);
    }

    private void createTabs() {
        Tab detailsTabHeader = new Tab("Details");
        Tab versionsTabHeader = new Tab("Versions");
        
        tabs.add(detailsTabHeader, versionsTabHeader);
        tabs.setSelectedTab(detailsTabHeader);
        
        tabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab() == detailsTabHeader) {
                showDetailsTab();
            } else if (event.getSelectedTab() == versionsTabHeader) {
                showVersionsTab();
            }
        });
    }

    private void showDetailsTab() {
        content.removeAll();
        
        if (detailsTab == null) {
            detailsTab = createDetailsTab();
        }
        
        content.add(detailsTab);
    }

    private void showVersionsTab() {
        content.removeAll();
        
        if (versionsTab == null) {
            versionsTab = createVersionsTab();
        }
        
        content.add(versionsTab);
    }

    private VerticalLayout createDetailsTab() {
        VerticalLayout layout = new VerticalLayout();
        
        // Basic information
        H3 basicInfoHeader = new H3("Basic Information");
        
        TextField pathField = new TextField("Path");
        pathField.setValue(secret.getPath() != null ? secret.getPath() : "");
        pathField.setReadOnly(true);
        pathField.setWidth("100%");
        
        TextField keyField = new TextField("Key");
        keyField.setValue(secret.getKey() != null ? secret.getKey() : "");
        keyField.setReadOnly(true);
        keyField.setWidth("100%");
        
        PasswordField valueField = new PasswordField("Value");
        valueField.setValue(secret.getValue() != null ? secret.getValue() : "");
        valueField.setReadOnly(true);
        valueField.setRevealButtonVisible(true);
        valueField.setWidth("100%");
        
        TextField versionField = new TextField("Version");
        versionField.setValue(secret.getVersion() != null ? "v" + secret.getVersion() : "");
        versionField.setReadOnly(true);
        versionField.setWidth("200px");
        
        // Audit information
        H3 auditHeader = new H3("Audit Information");
        
        TextField createdAtField = new TextField("Created At");
        if (secret.getCreatedAt() != null) {
            createdAtField.setValue(secret.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        createdAtField.setReadOnly(true);
        createdAtField.setWidth("300px");
        
        TextField createdByField = new TextField("Created By");
        createdByField.setValue(secret.getCreatedBy() != null ? secret.getCreatedBy() : "");
        createdByField.setReadOnly(true);
        createdByField.setWidth("200px");
        
        TextField updatedAtField = new TextField("Updated At");
        if (secret.getUpdatedAt() != null) {
            updatedAtField.setValue(secret.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        updatedAtField.setReadOnly(true);
        updatedAtField.setWidth("300px");
        
        TextField updatedByField = new TextField("Updated By");
        updatedByField.setValue(secret.getUpdatedBy() != null ? secret.getUpdatedBy() : "");
        updatedByField.setReadOnly(true);
        updatedByField.setWidth("200px");
        
        // Metadata section
        Details metadataDetails = createMetadataDetails();
        
        HorizontalLayout auditLayout = new HorizontalLayout(
            new VerticalLayout(createdAtField, updatedAtField),
            new VerticalLayout(createdByField, updatedByField)
        );
        auditLayout.setWidthFull();
        
        layout.add(
            basicInfoHeader,
            pathField,
            keyField,
            valueField,
            versionField,
            auditHeader,
            auditLayout,
            metadataDetails
        );
        
        layout.setPadding(true);
        layout.setSpacing(true);
        
        return layout;
    }

    private Details createMetadataDetails() {
        VerticalLayout metadataContent = new VerticalLayout();
        
        if (secret.getMetadata() != null && !secret.getMetadata().isEmpty()) {
            for (Map.Entry<String, Object> entry : secret.getMetadata().entrySet()) {
                HorizontalLayout row = new HorizontalLayout();
                
                Span keySpan = new Span(entry.getKey() + ":");
                keySpan.getStyle().set("font-weight", "bold");
                keySpan.setWidth("150px");
                
                Span valueSpan = new Span(entry.getValue().toString());
                
                row.add(keySpan, valueSpan);
                row.setAlignItems(FlexComponent.Alignment.CENTER);
                metadataContent.add(row);
            }
        } else {
            metadataContent.add(new Span("No metadata available"));
        }
        
        Details details = new Details("Metadata", metadataContent);
        details.setOpened(false);
        
        return details;
    }

    private VerticalLayout createVersionsTab() {
        VerticalLayout layout = new VerticalLayout();
        
        try {
            // For now, using admin policies - will be updated with authentication
            List<String> adminPolicies = Arrays.asList("admin");
            
            List<Map<String, Object>> versions = secretService.listSecretVersions(secret.getPath(), secret.getKey(), adminPolicies);
            
            if (versions.isEmpty()) {
                layout.add(new Span("No versions found"));
                return layout;
            }
            
            Grid<Map<String, Object>> versionsGrid = new Grid<>();
            versionsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
            versionsGrid.setHeight("400px");
            
            versionsGrid.addColumn(version -> "v" + version.get("version"))
                    .setHeader("Version")
                    .setWidth("100px");
            
            versionsGrid.addColumn(version -> {
                Object createdAt = version.get("created_at");
                if (createdAt instanceof java.time.LocalDateTime) {
                    return ((java.time.LocalDateTime) createdAt).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                }
                return "";
            })
                    .setHeader("Created")
                    .setWidth("150px");
            
            versionsGrid.addColumn(version -> version.get("created_by"))
                    .setHeader("Created By")
                    .setWidth("120px");
            
            versionsGrid.addColumn(version -> {
                Object updatedAt = version.get("updated_at");
                if (updatedAt instanceof java.time.LocalDateTime) {
                    return ((java.time.LocalDateTime) updatedAt).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                }
                return "";
            })
                    .setHeader("Updated")
                    .setWidth("150px");
            
            versionsGrid.addColumn(version -> version.get("updated_by"))
                    .setHeader("Updated By")
                    .setWidth("120px");
            
            versionsGrid.addColumn(version -> {
                Boolean deleted = (Boolean) version.get("deleted");
                return deleted != null && deleted ? "Deleted" : "Active";
            })
                    .setHeader("Status")
                    .setWidth("80px");
            
            versionsGrid.addComponentColumn(this::createVersionActions)
                    .setHeader("Actions")
                    .setWidth("150px");
            
            versionsGrid.setItems(versions);
            
            H3 versionsHeader = new H3("Version History");
            layout.add(versionsHeader, versionsGrid);
            
        } catch (Exception e) {
            Notification notification = Notification.show("Error loading versions: " + e.getMessage());
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            layout.add(new Span("Error loading versions"));
        }
        
        layout.setPadding(true);
        layout.setSpacing(true);
        
        return layout;
    }

    private HorizontalLayout createVersionActions(Map<String, Object> version) {
        HorizontalLayout actions = new HorizontalLayout();
        
        Button viewButton = new Button("View", VaadinIcon.EYE.create());
        viewButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        viewButton.addClickListener(e -> viewVersion(version));
        
        Boolean deleted = (Boolean) version.get("deleted");
        if (deleted != null && deleted) {
            Button restoreButton = new Button("Restore", VaadinIcon.REFRESH.create());
            restoreButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
            restoreButton.addClickListener(e -> restoreVersion(version));
            actions.add(restoreButton);
        } else {
            Button deleteButton = new Button("Delete", VaadinIcon.TRASH.create());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e -> deleteVersion(version));
            actions.add(deleteButton);
        }
        
        actions.add(viewButton);
        actions.setSpacing(true);
        
        return actions;
    }

    private void viewVersion(Map<String, Object> version) {
        try {
            Integer versionNumber = (Integer) version.get("version");
            List<String> adminPolicies = Arrays.asList("admin");
            
            Optional<Map<String, Object>> secretData = secretService.getSecret(secret.getPath(), secret.getKey(), versionNumber, adminPolicies);
            
            if (secretData.isPresent()) {
                SecretUI versionSecret = mapToSecretUI(secret.getPath(), secret.getKey(), secretData.get());
                SecretDetailDialog versionDialog = new SecretDetailDialog(secretService, versionSecret);
                versionDialog.open();
            } else {
                Notification.show("Version not found").addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception e) {
            Notification notification = Notification.show("Error loading version: " + e.getMessage());
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void restoreVersion(Map<String, Object> version) {
        try {
            Integer versionNumber = (Integer) version.get("version");
            List<String> adminPolicies = Arrays.asList("admin");
            
            boolean restored = secretService.restoreSecretVersion(secret.getPath(), secret.getKey(), versionNumber, adminPolicies);
            
            if (restored) {
                Notification.show("Version restored successfully").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                // Refresh versions tab
                versionsTab = null;
                showVersionsTab();
            } else {
                Notification.show("Failed to restore version").addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception e) {
            Notification notification = Notification.show("Error restoring version: " + e.getMessage());
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void deleteVersion(Map<String, Object> version) {
        try {
            Integer versionNumber = (Integer) version.get("version");
            List<String> adminPolicies = Arrays.asList("admin");
            
            boolean deleted = secretService.deleteSecretVersion(secret.getPath(), secret.getKey(), versionNumber, adminPolicies);
            
            if (deleted) {
                Notification.show("Version deleted successfully").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                // Refresh versions tab
                versionsTab = null;
                showVersionsTab();
            } else {
                Notification.show("Failed to delete version").addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception e) {
            Notification notification = Notification.show("Error deleting version: " + e.getMessage());
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private SecretUI mapToSecretUI(String path, String key, Map<String, Object> secretData) {
        SecretUI secretUI = new SecretUI(path, key);
        secretUI.setValue((String) secretData.get("value"));
        secretUI.setVersion((Integer) secretData.get("version"));
        secretUI.setCreatedAt((java.time.LocalDateTime) secretData.get("created_at"));
        secretUI.setUpdatedAt((java.time.LocalDateTime) secretData.get("updated_at"));
        secretUI.setMetadata((Map<String, Object>) secretData.get("metadata"));
        return secretUI;
    }
}
