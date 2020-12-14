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
 * Header bar extension to import selected items from an {@link ExecutionTableView} to an {@link ExecutionTableView}.
 * 
 * @author jdornseifer
 * @version 1.0
 */
public class ListViewExerciseExt extends ViewHeaderBarExtension {
	
	/** the view */
	private final ExecutionTableView view;
	/** the import view */
	private final ExecutionTableView importView;
	/** the icon of the import button */
	private final Icon importIcon;

	/**
	 * Creates a new header bar extension to import selected items from one {@link ExecutionTableView} to another.
	 * 
	 * @param view the execution table view
	 * @param importIcon the icon of the import button
	 * @param langFile the language file
	 * @param langID the language id
	 * @throws IllegalArgumentException
	 * <ul>
	 * 		<li>if view is null</li>
	 * </ul>
	 * @since 1.0
	 */
	public ListViewExerciseExt(final ExecutionTableView view, final ExecutionTableView importView, final Icon importIcon, final LanguageFile langFile, final String langID) throws IllegalArgumentException {
		super(view, true, langFile, langID);
		
		this.view = view;
		this.importView = importView;
		this.importIcon = importIcon;
	}
	
	@Override
	protected void createExtension() {
		final JButton addBtn = new JButton(importIcon);
		addBtn.setToolTipText(LanguageFile.getLabel(langFile, "EXERCISE_STEP3_BTN_IMPORT_TOOLTIP", langID, "Import selected items from the Savings view"));
		addBtn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				ExecutionTableItem importItem;
				ExecutionTableItem newItem;
				VertexPair vp;
				
				ListViewExerciseExt.this.view.removeAllItems();
				
				for(int i = 0; i < ListViewExerciseExt.this.importView.getSelectedItemCount(); i++) {
					importItem = ListViewExerciseExt.this.importView.getSelectedItem(i);
					vp = (VertexPair)importItem.getUserData();
					newItem = new ExecutionTableItem(new Object[] { importItem.getCellObject(0), importItem.getCellObject(1), importItem.getCellObject(2) }, vp.id);
					newItem.setUserData(vp);
					ListViewExerciseExt.this.view.add(newItem);
				}
			}
		});
		addComponent(addBtn);
	}

}
