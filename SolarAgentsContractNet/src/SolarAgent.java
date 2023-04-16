import jade.core.AID;
import jade.core.Agent;
import jade.core.NotFoundException;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class SolarAgent extends Agent {

    //behaviours for agent's life
    private ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
    private PrincipalBehaviour PB = new PrincipalBehaviour();
    private TradeBehaviour TB = new TradeBehaviour();
    private ProposalBehaviour SB = new ProposalBehaviour();
    //to affect the environment via messages
    private SensorBehaviour BB = new SensorBehaviour();

    //attributes to compute the energy balance and set the actions for the agent
    public ArrayList<EnergyTrade> EnergyReceived;
    public ArrayList<EnergyTrade> EnergySent;
    private double EnergyConsumed;
    private double EnergyProduced;

    //object to compute the energy used by the agent
    public TimeEnergyConsume TEC;

    //list of other agents
    public ArrayList<AID> SolarAgents;

    //gui interface, to visualize the system
    public GUI gui;


    protected void setup() {

        //presenting itself
        System.out.println("Hello world! I'm an agent!\n" + "My local name is " + getAID().getLocalName() + "\n");

        //setting agent's attributes
        this.EnergySent = new ArrayList<>();
        this.EnergyReceived = new ArrayList<>();

        this.SolarAgents = new ArrayList<>();

        //initialize values (for simulation)
        this.setEnergyProduced(1.7);
        this.setEnergyConsumed(0.0);


        //set up the energy compute system (for results and testing)
        this.TEC = new TimeEnergyConsume(this);

        //registering as a potential sender agent
        this.RegisterToServices();

        //wait for a bit
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //getting the list of others senders
        this.DiscoverAgents();

        //setting behaviours (each one with a thread)
        this.addBehaviour(this.tbf.wrap(this.TB));
        this.addBehaviour(this.tbf.wrap(this.PB));
        this.addBehaviour(this.tbf.wrap(this.SB));
        this.addBehaviour(this.tbf.wrap(this.BB));

        //use the arguments to set the gui initial position
        Object[] args = this.getArguments();
        int x = Integer.parseInt(args[0].toString());
        int y = Integer.parseInt(args[1].toString());
        this.gui = new GUI(this, x, y);
        gui.setVisible(true);
    }


    //methods to enter the multi-agents system and discover other agents
    private void RegisterToServices(){
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Sender");
        sd.setName(this.getLocalName()+"-Sender");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
    public void DiscoverAgents(){
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription s = new ServiceDescription();
        s.setType("Sender");
        template.addServices(s);
        try {
            DFAgentDescription[] result = DFService.search(this, template);

            for (int i = 0; i < result.length; ++i) {
                if(!result[i].getName().getLocalName().matches(this.getLocalName()))
                    this.SolarAgents.add(result[i].getName());
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }


    //methods to calculate the current energy balance for the agent based on the current situation
    public synchronized double computeReceiverBalance(){
        double value = this.getEnergyBalance();
        for (EnergyTrade E : this.EnergyReceived) {
                value += E.getEnergyValue();
        }

        return value;
    }
    public synchronized double computeSenderBalance() {
        double value = this.getEnergyBalance();
        for (EnergyTrade E : this.EnergySent) {
            value -= E.getEnergyValue();
        }

        return value;
    }


    //getter and setter for agent's attributes
    public void setEnergyProduced(double EP){
        this.EnergyProduced = EP;
    }
    public void setEnergyConsumed(double EC){
        this.EnergyConsumed = EC;
    }

    public synchronized double getEnergyProduced(){
        return this.EnergyProduced;
    }
    public synchronized double getEnergyConsumed(){
        return this.EnergyConsumed;
    }

    public synchronized double getEnergyBalance(){
        return this.EnergyProduced - this.EnergyConsumed;
    }


    //end of agent's life
    protected void takeDown(){

        this.TEC.setEnergyTimeStep();

        System.out.println(this.getLocalName() + ": Goodbye world!");
        System.out.println("Energia ricevuta da rete esterna: " + this.TEC.getTotalEnergyBalance());
        System.out.println("Energia senza agenti ricevuta: " + this.TEC.getReferenceEnergyBalance());
        System.out.println("Tempo totale di vita dell'agente: " + this.TEC.getTotalTime() + " secondi");

        //close threads of agent
        try {
            this.tbf.interrupt(this.TB);
            this.tbf.interrupt(this.SB);
            this.tbf.interrupt(this.PB);
            this.tbf.interrupt(this.BB);
        } catch (NotFoundException e) {
            System.out.println(this.getLocalName() + ": non ho attivo questo behaviour");
        }

        //deregister from services
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        //write end results
        /*try {
            //write local directory to get results
            File result = new File("C:\\Users\\ilcai\\SolarAgentResults\\" + this.getLocalName() + ".txt");
            if(!result.exists()){
                result.createNewFile();
            }
            FileWriter fw = new FileWriter(result);
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write(this.TEC.getCSV());
            bw.flush();
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
    }
}
