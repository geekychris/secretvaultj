package com.example.vault.ui.views;

import com.example.vault.ui.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@PageTitle("Audit")
@Route(value = "audit", layout = MainLayout.class)
public class AuditView extends VerticalLayout {

    private final Grid<AuditEntry> grid;
    private final TextField searchField;
    private List<AuditEntry> auditEntries;

    public AuditView() {
        this.grid = new Grid<>(AuditEntry.class, false);
        this.searchField = new TextField();

        setSizeFull();
        createSampleData();
        configureGrid();
        configureSearch();
        add(getToolbar(), getContent());
        updateList();
    }

    private void configureSearch() {
        searchField.setPlaceholder("Search audit logs...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> updateList());
    }

    private void configureGrid() {
        grid.addClassNames("audit-grid");
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        grid.addColumn(entry -> entry.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .setHeader("Timestamp")
                .setSortable(true)
                .setWidth("180px")
                .setFlexGrow(0);

        grid.addColumn(AuditEntry::getUser)
                .setHeader("User")
                .setSortable(true)
                .setWidth("120px")
                .setFlexGrow(0);

        grid.addColumn(AuditEntry::getAction)
                .setHeader("Action")
                .setSortable(true)
                .setWidth("150px")
                .setFlexGrow(0);

        grid.addColumn(AuditEntry::getResource)
                .setHeader("Resource")
                .setSortable(true)
                .setFlexGrow(2);

        grid.addColumn(AuditEntry::getResult)
                .setHeader("Result")
                .setSortable(true)
                .setWidth("100px")
                .setFlexGrow(0);

        grid.addColumn(AuditEntry::getDetails)
                .setHeader("Details")
                .setSortable(false)
                .setFlexGrow(2);
    }

    private HorizontalLayout getToolbar() {
        Button refreshButton = new Button("Refresh");
        refreshButton.setIcon(VaadinIcon.REFRESH.create());
        refreshButton.addClickListener(click -> updateList());

        Button exportButton = new Button("Export");
        exportButton.setIcon(VaadinIcon.DOWNLOAD.create());
        exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        exportButton.addClickListener(click -> exportLogs());

        HorizontalLayout toolbar = new HorizontalLayout(searchField, refreshButton, exportButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        
        return toolbar;
    }

    private VerticalLayout getContent() {
        VerticalLayout content = new VerticalLayout();
        
        // Info message
        H3 title = new H3("Audit Logs");
        Paragraph info = new Paragraph("This view shows audit logs of user activities. In a production system, this would display real audit data from the database.");
        info.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        content.add(title, info, grid);
        content.addClassNames("content");
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(true);
        return content;
    }

    private void updateList() {
        String searchTerm = searchField.getValue() != null ? searchField.getValue().toLowerCase() : "";
        List<AuditEntry> filteredEntries = auditEntries.stream()
                .filter(entry -> searchTerm.isEmpty() || 
                        entry.getUser().toLowerCase().contains(searchTerm) ||
                        entry.getAction().toLowerCase().contains(searchTerm) ||
                        entry.getResource().toLowerCase().contains(searchTerm))
                .collect(Collectors.toList());

        grid.setItems(filteredEntries);
    }

    private void exportLogs() {
        try {
            StringBuilder csv = new StringBuilder();
            csv.append("Timestamp,User,Action,Resource,Result,Details\n");
            
            List<AuditEntry> filteredEntries = getFilteredEntries();
            for (AuditEntry entry : filteredEntries) {
                csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    entry.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    entry.getUser(),
                    entry.getAction(),
                    entry.getResource(),
                    entry.getResult(),
                    entry.getDetails().replace("\"", "\"\"")
                ));
            }
            
            // Create a simple download by showing the data in a text area dialog
            showExportDialog(csv.toString());
            
        } catch (Exception e) {
            com.vaadin.flow.component.notification.Notification.show("Error exporting logs: " + e.getMessage())
                .addThemeVariants(com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR);
        }
    }
    
    private List<AuditEntry> getFilteredEntries() {
        String searchTerm = searchField.getValue() != null ? searchField.getValue().toLowerCase() : "";
        return auditEntries.stream()
                .filter(entry -> searchTerm.isEmpty() || 
                        entry.getUser().toLowerCase().contains(searchTerm) ||
                        entry.getAction().toLowerCase().contains(searchTerm) ||
                        entry.getResource().toLowerCase().contains(searchTerm))
                .collect(Collectors.toList());
    }
    
    private void showExportDialog(String csvData) {
        com.vaadin.flow.component.dialog.Dialog dialog = new com.vaadin.flow.component.dialog.Dialog();
        dialog.setModal(true);
        dialog.setWidth("800px");
        dialog.setHeight("600px");
        dialog.setHeaderTitle("Export Data (CSV Format)");
        
        com.vaadin.flow.component.textfield.TextArea textArea = new com.vaadin.flow.component.textfield.TextArea();
        textArea.setValue(csvData);
        textArea.setSizeFull();
        textArea.setReadOnly(true);
        
        com.vaadin.flow.component.html.Span instruction = new com.vaadin.flow.component.html.Span(
            "Copy the data below and save it as a .csv file:");
        instruction.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        com.vaadin.flow.component.button.Button closeButton = new com.vaadin.flow.component.button.Button("Close", e -> dialog.close());
        closeButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
        
        dialog.add(new com.vaadin.flow.component.orderedlayout.VerticalLayout(instruction, textArea));
        dialog.getFooter().add(closeButton);
        dialog.open();
    }

    private void createSampleData() {
        // Sample audit data for demonstration
        auditEntries = Arrays.asList(
                new AuditEntry(LocalDateTime.now().minusHours(1), "admin", "CREATE", "secret:/app/config/database/password", "SUCCESS", "Created new secret"),
                new AuditEntry(LocalDateTime.now().minusHours(2), "admin", "READ", "secret:/app/api-keys/stripe", "SUCCESS", "Accessed secret value"),
                new AuditEntry(LocalDateTime.now().minusHours(3), "demo", "READ", "secret:/dev/config/debug_token", "FAILED", "Access denied - insufficient permissions"),
                new AuditEntry(LocalDateTime.now().minusHours(4), "admin", "UPDATE", "policy:/readonly", "SUCCESS", "Modified policy rules"),
                new AuditEntry(LocalDateTime.now().minusHours(5), "admin", "DELETE", "secret:/temp/old_key", "SUCCESS", "Removed temporary secret"),
                new AuditEntry(LocalDateTime.now().minusHours(6), "demo", "LOGIN", "system", "SUCCESS", "User logged in"),
                new AuditEntry(LocalDateTime.now().minusHours(7), "admin", "CREATE", "user:/test_user", "SUCCESS", "Created new user account"),
                new AuditEntry(LocalDateTime.now().minusHours(8), "admin", "READ", "secret:/shared/certificates/ca_cert", "SUCCESS", "Retrieved certificate"),
                new AuditEntry(LocalDateTime.now().minusHours(9), "demo", "CREATE", "secret:/test/sample", "FAILED", "Access denied - create permission required"),
                new AuditEntry(LocalDateTime.now().minusHours(10), "admin", "UPDATE", "secret:/app/config/database/host", "SUCCESS", "Updated secret value")
        );
    }

    // Simple audit entry class for demo purposes
    public static class AuditEntry {
        private LocalDateTime timestamp;
        private String user;
        private String action;
        private String resource;
        private String result;
        private String details;

        public AuditEntry(LocalDateTime timestamp, String user, String action, String resource, String result, String details) {
            this.timestamp = timestamp;
            this.user = user;
            this.action = action;
            this.resource = resource;
            this.result = result;
            this.details = details;
        }

        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getUser() { return user; }
        public String getAction() { return action; }
        public String resource() { return resource; }
        public String getResult() { return result; }
        public String getDetails() { return details; }
        public String getResource() { return resource; }
    }
}
