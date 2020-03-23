package es.um.poa.agents.fishmarket;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;

import org.yaml.snakeyaml.Yaml;

import es.um.poa.agents.POAAgent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class FishMarketAgent extends POAAgent {

	private static final long serialVersionUID = 1L;
	private double dineroMinimo = 50;
	// Lista con los AID de los compradores
	private HashMap<AID, Double> compradoresAID;
	private LinkedList<AID> vendedoresAID;
	private FishMarketAgent fishMarket;
	
	public boolean performActionAddBuyerProtocol(AID sender) {
		// Hacer las Acciones correspondientes.
		compradoresAID.put(sender, new Double(0));
		return true;
	}

	public boolean checkActionAddBuyerProtocol(AID sender) {

		// Hacer las Acciones correspondientes.
		return !compradoresAID.containsKey(sender);
	}

	public boolean performActionAddSellerProtocol(AID sender) {
		// Hacer las Acciones correspondientes.
		return vendedoresAID.add(sender);
	}

	public boolean checkActionAddSellerProtocol(AID sender) {

		// Hacer las Acciones correspondientes.
		return !vendedoresAID.contains(sender);
	}

	public boolean checkActionOpenCreditProtocol(String content) {
		return Double.valueOf(content) < 0;
	}

	public boolean performActionOpenCreditProtocol(AID sender, String content) {
		compradoresAID.put(sender, Double.valueOf(content));
		return true;
	}

	public void setup() {
		super.setup();
		fishMarket = this;
		Object[] args = getArguments();
		String configFile = (String) args[0];
		FishMarketAgentConfig config = initAgentFromConfigFile(configFile);

		if (args != null && args.length == 1) {

			if (config != null) {
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
				addBehaviour(new DescubrirComprador());
				addBehaviour(new DescubrirVendedor());
				addBehaviour(new ComprobarComprador());

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

	private MessageTemplate crearPlantilla(String protocolo, int mensaje, String conversacion) {
		MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchProtocol(protocolo),
				MessageTemplate.MatchPerformative(mensaje));
		MessageTemplate templateAddBuyerProtocol = MessageTemplate.and(mt,
				MessageTemplate.MatchConversationId(conversacion));
		return templateAddBuyerProtocol;
	}

	private class DescubrirComprador extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			MessageTemplate mt = crearPlantilla(FIPANames.InteractionProtocol.FIPA_REQUEST, ACLMessage.REQUEST,
					"AddBuyerProtocol");
			/*
			 * Recibe un mensaje de ACL que coincide con una plantilla determinada. Este
			 * método no bloquea y devuelve el primer mensaje coincidente en la cola, si lo
			 * hay.
			 */
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				fishMarket.getLogger().info("INFO", fishMarket.getLocalName() + ": REQUEST to admit a buyer received from "
						+ msg.getSender().getLocalName());
				compradoresAID.put(msg.getSender(), new Double(0));
				fishMarket.getLogger().info("INFO", fishMarket.getLocalName() + ": Action successfully performed for "
						+ msg.getSender().getLocalName() + " [AddBuyerProtocol]");
				ACLMessage reply = msg.createReply();
				reply.setConversationId("RegistroCorrecto");
				reply.setPerformative(ACLMessage.INFORM);
				myAgent.send(reply);
			}

		}

	}
	
	private class DescubrirVendedor extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			MessageTemplate mt = crearPlantilla(FIPANames.InteractionProtocol.FIPA_REQUEST, ACLMessage.REQUEST,
					"AddSellerProtocol");
			/*
			 * Recibe un mensaje de ACL que coincide con una plantilla determinada. Este
			 * método no bloquea y devuelve el primer mensaje coincidente en la cola, si lo
			 * hay.
			 */
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				fishMarket.getLogger().info("INFO", fishMarket.getLocalName() + ": REQUEST to admit a seller received from "
						+ msg.getSender().getLocalName());
				compradoresAID.put(msg.getSender(), new Double(0));
				fishMarket.getLogger().info("INFO", fishMarket.getLocalName() + ": Action successfully performed for "
						+ msg.getSender().getLocalName() + " [AddSellerProtocol]");
				ACLMessage reply = msg.createReply();
				reply.setConversationId("RegistroCorrecto");
				reply.setPerformative(ACLMessage.INFORM);
				myAgent.send(reply);
			}

		}

	}

	private class ComprobarComprador extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			MessageTemplate mt = crearPlantilla(FIPANames.InteractionProtocol.FIPA_REQUEST, ACLMessage.REQUEST,
					"OpenBuyerCreditProtocol");
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				fishMarket.getLogger().info("INFO", fishMarket.getLocalName()
						+ ": REQUEST to open a credit to a buyer received from " + msg.getSender().getLocalName());
				ACLMessage reply = msg.createReply();
				if (compradoresAID.containsKey(msg.getSender())) {

					Double credito = Double.valueOf(msg.getContent());
					if (credito >= dineroMinimo) {
						compradoresAID.put(msg.getSender(), credito);
						fishMarket.getLogger().info("INFO", fishMarket.getLocalName() + ": Action successfully performed for "
								+ msg.getSender().getLocalName() + " [OpenCreditProtocol]" + "Credit:" + msg.getContent());
						reply.setPerformative(ACLMessage.INFORM);
					} else {
						reply.setPerformative(ACLMessage.FAILURE);
					}
					myAgent.send(reply);
				} else {
					reply.setPerformative(ACLMessage.REFUSE);
					myAgent.send(reply);
				}

			}
		}

	}
}
