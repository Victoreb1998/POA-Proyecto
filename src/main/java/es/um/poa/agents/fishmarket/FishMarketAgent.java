package es.um.poa.agents.fishmarket;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;

import behaviours.DelayBehaviour;
import es.um.poa.agents.POAAgent;
import es.um.poa.agents.seller.Lot;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
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
	private HashMap<AID, List<Lot>> vendedoresAID;
	private FishMarketAgent fishMarket;

	public void setup() {
		super.setup();
		fishMarket = this;
		Object[] args = getArguments();
		String configFile = (String) args[0];
		FishMarketAgentConfig config = initAgentFromConfigFile(configFile);

		if (args != null && args.length == 1) {

			if (config != null) {
				compradoresAID = new HashMap<AID, Double>();
				vendedoresAID = new HashMap<AID, List<Lot>>();
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
				addBehaviour(new RecibirLot());
				addBehaviour(new EnviarInfoSubasta(this,15000));

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

	public static MessageTemplate crearPlantilla(String protocolo, int mensaje, String conversacion) {
		MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchProtocol(protocolo),
				MessageTemplate.MatchPerformative(mensaje));
		MessageTemplate templateAddBuyerProtocol = MessageTemplate.and(mt,
				MessageTemplate.MatchConversationId(conversacion));
		return templateAddBuyerProtocol;
	}

	public MessageTemplate crearPlantilla(String protocolo, int mensaje1, int mensaje2, String conversacion) {
		MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchProtocol(protocolo), MessageTemplate
				.or(MessageTemplate.MatchPerformative(mensaje1), MessageTemplate.MatchPerformative(mensaje2)));
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
				fishMarket.getLogger().info("INFO", fishMarket.getLocalName()
						+ ": REQUEST to admit a buyer received from " + msg.getSender().getLocalName());
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
				fishMarket.getLogger().info("INFO", fishMarket.getLocalName()
						+ ": REQUEST to admit a seller received from " + msg.getSender().getLocalName());
				vendedoresAID.put(msg.getSender(), new LinkedList<Lot>());
				fishMarket.getLogger().info("INFO", fishMarket.getLocalName() + ": Action successfully performed for "
						+ msg.getSender().getLocalName() + " [AddSellerProtocol]");
				ACLMessage reply = msg.createReply();
				reply.setConversationId("RegistroCorrecto");
				reply.setPerformative(ACLMessage.INFORM);
				myAgent.send(reply);
			}

		}

	}

	private class RecibirLot extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			// plantilla que se ha de corresponder con el protocolo de recibir los peces
			MessageTemplate mt = crearPlantilla(FIPANames.InteractionProtocol.FIPA_REQUEST, ACLMessage.REQUEST,
					"SellerLot");
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				fishMarket.getLogger().info("INFO", fishMarket.getLocalName() + ": REQUEST to sell fish received from "
						+ msg.getSender().getLocalName());
				// la contestacion que enviaremos
				ACLMessage reply = msg.createReply();
				if (vendedoresAID.containsKey(msg.getSender())) {
					// Aqui separamos los diferentes lots, que esta divididos por comas
					String[] parse = msg.getContent().split(",");
					// recuperamos los lotes que podamos tener con anterioridad
					// como la inicializamos a una lista vacia al registrar el vendedor no hay
					// problema con los null
					List<Lot> lotes = vendedoresAID.get(msg.getSender());
					for (String s : parse) {
						Lot l = new Lot();
						// Aqui separamos cada componente del lot, que estan separados por espacios
						String[] parseAux = s.split(" ");

						l.setKg(Float.valueOf(parseAux[0]));
						// si es el segundo elemento es el tipo de pescado

						l.setType(parseAux[1]);
						// si es el tercer elemento es el precio minimo puesto por el vendedor para
						// retirar el pescado si
						// no hay pujas

						l.setPrecioMin(Float.valueOf(parseAux[2]));
						// finalmente si es el cuarto elemento es el precio de inicio fijado por el
						// vendedor para
						// el inicio de la subasta

						l.setPrecioInicio(Float.valueOf(parseAux[3]));
						lotes.add(l);
						fishMarket.getLogger().info("INFO", "Lot recibido correctamente: " + l.toString());
					}
					vendedoresAID.put(msg.getSender(), lotes);
					// contestación
					reply.setConversationId("Recepcion de pescados");
					reply.setPerformative(ACLMessage.INFORM);
					myAgent.send(reply);

				} else {
					reply.setPerformative(ACLMessage.REFUSE);
					myAgent.send(reply);
				}

			}

		}
	}

	private class ComprobarComprador extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			// plantilla que corresponde con la apertura de credito
			MessageTemplate mt = crearPlantilla(FIPANames.InteractionProtocol.FIPA_REQUEST, ACLMessage.REQUEST,
					"OpenBuyerCreditProtocol");
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				fishMarket.getLogger().info("INFO", fishMarket.getLocalName()
						+ ": REQUEST to open a credit to a buyer received from " + msg.getSender().getLocalName());
				ACLMessage reply = msg.createReply();
				// comprobamos si el comprador esta registrado
				if (compradoresAID.containsKey(msg.getSender())) {

					Double credito = Double.valueOf(msg.getContent());
					// comprobamos si la apertura de credito del vendedor es suficiente
					// para el minimo de la lonja
					if (credito >= dineroMinimo) {
						// abrimos el credito
						compradoresAID.put(msg.getSender(), credito);
						fishMarket.getLogger().info("INFO",
								fishMarket.getLocalName() + ": Action successfully performed for "
										+ msg.getSender().getLocalName() + " [OpenCreditProtocol]" + "Credit:"
										+ msg.getContent());
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

	private class EnviarInfoSubasta extends TickerBehaviour {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Boolean Parada;
		float precio;
		public EnviarInfoSubasta(Agent a, long period) {
			super(a, period);
			Parada = true;
			precio=0;

		}

		@Override
		protected void onTick() {
			Set<AID> vendedores = vendedoresAID.keySet();
			for (AID vendedor : vendedores) {
				ACLMessage identificacion = new ACLMessage(ACLMessage.PROPOSE);
				identificacion.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
				identificacion.setConversationId("OfertaLonjaProtocolo");
				// añadimos como receptor a todos los compradores
				for (AID aid : compradoresAID.keySet())
					identificacion.addReceiver(aid);

				List<Lot> lots = vendedoresAID.get(vendedor);

				for (Lot lot : lots) {
					// para cada lote empezamos la subasta
					precio = lot.getPrecioInicio();
					String puja;
					float minimo = lot.getPrecioMin();
					MessageTemplate mt = crearPlantilla(FIPANames.InteractionProtocol.FIPA_PROPOSE,
							ACLMessage.ACCEPT_PROPOSAL, "RespuestaOfertaProtocolo");
					while (precio >= minimo && Parada) {

						puja = lot.paraPuja(precio);
						identificacion.setContent(puja);
						fishMarket.getLogger().info("INFO", "Enviando puja " + puja + " a los compradores");
						myAgent.send(identificacion);

						myAgent.addBehaviour(new DelayBehaviour(myAgent, 500) {

							/**
							 * 
							 */
							private static final long serialVersionUID = 1L;

							protected void handleElapsedTimeout() {
								ACLMessage msg = myAgent.receive(mt);
								if (msg != null) {
									AID ganador = msg.getSender();
									fishMarket.getLogger().info("INFO", "El comprador ganador es: "+ ganador.getName());
									Double dinero = compradoresAID.get(ganador);
									dinero -= precio;
									compradoresAID.put(ganador, dinero);
									Parada = false;

									// informar al comprador que ha ganado
									ACLMessage respuesta = new ACLMessage(ACLMessage.REQUEST);
									respuesta.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
									respuesta.setConversationId("OfertaAceptadaProtocolo");
									respuesta.addReceiver(ganador);
									respuesta.setContent(String.valueOf(precio));
									myAgent.send(respuesta);
									
									//TODO eliminar el pescado de la subasta y notificar al vendedor
									
								} else {
									fishMarket.getLogger().info("INFO", "Ningun comprador ha pujado");
									precio -= precio / 10;
								}

							}

						}

						);

					}

				}

			}

		}

	}
}
