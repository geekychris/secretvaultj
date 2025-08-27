package com.example.vault.ui.views;

import com.example.vault.service.SecretService;
import com.example.vault.ui.dto.SecretUI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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
    
    // Bulk selection for versions
    private final Set<Integer> selectedVersions = new HashSet<>();
    private Grid<Map<String, Object>> versionsGrid;
    private Button bulkDeleteButton;
    private Checkbox selectAllCheckbox;
    
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
            
            // Clear selected versions when creating new tab
            selectedVersions.clear();
            
            // Create bulk actions toolbar
            HorizontalLayout bulkActionsToolbar = createBulkActionsToolbar();
            
            versionsGrid = new Grid<>();
            this.versionsGrid = versionsGrid;
            versionsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
            versionsGrid.setHeight("350px");
            
            // Enable column resizing
            versionsGrid.setColumnReorderingAllowed(true);
            
            // Add selection column with checkboxes
            selectAllCheckbox = new Checkbox();
            selectAllCheckbox.addValueChangeListener(e -> {
                if (e.getValue()) {
                    // Select all versions
                    versions.forEach(version -> selectedVersions.add((Integer) version.get("version")));
                } else {
                    // Deselect all
                    selectedVersions.clear();
                }
                versionsGrid.getDataProvider().refreshAll();
                updateBulkActionButtons();
            });
            
            versionsGrid.addComponentColumn(version -> {
                Integer versionNum = (Integer) version.get("version");
                Checkbox checkbox = new Checkbox();
                checkbox.setValue(selectedVersions.contains(versionNum));
                checkbox.addValueChangeListener(e -> {
                    if (e.getValue()) {
                        selectedVersions.add(versionNum);
                    } else {
                        selectedVersions.remove(versionNum);
                    }
                    updateBulkActionButtons();
                });
                return checkbox;
            })
                    .setHeader(selectAllCheckbox)
                    .setWidth("50px")
                    .setFlexGrow(0)
                    .setResizable(false);
            
            versionsGrid.addColumn(version -> "v" + version.get("version"))
                    .setHeader("Version")
                    .setWidth("80px")
                    .setFlexGrow(0)
                    .setResizable(true);
            
            versionsGrid.addColumn(version -> {
                Object createdAt = version.get("created_at");
                if (createdAt instanceof java.time.LocalDateTime) {
                    return ((java.time.LocalDateTime) createdAt).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }
                return "";
            })
                    .setHeader("Created At")
                    .setWidth("180px")
                    .setFlexGrow(0)
                    .setResizable(true);
            
            versionsGrid.addColumn(version -> version.get("created_by"))
                    .setHeader("Created By")
                    .setWidth("120px")
                    .setFlexGrow(0)
                    .setResizable(true);
            
            versionsGrid.addColumn(version -> {
                Object updatedAt = version.get("updated_at");
                if (updatedAt instanceof java.time.LocalDateTime) {
                    return ((java.time.LocalDateTime) updatedAt).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }
                return "";
            })
                    .setHeader("Updated At")
                    .setWidth("180px")
                    .setFlexGrow(0)
                    .setResizable(true);
            
            versionsGrid.addColumn(version -> version.get("updated_by"))
                    .setHeader("Updated By")
                    .setWidth("120px")
                    .setFlexGrow(0)
                    .setResizable(true);
            
            versionsGrid.addColumn(version -> {
                Boolean deleted = (Boolean) version.get("deleted");
                return deleted != null && deleted ? "Deleted" : "Active";
            })
                    .setHeader("Status")
                    .setWidth("90px")
                    .setFlexGrow(0)
                    .setResizable(true);
            
            versionsGrid.addComponentColumn(this::createSecretValueDisplay)
                    .setHeader("Secret Value")
                    .setWidth("200px")
                    .setFlexGrow(0)
                    .setResizable(true);
            
            versionsGrid.addComponentColumn(this::createVersionActions)
                    .setHeader("Actions")
                    .setWidth("200px")
                    .setFlexGrow(1)
                    .setResizable(true);
            
            versionsGrid.setItems(versions);
            
            H3 versionsHeader = new H3("Version History");
            layout.add(versionsHeader, bulkActionsToolbar, versionsGrid);
            
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

    private HorizontalLayout createBulkActionsToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        
        Span selectedCountLabel = new Span("0 selected");
        selectedCountLabel.setId("selected-count-label");
        
        bulkDeleteButton = new Button("Delete Selected", VaadinIcon.TRASH.create());
        bulkDeleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        bulkDeleteButton.setEnabled(false);
        bulkDeleteButton.addClickListener(e -> confirmBulkDelete());
        
        Button clearSelectionButton = new Button("Clear Selection", VaadinIcon.CLOSE_SMALL.create());
        clearSelectionButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        clearSelectionButton.setEnabled(false);
        clearSelectionButton.addClickListener(e -> {
            selectedVersions.clear();
            selectAllCheckbox.setValue(false);
            if (versionsGrid != null) {
                versionsGrid.getDataProvider().refreshAll();
            }
            updateBulkActionButtons();
        });
        
        toolbar.add(selectedCountLabel, bulkDeleteButton, clearSelectionButton);
        toolbar.setSpacing(true);
        
        // Store references for updating
        
        return toolbar;
    }
    
    private void updateBulkActionButtons() {
        int selectedCount = selectedVersions.size();
        
        // Update selected count label
        getUI().ifPresent(ui -> ui.access(() -> {
            // Find the toolbar components
            versionsTab.getChildren()
                .filter(component -> component instanceof HorizontalLayout)
                .map(component -> (HorizontalLayout) component)
                .filter(layout -> layout.getChildren()
                    .anyMatch(child -> child instanceof Span && 
                        ((Span) child).getId().orElse("").equals("selected-count-label")))
                .findFirst()
                .ifPresent(toolbar -> {
                    toolbar.getChildren()
                        .filter(child -> child instanceof Span)
                        .map(child -> (Span) child)
                        .filter(span -> span.getId().orElse("").equals("selected-count-label"))
                        .findFirst()
                        .ifPresent(label -> label.setText(selectedCount + " selected"));
                    
                    // Update button states
                    toolbar.getChildren()
                        .filter(child -> child instanceof Button)
                        .map(child -> (Button) child)
                        .forEach(button -> {
                            if (button.getText().equals("Clear Selection")) {
                                button.setEnabled(selectedCount > 0);
                            }
                        });
                });
        }));
        
        // Update bulk delete button
        if (bulkDeleteButton != null) {
            bulkDeleteButton.setEnabled(selectedCount > 0);
        }
    }
    
    private void confirmBulkDelete() {
        if (selectedVersions.isEmpty()) {
            return;
        }
        
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Selected Versions");
        dialog.setText(String.format("Are you sure you want to delete %d selected version(s)? This action cannot be undone.", selectedVersions.size()));
        dialog.setConfirmText("Delete All");
        dialog.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR + " " + ButtonVariant.LUMO_PRIMARY);
        dialog.addConfirmListener(event -> performBulkDelete());
        dialog.open();
    }
    
    private void performBulkDelete() {
        List<String> adminPolicies = Arrays.asList("admin");
        int successCount = 0;
        int failureCount = 0;
        List<String> failedVersions = new ArrayList<>();
        
        for (Integer versionNumber : new ArrayList<>(selectedVersions)) {
            try {
                boolean deleted = secretService.deleteSecretVersion(secret.getPath(), secret.getKey(), versionNumber, adminPolicies);
                if (deleted) {
                    successCount++;
                } else {
                    failureCount++;
                    failedVersions.add("v" + versionNumber);
                }
            } catch (Exception e) {
                failureCount++;
                failedVersions.add("v" + versionNumber + " (" + e.getMessage() + ")");
            }
        }
        
        // Clear selection and refresh
        selectedVersions.clear();
        selectAllCheckbox.setValue(false);
        versionsTab = null;
        showVersionsTab();
        
        // Show results
        if (successCount > 0 && failureCount == 0) {
            Notification.show(String.format("Successfully deleted %d version(s)", successCount))
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else if (successCount > 0 && failureCount > 0) {
            Notification.show(String.format("Deleted %d version(s), %d failed: %s", 
                successCount, failureCount, String.join(", ", failedVersions)))
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
        } else {
            Notification.show(String.format("Failed to delete versions: %s", 
                String.join(", ", failedVersions)))
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private HorizontalLayout createSecretValueDisplay(Map<String, Object> version) {
        HorizontalLayout container = new HorizontalLayout();
        container.setAlignItems(FlexComponent.Alignment.CENTER);
        container.setSpacing(false);
        
        Integer versionNumber = (Integer) version.get("version");
        Boolean deleted = (Boolean) version.get("deleted");
        
        if (deleted != null && deleted) {
            // For deleted versions, show a placeholder
            Span deletedSpan = new Span("[Deleted]");
            deletedSpan.getStyle().set("font-style", "italic");
            deletedSpan.getStyle().set("color", "var(--lumo-disabled-text-color)");
            container.add(deletedSpan);
            return container;
        }
        
        // Create masked value display
        Span maskedValue = new Span("•••••••••");
        maskedValue.getStyle().set("font-family", "monospace");
        maskedValue.getStyle().set("color", "var(--lumo-secondary-text-color)");
        maskedValue.setVisible(true);
        
        // Create actual value display (initially hidden)
        Span actualValue = new Span();
        actualValue.getStyle().set("font-family", "monospace");
        actualValue.getStyle().set("word-break", "break-all");
        actualValue.getStyle().set("max-width", "150px");
        actualValue.setVisible(false);
        
        // Create reveal/hide button
        Button revealButton = new Button(VaadinIcon.EYE.create());
        revealButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);
        revealButton.getStyle().set("margin-left", "5px");
        
        revealButton.addClickListener(e -> {
            if (actualValue.isVisible()) {
                // Hide the value
                actualValue.setVisible(false);
                maskedValue.setVisible(true);
                revealButton.setIcon(VaadinIcon.EYE.create());
                revealButton.getElement().setAttribute("title", "Show secret value");
            } else {
                // Load and show the value
                loadAndShowSecretValue(versionNumber, actualValue, maskedValue, revealButton);
            }
        });
        
        revealButton.getElement().setAttribute("title", "Show secret value");
        
        container.add(maskedValue, actualValue, revealButton);
        return container;
    }
    
    private void loadAndShowSecretValue(Integer versionNumber, Span actualValue, Span maskedValue, Button revealButton) {
        try {
            List<String> adminPolicies = Arrays.asList("admin");
            Optional<Map<String, Object>> secretData = secretService.getSecret(secret.getPath(), secret.getKey(), versionNumber, adminPolicies);
            
            if (secretData.isPresent()) {
                String value = (String) secretData.get().get("value");
                if (value != null) {
                    // Truncate long values
                    String displayValue = value.length() > 30 ? value.substring(0, 30) + "..." : value;
                    actualValue.setText(displayValue);
                    actualValue.getElement().setAttribute("title", value); // Full value in tooltip
                    
                    actualValue.setVisible(true);
                    maskedValue.setVisible(false);
                    revealButton.setIcon(VaadinIcon.EYE_SLASH.create());
                    revealButton.getElement().setAttribute("title", "Hide secret value");
                } else {
                    actualValue.setText("[No value]");
                    actualValue.setVisible(true);
                    maskedValue.setVisible(false);
                }
            } else {
                Notification.show("Failed to load secret value for version " + versionNumber)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception e) {
            Notification.show("Error loading secret value: " + e.getMessage())
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
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
