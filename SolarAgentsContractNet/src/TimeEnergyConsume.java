import java.util.ArrayList;

public class TimeEnergyConsume {

    //temporal span of agent's life
    private long first_time;
    private long last_time;

    //balance of energy (to compute the energy used, received, sent etc...)
    private double TotalEnergyBalance = 0;
    private double ReferenceEnergyBalance = 0;

    private double InstantPowerBalance = 0;
    private double ReferencePowerBalance = 0;

    //arrays for storing the various time steps (and then print on the file)
    ArrayList<Double> EnCon = new ArrayList<>();
    ArrayList<Double> EnPro = new ArrayList<>();
    ArrayList<Double> Rec = new ArrayList<>();
    ArrayList<Double> Sent = new ArrayList<>();
    ArrayList<Double> Times = new ArrayList<>();
    private SolarAgent myAgent;


    public TimeEnergyConsume(SolarAgent myAgent){
        this.myAgent = myAgent;

        //set the initial time
        this.first_time = System.currentTimeMillis();
        this.last_time = this.first_time;
        this.setInstantPowerBalance();
    }


    //to compute energy used in last time step and set up the next one;

    private synchronized void setInstantPowerBalance(){

        //compute instant energy balance and store it
        this.InstantPowerBalance = myAgent.getEnergyBalance();
        this.ReferencePowerBalance = myAgent.getEnergyBalance();

        //get information from received and sent energy
        double rec = 0.0;
        for (EnergyTrade e: myAgent.EnergyReceived) {
            this.InstantPowerBalance += e.getEnergyValue();
            rec += e.getEnergyValue();
        }

        double sent = 0.0;
        for (EnergyTrade e: myAgent.EnergySent) {
            this.InstantPowerBalance -= e.getEnergyValue();
            sent += e.getEnergyValue();
        }

        //for final results
        this.EnCon.add(myAgent.getEnergyConsumed());
        this.EnPro.add(myAgent.getEnergyProduced());
        this.Rec.add(rec);
        this.Sent.add(sent);
    }
    public synchronized void setEnergyTimeStep(){
        //compute the energy expended in last time step and set next one;
        long curr_time = System.currentTimeMillis();

        //compute the elapsed time from last change in balance and current one, update step
        double diff = ((double)(curr_time - last_time)) / 1000;

        //for final results
        this.Times.add(diff);


        //compute energy requested
        if(this.InstantPowerBalance < 0)
            this.TotalEnergyBalance += (-this.InstantPowerBalance*diff);

        //compute reference energy (no agent's communication involved)
        if(this.ReferencePowerBalance < 0){
            this.ReferenceEnergyBalance += (-this.ReferencePowerBalance*diff);
        }


        //set for new time step
        this.last_time = curr_time;
        this.setInstantPowerBalance();
    }


    public double getTotalEnergyBalance(){
        return this.TotalEnergyBalance;
    }

    public double getReferenceEnergyBalance(){
        return this.ReferenceEnergyBalance;
    }

    public double getTotalTime(){
        return ((double)(this.last_time - this.first_time))/1000;
    }

    public String getCSV() {
        //return CSV format of all the time steps and energy consumption
        String CSV = "Consumed;Produced;Received;Sent;Timestep\n";

        int i=0;
        while(i<this.Times.size()){
            CSV += (this.EnCon.get(i) + ";" +
                    this.EnPro.get(i) + ";" +
                    this.Rec.get(i) + ";" +
                    this.Sent.get(i) + ";"
                    + this.Times.get(i) +"\n");
            i++;
        }

        return CSV;
    }
}
