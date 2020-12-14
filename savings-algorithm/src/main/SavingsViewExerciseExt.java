package main;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;

import lavesdk.algorithm.plugin.views.ExecutionTableView;
import lavesdk.algorithm.plugin.views.ViewHeaderBarExtension;
import lavesdk.gui.widgets.ExecutionTableItem;
import lavesdk.language.LanguageFile;

/**
 * Header bar extension to add and remove items from an {@link ExecutionTableView}.
 * <br><br>
 * The added items are editable by default.
 * 
 * @author jdornseifer
 * @version 1.0
 */
public class SavingsViewExerciseExt extends ViewHeaderBarExtension {
	
	/** the view */
	private final ExecutionTableView view;
	/** the icon of the add button */
	private final Icon addIcon;
	/** the icon of the remove button */
	private final Icon removeIcon;

	/**
	 * Creates a new header bar extension to add or remove item in an {@link ExecutionTableView}.
	 * 
	 * @param view the execution table view
	 * @param addIcon the icon of the add button
	 * @param removeIcon the icon of the remove button
	 * @param langFile the language file
	 * @param langID the language id
	 * @throws IllegalArgumentException
	 * <ul>
	 * 		<li>if view is null</li>
	 * </ul>
	 * @since 1.0
	 */
	public SavingsViewExerciseExt(final ExecutionTableView view, final Icon addIcon, final Icon removeIcon, final LanguageFile langFile, final String langID) throws IllegalArgumentException {
		super(view, true, langFile, langID);
		
		this.view = view;
		this.addIcon = addIcon;
		this.removeIcon = removeIcon;
	}
	
	@Override
	protected void createExtension() {
		final JButton addBtn = new JButton(addIcon);
		addBtn.setToolTipText(LanguageFile.getLabel(langFile, "EXERCISE_STEP2_BTN_ADD_TOOLTIP", langID, "Add new item"));
		addBtn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				final ExecutionTableItem item = new ExecutionTableItem();
				item.setEditable(true);
				item.setCellInputParser(0, new ExecutionTableItem.StringInputParser());
				item.setCellInputParser(1, new ExecutionTableItem.StringInputParser());
				item.setCellInputParser(2, new ExecutionTableItem.NumericInputParser());
				
				SavingsViewExerciseExt.this.view.add(item);
			}
		});
		addComponent(addBtn);
		
		final JButton removeBtn = new JButton(removeIcon);
		removeBtn.setToolTipText(LanguageFile.getLabel(langFile, "EXERCISE_STEP2_BTN_REMOVE_TOOLTIP", langID, "Remove last added item"));
		removeBtn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				SavingsViewExerciseExt.this.view.remove(SavingsViewExerciseExt.this.view.getLastItem());
			}
		});
		addComponent(removeBtn);
	}

}
