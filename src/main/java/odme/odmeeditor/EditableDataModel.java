package odme.odmeeditor;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/*
 * this class exists purely to let the table check for lower and upper bound
 * values exceeding. an object of this class gets assigned to the table in ODD Manager
 * and the table uses that object when trying to change it's values
 * 
 * methods "isCellEditable" and "setValueAt" are the ones that exist in DefaultTableModel 
 * and if you want to set values in the corresponding table you need to replace them with 
 * your own implementation (Override them)
 * */
public class EditableDataModel extends DefaultTableModel{
	
	private static final long serialVersionUID = 1L;

	public EditableDataModel(String[] nodeheaders, int i) {
		super(nodeheaders,i);
	}

	@Override 
	public boolean isCellEditable(int row,int column) {
		String val=(String)super.getValueAt(row, column);
		if(val!=null) val=val.trim();
		
		// either "lower and upper bound" , if they're not empty || or "comment", in any situation
		return ((column==3 || column==4) && val!=null && !val.isEmpty() && !val.isBlank() )|| column==5;
	}
	
	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		switch (columnIndex) {
			case 0:case 1: case 2: case 5:
				super.setValueAt(value, rowIndex, columnIndex);
			break;
			case 3:
				handleLowerBound((String)value, rowIndex);
			break;
			case 4:
				handleUpperBound((String)value, rowIndex);			
			break;
			default:break;
		}
	}
	
	private void handleUpperBound(String value,int rowIndex) {
		float upperBound;
		float newVal;
		
		// read User input (in table cell) and try to convert it to number or 
		// show an error
		try {
			upperBound=Float.parseFloat((String)super.getValueAt(rowIndex,4));
			newVal=Float.parseFloat(value);
		}catch(NumberFormatException nfe) {
			JOptionPane.showMessageDialog(
			null, "Wrong Number Format","Error"
			,JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		// check bound exceeding and ask user about it
		if(upperBound<newVal &&
			JOptionPane.showConfirmDialog(
			null, "Upper Bound Exceeded, proceed?",null
			,JOptionPane.YES_NO_OPTION)==JOptionPane.NO_OPTION) {
			return;
		}
		
		// finally set the value if there's no problem
		super.setValueAt(value, rowIndex, 4); // move the upper bound
		
	}
	
	private void handleLowerBound(String value,int rowIndex) throws NumberFormatException {
		float lowerBound;
		float newVal;
		
		// read User input (in table cell) and try to convert it to number or 
		// show an error
		try {
			lowerBound=Float.parseFloat((String)super.getValueAt(rowIndex,3));
			newVal=Float.parseFloat(value);
		}catch(NumberFormatException nfe) {
			JOptionPane.showMessageDialog(
			null, "Wrong Number Format","Error"
			,JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		// check bound exceeding and ask user about it
		if(lowerBound>newVal &&
			JOptionPane.showConfirmDialog(
			null, "Lower Bound Preceeded, proceed?",null
			,JOptionPane.YES_NO_OPTION)==JOptionPane.NO_OPTION) {
			 return;
		}
		
		// finally set the value if there's no problem
		super.setValueAt(value, rowIndex, 3);
		
	}
	
	
}