package es.um.poa.agents.seller;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import org.yaml.snakeyaml.Yaml;

import behaviours.DelayBehaviour;
import es.um.poa.agents.POAAgent;
import es.um.poa.agents.fishmarket.FishMarketAgent;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class SellerAgent extends POAAgent {

	private static final long serialVersionUID = 1L;
	// duda precio minimo
	private List<Lot> catalogue;
	
	private AID LonjaAgent;
	private float dinero = 0; 
	private float acumulado = 0; 

	public void setup() {
		super.setup();

		Object[] args = getArguments();
		if (args != null && args.length == 1) {
			String configFile = (String) args[0];
			SellerAgentConfig config = initAgentFromConfigFile(configFile);

			if (config != null) {
				catalogue = config.getLots();

				// Registrar el servicio de venta de libros en las paginas amarillas
				SequentialBehaviour seq = new SequentialBehaviour();
				// No podemos dejar que el vendedor busque a la lonja antes de que esta este
				// registrada
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
				seq.addSubBehaviour(new OneShotBehaviour() {

					private static final long serialVersionUID = 1L;

					@Override
					public void action() {
						// le enviamos un mensaje de tipo subscribe para que la lonja
						// si cree que el comprador es valido se comprometa a informar
						// cuando haya un pescado disponible
						ACLMessage identificacion = new ACLMessage(ACLMessage.REQUEST);
						identificacion.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
						identificacion.setConversationId("AddSellerProtocol");
						identificacion.addReceiver(LonjaAgent);
						// le envia su nombre
						identificacion.setContent(getName());
						identificacion.setReplyWith("subscribe" + System.currentTimeMillis());
						myAgent.send(identificacion);
						

					}
				});
				seq.addSubBehaviour(new DelayBehaviour(this,5000) {
					private MessageTemplate mt;
					private static final long serialVersionUID = 1L;

					@Override
					protected void handleElapsedTimeout() {
						//recibimos la respuesta
						mt = MessageTemplate.MatchConversationId("RegistroCorrecto");
						ACLMessage msg = myAgent.receive(mt);
						if (msg != null) {
							//si el comprador se ha podido registrar(AGREE) podr· abrir un credito
							if (msg.getPerformative() == ACLMessage.AGREE) {
								ACLMessage identificacion = new ACLMessage(ACLMessage.REQUEST);
								identificacion.addReceiver(LonjaAgent);
								String paraEnviar = "";
								for (Lot l: catalogue) {
									paraEnviar += l.paraEnviar()+",";
								}
								identificacion.setContent(paraEnviar);
								identificacion.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
								identificacion.setConversationId("SellerLot");
								getLogger().info("INFO", "Lot enviado correctamente: "+paraEnviar);
								myAgent.send(identificacion);
							} else {
								block();
							}
						}
					}
				});
				seq.addSubBehaviour(new DelayBehaviour(this, 3000));
				seq.addSubBehaviour(new TickerBehaviour(this,4000) {
					
					private static final long serialVersionUID = 1L;

					@Override
					protected void onTick() {
						
						MessageTemplate mt = FishMarketAgent.crearPlantilla(FIPANames.InteractionProtocol.FIPA_REQUEST,
								ACLMessage.REQUEST, "PescadoVendidoProtocolo");
						
						ACLMessage msg = myAgent.receive(mt);
						if (msg != null) {
							String[] contenidos = msg.getContent().split(",");
							Float precio = Float.valueOf(contenidos[0]);
							acumulado+=precio;
							String pescado = contenidos[1];
							getLogger().info("INFO", "El agente " + getName() + " ha recibido la venta de " + pescado);
							//retiramos el dinero con probabilidad 1/2
							if (Math.random() > 0.5) {
								
								ACLMessage rVendedor = new ACLMessage(ACLMessage.AGREE);
								rVendedor.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
								rVendedor.setConversationId("SacarDineroProtocolo");
								rVendedor.addReceiver(LonjaAgent);
			
								myAgent.send(rVendedor);
								
								dinero += acumulado;
								acumulado=0;
								getLogger().info("INFO", "El agente " + getName() + " retira el dinero");
							} else {//no saca el dinero
								ACLMessage rVendedor = new ACLMessage(ACLMessage.REFUSE);
								rVendedor.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
								rVendedor.setConversationId("SacarDineroProtocolo");
								rVendedor.addReceiver(LonjaAgent);
								myAgent.send(rVendedor);
								getLogger().info("INFO", "El agente " + getName() + " NO retira el dinero");
							}
						}
					}
				});
				addBehaviour(seq);
			} else {
				doDelete();
			}
		} else {
			getLogger().info("ERROR", "Requiere fichero de cofiguraci√≥n.");
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
}
