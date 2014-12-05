package org.cobweb.cobweb2.ui.swing.config;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableColumnModel;

import org.cobweb.cobweb2.core.params.ComplexAgentParams;


public class AgentConfigPage implements ConfigPage {
	JPanel agentPanel;
	private MixedValueJTable agentParamTable;

	public AgentConfigPage(ComplexAgentParams[] params) {
		agentPanel = new JPanel();
		agentPanel.setLayout(new BoxLayout(agentPanel, BoxLayout.X_AXIS));
		GUI.makeGroupPanel(agentPanel, "Agent Parameters");

		agentParamTable = new MixedValueJTable();
		agentParamTable.setModel(new ConfigTableModel(params, "Agent "));

		TableColumnModel agParamColModel = agentParamTable.getColumnModel();
		// Get the column at index pColumn, and set its preferred width.
		agParamColModel.getColumn(0).setPreferredWidth(200);


		GUI.colorHeaders(agentParamTable, true);
		JScrollPane agentScroll = new JScrollPane(agentParamTable);
		// Add the scroll pane to this panel.
		agentPanel.add(agentScroll);
	}

	public JPanel getPanel() {
		return agentPanel;
	}

	public void validateUI() throws IllegalArgumentException {
		GUI.updateTable(agentParamTable);
	}

}
