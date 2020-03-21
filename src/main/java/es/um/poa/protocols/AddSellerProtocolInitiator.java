package es.um.poa.protocols;

import es.um.poa.agents.seller.SellerAgent;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;

/**
 * Clase que modela el protocolo de a�adir un comprador
 * en la parte del initiator (el que implementa comprador).
 * 
 * @author Javier Martinez Valverde
 * @author Albano Castillo Callejas
 *
 */
@SuppressWarnings("serial")
public class AddSellerProtocolInitiator extends AchieveREInitiator{

	/*
	 * Referencia al agente Comprador.
	 */
	private SellerAgent seller;
	
	/**
	 * Constructor
	 * 
	 * @param a Agente que implementa el protocolo.
	 * @param msg Mensaje ACL que enviara el agente.
	 */
	public AddSellerProtocolInitiator(Agent a, ACLMessage msg) {
		
		// LLamamos al constructor del padre.
		super(a, msg);
		
		// Guardamos el agente que implemnta el protocolo (el agente comprador).
		try {
			seller = (SellerAgent) a;
		} catch (ClassCastException e) {
			// Si ocurre algún error informamos de ello y lo lanzamos hacia arriba.
			seller.getLogger().info("ERROR", "The agent is not an instance of class that we expect (" + SellerAgent.class.getName() + ").");
			e.printStackTrace();
			throw e;
		}
	}
	/**
	 * Maneja los mensajes inform recibidos.
	 * Se llama al padre y se añaden ordenes para la depuración.
	 */
	@Override
	protected void handleInform(ACLMessage msg) {
		super.handleInform(msg);
		seller.getLogger().info("INFO", seller.getLocalName() + ": INFORM response recived from " + msg.getSender().getLocalName() + " [AddSellerProtocol].");
	}
	/**
	 * Maneja los mensajes failure recibidos.
	 * Se llama al padre y se añaden ordenes para la depuración.
	 */
	@Override
	protected void handleFailure(ACLMessage msg) {
		super.handleFailure(msg);
		seller.getLogger().info("INFO",seller.getLocalName() + ": FAILURE response recived from " + msg.getSender().getLocalName() + " [AddSellerProtocol].");
	}
	/**
	 * Maneja los mensajes refuse recibidos.
	 * Se llama al padre y se añaden ordenes para la depuracion.
	 */
	@Override
	protected void handleRefuse(ACLMessage msg) {
		super.handleRefuse(msg);
		seller.getLogger().info("INFO",seller.getLocalName() + ": REFUSE response recived from " + msg.getSender().getLocalName() + " [AddSellerProtocol].");
	}
	/**
	 * Maneja los mensajes notUnderstood recibidos.
	 * Se llama al padre y se aÑaden ordenes para la depuración.
	 */
	@Override
	protected void handleNotUnderstood(ACLMessage msg) {
		super.handleNotUnderstood(msg);
		seller.getLogger().info("INFO",seller.getLocalName() + ": NOTUNDERSTOOD response recived from " + msg.getSender().getLocalName() + " [AddSellerProtocol].");
	}
}
