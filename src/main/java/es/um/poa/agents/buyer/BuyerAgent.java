package es.um.poa.agents.buyer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.LinkedList;

import org.yaml.snakeyaml.Yaml;

import es.um.poa.agents.POAAgent;
import es.um.poa.guis.FishBuyerGui;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
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
				seq.addSubBehaviour(new WakerBehaviour(this, 10000) {

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
				//protocolo de inicio para registrar a un comprador
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
				//protocolo de inicio para abrir un credito, tiene que ser un waker behaviour pq tiene que suceder despues de 
				//que se haya registrado el usuario, si no le ponemos aun que sea unas milesimas de tiempo de separacion recibira
				//un mensaje nulo
				seq.addSubBehaviour(new WakerBehaviour(this, 500) {
					private MessageTemplate mt;
					private static final long serialVersionUID = 1L;
  
					@Override
					protected void handleElapsedTimeout() {
						//recibimos la respuesta
						mt = MessageTemplate.MatchConversationId("RegistroCorrecto");
						ACLMessage msg = myAgent.receive(mt);
						if (msg != null) {
							//si el comprador se ha podido registrar(INFORM) podrá abrir un credito
							if (msg.getPerformative() == ACLMessage.INFORM) {
								ACLMessage identificacion = new ACLMessage(ACLMessage.REQUEST);
								identificacion.addReceiver(LonjaAgent);
								identificacion.setContent(String.valueOf(dineroDisponible));
								identificacion.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
								identificacion.setConversationId("OpenBuyerCreditProtocol");
								creditoDisponible += dineroDisponible;
								dineroDisponible = 0;
								myAgent.send(identificacion);
							} else {
								block();
							}
						}
					}
				});
				addBehaviour(seq);
				getLogger().info("INFO", "Peticion de saldo enviada");
			} else {
				doDelete();
			}
		} else {
			getLogger().info("ERROR", "Requiere fichero de cofiguración.");
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
