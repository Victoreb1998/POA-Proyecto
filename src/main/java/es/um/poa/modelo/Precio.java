package es.um.poa.modelo;

public class Precio {

	private final double startingPrice;
	private final double minimunPrice;
	
	public Precio(double startingPrice, double minimunPrice) {
		this.startingPrice = startingPrice;
		this.minimunPrice = minimunPrice;
	}

	public double getStartingPrice() {
		return startingPrice;
	}

	public double getMinimunPrice() {
		return minimunPrice;
	}
	
	
}
