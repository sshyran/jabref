package org.jabref.gui.mergeentries.newmergedialog;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.strings.StringUtil;

import com.tobiasdiez.easybind.EasyBind;

public class ThreeFieldValuesViewModel {
    public enum Selection {
        LEFT,
        RIGHT,
        /**
         * When the user types something into the merged field value and neither the left nor
         * right values match it, NONE is selected
         * */
        NONE
    }


    private final BooleanProperty isFieldsMerged = new SimpleBooleanProperty(Boolean.FALSE);

    private final ObjectProperty<Selection> selection = new SimpleObjectProperty<>();

    private final StringProperty leftFieldValue = new SimpleStringProperty("");
    private final StringProperty rightFieldValue = new SimpleStringProperty("");
    private final StringProperty mergedFieldValue = new SimpleStringProperty("");

    private final Field field;

    private final BibEntry leftEntry;

    private final BibEntry rightEntry;

    private final BooleanBinding hasEqualLeftAndRight;

    public ThreeFieldValuesViewModel(Field field, BibEntry leftEntry, BibEntry rightEntry) {
        this.field = field;
        this.leftEntry = leftEntry;
        this.rightEntry = rightEntry;

        if (field.equals(InternalField.TYPE_HEADER)) {
            setLeftFieldValue(leftEntry.getType().getDisplayName());
            setRightFieldValue(rightEntry.getType().getDisplayName());
        } else {
            setLeftFieldValue(leftEntry.getField(field).orElse(""));
            setRightFieldValue(rightEntry.getField(field).orElse(""));
        }

        EasyBind.listen(leftFieldValueProperty(), (obs, old, leftValue) -> leftEntry.setField(field, leftValue));
        EasyBind.listen(rightFieldValueProperty(), (obs, old, rightValue) -> rightEntry.setField(field, rightValue));

        hasEqualLeftAndRight = Bindings.createBooleanBinding(this::hasEqualLeftAndRightValues, leftFieldValueProperty(), rightFieldValueProperty());

        if (StringUtil.isNullOrEmpty(leftFieldValue.get())) {
            selectRightValue();
        } else {
            selectLeftValue();
        }

        EasyBind.subscribe(selectionProperty(), selection -> {
            switch (selection) {
                case LEFT -> mergedFieldValueProperty().bind(leftFieldValueProperty());
                case RIGHT -> mergedFieldValueProperty().bind(rightFieldValueProperty());
            }
        });

        EasyBind.subscribe(mergedFieldValueProperty(), mergedValue -> {
            if (mergedValue.equals(getLeftFieldValue())) {
                selectLeftValue();
            } else if (getMergedFieldValue().equals(getRightFieldValue())) {
                selectRightValue();
            } else {
                selectNone();
            }
        });

        EasyBind.subscribe(hasEqualLeftAndRightBinding(), this::setIsFieldsMerged);
    }

    public boolean hasEqualLeftAndRightValues() {
        return (!StringUtil.isNullOrEmpty(leftFieldValue.get()) &&
                !StringUtil.isNullOrEmpty(rightFieldValue.get()) &&
                leftFieldValue.get().equals(rightFieldValue.get()));
    }

    public void selectLeftValue() {
        setSelection(Selection.LEFT);
    }

    public void selectRightValue() {
        if (isIsFieldsMerged()) {
            selectLeftValue();
        } else {
            setSelection(Selection.RIGHT);
        }
    }

    public void selectNone() {
        setSelection(Selection.NONE);
    }

    public void setMergedFieldValue(String mergedFieldValue) {
        mergedFieldValueProperty().set(mergedFieldValue);
    }

    public StringProperty mergedFieldValueProperty() {
        return mergedFieldValue;
    }

    public String getMergedFieldValue() {
        return mergedFieldValue.get();
    }

    public void merge() {
        setIsFieldsMerged(true);
    }

    public BooleanBinding hasEqualLeftAndRightBinding() {
        return hasEqualLeftAndRight;
    }

    public ObjectProperty<Selection> selectionProperty() {
        return selection;
    }

    public void setSelection(Selection select) {
        selectionProperty().set(select);
    }

    public Selection getSelection() {
        return selectionProperty().get();
    }

    public boolean isIsFieldsMerged() {
        return isFieldsMerged.get();
    }

    public BooleanProperty isFieldsMergedProperty() {
        return isFieldsMerged;
    }

    public void setIsFieldsMerged(boolean isFieldsMerged) {
        this.isFieldsMerged.set(isFieldsMerged);
    }

    public String getLeftFieldValue() {
        return leftFieldValue.get();
    }

    public StringProperty leftFieldValueProperty() {
        return leftFieldValue;
    }

    public void setLeftFieldValue(String leftFieldValue) {
        this.leftFieldValue.set(leftFieldValue);
    }

    public String getRightFieldValue() {
        return rightFieldValue.get();
    }

    public StringProperty rightFieldValueProperty() {
        return rightFieldValue;
    }

    public void setRightFieldValue(String rightFieldValue) {
        this.rightFieldValue.set(rightFieldValue);
    }

    public Field getField() {
        return field;
    }

    public BibEntry getLeftEntry() {
        return leftEntry;
    }

    public BibEntry getRightEntry() {
        return rightEntry;
    }
}
