
public class EnergyTrade implements Comparable<EnergyTrade>{

    private final String ProcessName;
    private double EnergyValue;
    private double Price;


    public EnergyTrade(String PID, double EV, double Price) {
        this.ProcessName = PID;
        this.EnergyValue = EV;
        this.Price = Price;
    }

    //create energy trade from message of agent
    public EnergyTrade(String represent){
        String[] Exchange = represent.split("@");
        if(Exchange.length != 2 && Exchange.length != 3){
            this.ProcessName = "Error";
            this.EnergyValue = 0;
        }

        else{
            this.ProcessName = Exchange[0];
            this.EnergyValue = Double.parseDouble(Exchange[1]);
            if(Exchange.length == 3)
                this.Price = Double.parseDouble(Exchange[2]);
            else
                this.Price = 0.0;
        }
    }


    public String getProcessName() {
        return this.ProcessName;
    }

    public double getEnergyValue() {
        return this.EnergyValue;
    }

    public double getPrice(){
        return this.Price;
    }

    public void setEnergyValue(double EV) {
        this.EnergyValue = EV;
    }

    public void setPrice(double Price){
        this.Price = Price;
    }



    public String toString(){
        String represent = ProcessName + '@' + this.EnergyValue + '@' + this.Price;
        return represent;
    }


    @Override
    public int compareTo(EnergyTrade o) {
        if(this.getEnergyValue() < o.getEnergyValue())
            return -1;
        else if(this.getEnergyValue() == o.getEnergyValue())
            return 0;
        else
            return 1;
    }
}