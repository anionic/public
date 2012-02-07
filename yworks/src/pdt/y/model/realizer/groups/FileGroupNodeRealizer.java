package pdt.y.model.realizer.groups;

import java.awt.Color;
import java.awt.Graphics2D;

import pdt.y.model.GraphModel;
import pdt.y.preferences.AppearancePreferences;
import y.view.NodeLabel;
import y.view.NodeRealizer;


public class FileGroupNodeRealizer extends PrologGroupNodeRealizer {

	public FileGroupNodeRealizer(GraphModel model) {
		super(model);
	}

	public FileGroupNodeRealizer(NodeRealizer nr) {
		super(nr);
	}
	
	@Override
	public void paintText(Graphics2D gfx) {
		NodeLabel label = getLabel();
		String text = label.getText();
		if (!text.startsWith("...")) {
			int cutPoint = text.lastIndexOf('/');
			if (cutPoint != -1) {
				label.setText("..." + text.substring(cutPoint));
			}
		}
		super.paintText(gfx);
	}

	@Override
	protected void createHeaderLabel() {
		NodeLabel label = getLabel();
		label.setAlignment(NodeLabel.LEFT);
		label.setBackgroundColor(AppearancePreferences.getFileHeaderColor());
		label.setTextColor(Color.BLACK);
		label.setUnderlinedTextEnabled(true);
		label.setModel(NodeLabel.INTERNAL);
		//label.setConfiguration(LayoutPreferences.getNameCroppingConfiguration());
		
	}

	@Override
	public NodeRealizer createCopy(NodeRealizer nr) {
		return new FileGroupNodeRealizer(nr);
	}
}
