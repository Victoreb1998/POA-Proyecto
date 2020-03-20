package es.um.poa.guis;

import javax.swing.JFrame;
import javax.swing.JPanel;

import java.awt.GridLayout;
import java.awt.Toolkit;

import javax.swing.JLabel;
import javax.swing.JTextField;

import es.um.poa.agents.buyer.BuyerAgent;

import java.awt.Button;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class FishBuyerGui extends JFrame {
	private JTextField nuevoSaldo;
	private JTextField nombrePescado;
	private BuyerAgent myAgent;
	private static final long serialVersionUID = 1L;
	
	public FishBuyerGui(BuyerAgent fishBuyerAgent) {
		myAgent = fishBuyerAgent;
		
		getContentPane().setLayout(new GridLayout(1, 0, 0, 0));
		
		JPanel panel = new JPanel();
		getContentPane().add(panel);
		
		JLabel lblNombreDelPescado = new JLabel("Nombre del Pescado:");
		panel.add(lblNombreDelPescado);
		
		nombrePescado = new JTextField();
		panel.add(nombrePescado);
		nombrePescado.setColumns(10);
		
		Button intentarComprar = new Button("Intentar comprar");
		intentarComprar.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				myAgent.anadirPez(nombrePescado.getText());
			}
		});
		panel.add(intentarComprar);
		
		JPanel panel_1 = new JPanel();
		getContentPane().add(panel_1);
		
		JLabel lblAadirSaldo = new JLabel("A\u00F1adir saldo:");
		panel_1.add(lblAadirSaldo);
		
		nuevoSaldo = new JTextField();
		panel_1.add(nuevoSaldo);
		nuevoSaldo.setColumns(10);
		
		Button anadirSaldo = new Button("A\u00F1adir saldo");
		anadirSaldo.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				//se añade saldo al comprador
				myAgent.anadirSaldo(Double.parseDouble(nuevoSaldo.getText().trim()));
			}
		});
		panel_1.add(anadirSaldo);
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
