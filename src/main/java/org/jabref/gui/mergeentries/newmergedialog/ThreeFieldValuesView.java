package org.jabref.gui.mergeentries.newmergedialog;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.undo.CompoundEdit;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.mergeentries.newmergedialog.cell.FieldNameCell;
import org.jabref.gui.mergeentries.newmergedialog.cell.FieldNameCellFactory;
import org.jabref.gui.mergeentries.newmergedialog.cell.FieldValueCell;
import org.jabref.gui.mergeentries.newmergedialog.cell.MergeableFieldCell;
import org.jabref.gui.mergeentries.newmergedialog.cell.MergedFieldCell;
import org.jabref.gui.mergeentries.newmergedialog.diffhighlighter.SplitDiffHighlighter;
import org.jabref.gui.mergeentries.newmergedialog.diffhighlighter.UnifiedDiffHighlighter;
import org.jabref.gui.mergeentries.newmergedialog.toolbar.ThreeWayMergeToolbar;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.strings.StringUtil;

import com.tobiasdiez.easybind.EasyBind;
import org.fxmisc.richtext.StyleClassedTextArea;

import static org.jabref.gui.mergeentries.newmergedialog.ThreeFieldValuesViewModel.Selection;

/**
 * A controller class to control left, right and merged field values
 */
public class ThreeFieldValuesView {
    private final FieldNameCell fieldNameCell;
    private final FieldValueCell leftValueCell;
    private FieldValueCell rightValueCell;
    private final MergedFieldCell mergedValueCell;

    private final ToggleGroup toggleGroup = new ToggleGroup();

    private final ThreeFieldValuesViewModel viewModel;

    private final CompoundEdit fieldsMergedEdit = new CompoundEdit();

    public ThreeFieldValuesView(Field field, BibEntry leftEntry, BibEntry rightEntry, int rowIndex) {
        viewModel = new ThreeFieldValuesViewModel(field, leftEntry, rightEntry);

        fieldNameCell = FieldNameCellFactory.create(field, rowIndex);
        leftValueCell = new FieldValueCell(viewModel.getLeftFieldValue(), rowIndex);
        rightValueCell = new FieldValueCell(viewModel.getRightFieldValue(), rowIndex);
        mergedValueCell = new MergedFieldCell(viewModel.getMergedFieldValue(), rowIndex);

        if (FieldNameCellFactory.isMergeableField(field)) {
            MergeableFieldCell mergeableFieldCell = (MergeableFieldCell) fieldNameCell;
            mergeableFieldCell.setMergeCommand(new MergeCommand(mergeableFieldCell));
            mergeableFieldCell.setUnmergeCommand(new UnmergeCommand(mergeableFieldCell));
            mergeableFieldCell.setMergeAction(MergeableFieldCell.MergeAction.MERGE);
        }

        toggleGroup.getToggles().addAll(leftValueCell, rightValueCell);

        mergedValueCell.textProperty().bindBidirectional(viewModel.mergedFieldValueProperty());
        leftValueCell.textProperty().bindBidirectional(viewModel.leftFieldValueProperty());
        rightValueCell.textProperty().bindBidirectional(viewModel.rightFieldValueProperty());

        EasyBind.subscribe(viewModel.selectionProperty(), selection -> {
            if (selection == Selection.LEFT) {
                toggleGroup.selectToggle(leftValueCell);
            } else if (selection == Selection.RIGHT) {
                toggleGroup.selectToggle(rightValueCell);
            } else if (selection == Selection.NONE) {
                toggleGroup.selectToggle(null);
            }
        });

        EasyBind.subscribe(toggleGroup.selectedToggleProperty(), selectedToggle -> {
            if (selectedToggle == leftValueCell) {
                selectLeftValue();
            } else if (selectedToggle == rightValueCell) {
                selectRightValue();
            } else {
                selectNone();
            }
        });

        // Hide rightValueCell and extend leftValueCell to 2 columns when fields are merged
        EasyBind.subscribe(viewModel.isFieldsMergedProperty(), isFieldsMerged -> {
            if (isFieldsMerged) {
                rightValueCell.setVisible(false);
                GridPane.setColumnSpan(leftValueCell, 2);
            } else {
                rightValueCell.setVisible(true);
                GridPane.setColumnSpan(leftValueCell, 1);
            }
        });
    }

    public void selectLeftValue() {
        viewModel.selectLeftValue();
    }

    public void selectRightValue() {
        viewModel.selectRightValue();
    }

    public void selectNone() {
        viewModel.selectNone();
    }

    public String getMergedValue() {
        return mergedValueProperty().getValue();
    }

    public ReadOnlyStringProperty mergedValueProperty() {
        return viewModel.mergedFieldValueProperty();
    }

    public FieldNameCell getFieldNameCell() {
        return fieldNameCell;
    }

    public FieldValueCell getLeftValueCell() {
        return leftValueCell;
    }

