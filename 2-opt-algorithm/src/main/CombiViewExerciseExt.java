package main;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;

import lavesdk.algorithm.plugin.views.ExecutionTableView;
import lavesdk.algorithm.plugin.views.ViewHeaderBarExtension;
import lavesdk.gui.widgets.ExecutionTableItem;
import lavesdk.language.LanguageFile;
import lavesdk.math.graph.Edge;
import lavesdk.math.graph.Graph;
import lavesdk.math.graph.Vertex;

/**
 * Header bar extension to add and remove items from an {@link ExecutionTableView}.
 * <br><br>
 * The added items are editable by default and use the {@link EdgePairInputParser}.
 * 
 * @author jdornseifer
 * @version 1.2
 */
public class CombiViewExerciseExt extends ViewHeaderBarExtension {
	
	/** the view */
	private final ExecutionTableView view;
	/** the index of the last element that can be removed from the table  */
	private final int removeLimit;
	/** the icon of the add button */
	private final Icon addIcon;
	/** the icon of the remove button */
	private final Icon removeIcon;
	/** the default input parser */
	private final EdgePairInputParser inputParser;

	/**
	 * Creates a new header bar extension to add or remove item in an {@link ExecutionTableView}.
	 * 
	 * @param view the execution table view
	 * @param graph the current graph
	 * @param addIcon the icon of the add button
	 * @param removeIcon the icon of the remove button
	 * @param langFile the language file
	 * @param langID the language id
	 * @throws IllegalArgumentException
	 * <ul>
	 * 		<li>if view is null</li>
	 * 		<li>if graph is null</li>
	 * </ul>
	 * @since 1.0
	 */
	public CombiViewExerciseExt(final ExecutionTableView view, final Graph<Vertex, Edge> graph, final Icon addIcon, final Icon removeIcon, final LanguageFile langFile, final String langID) throws IllegalArgumentException {
		super(view, true, langFile, langID);
		
		if(graph == null)
			throw new IllegalArgumentException("No valid argument!");
		
		this.view = view;
		this.removeLimit = view.getItemCount();
		this.addIcon = addIcon;
		this.removeIcon = removeIcon;
		this.inputParser = new EdgePairInputParser(view, graph, langFile, langID);
	}
	
	/**
	 * Gets the input parser of the cells.
	 * 
	 * @return {@link EdgePairInputParser}
	 * @since 1.2
	 */
	public EdgePairInputParser getInputParser() {
		return inputParser;
	}
	
	@Override
	protected void createExtension() {
		final JButton addBtn = new JButton(addIcon);
		addBtn.setToolTipText(LanguageFile.getLabel(langFile, "EXERCISE_STEP1_BTN_ADD_TOOLTIP", langID, "Add new item"));
		addBtn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				final ExecutionTableItem item = new ExecutionTableItem();
				item.setEditable(true);
				item.setDefaultInputParser(inputParser);
				
				CombiViewExerciseExt.this.view.add(item);
			}
		});
		addComponent(addBtn);
		
		final JButton removeBtn = new JButton(removeIcon);
		removeBtn.setToolTipText(LanguageFile.getLabel(langFile, "EXERCISE_STEP1_BTN_REMOVE_TOOLTIP", langID, "Remove last added item"));
		removeBtn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(CombiViewExerciseExt.this.view.getItemCount() > CombiViewExerciseExt.this.removeLimit)
					CombiViewExerciseExt.this.view.remove(CombiViewExerciseExt.this.view.getLastItem());
			}
		});
		addComponent(removeBtn);
	}

}
