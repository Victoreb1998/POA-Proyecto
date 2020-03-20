package es.um.poa.agents.buyer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.LinkedList;

import org.yaml.snakeyaml.Yaml;

import es.um.poa.agents.POAAgent;
import es.um.poa.guis.FishBuyerGui;
import es.um.poa.protocols.addbuyer.AddBuyerProtocolInitiator;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class BuyerAgent extends POAAgent {

	private static final long serialVersionUID = 1L;
	private LinkedList<String> targetFishName;
	private double dineroDisponible; // dinero del comprador
	private double creditoDisponible = 0; // credito en la lonja
	private FishBuyerGui myGui;
	// lonja conocida
	private AID LonjaAgent;

	public void setup() {
		super.setup();
		Object[] args = getArguments();
		if (args != null && args.length == 1) {
			String configFile = (String) args[0];
			BuyerAgentConfig config = initAgentFromConfigFile(configFile);

			if (config != null) {
				System.out.println("Hola! Agente-comprador: " + this.getName() + " est· listo");
				targetFishName = new LinkedList<String>();
				// Creo y muestro la GUI
				/*myGui = new FishBuyerGui(this);
				myGui.showGui();*/
				
				SequentialBehaviour seq = new SequentialBehaviour();
				seq.addSubBehaviour(new OneShotBehaviour() {
					
					private static final long serialVersionUID = 1L;

					@Override
					public void action() {
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
				seq.addSubBehaviour(new OneShotBehaviour() {

					
					private static final long serialVersionUID = 1L;

					@Override
					public void action() {
						// le enviamos un mensaje de tipo subscribe para que la lonja
						// si cree que el comprador es valido se comprometa a informar
						// cuando haya un pescado disponible
						ACLMessage identificacion = new ACLMessage(ACLMessage.REQUEST);
						identificacion.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
						identificacion.setConversationId("AddBuyerProtocol");
						identificacion.addReceiver(LonjaAgent);
						// le envia su nombre
						identificacion.setContent(getName());
						identificacion.setReplyWith("subscribe" + System.currentTimeMillis());
						myAgent.send(identificacion);
						//no se si habria que utilizar esto
						

					}
				});
				addBehaviour(seq);
				/*seq.addSubBehaviour(new OneShotBehaviour() {

					
					private MessageTemplate mt;
					private static final long serialVersionUID = 1L;

					@Override
					public void action() {
						// le enviamos un mensaje de tipo subscribe para que la lonja
						// si cree que el comprador es valido se comprometa a informar
						// cuando haya un pescado disponible
						mt = MessageTemplate.MatchConversationId("registrar-comprador");
						ACLMessage msg = myAgent.receive(mt);
						if (msg != null) {
							// si es un AGREE podemos deducir que si es un comprador
							// valido
							if (msg.getPerformative() == ACLMessage.AGREE) {
								ACLMessage identificacion = new ACLMessage(ACLMessage.REQUEST);
								identificacion.addReceiver(LonjaAgent);
								// le envia el saldo a la lonja para saber si es un comprador
								// potencial
								identificacion.setContent(String.valueOf(dineroDisponible));
								creditoDisponible += dineroDisponible;
								dineroDisponible = 0;
								myAgent.send(identificacion);
							} else {
								block();
							}
						}
					}
				});*/
			} else {
				doDelete();
			}
		} else {
			getLogger().info("ERROR", "Requiere fichero de cofiguraci√≥n.");
			doDelete();
		}
	}

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
	public void anadirSaldo(double masSaldo) {
		dineroDisponible += masSaldo;

	}

	public void anadirPez(String fish) {
		targetFishName.add(fish);

	}
}
