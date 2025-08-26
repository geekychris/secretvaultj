package com.example.vault.ui.views;

import com.example.vault.service.SecretService;
import com.example.vault.ui.dto.SecretUI;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.shared.Registration;

import java.util.HashMap;
import java.util.Map;

/**
 * Dialog for creating and editing secrets
 */
public class SecretFormDialog extends Dialog {

    public static abstract class SecretFormEvent extends ComponentEvent<SecretFormDialog> {
        private SecretUI secret;

        protected SecretFormEvent(SecretFormDialog source, SecretUI secret) {
            super(source, false);
            this.secret = secret;
        }

        public SecretUI getSecret() {
            return secret;
        }
    }

    public static class SaveEvent extends SecretFormEvent {
        SaveEvent(SecretFormDialog source, SecretUI secret) {
            super(source, secret);
        }
    }

    private final SecretService secretService;
    private final Binder<SecretUI> binder = new Binder<>(SecretUI.class);
    private SecretUI secret;

    private TextField pathField = new TextField("Path");
    private TextField keyField = new TextField("Key");
    private PasswordField valueField = new PasswordField("Value");
    private TextArea metadataField = new TextArea("Metadata (JSON)");
    
    private Button saveButton = new Button("Save");
    private Button cancelButton = new Button("Cancel");

    public SecretFormDialog(SecretService secretService) {
        this(secretService, new SecretUI());
    }

    public SecretFormDialog(SecretService secretService, SecretUI secret) {
        this.secretService = secretService;
        this.secret = secret;
        
        configureDialog();
        configureForm();
        configureBinder();
        populateForm();
    }

    private void configureDialog() {
        setModal(true);
        setDraggable(true);
        setResizable(true);
        setWidth("600px");
        setHeight("500px");
        
        String title = secret.getPath() != null && !secret.getPath().isEmpty() ? "Edit Secret" : "New Secret";
        setHeaderTitle(title);
    }

    private void configureForm() {
        FormLayout formLayout = new FormLayout();
        
        pathField.setPlaceholder("e.g., app/config/database");
        pathField.setRequired(true);
        
        keyField.setPlaceholder("e.g., password");
        keyField.setRequired(true);
        
        valueField.setPlaceholder("Enter the secret value");
        valueField.setRequired(true);
        valueField.setRevealButtonVisible(true);
        
        metadataField.setPlaceholder("{ \"environment\": \"production\", \"owner\": \"team-alpha\" }");
        metadataField.setHeight("100px");
        
        formLayout.add(pathField, keyField, valueField, metadataField);
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
        binder.forField(pathField)
                .withValidator(new StringLengthValidator("Path is required", 1, null))
                .withValidator(path -> !path.startsWith("/"), "Path should not start with /")
                .withValidator(path -> !path.endsWith("/"), "Path should not end with /")
                .bind(SecretUI::getPath, SecretUI::setPath);
        
        binder.forField(keyField)
                .withValidator(new StringLengthValidator("Key is required", 1, null))
                .bind(SecretUI::getKey, SecretUI::setKey);
        
        binder.forField(valueField)
                .withValidator(new StringLengthValidator("Value is required", 1, null))
                .bind(SecretUI::getValue, SecretUI::setValue);

        binder.forField(metadataField)
                .withConverter(this::stringToMetadata, this::metadataToString)
                .bind(SecretUI::getMetadata, SecretUI::setMetadata);
    }

    private Map<String, Object> stringToMetadata(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            // Simple JSON parsing for demo - in production, use Jackson
            Map<String, Object> metadata = new HashMap<>();
            value = value.trim();
            if (value.startsWith("{") && value.endsWith("}")) {
                // Remove braces and parse key-value pairs
                String content = value.substring(1, value.length() - 1);
                if (!content.trim().isEmpty()) {
                    String[] pairs = content.split(",");
                    for (String pair : pairs) {
                        String[] keyValue = pair.split(":");
                        if (keyValue.length == 2) {
                            String key = keyValue[0].trim().replaceAll("\"", "");
                            String val = keyValue[1].trim().replaceAll("\"", "");
                            metadata.put(key, val);
                        }
                    }
                }
            }
            return metadata;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String metadataToString(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder("{ ");
        boolean first = true;
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append("\"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
            first = false;
        }
        sb.append(" }");
        return sb.toString();
    }

    private void populateForm() {
        binder.readBean(secret);
    }

    private void validateAndSave() {
        try {
            binder.writeBean(secret);
            fireEvent(new SaveEvent(this, secret));
            close();
        } catch (ValidationException e) {
            // Validation errors are already shown in the form
        }
    }

    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }
}
