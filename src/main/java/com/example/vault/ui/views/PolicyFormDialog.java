package com.example.vault.ui.views;

import com.example.vault.entity.Policy;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.shared.Registration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dialog for creating and editing policies
 */
public class PolicyFormDialog extends Dialog {

    public static abstract class PolicyFormEvent extends ComponentEvent<PolicyFormDialog> {
        private Policy policy;

        protected PolicyFormEvent(PolicyFormDialog source, Policy policy) {
            super(source, false);
            this.policy = policy;
        }

        public Policy getPolicy() {
            return policy;
        }
    }

    public static class SaveEvent extends PolicyFormEvent {
        SaveEvent(PolicyFormDialog source, Policy policy) {
            super(source, policy);
        }
    }

    private final Binder<PolicyFormData> binder = new Binder<>(PolicyFormData.class);
    private PolicyFormData formData;
    private Policy originalPolicy;

    private TextField nameField = new TextField("Policy Name");
    private TextField descriptionField = new TextField("Description");
    private TextArea rulesField = new TextArea("Rules");
    
    private Button saveButton = new Button("Save");
    private Button cancelButton = new Button("Cancel");

    public PolicyFormDialog() {
        this(null);
    }

    public PolicyFormDialog(Policy policy) {
        this.originalPolicy = policy;
        this.formData = new PolicyFormData();
        
        if (policy != null) {
            populateFormData(policy);
        }
        
        configureDialog();
        configureForm();
        configureBinder();
        populateForm();
    }

    private void populateFormData(Policy policy) {
        formData.setName(policy.getName());
        formData.setDescription(policy.getDescription() != null ? policy.getDescription() : "");
        formData.setRules(policy.getRules() != null ? 
            String.join("\n", policy.getRules()) : "");
    }

    private void configureDialog() {
        setModal(true);
        setDraggable(true);
        setResizable(true);
        setWidth("700px");
        setHeight("600px");
        
        String title = originalPolicy != null ? "Edit Policy" : "New Policy";
        setHeaderTitle(title);
    }

    private void configureForm() {
        FormLayout formLayout = new FormLayout();
        
        nameField.setRequired(true);
        nameField.setPlaceholder("Enter policy name");
        nameField.setReadOnly(originalPolicy != null); // Don't allow editing policy name
        
        descriptionField.setPlaceholder("Enter policy description");
        descriptionField.setWidth("100%");
        
        rulesField.setPlaceholder("Enter rules, one per line (e.g., 'read:secret/*')");
        rulesField.setHeight("200px");
        rulesField.setWidth("100%");
        
        // Add help text for rules
        VerticalLayout rulesSection = new VerticalLayout();
        rulesSection.setPadding(false);
        rulesSection.setSpacing(false);
        
        H4 rulesHeader = new H4("Policy Rules");
        rulesHeader.getStyle().set("margin-top", "0");
        
        Span rulesHelp = new Span("Enter one rule per line. Rules format: action:resource");
        rulesHelp.getStyle().set("color", "var(--lumo-secondary-text-color)");
        rulesHelp.getStyle().set("font-size", "var(--lumo-font-size-s)");
        
        Span examplesHelp = new Span("Examples:");
        examplesHelp.getStyle().set("color", "var(--lumo-secondary-text-color)");
        examplesHelp.getStyle().set("font-size", "var(--lumo-font-size-s)");
        examplesHelp.getStyle().set("font-weight", "bold");
        
        Span example1 = new Span("• read:secret/* (read access to all secrets)");
        Span example2 = new Span("• create:secret/dev/* (create access to dev secrets)");
        Span example3 = new Span("• *:* (full access to everything)");
        
        example1.getStyle().set("color", "var(--lumo-secondary-text-color)");
        example1.getStyle().set("font-size", "var(--lumo-font-size-s)");
        example2.getStyle().set("color", "var(--lumo-secondary-text-color)");
        example2.getStyle().set("font-size", "var(--lumo-font-size-s)");
        example3.getStyle().set("color", "var(--lumo-secondary-text-color)");
        example3.getStyle().set("font-size", "var(--lumo-font-size-s)");
        
        rulesSection.add(rulesHeader, rulesHelp, examplesHelp, example1, example2, example3, rulesField);
        
        formLayout.add(nameField, descriptionField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        
        add(formLayout, rulesSection, createButtonLayout());
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
        binder.forField(nameField)
                .withValidator(new StringLengthValidator("Policy name is required", 1, 50))
                .bind(PolicyFormData::getName, PolicyFormData::setName);

        binder.forField(descriptionField)
                .bind(PolicyFormData::getDescription, PolicyFormData::setDescription);

        binder.forField(rulesField)
                .withValidator(rules -> !rules.trim().isEmpty(), "At least one rule is required")
                .withValidator(this::validateRules, "Invalid rule format")
                .bind(PolicyFormData::getRules, PolicyFormData::setRules);
    }

    private boolean validateRules(String rulesText) {
        if (rulesText == null || rulesText.trim().isEmpty()) {
            return false;
        }
        
        String[] rules = rulesText.split("\n");
        for (String rule : rules) {
            rule = rule.trim();
            if (rule.isEmpty()) continue;
            
            // Basic validation: should contain at least one colon
            if (!rule.contains(":")) {
                return false;
            }
            
            // Could add more sophisticated validation here
        }
        return true;
    }

    private void populateForm() {
        binder.readBean(formData);
    }

    private void validateAndSave() {
        try {
            binder.writeBean(formData);
            
            Policy policy;
            if (originalPolicy != null) {
                policy = originalPolicy;
                policy.setDescription(formData.getDescription());
                policy.setRules(parseRules(formData.getRules()));
            } else {
                policy = new Policy();
                policy.setName(formData.getName());
                policy.setDescription(formData.getDescription());
                policy.setRules(parseRules(formData.getRules()));
            }
            
            fireEvent(new SaveEvent(this, policy));
            close();
        } catch (ValidationException e) {
            Notification.show("Please fix validation errors").addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private Set<String> parseRules(String rulesText) {
        if (rulesText == null || rulesText.trim().isEmpty()) {
            return new HashSet<>();
        }
        
        return Arrays.stream(rulesText.split("\n"))
                .map(String::trim)
                .filter(rule -> !rule.isEmpty())
                .collect(Collectors.toSet());
    }

    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }

    // Helper class for form binding
    public static class PolicyFormData {
        private String name = "";
        private String description = "";
        private String rules = "";

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getRules() { return rules; }
        public void setRules(String rules) { this.rules = rules; }
    }
}