    public FieldValueCell getRightValueCell() {
        return rightValueCell;
    }

    public MergedFieldCell getMergedValueCell() {
        return mergedValueCell;
    }

    public boolean hasEqualLeftAndRightValues() {
        return viewModel.hasEqualLeftAndRightValues();
    }

    public void showDiff(ShowDiffConfig diffConfig) {
        if (isRightValueCellHidden()) {
            return;
        }

        StyleClassedTextArea leftLabel = leftValueCell.getStyleClassedLabel();
        StyleClassedTextArea rightLabel = rightValueCell.getStyleClassedLabel();
        // Clearing old diff styles based on previous diffConfig
        hideDiff();
        if (diffConfig.diffView() == ThreeWayMergeToolbar.DiffView.UNIFIED) {
            new UnifiedDiffHighlighter(leftLabel, rightLabel, diffConfig.diffHighlightingMethod()).highlight();
        } else {
            new SplitDiffHighlighter(leftLabel, rightLabel, diffConfig.diffHighlightingMethod()).highlight();
        }
    }

    public void hideDiff() {
        if (isRightValueCellHidden()) {
            return;
        }

        int leftValueLength = getLeftValueCell().getStyleClassedLabel().getLength();
        getLeftValueCell().getStyleClassedLabel().clearStyle(0, leftValueLength);
        getLeftValueCell().getStyleClassedLabel().replaceText(viewModel.getLeftFieldValue());

        int rightValueLength = getRightValueCell().getStyleClassedLabel().getLength();
        getRightValueCell().getStyleClassedLabel().clearStyle(0, rightValueLength);
        getRightValueCell().getStyleClassedLabel().replaceText(viewModel.getRightFieldValue());
    }

    private boolean isRightValueCellHidden() {
        return rightValueCell == null;
    }

    private ObjectProperty<ThreeFieldValuesViewModel.Selection> selectionProperty() {
        return viewModel.selectionProperty();
    }

    public class MergeCommand extends SimpleCommand {
        private final MergeableFieldCell groupsFieldNameCell;

        public MergeCommand(MergeableFieldCell groupsFieldCell) {
            this.groupsFieldNameCell = groupsFieldCell;

            this.executable.bind(Bindings.createBooleanBinding(() -> {
                String leftEntryGroups = viewModel.getLeftEntry().getField(viewModel.getField()).orElse("");
                String rightEntryGroups = viewModel.getRightEntry().getField(viewModel.getField()).orElse("");

                return !leftEntryGroups.equals(rightEntryGroups);
            }));
        }

        @Override
        public void execute() {
            BibEntry leftEntry = viewModel.getLeftEntry();
            BibEntry rightEntry = viewModel.getRightEntry();

            String leftEntryGroups = leftEntry.getField(viewModel.getField()).orElse("");
            String rightEntryGroups = rightEntry.getField(viewModel.getField()).orElse("");

            assert !leftEntryGroups.equals(rightEntryGroups);

            String mergedGroups = mergeLeftAndRightEntryGroups(leftEntryGroups, rightEntryGroups);
            viewModel.getLeftEntry().setField(viewModel.getField(), mergedGroups);
            viewModel.getRightEntry().setField(viewModel.getField(), mergedGroups);

            if (fieldsMergedEdit.canRedo()) {
                fieldsMergedEdit.redo();
            } else {
                fieldsMergedEdit.addEdit(new UndoableFieldChange(leftEntry, viewModel.getField(), leftEntryGroups, mergedGroups));
                fieldsMergedEdit.addEdit(new UndoableFieldChange(rightEntry, viewModel.getField(), rightEntryGroups, mergedGroups));
                fieldsMergedEdit.end();
            }

            groupsFieldNameCell.setMergeAction(MergeableFieldCell.MergeAction.UNMERGE);
            viewModel.setIsFieldsMerged(true);
        }

        private String mergeLeftAndRightEntryGroups(String left, String right) {
            if (StringUtil.isBlank(left)) {
                return right;
            } else if (StringUtil.isBlank(right)) {
                return left;
            } else {
                Set<String> leftGroups = new HashSet<>(Arrays.stream(left.split(", ")).toList());
                List<String> rightGroups = Arrays.stream(right.split(", ")).toList();
                leftGroups.addAll(rightGroups);

                return String.join(", ", leftGroups);
            }
        }
    }

    public class UnmergeCommand extends SimpleCommand {
        private final MergeableFieldCell groupsFieldCell;

        public UnmergeCommand(MergeableFieldCell groupsFieldCell) {
            this.groupsFieldCell = groupsFieldCell;
        }

        @Override
        public void execute() {
            if (fieldsMergedEdit.canUndo()) {
                fieldsMergedEdit.undo();
                groupsFieldCell.setMergeAction(MergeableFieldCell.MergeAction.MERGE);
                viewModel.setIsFieldsMerged(false);
            }
        }
    }
}
