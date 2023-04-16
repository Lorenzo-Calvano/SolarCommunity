import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class FileBehaviour extends OneShotBehaviour {

    //file information
    private ArrayList<AID> Agents = new ArrayList<>();
    private ArrayList<String> Types = new ArrayList<>();
    private ArrayList<Double> Values = new ArrayList<>();
    private ArrayList<Double> Timesteps = new ArrayList<>();

    private DecimalFormat format = new DecimalFormat();


    @Override
    public void action() {
        this.format.setMaximumFractionDigits(3);

        //execute commands read in the file
        int i=0;
        while (i < Agents.size()){

            //send message to agent
            ACLMessage msg = new ACLMessage(ACLMessage.PROPAGATE);
            msg.addReceiver(Agents.get(i));

            String payload = Types.get(i) + "@" + Values.get(i);
            msg.setContent(payload);
            myAgent.send(msg);

            //show graphical
            payload = Types.get(i) + '@';
            if(payload.matches("consumed@"))
                payload = "consumo";
            else
                payload = "produzione";
            ((Control_Agent) myAgent).gui.updateAutoInterface(Agents.get(i).getLocalName() + " ----> " + payload + ": " + this.format.format(Values.get(i)));

            //wait as requested by file
            try {
                Thread.sleep((long)(Timesteps.get(i)*1000));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            i++;
        }

        //empty memory
        this.Agents.clear();
        this.Values.clear();
        this.Timesteps.clear();
        this.Types.clear();
        ((Control_Agent) myAgent).gui.updateAutoInterface(" ");
        ((Control_Agent) myAgent).gui.updateAutoInterface("Fine del file");
        ((Control_Agent) myAgent).gui.updateAutoInterface(" ");
    }

    public void readCSV(String filepath) {
        //read all the file to get values
        try{
            File csv = new File(filepath);
            FileReader fr = new FileReader(csv);
            BufferedReader br = new BufferedReader(fr);
            String line = br.readLine();
            while(line != null){
                //get values adn put them in arrays
                String[] s = line.split(";");
                Agents.add(new AID(s[0], AID.ISLOCALNAME));
                Types.add(s[1]);
                Values.add(Double.parseDouble(s[2]));
                Timesteps.add(Double.parseDouble(s[3]));

                //next line
                line = br.readLine();
            }
        }
        catch (Exception e){
            System.out.println("Problema nella lettura!");
        }


    }
}
