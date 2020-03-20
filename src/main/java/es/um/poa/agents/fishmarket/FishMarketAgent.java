package es.um.poa.agents.fishmarket;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;

import org.yaml.snakeyaml.Yaml;

import es.um.poa.agents.POAAgent;
import es.um.poa.protocols.addbuyer.AddBuyerProtocolResponder;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class FishMarketAgent extends POAAgent {

	private static final long serialVersionUID = 1L;
	private double dineroMinimo = 3000;
	// Lista con los AID de los compradores
	private HashMap<AID, Double> compradoresAID;
	private LinkedList<AID> vendedoresAID;

	public boolean performActionAddBuyerProtocol(AID sender) {

		// Hacer las Acciones correspondientes.
		return true;
	}

	public boolean checkActionAddBuyerProtocol(AID sender) {

		// Hacer las Acciones correspondientes.
		return true;
	}

	public void setup() {
		super.setup();
		Object[] args = getArguments();
		String configFile = (String) args[0];
		FishMarketAgentConfig config = initAgentFromConfigFile(configFile);

		if (args != null && args.length == 1) {

			if (config != null) {

				// Crear los comportamientos correspondientes
				/*
				 * MessageTemplate messageTemplate = null; // Completa con el protocolo FIPA
				 * correspondiente y el mensajes correspondiente //MessageTemplate.and(
				 * //MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.<>),
				 * //MessageTemplate.MatchPerformative(ACLMessage.<>) //);
				 * 
				 * 
				 * 
				 * MessageTemplate templateAddBuyerProtocol = MessageTemplate.and(
				 * messageTemplate, MessageTemplate.MatchConversationId("AddBuyerProtocol"));
				 * 
				 * // AÃ±adimos el protocolo de adicion del comprador. addBehaviour(new
				 * AddBuyerProtocolResponder(this,templateAddBuyerProtocol));
				 * this.getLogger().info("INFO", "AddBuyerProtocol sucessfully added");
				 */
				compradoresAID = new HashMap<AID, Double>();
				vendedoresAID = new LinkedList<AID>();
				// Registrar el servicio en las paginas amarillas
				DFAgentDescription dfd = new DFAgentDescription();
				dfd.setName(getAID());

				ServiceDescription sd = new ServiceDescription();
				sd.setType("lonja");
				sd.setName(getLocalName());

				dfd.addServices(sd);
				try {
					DFService.register(this, dfd);
				} catch (FIPAException fe) {
					fe.printStackTrace();
				}
				// TODO añadir comportamientos
				addBehaviour(new DescubrirComprador());
				addBehaviour(new ComprobarComprador());
				addBehaviour(new DescubirVendedor());

			} else {
				doDelete();
			}
		} else {
			this.getLogger().info("ERROR", "Requiere fichero de configuraciÃ³n.");
			doDelete();
		}
	}

	private FishMarketAgentConfig initAgentFromConfigFile(String fileName) {
		FishMarketAgentConfig config = null;
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

	private class DescubrirComprador extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE);
			/*
			 * Recibe un mensaje de ACL que coincide con una plantilla determinada. Este
			 * método no bloquea y devuelve el primer mensaje coincidente en la cola, si lo
			 * hay.
			 */
			ACLMessage msg = myAgent.receive(mt);

			if (msg != null) {
				compradoresAID.put(msg.getSender(), new Double(0));
				ACLMessage reply = msg.createReply();

				reply.setPerformative(ACLMessage.AGREE);
				reply.setConversationId("registrar-comprador");
				myAgent.send(reply);
			} else {
				System.err.println("El mensaje de registro del comprador es nulo");

			}

		}

	}

	private class ComprobarComprador extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = myAgent.receive(mt);

			if (msg != null) {
				ACLMessage reply = msg.createReply();
				// si esta registrado en los compradores
				if (compradoresAID.containsKey(msg.getSender())) {
					reply.setPerformative(ACLMessage.AGREE);
					myAgent.send(reply);

					Double credito = Double.valueOf(msg.getContent());
					// si la liquidez del comprador es mayor a la minima
					if (credito >= dineroMinimo) {
						compradoresAID.put(msg.getSender(), credito);
						reply.setPerformative(ACLMessage.INFORM);
					} else {
						reply.setPerformative(ACLMessage.FAILURE);
					}
					myAgent.send(reply);
				} else {
					reply.setPerformative(ACLMessage.REFUSE);
					myAgent.send(reply);
				}

			} else {
				System.err.println("El mensaje del dinero del registro del comprador es nulo");

			}
		}

	}

	public class DescubirVendedor extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE);
			/*
			 * Recibe un mensaje de ACL que coincide con una plantilla determinada. Este
			 * método no bloquea y devuelve el primer mensaje coincidente en la cla, si lo
			 * hay.
			 */
			ACLMessage msg = myAgent.receive(mt);

			if (msg != null) {
				vendedoresAID.add(msg.getSender());
				ACLMessage reply = msg.createReply();

				reply.setPerformative(ACLMessage.AGREE);
				reply.setConversationId("registrar-vendedor");
				myAgent.send(reply);
			} else {
				System.err.println("El mensaje de registro del comprador es nulo");

			}

		}

	}
}
