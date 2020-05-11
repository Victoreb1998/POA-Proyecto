package es.um.poa.agents.buyer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.LinkedList;

import org.yaml.snakeyaml.Yaml;

import behaviours.DelayBehaviour;
import es.um.poa.agents.POAAgent;
import es.um.poa.agents.fishmarket.FishMarketAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * Clase que representa al agente comprador en la lonja
 * 
 * @author victor
 *
 */
public class BuyerAgent extends POAAgent {

	private static final long serialVersionUID = 1L;
	private LinkedList<String> targetFishName;
	private double dineroDisponible; // dinero del comprador
	private double creditoDisponible = 0; // credito en la lonja

	// lonja conocida
	private AID LonjaAgent;
	private int ganador = 0;

	/**
	 * Metodo que se ejecuta automaticamente cuando arranca el agente y se encarga
	 * de inicializar las variables del agente y añadir los comportamientos
	 */
	public void setup() {
		super.setup();
		Object[] args = getArguments();
		if (args != null && args.length == 1) {
			String configFile = (String) args[0];
			BuyerAgentConfig config = initAgentFromConfigFile(configFile);

			if (config != null) {
				System.out.println("Hola! Agente-comprador: " + this.getName() + " está listo");
				targetFishName = new LinkedList<String>();
				// Creo y muestro la GUI
				/*
				 * myGui = new FishBuyerGui(this); myGui.showGui();
				 */
				dineroDisponible = config.getBudget();
				SequentialBehaviour seq = new SequentialBehaviour();
				// No podemos dejar que el comprador busque a la lonja antes de que esta este
				// registrada
				/**
				 * Comportamiento que para esperar a que la lonja este registrada en el
				 * directorio
				 */
				seq.addSubBehaviour(new DelayBehaviour(this, 10000) {

					private static final long serialVersionUID = 1L;

					@Override
					protected void handleElapsedTimeout() {
						DFAgentDescription template = new DFAgentDescription();
						ServiceDescription sd = new ServiceDescription();
						sd.setType("lonja");
						template.addServices(sd);
						try {
							DFAgentDescription[] result = DFService.search(myAgent, template);
							LonjaAgent = result[0].getName();

						} catch (FIPAException fe) {
							fe.printStackTrace();
						}

					}
				});
				/**
				 * Comportamiento para enviarle una petición de registro a la lonja
				 */
				seq.addSubBehaviour(new OneShotBehaviour() {

					private static final long serialVersionUID = 1L;

					@Override
					public void action() {
						// le enviamos un mensaje de tipo request para que la lonja
						// si cree que el comprador es valido se comprometa a informar
						// cuando haya un pescado disponible
						ACLMessage identificacion = new ACLMessage(ACLMessage.REQUEST);
						identificacion.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
						identificacion.setConversationId("AddBuyerProtocol");
						identificacion.addReceiver(LonjaAgent);
						// le envia su nombre
						identificacion.setContent(getName());
						myAgent.send(identificacion);

					}
				});
				// protocolo de inicio para abrir un credito, tiene que ser un waker behaviour
				// pq tiene que suceder despues de
				// que se haya registrado el usuario, si no le ponemos aun que sea unas
				// milesimas de tiempo de separacion recibira
				// un mensaje nulo
				/**
				 * Comportamiento que recibe la respuesta del registro de la lonja y le contesta
				 * con el credito que quiere abrir en la lonja
				 */
				seq.addSubBehaviour(new DelayBehaviour(this, 5000) {
					private MessageTemplate mt;
					private static final long serialVersionUID = 1L;

					@Override
					protected void handleElapsedTimeout() {
						// recibimos la respuesta
						mt = MessageTemplate.MatchConversationId("RegistroCorrecto");
						ACLMessage msg = myAgent.receive(mt);
						if (msg != null) {
							// si el comprador se ha podido registrar(AGREE) podrá abrir un credito
							if (msg.getPerformative() == ACLMessage.AGREE) {
								ACLMessage identificacion = new ACLMessage(ACLMessage.REQUEST);
								identificacion.addReceiver(LonjaAgent);
								identificacion.setContent(String.valueOf(dineroDisponible));
								identificacion.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
								identificacion.setConversationId("OpenBuyerCreditProtocol");

								myAgent.send(identificacion);
							} else {
								block();
							}
						}
					}
				});
				/**
				 * Comportamiento que recibe la respuesta de si el credito que hemos querido
				 * abrir es suficiente
				 */
				seq.addSubBehaviour(new DelayBehaviour(this, 3000) {

					private static final long serialVersionUID = 1L;

					@Override
					protected void handleElapsedTimeout() {
						MessageTemplate mt = MessageTemplate.MatchConversationId("RespuestaCredito");
						ACLMessage msg = myAgent.receive(mt);

						if (msg != null) {
							if (msg.getPerformative() == ACLMessage.INFORM) {
								creditoDisponible += dineroDisponible;
								dineroDisponible = 0;
								getLogger().info("INFO",
										"El agente " + getName() + " recibe la confirmación de credito");
							} else {
								getLogger().info("INFO", "El agente " + getName() + " no tiene el suficiente dinero");
								doDelete();
							}
						} else {
							getLogger().info("INFO",
									"No se ha recibido respuesta del credito para el agente " + getName());
							doDelete();
						}
					}
				});
				seq.addSubBehaviour(new DecidirPuja(this, 4000));
				addBehaviour(seq);
			} else {
				doDelete();
			}
		} else {
			getLogger().info("ERROR", "Requiere fichero de cofiguración.");
			doDelete();
		}
	}

