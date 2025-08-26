package com.example.vault.ui.views;

import com.example.vault.service.SecretService;
import com.example.vault.ui.dto.SecretUI;
import com.example.vault.ui.layout.MainLayout;
import com.example.vault.repository.IdentityRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;
import java.util.*;

@PageTitle("Secrets")
@Route(value = "secrets", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class SecretsListView extends VerticalLayout {

    private final SecretService secretService;
    private final IdentityRepository identityRepository;
    private final Grid<SecretUI> grid;
    private final TextField searchField;
    private List<SecretUI> secrets;

    @Autowired
    public SecretsListView(SecretService secretService, IdentityRepository identityRepository) {
        this.secretService = secretService;
        this.identityRepository = identityRepository;
        this.grid = new Grid<>(SecretUI.class, false);
        this.searchField = new TextField();

        setSizeFull();
        configureGrid();
        configureSearch();
        add(getToolbar(), getContent());
        updateList();
    }

    private void configureSearch() {
        searchField.setPlaceholder("Search secrets...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> updateList());
    }

    private void configureGrid() {
        grid.addClassNames("secrets-grid");
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        grid.addColumn(SecretUI::getPath)
                .setHeader("Path")
                .setSortable(true)
                .setFlexGrow(2);

        grid.addColumn(SecretUI::getKey)
                .setHeader("Key")
                .setSortable(true)
                .setFlexGrow(1);

        grid.addColumn(secret -> secret.getVersion() != null ? "v" + secret.getVersion() : "")
                .setHeader("Version")
                .setSortable(true)
                .setWidth("100px")
                .setFlexGrow(0);

        grid.addColumn(secret -> {
            if (secret.getCreatedAt() != null) {
                return secret.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            }
            return "";
        })
                .setHeader("Created")
                .setSortable(true)
                .setWidth("150px")
                .setFlexGrow(0);

        grid.addColumn(SecretUI::getCreatedBy)
                .setHeader("Created By")
                .setSortable(true)
                .setWidth("120px")
                .setFlexGrow(0);

        grid.addComponentColumn(this::createActionButtons)
                .setHeader("Actions")
                .setWidth("200px")
                .setFlexGrow(0);

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                editSecret(event.getValue());
            }
        });
    }

    private HorizontalLayout createActionButtons(SecretUI secret) {
        Button editButton = new Button("Edit", VaadinIcon.EDIT.create());
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        editButton.addClickListener(e -> editSecret(secret));

        Button viewButton = new Button("View", VaadinIcon.EYE.create());
        viewButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        viewButton.addClickListener(e -> viewSecret(secret));

        Button deleteButton = new Button("Delete", VaadinIcon.TRASH.create());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        deleteButton.addClickListener(e -> confirmDelete(secret));

        HorizontalLayout actions = new HorizontalLayout(viewButton, editButton, deleteButton);
        actions.setSpacing(true);
        return actions;
    }

    private HorizontalLayout getToolbar() {
        Button addSecretButton = new Button("New Secret");
        addSecretButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addSecretButton.setIcon(VaadinIcon.PLUS.create());
        addSecretButton.addClickListener(click -> addSecret());

        Button refreshButton = new Button("Refresh");
        refreshButton.setIcon(VaadinIcon.REFRESH.create());
        refreshButton.addClickListener(click -> updateList());

        HorizontalLayout toolbar = new HorizontalLayout(searchField, addSecretButton, refreshButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        
        return toolbar;
    }

    private Div getContent() {
        Div content = new Div(grid);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    private void updateList() {
        try {
            // For now, using admin policies to access all secrets
            List<String> adminPolicies = Arrays.asList("admin");
            List<SecretUI> secretsList = new ArrayList<>();
            
            // First discover all available paths by listing from root
            Set<String> allPaths = new HashSet<>();
            
            // Try to discover paths by listing from common root paths
            String[] rootPaths = {"app", "dev", "shared", "prod", "test", "foo"};
            
            for (String rootPath : rootPaths) {
                try {
                    List<String> paths = secretService.listPaths(rootPath, adminPolicies);
                    allPaths.addAll(paths);
                } catch (Exception e) {
                    // Skip roots we don't have access to (this is expected for many paths)
                    // System.err.println("Failed to list paths from root " + rootPath + ": " + e.getMessage());
                }
            }
            
            // Also include known/common paths to ensure we don't miss any
            String[] knownPaths = {"app/config/database", "app/api-keys", "dev/config", "shared/config", "shared/certificates", "foo/bla", "app/config/blaaa"};
            allPaths.addAll(Arrays.asList(knownPaths));
            
            // Now get secrets from all discovered paths
            for (String path : allPaths) {
                try {
                    List<String> secretKeys = secretService.listSecrets(path, adminPolicies);
                    for (String fullPath : secretKeys) {
                        String[] parts = fullPath.split("/");
                        if (parts.length >= 2) {
                            String secretPath = String.join("/", Arrays.copyOf(parts, parts.length - 1));
                            String secretKey = parts[parts.length - 1];
                            
                            try {
                                Optional<Map<String, Object>> secretData = secretService.getSecret(secretPath, secretKey, adminPolicies);
                                if (secretData.isPresent()) {
                                    SecretUI secretUI = mapToSecretUI(secretPath, secretKey, secretData.get());
                                    secretsList.add(secretUI);
                                }
                            } catch (Exception e) {
                                // Skip individual secrets that can't be accessed
                                System.err.println("Failed to load secret " + fullPath + ": " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    // Skip paths we don't have access to
                    System.err.println("Failed to list secrets at path " + path + ": " + e.getMessage());
                    continue;
                }
            }

            String searchTerm = searchField.getValue() != null ? searchField.getValue().toLowerCase() : "";
            secrets = secretsList.stream()
                    .filter(secret -> searchTerm.isEmpty() || 
                            secret.getPath().toLowerCase().contains(searchTerm) ||
                            secret.getKey().toLowerCase().contains(searchTerm))
                    .toList();

            grid.setItems(secrets);
        } catch (Exception e) {
            System.err.println("Error in updateList: " + e.getMessage());
            e.printStackTrace();
            Notification notification = Notification.show("Error loading secrets: " + e.getMessage());
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

    private void addSecret() {
        SecretFormDialog dialog = new SecretFormDialog(secretService);
        dialog.addSaveListener(event -> saveSecret(event.getSecret()));
        dialog.open();
    }

    private void editSecret(SecretUI secret) {
        SecretFormDialog dialog = new SecretFormDialog(secretService, secret);
        dialog.addSaveListener(event -> saveSecret(event.getSecret()));
        dialog.open();
    }

    private void viewSecret(SecretUI secret) {
        SecretDetailDialog dialog = new SecretDetailDialog(secretService, secret);
        dialog.open();
    }

    private void confirmDelete(SecretUI secret) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Secret");
        dialog.setText("Are you sure you want to delete the secret at " + secret.getFullPath() + "?");
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR + " " + ButtonVariant.LUMO_PRIMARY);
        dialog.addConfirmListener(event -> deleteSecret(secret));
        dialog.open();
    }

    private void saveSecret(SecretUI secret) {
        try {
            List<String> adminPolicies = Arrays.asList("admin");
            
            // Get the actual admin identity from the database
            Optional<com.example.vault.entity.Identity> adminIdentityOpt = identityRepository.findByName("admin");
            if (adminIdentityOpt.isEmpty()) {
                Notification.show("Admin user not found in database").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            com.example.vault.entity.Identity adminIdentity = adminIdentityOpt.get();
            
            Map<String, Object> metadata = secret.getMetadata() != null ? secret.getMetadata() : new HashMap<>();
            
            if (secret.getVersion() == null) {
                // Create new secret
                secretService.createSecret(secret.getPath(), secret.getKey(), secret.getValue(), metadata, adminIdentity, adminPolicies);
                Notification.show("Secret created successfully").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                // Update existing secret
                secretService.updateSecret(secret.getPath(), secret.getKey(), secret.getValue(), metadata, adminIdentity, adminPolicies);
                Notification.show("Secret updated successfully").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
            
            updateList();
        } catch (Exception e) {
            e.printStackTrace();
            Notification notification = Notification.show("Error saving secret: " + e.getMessage());
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void deleteSecret(SecretUI secret) {
        try {
            List<String> adminPolicies = Arrays.asList("admin");
            boolean deleted = secretService.deleteSecret(secret.getPath(), secret.getKey(), adminPolicies);
            
            if (deleted) {
                Notification.show("Secret deleted successfully").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                updateList();
            } else {
                Notification.show("Failed to delete secret").addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception e) {
            Notification notification = Notification.show("Error deleting secret: " + e.getMessage());
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
