package org.cobweb.cobweb2.ui.swing.config;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.cobweb.cobweb2.core.params.ComplexAgentParams;
import org.cobweb.cobweb2.core.params.FoodwebParams;


public class FoodwebConfigPage implements ConfigPage {

	private JPanel foodPanel;
	private MixedValueJTable foodTable;

	public FoodwebConfigPage(ComplexAgentParams[] params) {
		foodPanel = new JPanel();
		// tabbedPane.addTab("Agents", panel3);

		foodTable = new MixedValueJTable();

		FoodwebParams[] foodweb = new FoodwebParams[params.length];
		for (int i = 0; i < params.length; i++) {
			foodweb[i] = params[i].foodweb;
		}
		foodTable.setModel(new ConfigTableModel(foodweb, "Agent "));

		GUI.colorHeaders(foodTable, true);

		// Create the scroll pane and add the table to it.
		JScrollPane foodScroll = new JScrollPane(foodTable);

		foodPanel.setLayout(new BoxLayout(foodPanel, BoxLayout.X_AXIS));
		GUI.makeGroupPanel(foodPanel, "Food Parameters");
		foodPanel.add(foodScroll);
	}

	public JPanel getPanel() {
		return foodPanel;
	}

	public void validateUI() throws IllegalArgumentException {
		GUI.updateTable(foodTable);
	}

}