	/**
	 * Función que lee un fichero y devuelvo la configuración inicial del agente
	 * vendedor
	 * 
	 * @param fileName Fichero donde esta la configuración
	 * @return config Configuración inicial del vendedor
	 */
	private BuyerAgentConfig initAgentFromConfigFile(String fileName) {
		BuyerAgentConfig config = null;
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

	/**
	 * Clase que implementa el comportamiento de la subasta del comprador, recibe
	 * las pujas y con una probabilidad del 50% y si tiene dinero realiza la puja y
	 * espera un mensaje para ver si ha ganado la puja
	 * 
	 * @author victor
	 *
	 */
	private class DecidirPuja extends TickerBehaviour {

		public DecidirPuja(Agent a, long period) {
			super(a, period);

		}

		private static final long serialVersionUID = 1L;

		@Override
		protected void onTick() {
			switch (ganador) {
			case 0:
				MessageTemplate mt = FishMarketAgent.crearPlantilla(FIPANames.InteractionProtocol.FIPA_PROPOSE,
						ACLMessage.PROPOSE, "OfertaLonjaProtocolo");
				ACLMessage msg = myAgent.receive(mt);
				if (msg != null) {
					String contenido = msg.getContent();
					String[] contenidos = contenido.split(" ");
					// Si buscamos el pez ofrecido
					// if (targetFishName.contains(contenidos[1])) {
					double precio = Double.valueOf(contenidos[2]);
					if (precio <= creditoDisponible && Math.random() > 0.5) {
						ganador = 1;
						getLogger().info("INFO",
								"Agente: " + myAgent.getName() + " intentado pujar por " + contenidos[1]);
						ACLMessage respuesta = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
						respuesta.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
						respuesta.setConversationId("RespuestaOfertaProtocolo");
						respuesta.addReceiver(LonjaAgent);
						myAgent.send(respuesta);

					}

				}
				break;
			case 1:
				MessageTemplate plantilla = FishMarketAgent.crearPlantilla(FIPANames.InteractionProtocol.FIPA_REQUEST,
						ACLMessage.REQUEST, "OfertaAceptadaProtocolo");
				ACLMessage msgaux = myAgent.receive(plantilla);
				ganador = 0;
				if (msgaux != null) {
					Double pagado = Double.valueOf(msgaux.getContent());
					creditoDisponible -= pagado;
					getLogger().info("INFO",
							"Agente: " + myAgent.getName() + "ha sido el ganador, decrementando dinero");
				}
				break;

			}
		}
	}

	public void anadirSaldo(double masSaldo) {
		dineroDisponible += masSaldo;

	}

	public void anadirPez(String fish) {
		targetFishName.add(fish);

	}
}
