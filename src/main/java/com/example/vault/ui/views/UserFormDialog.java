package com.example.vault.ui.views;

import com.example.vault.entity.Identity;
import com.example.vault.entity.Policy;
import com.example.vault.repository.PolicyRepository;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.shared.Registration;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dialog for creating and editing users
 */
public class UserFormDialog extends Dialog {

    public static abstract class UserFormEvent extends ComponentEvent<UserFormDialog> {
        private Identity user;

        protected UserFormEvent(UserFormDialog source, Identity user) {
            super(source, false);
            this.user = user;
        }

        public Identity getUser() {
            return user;
        }
    }

    public static class SaveEvent extends UserFormEvent {
        SaveEvent(UserFormDialog source, Identity user) {
            super(source, user);
        }
    }

    private final PolicyRepository policyRepository;
    private final Binder<UserFormData> binder = new Binder<>(UserFormData.class);
    private UserFormData formData;
    private Identity originalUser;

    private TextField usernameField = new TextField("Username");
    private PasswordField passwordField = new PasswordField("Password");
    private PasswordField confirmPasswordField = new PasswordField("Confirm Password");
    private Select<Identity.IdentityType> userTypeSelect = new Select<>();
    private CheckboxGroup<Policy> policiesGroup = new CheckboxGroup<>();
    
    private Button saveButton = new Button("Save");
    private Button cancelButton = new Button("Cancel");

    public UserFormDialog(PolicyRepository policyRepository) {
        this(policyRepository, null);
    }

    public UserFormDialog(PolicyRepository policyRepository, Identity user) {
        this.policyRepository = policyRepository;
        this.originalUser = user;
        this.formData = new UserFormData();
        
        if (user != null) {
            populateFormData(user);
        }
        
        configureDialog();
        configureForm();
        configureBinder();
        populateForm();
    }

    private void populateFormData(Identity user) {
        formData.setUsername(user.getName());
        formData.setUserType(user.getType() != null ? user.getType() : Identity.IdentityType.USER);
        formData.setPolicies(user.getPolicies());
        // Don't populate password for existing users
    }

    private void configureDialog() {
        setModal(true);
        setDraggable(true);
        setResizable(true);
        setWidth("600px");
        setHeight("500px");
        
        String title = originalUser != null ? "Edit User" : "New User";
        setHeaderTitle(title);
    }

    private void configureForm() {
        FormLayout formLayout = new FormLayout();
        
        usernameField.setRequired(true);
        usernameField.setPlaceholder("Enter username");
        
        passwordField.setRequired(originalUser == null); // Required for new users only
        passwordField.setPlaceholder(originalUser != null ? "Leave empty to keep current password" : "Enter password");
        passwordField.setRevealButtonVisible(true);
        
        confirmPasswordField.setRequired(originalUser == null);
        confirmPasswordField.setPlaceholder("Confirm password");
        confirmPasswordField.setRevealButtonVisible(true);
        
        userTypeSelect.setLabel("User Type");
        userTypeSelect.setItems(Identity.IdentityType.values());
        userTypeSelect.setValue(Identity.IdentityType.USER);
        userTypeSelect.setItemLabelGenerator(type -> 
            type.name().charAt(0) + type.name().substring(1).toLowerCase());
        
        policiesGroup.setLabel("Policies");
        policiesGroup.setItems(policyRepository.findAll());
        policiesGroup.setItemLabelGenerator(Policy::getName);
        
        formLayout.add(usernameField, passwordField, confirmPasswordField, userTypeSelect, policiesGroup);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        
        add(formLayout, createButtonLayout());
    }

    private HorizontalLayout createButtonLayout() {
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        
        saveButton.addClickListener(event -> validateAndSave());
        cancelButton.addClickListener(event -> close());
        
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setSpacing(true);
        buttonLayout.setPadding(true);
        
        return buttonLayout;
    }

    private void configureBinder() {
        binder.forField(usernameField)
                .withValidator(new StringLengthValidator("Username is required", 1, 50))
                .bind(UserFormData::getUsername, UserFormData::setUsername);

        binder.forField(passwordField)
                .withValidator(password -> originalUser != null || !password.isEmpty(), 
                             "Password is required for new users")
                .withValidator(password -> password.isEmpty() || password.length() >= 6,
                             "Password must be at least 6 characters")
                .bind(UserFormData::getPassword, UserFormData::setPassword);

        binder.forField(confirmPasswordField)
                .withValidator(this::passwordsMatch, "Passwords must match")
                .bind(UserFormData::getConfirmPassword, UserFormData::setConfirmPassword);

        binder.forField(userTypeSelect)
                .bind(UserFormData::getUserType, UserFormData::setUserType);

        binder.forField(policiesGroup)
                .bind(UserFormData::getPolicies, UserFormData::setPolicies);
    }

    private boolean passwordsMatch(String confirmPassword) {
        String password = passwordField.getValue();
        return password.equals(confirmPassword);
    }

    private void populateForm() {
        binder.readBean(formData);
    }

    private void validateAndSave() {
        try {
            binder.writeBean(formData);
            
            Identity user;
            if (originalUser != null) {
                user = originalUser;
                user.setName(formData.getUsername());
                user.setType(formData.getUserType());
                user.setPolicies(formData.getPolicies());
                // Only update password if provided
                if (!formData.getPassword().isEmpty()) {
                    // Note: Password will be encoded in the service layer
                }
            } else {
                user = new Identity();
                user.setName(formData.getUsername());
                user.setType(formData.getUserType());
                user.setPolicies(formData.getPolicies());
                // Note: Password will be encoded in the service layer
            }
            
            fireEvent(new SaveEvent(this, user));
            close();
        } catch (ValidationException e) {
            Notification.show("Please fix validation errors").addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }

    // Helper class for form binding
    public static class UserFormData {
        private String username = "";
        private String password = "";
        private String confirmPassword = "";
        private Identity.IdentityType userType = Identity.IdentityType.USER;
        private Set<Policy> policies = Set.of();

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

        public Identity.IdentityType getUserType() { return userType; }
        public void setUserType(Identity.IdentityType userType) { this.userType = userType; }

        public Set<Policy> getPolicies() { return policies; }
        public void setPolicies(Set<Policy> policies) { this.policies = policies; }
    }
}
