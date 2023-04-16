import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;

import java.text.DecimalFormat;
import java.util.Random;

public class AutoBehaviour extends CyclicBehaviour {

    private DecimalFormat format = new DecimalFormat();


    public void action() {
        //set variables needed
        Random rand = new Random();
        this.format.setMaximumFractionDigits(3);

        //decide randomly agent
        int random_index = rand.nextInt(((Control_Agent)myAgent).SolarAgents.size());
        AID receiver = ((Control_Agent)myAgent).SolarAgents.get(random_index);

        //decide randomly if consumed or produced value
        String payload = "";
        int random_en = rand.nextInt(2);
        payload = "consumed@";
        /*if(random_en == 0)
            payload = "consumed@";
        else
            payload = "produced@";*/

        //decide randomly new value to notify (inside range 0.0 and 4.0)
        double value = rand.nextDouble() * 4.0;

        //send message to agent selected
        ((Control_Agent) myAgent).send_change(receiver.getLocalName(), value, payload);

        //show graphical
        if(payload.matches("consumed@"))
            payload = "consumo";
        else
            payload = "produzione";
        String message = receiver.getLocalName() + " --- " + payload +": " + this.format.format(value);
        ((Control_Agent) myAgent).gui.updateAutoInterface(message);
        //wait before changing other values
        try {
            Thread.sleep(3500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
