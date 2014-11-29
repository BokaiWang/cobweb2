package driver.config;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableColumnModel;

import cwcore.complexParams.ComplexFoodParams;


public class ResourceConfigPage implements ConfigPage {

	private MixedValueJTable resourceParamTable;

	JPanel myPanel = new JPanel();

	public ResourceConfigPage(ComplexFoodParams[] params) {
		resourceParamTable = new MixedValueJTable();
		resourceParamTable.setModel(new ConfigTableModel(params, "Food "));

		TableColumnModel colModel = resourceParamTable.getColumnModel();
		colModel.getColumn(0).setPreferredWidth(120);

		GUI.colorHeaders(resourceParamTable, true);

		JScrollPane resourceScroll = new JScrollPane(resourceParamTable);
		myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.X_AXIS));
		GUI.makeGroupPanel(myPanel, "Resource Parameters");
		myPanel.add(resourceScroll);
	}

	public JPanel getPanel() {
		return myPanel;
	}

	public void validateUI() throws IllegalArgumentException {
		GUI.updateTable(resourceParamTable);
	}

}
