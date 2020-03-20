package es.um.poa.agents.seller;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

import org.yaml.snakeyaml.Yaml;

import es.um.poa.agents.POAAgent;
import es.um.poa.guis.FishSellerGui;
import es.um.poa.modelo.Precio;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class SellerAgent extends POAAgent  {
		
	private static final long serialVersionUID = 1L;
	// duda precio minimo
	private HashMap<String, Precio> catalogue;
	private FishSellerGui myGui;
	private AID LonjaAgent;
	private int dinero = 0;

	public void setup() {
		super.setup();
		
		Object[] args = getArguments();
		if (args != null && args.length == 1) {
			String configFile = (String) args[0];
			SellerAgentConfig config = initAgentFromConfigFile(configFile);
			
			if(config != null) {
				catalogue = new HashMap<String, Precio>();

				// Registrar el servicio de venta de libros en las paginas amarillas
				DFAgentDescription dfd = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("lonja");
				dfd.addServices(sd);
				try {
					DFAgentDescription[] result = DFService.search(this, dfd);
					LonjaAgent = result[0].getName();

				} catch (FIPAException fe) {
					fe.printStackTrace();
				}

				SequentialBehaviour seq = new SequentialBehaviour();
				seq.addSubBehaviour(new OneShotBehaviour() {

					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void action() {
						// le enviamos un mensaje de tipo subscribe para que la lonja
						// si cree que el vendedorr es valido se comprometa a informar
						// cuando se haya vendido un pescado suyo
						ACLMessage identificacion = new ACLMessage(ACLMessage.SUBSCRIBE);
						identificacion.addReceiver(LonjaAgent);
						// le envia su nombre
						identificacion.setContent(getName());
						myAgent.send(identificacion);

					}
				});
				//protocolo para enviar capturas de pescados, deber�a ser un cyclic si tenemos varios barcos, 
				//le he enviado una duda al profesor, no hacer a�n
				seq.addSubBehaviour(new OneShotBehaviour() {

					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void action() {

						// no se si es subscribe
						ACLMessage identificacion = new ACLMessage(ACLMessage.SUBSCRIBE);
						identificacion.addReceiver(LonjaAgent);
						// le envia su nombre
						identificacion.setContent(getName());
						myAgent.send(identificacion);

					}
				});
				//protocolo para recepci�n de pago. El vendedor recibir� un inform dado que antes le ha enviado un subscribe a la lonja
				//Despues extraer� el dinero que tiene que venir en el inform y lo sumar� a su cuenta
				//solo se debe pagar al vendedor cuando lo decida la lonja
				//Es decir podemos ir pagando cada vez que se venda un pescado o directamente cuando termine la subasta
				//En las diapositivas pone que podemos ignorar intentos de pago para que el vendedor decida cuando vaciar su cuenta pero no le veo mucho
				//sentido, ser�a mejor vaciarla al final. Pero si quieres simplemente seria poner un if, poner el dinero a 0 y sumar el pago despues
				seq.addSubBehaviour(new CyclicBehaviour() {
					
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void action() {
						MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
						ACLMessage msg = myAgent.receive(mt);
						if (msg != null) {
							//Algo asi seria lo de ignorar para retirar dinero, o eso entiendo yo, si no simplemente ser�a que la lonja avise al terminar 
							//y retiramos el dinero
							/*if(dinero>9999) {
								dinero=0;
							}*/
							int price = Integer.parseInt(msg.getContent());
							dinero+=price;
						}
						
					}
				});

				addBehaviour(seq);

				// Estos casi seguro que sobran, dado que el vendedor no se ofrece directamente a los compradores, sino a la lonja
				//addBehaviour(new OfferRequestsServer());
				//addBehaviour(new PurchaseOrdersServer());

				// Creo y muestro la GUI
				myGui = new FishSellerGui(this);
				myGui.showGui();
				
			} else {
				doDelete();
			}
		} else {
			getLogger().info("ERROR", "Requiere fichero de cofiguración.");
			doDelete();
		}
	}
	
	private SellerAgentConfig initAgentFromConfigFile(String fileName) {
		SellerAgentConfig config = null;
		try {
			Yaml yaml = new Yaml();
			InputStream inputStream;
			inputStream = new FileInputStream(fileName);
			config = yaml.load(inputStream);
			getLogger().info("initAgentFromConfigFile", config.toString());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return config;
	}
	public void updateCatalogue(String name, Precio precio) {

		addBehaviour(new OneShotBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public void action() {
				catalogue.put(name, precio);

			}
		});
	}
}
