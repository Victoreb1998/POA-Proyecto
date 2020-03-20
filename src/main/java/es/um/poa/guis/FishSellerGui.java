/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop 
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
*****************************************************************/

package es.um.poa.guis;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


import es.um.poa.agents.seller.SellerAgent;
import es.um.poa.modelo.Precio;

/**
  @author Giovanni Caire - TILAB
 */
public class FishSellerGui extends JFrame {	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private SellerAgent myAgent;
	
	private JTextField fishName, precioSalida;
	private JTextField precioMinimo;
	
	public FishSellerGui(SellerAgent a) {
		super(a.getLocalName());
		
		myAgent = a;
		
		JPanel p = new JPanel();
		p.setLayout(new GridLayout(0, 1, 0, 0));
		JLabel label = new JLabel("Pescado:");
		p.add(label);
		fishName = new JTextField(15);
		p.add(fishName);
		p.add(new JLabel("Precio de salida:"));
		precioSalida = new JTextField(15);
		p.add(precioSalida);
		getContentPane().add(p, BorderLayout.CENTER);
		
		p.add(new JLabel("Precio minimo:"));
		
		precioMinimo = new JTextField();
		p.add(precioMinimo);
		precioMinimo.setColumns(10);
		
		JButton addButton = new JButton("Add");
		addButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					String name = fishName.getText().trim();
					String startingPrice = precioSalida.getText().trim();
					String minimunPrice = precioMinimo.getText().trim();
					myAgent.updateCatalogue(name, new Precio(Integer.parseInt(startingPrice), Integer.parseInt(minimunPrice)));
					fishName.setText("");
					precioSalida.setText("");
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(FishSellerGui.this, "Invalid values. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
				}
			}
		} );
		p = new JPanel();
		p.add(addButton);
		getContentPane().add(p, BorderLayout.SOUTH);
		
		// Make the agent terminate when the user closes 
		// the GUI using the button on the upper right corner	
		addWindowListener(new	WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				myAgent.doDelete();
			}
		} );
		
		setResizable(false);
	}
	
	public void showGui() {
		pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = (int)screenSize.getWidth() / 2;
		int centerY = (int)screenSize.getHeight() / 2;
		setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
		super.setVisible(true);
	}	
}
