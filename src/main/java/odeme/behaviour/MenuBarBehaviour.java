package odeme.behaviour;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.TreePath;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxUndoManager;
import com.mxgraph.util.svg.ParseException;

import odme.jtreetograph.JtreeToGraphConvert;
import odme.jtreetograph.JtreeToGraphGeneral;
import odme.jtreetograph.JtreeToGraphModify;
import odme.jtreetograph.JtreeToGraphSave;
import odme.jtreetograph.JtreeToGraphVariables;

import static odme.odmeeditor.XmlUtils.sesview;



public class MenuBarBehaviour {

	private JMenuBar menuBar;

	public MenuBarBehaviour(JFrame frame) {
		menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
	}

	public void show() {
		// File Menu
		final String[] items_file =  {"Save"       , "Save As"    , "Save as PNG" , null, "Exit"       };
		final int[] keyevents_file = {KeyEvent.VK_S, KeyEvent.VK_A, 0             , 0   , KeyEvent.VK_X};
		final String[] keys_file =   {"control S"  ,"control A"   , null          , null, "control X"  };
		final String[] images_file = {"save_icon"  , "save_icon"  , "png_icon"    , null, "exit_icon"  };

		addMenu("File", KeyEvent.VK_F, items_file, keyevents_file, keys_file, images_file);

	}

	private void addMenu(String name, int key_event, String[] items, int[] keyevents, String[] keys, String[] images) {

		JMenu menu = new JMenu(name);
		menu.setMnemonic(key_event);
		menu.setBorder( new EmptyBorder(10,20,10,20));

		menuBar.add(menu);
	}
}    
