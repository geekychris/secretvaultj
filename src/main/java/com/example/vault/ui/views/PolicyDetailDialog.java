package com.example.vault.ui.views;

import com.example.vault.entity.Policy;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import java.time.format.DateTimeFormatter;

/**
 * Dialog for viewing policy details
 */
public class PolicyDetailDialog extends Dialog {

    private final Policy policy;

    public PolicyDetailDialog(Policy policy) {
        this.policy = policy;
        
        configureDialog();
        createContent();
    }

    private void configureDialog() {
        setModal(true);
        setDraggable(true);
        setResizable(true);
        setWidth("600px");
        setHeight("500px");
        
        setHeaderTitle("Policy: " + policy.getName());
        
        Button closeButton = new Button("Close", e -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        getFooter().add(closeButton);
    }

    private void createContent() {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);
        
        // Policy name
        TextField nameField = new TextField("Policy Name");
        nameField.setValue(policy.getName());
        nameField.setReadOnly(true);
        nameField.setWidth("100%");
        
        // Description
        TextField descriptionField = new TextField("Description");
        descriptionField.setValue(policy.getDescription() != null ? policy.getDescription() : "No description");
        descriptionField.setReadOnly(true);
        descriptionField.setWidth("100%");
        
        // Created date
        TextField createdField = new TextField("Created");
        if (policy.getCreatedAt() != null) {
            createdField.setValue(policy.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        createdField.setReadOnly(true);
        createdField.setWidth("100%");
        
        // Rules section
        H3 rulesHeader = new H3("Policy Rules");
        
        if (policy.getRules() != null && !policy.getRules().isEmpty()) {
            StringBuilder rulesText = new StringBuilder();
            for (String rule : policy.getRules()) {
                rulesText.append(rule).append("\n");
            }
            
            Pre rulesDisplay = new Pre(rulesText.toString().trim());
            rulesDisplay.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius)")
                .set("white-space", "pre-wrap")
                .set("font-family", "monospace")
                .set("font-size", "var(--lumo-font-size-s)");
                
            content.add(nameField, descriptionField, createdField, rulesHeader, rulesDisplay);
        } else {
            Span noRules = new Span("No rules defined");
            noRules.getStyle().set("color", "var(--lumo-secondary-text-color)");
            content.add(nameField, descriptionField, createdField, rulesHeader, noRules);
        }
        
        add(content);
    }
}
