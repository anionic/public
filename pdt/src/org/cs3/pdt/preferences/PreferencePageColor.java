package org.cs3.pdt.preferences;

import org.cs3.pdt.PDTPlugin;
import org.cs3.pdt.internal.editors.PDTColors;
import org.cs3.pdt.ui.preferences.MyColorFieldEditor;
import org.cs3.pdt.ui.preferences.StructuredFieldEditorPreferencePage;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */

public class PreferencePageColor extends StructuredFieldEditorPreferencePage implements IWorkbenchPreferencePage {
	private ColorFieldEditor background;
	private ColorFieldEditor backgroundExtern;
	private ColorFieldEditor default_;
	private ColorFieldEditor string;
	private ColorFieldEditor comment;
	private ColorFieldEditor variable;
	private ColorFieldEditor undefined;
	private ColorFieldEditor keyword;
	private ColorFieldEditor dynamic;
	private ColorFieldEditor transparent;
	private ColorFieldEditor meta;

	
	public PreferencePageColor() {
		super(GRID);
		setPreferenceStore(PDTPlugin.getDefault().getPreferenceStore());
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common
	 * GUI blocks needed to manipulate various types of preferences. Each field
	 * editor knows how to save and restore itself.
	 */
	@Override
	public void createFieldEditors() {
		Group gBackground = new Group(getFieldEditorParent(), SWT.SHADOW_ETCHED_OUT);
		gBackground.setText("Background color");
		background = getColorFieldEditor(PDTColors.PREF_BACKGROUND, PDTColors.BACKGROUND_STRING, gBackground);
		backgroundExtern = getColorFieldEditor(PDTColors.PREF_BACKGROUND_EXTERNAL_FILES, PDTColors.BACKGROUND_EXTERN_STRING, gBackground);
		
		Group gHighlight = new Group(getFieldEditorParent(), SWT.SHADOW_ETCHED_OUT);
		gHighlight.setText("Font Color: Syntax Highlighting");
		string = getColorFieldEditor(PDTColors.PREF_STRING, PDTColors.STRING_STRING, gHighlight);
		comment = getColorFieldEditor(PDTColors.PREF_COMMENT, PDTColors.COMMENT_STRING, gHighlight);
		variable = getColorFieldEditor(PDTColors.PREF_VARIABLE, PDTColors.VARIABLE_STRING, gHighlight);
		default_ = getColorFieldEditor(PDTColors.PREF_DEFAULT, PDTColors.DEFAULT_STRING, gHighlight);
		
		Group gProps = new Group(getFieldEditorParent(), SWT.SHADOW_ETCHED_OUT);
		gProps.setText("Font Color: Predicate Properties");
		undefined = getColorFieldEditor(PDTColors.PREF_UNDEFINED, PDTColors.UNDEFINED_STRING, gProps);
		keyword = getColorFieldEditor(PDTColors.PREF_BUILTIN, PDTColors.BUILT_IN_STRING, gProps);
		dynamic = getColorFieldEditor(PDTColors.PREF_DYNAMIC, PDTColors.DYNAMIC_STRING, gProps);
		meta = getColorFieldEditor(PDTColors.PREF_META, PDTColors.META_PREDICATE_STRING, gProps);
		transparent = getColorFieldEditor(PDTColors.PREF_TRANSPARENT, PDTColors.MODULE_TRANSPARENT_STRING, gProps);
		
		addField(background);
		addField(backgroundExtern);
		addField(default_);
		addField(string);
		addField(comment);
		addField(variable);		
		addField(undefined);
		addField(keyword);
		addField(dynamic);
		addField(transparent);
		addField(meta);
		
		setColumsWithEqualWidth(gBackground);
		setColumsWithEqualWidth(gHighlight);
		setColumsWithEqualWidth(gProps);
	}
	
	private MyColorFieldEditor getColorFieldEditor(String name, String labelText, Composite parent) {
		MyColorFieldEditor editor = new MyColorFieldEditor(name, labelText, parent);
		Label labelControl = editor.getLabelControl();
		if (labelControl != null) {
			labelControl.setLayoutData(getGridData());
		}
		ColorSelector colorSelector = editor.getColorSelector();
		if (colorSelector != null) {
			colorSelector.getButton().setLayoutData(getGridData());
		}
		return editor;
	}

	private GridData getGridData() {
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.minimumWidth = SWT.DEFAULT;
		return gridData;
	}
	
	private void setColumsWithEqualWidth(Composite composite) {
		Layout layout = composite.getLayout();
		if (layout instanceof GridLayout) {
			GridLayout gridLayout = (GridLayout) layout;
			gridLayout.makeColumnsEqualWidth = true;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	@Override
	public void init(IWorkbench workbench) {
	}
	
}