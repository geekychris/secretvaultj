package com.example.vault.ui.views;

import com.example.vault.entity.Identity;
import com.example.vault.repository.IdentityRepository;
import com.example.vault.repository.PolicyRepository;
import com.example.vault.service.IdentityService;
import com.example.vault.ui.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@PageTitle("Users")
@Route(value = "users", layout = MainLayout.class)
public class UsersView extends VerticalLayout {

    private final IdentityRepository identityRepository;
    private final PolicyRepository policyRepository;
    private final IdentityService identityService;
    private final PasswordEncoder passwordEncoder;
    private final Grid<Identity> grid;
    private final TextField searchField;
    private List<Identity> users;

    @Autowired
    public UsersView(IdentityRepository identityRepository, PolicyRepository policyRepository, 
                     IdentityService identityService, PasswordEncoder passwordEncoder) {
        this.identityRepository = identityRepository;
        this.policyRepository = policyRepository;
        this.identityService = identityService;
        this.passwordEncoder = passwordEncoder;
        this.grid = new Grid<>(Identity.class, false);
        this.searchField = new TextField();

        setSizeFull();
        configureGrid();
        configureSearch();
        add(getToolbar(), getContent());
        updateList();
    }

    private void configureSearch() {
        searchField.setPlaceholder("Search users...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> updateList());
    }

    private void configureGrid() {
        grid.addClassNames("users-grid");
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        grid.addColumn(Identity::getName)
                .setHeader("Username")
                .setSortable(true)
                .setFlexGrow(2);

        grid.addColumn(identity -> {
            if (identity.getPolicies() != null && !identity.getPolicies().isEmpty()) {
                return identity.getPolicies().stream()
                        .map(policy -> policy.getName())
                        .collect(Collectors.joining(", "));
            }
            return "None";
        })
                .setHeader("Policies")
                .setSortable(false)
                .setFlexGrow(3);

        grid.addColumn(identity -> {
            if (identity.getCreatedAt() != null) {
                return identity.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            }
            return "";
        })
                .setHeader("Created")
                .setSortable(true)
                .setWidth("150px")
                .setFlexGrow(0);

        grid.addComponentColumn(this::createActionButtons)
                .setHeader("Actions")
                .setWidth("150px")
                .setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(Identity user) {
        Button editButton = new Button("Edit", VaadinIcon.EDIT.create());
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        editButton.addClickListener(e -> editUser(user));

        Button deleteButton = new Button("Delete", VaadinIcon.TRASH.create());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        deleteButton.addClickListener(e -> deleteUser(user));
        
        // Don't allow deleting admin user
        if ("admin".equals(user.getName())) {
            deleteButton.setEnabled(false);
            deleteButton.setText("Admin");
        }

        HorizontalLayout actions = new HorizontalLayout(editButton, deleteButton);
        actions.setSpacing(true);
        return actions;
    }

    private HorizontalLayout getToolbar() {
        Button addUserButton = new Button("New User");
        addUserButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addUserButton.setIcon(VaadinIcon.PLUS.create());
        addUserButton.addClickListener(click -> addUser());

        Button refreshButton = new Button("Refresh");
        refreshButton.setIcon(VaadinIcon.REFRESH.create());
        refreshButton.addClickListener(click -> updateList());

        HorizontalLayout toolbar = new HorizontalLayout(searchField, addUserButton, refreshButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        
        return toolbar;
    }

    private VerticalLayout getContent() {
        VerticalLayout content = new VerticalLayout(grid);
        content.addClassNames("content");
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(false);
        return content;
    }

    private void updateList() {
        try {
            List<Identity> allUsers = identityRepository.findAll();
            
            String searchTerm = searchField.getValue() != null ? searchField.getValue().toLowerCase() : "";
            users = allUsers.stream()
                    .filter(user -> searchTerm.isEmpty() || 
                            user.getName().toLowerCase().contains(searchTerm))
                    .collect(Collectors.toList());

            grid.setItems(users);
        } catch (Exception e) {
            Notification notification = Notification.show("Error loading users: " + e.getMessage());
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void addUser() {
        UserFormDialog dialog = new UserFormDialog(policyRepository);
        dialog.addSaveListener(event -> saveUser(event.getUser(), true));
        dialog.open();
    }

    private void editUser(Identity user) {
        UserFormDialog dialog = new UserFormDialog(policyRepository, user);
        dialog.addSaveListener(event -> saveUser(event.getUser(), false));
        dialog.open();
    }

    private void deleteUser(Identity user) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete User");
        dialog.setText("Are you sure you want to delete user '" + user.getName() + "'? This action cannot be undone.");
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR + " " + ButtonVariant.LUMO_PRIMARY);
        dialog.addConfirmListener(event -> {
            try {
                identityService.deleteUser(user.getName());
                Notification.show("User deleted successfully").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                updateList();
            } catch (Exception e) {
                Notification.show("Error deleting user: " + e.getMessage())
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }
    
    private void saveUser(Identity user, boolean isNew) {
        try {
            if (isNew) {
                // For new users, we need to encode the password
                user.setPasswordHash(passwordEncoder.encode("changeme"));
                identityRepository.save(user);
                Notification.show("User created successfully. Default password is 'changeme'")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                // For existing users, save directly (password encoding handled in form if provided)
                identityRepository.save(user);
                Notification.show("User updated successfully")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
            updateList();
        } catch (Exception e) {
            Notification.show("Error saving user: " + e.getMessage())
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
