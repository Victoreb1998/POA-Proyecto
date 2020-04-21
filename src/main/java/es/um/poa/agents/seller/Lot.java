package es.um.poa.agents.seller;

public class Lot {
	private float kg;
	private String type;
	private float precioMin;
	private float precioInicio;

	@Override
	public String toString() {
		return "Lot [kg=" + kg + ", type=" + type + ", precioMax=" + precioMin + ", precioInicio=" + precioInicio + "]";
	}

	public String paraEnviar() {
		return kg + " " + type + " " + precioMin + " " + precioInicio;
	}
 
	public String paraPuja(float precio) {
		return kg + " " + type + " " + precio;
	}
	public float getKg() {
		return kg;
	}

	public void setKg(float kg) {
		this.kg = kg;
	}

	public float getPrecioMin() {
		return precioMin;
	}

	public void setPrecioMin(float precioMin) {
		this.precioMin = precioMin;
	}

	public float getPrecioInicio() {
		return precioInicio;
	}

	public void setPrecioInicio(float precioInicio) {
		this.precioInicio = precioInicio;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
